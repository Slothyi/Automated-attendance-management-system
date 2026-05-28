from app.config.db import (
    students_collection,
    registered_students_collection
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

import os
import numpy as np

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
    # ✅ CREATE TOKEN
    # ============================
    token = create_token({

        "id": str(user["_id"]),

        "role": "student"
    })

    # ============================
    # ✅ SUCCESS
    # ============================
    return {

        "success": True,

        "token": token,

        "name": user["name"],

        "roll": user["roll"]
    }