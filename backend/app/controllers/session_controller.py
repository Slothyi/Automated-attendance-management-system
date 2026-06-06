from datetime import (
    datetime,
    timedelta,
    timezone
)

import random
import string
import uuid

from app.config.db import (
    attendance_sessions_collection,
    students_collection
)


# =========================
# 🔑 GENERATE SESSION CODE
# =========================
def generate_session_code():

    return ''.join(

        random.choices(

            string.ascii_uppercase +

            string.digits,

            k=6
        )
    )


# =========================
# ▶ START SESSION
# =========================
def start_attendance_session(
    class_id,
    classroom_beacon="CLASS_CSE_A"
):

    # =========================
    # ❌ DISABLE OLD SESSIONS
    # =========================
    attendance_sessions_collection.update_many(

        {
            "class_id": class_id,

            "active": True
        },

        {
            "$set": {
                "active": False
            }
        }
    )


    # =========================
    # 🔄 RESET ATTENDANCE STATUS
    # =========================
    students_collection.update_many(

    {
        "class_id": class_id
    },

    {
        "$set": {
            "attendance_status": "Absent"
        }
    }
    )

    # =========================
    # 📄 INITIALIZE CSV AS ABSENT
    # =========================
    try:
        from app.config.db import classes_collection
        from bson import ObjectId
        from app.utils.csv_export import initialize_class_csv
        
        class_data = classes_collection.find_one({"_id": ObjectId(class_id)})
        if class_data:
            students = list(students_collection.find({"class_id": class_id}))
            now_ist = datetime.now(timezone(timedelta(hours=5, minutes=30)))
            today_str = now_ist.strftime("%Y-%m-%d")
            
            initialize_class_csv(
                class_name=class_data.get("class_name", ""),
                students=students,
                date=today_str,
                section=class_data.get("section", ""),
                department=class_data.get("department", "")
            )
    except Exception as e:
        print(f"⚠️ Error initializing CSV on session start: {e}")

    session_code = generate_session_code()

    session_uuid = str(uuid.uuid4())
    
    bluetooth_name = (
        f"ATTENDANCE_{session_code}"
    )

    # Generate random unique 5-digit Hexadecimal number
    otp_code = ''.join(random.choices("0123456789ABCDEF", k=5))

    attendance_sessions_collection.insert_one({

        "class_id": class_id,

        "session_code": session_code,

        "session_uuid": session_uuid,
        
        "bluetooth_name":
            bluetooth_name,

        "classroom_beacon":
            classroom_beacon,

        "otp_code":
            otp_code,

        "active": True,

        "created_at":
            datetime.now(timezone.utc),

        "expires_at":

            datetime.now(
                timezone.utc
            ) + timedelta(minutes=10)
    })

    return {

        "success": True,

        "message":
            "Attendance session started",

        "session_code":
            session_code,
            
        "session_uuid":
            session_uuid,

        "bluetooth_name":
            bluetooth_name,

        "classroom_beacon":
            classroom_beacon,

        "otp_code":
            otp_code
    }


# =========================
# ⏹ STOP SESSION
# =========================
def stop_attendance_session(class_id):
    from app.config.db import classes_collection, attendance_collection, registered_students_collection
    from bson import ObjectId
    import csv
    import os

    # 1. Find the active session before stopping it to know when it started
    session = attendance_sessions_collection.find_one({
        "class_id": class_id,
        "active": True
    })

    if session:
        session_created_at = session.get("created_at", datetime.now(timezone.utc))
        session_date = session_created_at.strftime("%Y-%m-%d")
        
        # Stop the session
        attendance_sessions_collection.update_many(
            {"class_id": class_id, "active": True},
            {"$set": {"active": False}}
        )

        # 2. Get class data and students
        class_data = classes_collection.find_one({"_id": ObjectId(class_id)})
        if class_data and "students" in class_data:
            class_name = class_data.get("class_name", "Unknown Class")
            all_students = class_data["students"]

            # 3. Get students marked present today
            present_records = attendance_collection.find({
                "class_id": class_id,
                "date": session_date
            })
            present_rolls = {r["roll"] for r in present_records}

            # 4. Mark remaining as Absent
            absent_records_to_insert = []
            csv_rows_to_append = []
            now_time_str = datetime.now(timezone.utc).strftime("%H:%M:%S")

            for student in all_students:
                roll = student.get("roll")
                name = student.get("name")
                if roll and roll not in present_rolls:
                    # check if they are registered to get their user_id
                    reg_student = registered_students_collection.find_one({"roll": roll})
                    student_id = str(reg_student["_id"]) if reg_student else None

                    absent_records_to_insert.append({
                        "student_id": student_id,
                        "name": name,
                        "roll": roll,
                        "class_id": class_id,
                        "class_name": class_name,
                        "date": session_date,
                        "status": "Absent",
                        "created_at": datetime.now(timezone.utc)
                    })

                    csv_rows_to_append.append([
                        session_date,
                        roll,
                        name,
                        "Absent",
                        now_time_str
                    ])

            # Insert to DB
            if absent_records_to_insert:
                attendance_collection.insert_many(absent_records_to_insert)

            # Append to CSV
            if csv_rows_to_append:
                try:
                    os.makedirs("uploads", exist_ok=True)
                    csv_path = f"uploads/{class_id}_attendance.csv"
                    file_exists = os.path.isfile(csv_path)

                    with open(csv_path, mode='a', newline='', encoding='utf-8') as file:
                        writer = csv.writer(file)
                        if not file_exists:
                            writer.writerow(["Date", "Roll Number", "Name", "Status", "Time"])
                        writer.writerows(csv_rows_to_append)
                except Exception as e:
                    print("CSV Error (Absent):", e)

    return {
        "success": True,
        "message": "Attendance session stopped and missing students marked as absent"
    }


# =========================
# ✅ VERIFY SESSION
# =========================
def verify_attendance_session(
    session_code,
    session_uuid,
    classroom_beacon=None,
    otp_code=None
):

    session = (
        attendance_sessions_collection
        .find_one({

            "session_code":
                session_code,
                
            "session_uuid":
                session_uuid,

            "active": True
        })
    )

    if not session:
        return None

    # Verify classroom beacon if defined in session
    expected_beacon = session.get("classroom_beacon")
    if expected_beacon:
        if not classroom_beacon or classroom_beacon != expected_beacon:
            return None

    # Verify OTP manually entered code if defined in session
    expected_otp = session.get("otp_code")
    if expected_otp:
        if not otp_code or otp_code.strip().upper() != expected_otp.upper():
            return None
    
    expires_at = session["expires_at"]
    if expires_at.tzinfo is None:
        expires_at = expires_at.replace(tzinfo=timezone.utc)
        
    if datetime.now(timezone.utc) > expires_at:
        return None

    return session


# =========================
# 📡 GET ACTIVE SESSION
# =========================
def get_active_session(class_id, student_id=None):
    from app.config.db import registered_students_collection, students_collection
    from bson import ObjectId

    class_ids = [class_id]
    
    if student_id:
        student = registered_students_collection.find_one({"_id": ObjectId(student_id)})
        if student:
            roll = student.get("roll")
            if roll:
                classes = students_collection.find({"roll": roll})
                student_class_ids = [str(c["class_id"]) for c in classes]
                if student_class_ids:
                    class_ids = student_class_ids
                    if class_id not in class_ids:
                        class_ids.append(class_id)

    session = (
        attendance_sessions_collection
        .find_one({

            "class_id": {"$in": class_ids},

            "active": True
        })
    )

    if not session:

        return {

            "success": False,

            "message":
                "No active session"
        }

    return {

        "success": True,

        "session_code":
            session["session_code"],

        "session_uuid":
            session["session_uuid"],

        "bluetooth_name":
            session["bluetooth_name"],

        "classroom_beacon":
            session.get("classroom_beacon", "CLASS_CSE_A"),

        "otp_code":
            session.get("otp_code", "")
    }