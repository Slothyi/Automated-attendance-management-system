from app.config.db import (
    students_collection,
    registered_students_collection,
    email_verifications_collection,
    password_resets_collection
)

from app.controllers.class_controller import (
    student_exists
)

from passlib.context import CryptContext

from app.utils.jwt_handler import (
    create_token
)

from app.utils.face_recognition_utils import (
    get_face_encoding
)

from app.utils.email_utils import send_verification_email_smtp, send_password_reset_email_smtp
from fastapi.responses import HTMLResponse
from fastapi import BackgroundTasks
import os
import numpy as np
import uuid
import datetime


# ============================
# 🔐 PASSWORD HASHING
# ============================
pwd_context = CryptContext(

    schemes=["bcrypt"],

    deprecated="auto"
)


# ============================
# 🔥 FACE DUPLICATE CHECK
# ============================
def is_duplicate_face(new_encoding):

    if new_encoding is None:

        return False

    new_encoding = np.array(
        new_encoding
    )

    # ✅ CHECK ONLY REGISTERED USERS
    users = registered_students_collection.find()

    for user in users:

        stored_encoding = user.get(
            "face_encoding"
        )

        if not stored_encoding:
            continue

        stored_encoding = np.array(
            stored_encoding
        )

        # ✅ SAME VECTOR SHAPE
        if (
            stored_encoding.shape !=
            new_encoding.shape
        ):

            continue

        # ✅ FACE DISTANCE
        distance = np.linalg.norm(

            stored_encoding -
            new_encoding
        )

        print(
            "🔍 FACE DISTANCE:",
            distance
        )

        # ✅ MATCH FOUND
        if distance < 0.5:

            return True

    return False


# ============================
# 📝 REGISTER USER
# ============================
def register_user(
    name,
    roll,
    email,
    password,
    file
):

    # ============================
    # ✅ ADMIN AUTHORIZED?
    # ============================
    if not student_exists(
        name,
        roll
    ):

        return {

            "success": False,

            "message":
                "Student not authorized by admin"
        }

    # ============================
    # 🔁 EMAIL CHECK
    # ============================
    existing_email = (
        registered_students_collection
        .find_one({

            "email": email
        })
    )

    if existing_email:

        return {

            "success": False,

            "message":
                "Email already registered"
        }

    # ============================
    # 🔁 ROLL CHECK
    # ============================
    existing_roll = (
        registered_students_collection
        .find_one({

            "roll": roll
        })
    )

    if existing_roll:

        return {

            "success": False,

            "message":
                "Roll already registered"
        }

    # ============================
    # 🔐 HASH PASSWORD
    # ============================
    hashed = pwd_context.hash(
        password
    )

    # ============================
    # 📂 CREATE UPLOAD FOLDER
    # ============================
    os.makedirs(

        "uploads",

        exist_ok=True
    )

    file_path = (
        f"uploads/{email}_register.jpg"
    )

    with open(file_path, "wb") as f:

        f.write(
            file.file.read()
        )

    # ============================
    # 🤖 FACE ENCODING
    # ============================
    encoding = get_face_encoding(
        file_path
    )

    if encoding is None:

        return {

            "success": False,

            "message":
                "No face detected"
        }

    # ============================
    # 🔥 DUPLICATE FACE CHECK
    # ============================
    if is_duplicate_face(encoding):

        return {

            "success": False,

            "message":
                "Face already registered"
        }

    # ============================
    # ✅ GET AUTHORIZED STUDENT
    # ============================
    authorized_student = (
        students_collection.find_one({

            "roll": roll
        })
    )

    # ============================
    # ✅ SAVE REGISTERED STUDENT
    # ============================
    registered_students_collection.insert_one({

        "name": name,

        "roll": roll,

        "email": email,

        "password": hashed,

        "face_encoding":
            encoding.tolist(),

        "class_id":
            authorized_student.get(
                "class_id"
            ),

        "class_name":
            authorized_student.get(
                "class_name"
            ),

        "section":
            authorized_student.get(
                "section"
            ),

        "department":
            authorized_student.get(
                "department"
            ),

        "year":
            authorized_student.get(
                "year"
            ),

        "semester":
            authorized_student.get(
                "semester",
                "N/A"
            ),

        "registered": True,

        "last_attendance": None
    })

    return {

        "success": True,

        "message":
            "Registered successfully"
    }


# ============================
# 🔐 LOGIN USER
# ============================
def login_user(
    email,
    password
):

    # ============================
    # ✅ FIND REGISTERED USER
    # ============================
    user = (
        registered_students_collection
        .find_one({

            "email": email
        })
    )

    # ❌ USER NOT FOUND
    if not user:

        return {

            "success": False,

            "message":
                "User not found"
        }

    # ============================
    # ❌ WRONG PASSWORD
    # ============================
    if not pwd_context.verify(

        password,
        user["password"]
    ):

        return {

            "success": False,

            "message":
                "Wrong password"
        }

    # ============================
    # ✅ CREATE TOKEN & UPDATE DB
    # ============================
    token = create_token({

        "id": str(user["_id"]),

        "role": "student"
    })
    
    registered_students_collection.update_one(
        {"_id": user["_id"]},
        {"$set": {"active_token": token}}
    )

    # ============================
    # ✅ SUCCESS
    # ============================
    return {

        "success": True,

        "token": token,

        "name": user["name"],

        "roll": user["roll"],
        
        "class_id": user.get("class_id"),
        
        "class_name": user.get("class_name")
    }


# ============================
# 📧 EMAIL VERIFICATION
# ============================
def send_verification_email(email: str, base_url: str, background_tasks: BackgroundTasks):
    # Check if email is already registered in registered_students
    existing_student = registered_students_collection.find_one({"email": email})
    if existing_student:
        return {
            "success": False,
            "message": "Email already registered"
        }

    # Generate verification token
    token = str(uuid.uuid4())
    
    # Save/update verification record
    email_verifications_collection.update_one(
        {"email": email},
        {
            "$set": {
                "token": token,
                "verified": False,
                "created_at": datetime.datetime.utcnow()
            }
        },
        upsert=True
    )
    
    # Construct verification URL
    if not base_url.endswith("/"):
        base_url += "/"
    verification_url = f"{base_url}api/auth/verify-email?token={token}"
    
    # Send email in background
    background_tasks.add_task(send_verification_email_smtp, email, verification_url)
    
    return {
        "success": True,
        "message": "Verification email sent successfully"
    }


def verify_email(token: str):
    record = email_verifications_collection.find_one({"token": token})
    if not record:
        error_html = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Verification Failed</title>
            <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@400;600;800&display=swap" rel="stylesheet">
            <style>
                body {
                    font-family: 'Outfit', sans-serif;
                    background: radial-gradient(circle at top, #1e1b4b, #0f172a);
                    color: #f8fafc;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    min-height: 100vh;
                    margin: 0;
                }
                .card {
                    background: rgba(30, 41, 59, 0.7);
                    backdrop-filter: blur(16px);
                    border: 1px solid rgba(239, 68, 68, 0.3);
                    border-radius: 24px;
                    padding: 40px;
                    text-align: center;
                    max-width: 450px;
                    width: 90%;
                    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5), 0 0 40px rgba(239, 68, 68, 0.1);
                }
                .icon-container {
                    width: 80px;
                    height: 80px;
                    background: rgba(239, 68, 68, 0.1);
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    margin: 0 auto 24px;
                    border: 2px solid #ef4444;
                }
                .icon {
                    font-size: 40px;
                    color: #ef4444;
                }
                h1 {
                    font-size: 26px;
                    margin: 0 0 12px;
                    font-weight: 800;
                    color: #ffffff;
                }
                p {
                    font-size: 15px;
                    line-height: 1.6;
                    color: #94a3b8;
                    margin: 0 0 24px;
                }
                .error-text {
                    color: #ef4444;
                    font-weight: 600;
                }
            </style>
        </head>
        <body>
            <div class="card">
                <div class="icon-container">
                    <span class="icon">✗</span>
                </div>
                <h1>Verification Failed</h1>
                <p>The verification link is invalid, expired, or has already been used.</p>
                <div class="error-text">Invalid Verification Token</div>
            </div>
        </body>
        </html>
        """
        return HTMLResponse(content=error_html, status_code=400)
    
    # Mark as verified
    email_verifications_collection.update_one(
        {"token": token},
        {"$set": {"verified": True}}
    )
    
    success_html = """
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Email Verification Success</title>
        <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@400;600;800&display=swap" rel="stylesheet">
        <style>
            body {
                font-family: 'Outfit', sans-serif;
                background: radial-gradient(circle at top, #1e1b4b, #0f172a);
                color: #f8fafc;
                display: flex;
                align-items: center;
                justify-content: center;
                min-height: 100vh;
                margin: 0;
            }
            .card {
                background: rgba(30, 41, 59, 0.7);
                backdrop-filter: blur(16px);
                border: 1px solid rgba(6, 182, 212, 0.3);
                border-radius: 24px;
                padding: 40px;
                text-align: center;
                max-width: 450px;
                width: 90%;
                box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5), 0 0 40px rgba(6, 182, 212, 0.1);
            }
            .icon-container {
                width: 80px;
                height: 80px;
                background: rgba(6, 182, 212, 0.1);
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                margin: 0 auto 24px;
                border: 2px solid #06b6d4;
            }
            .icon {
                font-size: 40px;
                color: #06b6d4;
            }
            h1 {
                font-size: 26px;
                margin: 0 0 12px;
                font-weight: 800;
                color: #ffffff;
            }
            p {
                font-size: 15px;
                line-height: 1.6;
                color: #94a3b8;
                margin: 0 0 24px;
            }
            .success-text {
                color: #10b981;
                font-weight: 600;
            }
        </style>
    </head>
    <body>
        <div class="card">
            <div class="icon-container">
                <span class="icon">✓</span>
            </div>
            <h1>Email Verified</h1>
            <p>Your email has been verified successfully. You can now return to the app to complete registration.</p>
            <div class="success-text">Verification Successful!</div>
        </div>
    </body>
    </html>
    """
    return HTMLResponse(content=success_html)


def check_verification_status(email: str):
    record = email_verifications_collection.find_one({"email": email})
    if record and record.get("verified", False):
        return {"verified": True}
    return {"verified": False}


# ============================
# 🔑 RESET PASSWORD
# ============================
def reset_password(email: str, new_password: str, base_url: str, background_tasks: BackgroundTasks):

    # ✅ CHECK EMAIL IS REGISTERED
    user = registered_students_collection.find_one({"email": email})
    if not user:
        return {
            "success": False,
            "message": "No account found with this email"
        }

    # ✅ CHECK EMAIL IS VERIFIED
    record = email_verifications_collection.find_one({"email": email})
    if not record or not record.get("verified", False):
        return {
            "success": False,
            "message": "Email is not verified. Please verify your email first."
        }

    # ✅ GENERATE TOKEN & HASH NEW PASSWORD
    token = str(uuid.uuid4())
    hashed = pwd_context.hash(new_password)

    # ✅ STORE PENDING RESET REQUEST
    password_resets_collection.update_one(
        {"email": email},
        {
            "$set": {
                "token": token,
                "new_password": hashed,
                "created_at": datetime.datetime.utcnow()
            }
        },
        upsert=True
    )

    # ✅ CONSTRUCT CONFIRMATION URL
    if not base_url.endswith("/"):
        base_url += "/"
    reset_url = f"{base_url}api/auth/confirm-reset-password?token={token}"

    # ✅ SEND CONFIRMATION EMAIL
    background_tasks.add_task(send_password_reset_email_smtp, email, reset_url)

    return {
        "success": True,
        "message": "Password reset email sent. Please check your inbox to confirm."
    }

# ============================
# 🔑 CONFIRM RESET PASSWORD
# ============================
def confirm_reset_password(token: str):
    record = password_resets_collection.find_one({"token": token})
    if not record:
        error_html = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Reset Failed</title>
            <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@400;600;800&display=swap" rel="stylesheet">
            <style>
                body {
                    font-family: 'Outfit', sans-serif;
                    background: radial-gradient(circle at top, #1e1b4b, #0f172a);
                    color: #f8fafc;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    min-height: 100vh;
                    margin: 0;
                }
                .card {
                    background: rgba(30, 41, 59, 0.7);
                    backdrop-filter: blur(12px);
                    padding: 48px;
                    border-radius: 24px;
                    box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
                    border: 1px solid rgba(239, 68, 68, 0.2);
                    text-align: center;
                    max-width: 420px;
                    width: 90%;
                }
                .icon-container {
                    width: 80px;
                    height: 80px;
                    background: rgba(239, 68, 68, 0.1);
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    margin: 0 auto 24px;
                    border: 2px solid rgba(239, 68, 68, 0.5);
                }
                .icon {
                    font-size: 40px;
                    color: #ef4444;
                }
                h1 {
                    font-size: 28px;
                    font-weight: 800;
                    margin: 0 0 16px;
                    color: #f8fafc;
                }
                p {
                    font-size: 15px;
                    line-height: 1.6;
                    color: #94a3b8;
                    margin: 0 0 24px;
                }
            </style>
        </head>
        <body>
            <div class="card">
                <div class="icon-container">
                    <span class="icon">✕</span>
                </div>
                <h1>Invalid Link</h1>
                <p>This password reset link is invalid or has expired. Please request a new password reset from the app.</p>
            </div>
        </body>
        </html>
        """
        return HTMLResponse(content=error_html)

    # ✅ UPDATE PASSWORD IN DB
    registered_students_collection.update_one(
        {"email": record["email"]},
        {"$set": {"password": record["new_password"]}}
    )

    # ✅ DELETE RESET REQUEST
    password_resets_collection.delete_one({"_id": record["_id"]})

    success_html = """
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Password Reset Successful</title>
        <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@400;600;800&display=swap" rel="stylesheet">
        <style>
            body {
                font-family: 'Outfit', sans-serif;
                background: radial-gradient(circle at top, #1e1b4b, #0f172a);
                color: #f8fafc;
                display: flex;
                align-items: center;
                justify-content: center;
                min-height: 100vh;
                margin: 0;
            }
            .card {
                background: rgba(30, 41, 59, 0.7);
                backdrop-filter: blur(12px);
                padding: 48px;
                border-radius: 24px;
                box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
                border: 1px solid rgba(16, 185, 129, 0.2);
                text-align: center;
                max-width: 420px;
                width: 90%;
            }
            .icon-container {
                width: 80px;
                height: 80px;
                background: rgba(16, 185, 129, 0.1);
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                margin: 0 auto 24px;
                border: 2px solid rgba(16, 185, 129, 0.5);
            }
            .icon {
                font-size: 40px;
                color: #10b981;
            }
            h1 {
                font-size: 28px;
                font-weight: 800;
                margin: 0 0 16px;
                color: #f8fafc;
            }
            p {
                font-size: 15px;
                line-height: 1.6;
                color: #94a3b8;
                margin: 0 0 24px;
            }
            .success-text {
                color: #10b981;
                font-weight: 600;
            }
        </style>
    </head>
    <body>
        <div class="card">
            <div class="icon-container">
                <span class="icon">✓</span>
            </div>
            <h1>Password Reset!</h1>
            <p>Your password has been successfully updated. You can now log in to the AttendancePRO app using your new password.</p>
            <div class="success-text">Update Successful!</div>
        </div>
    </body>
    </html>
    """
    return HTMLResponse(content=success_html)