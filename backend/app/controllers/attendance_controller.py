from fastapi import UploadFile
from bson import ObjectId

from datetime import (
    datetime,
    timezone,
    timedelta
)

import os
import numpy as np

from app.config.db import (

    attendance_collection,

    registered_students_collection,

    classes_collection,
    
    students_collection
)


from app.utils.distance import (
    calculate_distance
)

from app.utils.face_recognition_utils import (
    get_face_encoding
)

from app.controllers.session_controller import (
    verify_attendance_session
)

from app.utils.csv_export import (
    append_attendance_csv
)

# =========================
# 📍 COLLEGE LOCATION
# =========================
# COLLEGE_LAT = 22.576028
# COLLEGE_LNG = 88.427458

COLLEGE_LAT = 23.526427
COLLEGE_LNG = 87.742537

# =========================
# 🔥 FACE MATCH THRESHOLD
# =========================
FACE_THRESHOLD = 0.5


# =========================
# ✅ MARK ATTENDANCE
# =========================
def mark_attendance(

    user_id,
    lat,
    lng,
    session_code,
    session_uuid,
    file: UploadFile,
    classroom_beacon: str = None,
    otp_code: str = None
):

    # =========================
    # 👨‍🎓 GET STUDENT
    # =========================
    student = (
        registered_students_collection
        .find_one({

            "_id":
                ObjectId(user_id)
        })
    )

    if not student:

        return {

            "status": "Error",

            "error":
                "Student not found"
        }

    # =========================
    # 📍 LOCATION CHECK
    # =========================
    distance = calculate_distance(

        lat,
        lng,

        COLLEGE_LAT,
        COLLEGE_LNG
    )

    if distance > 0.2:

        return {

            "status": "Error",

            "error":
                "Outside college area"
        }

    # =========================
    # 📡 VERIFY ATTENDANCE SESSION & GET SESSION INFO
    # =========================
    session_info = verify_attendance_session(
        session_code,
        session_uuid,
        classroom_beacon,
        otp_code
    )

    if not session_info:

        return {

            "status": "Error",

            "error":
                "No active session, classroom beacon mismatch, or incorrect manual verification code"
        }
        
    session_class_id = session_info["class_id"]

    # =========================
    # 📂 SAVE IMAGE
    # =========================
    os.makedirs(
        "uploads",
        exist_ok=True
    )

    file_path = (

        f"uploads/"
        f"{student['roll']}_attendance.jpg"
    )

    with open(file_path, "wb") as f:

        f.write(file.file.read())

    # =========================
    # 🤖 GET FACE ENCODING
    # =========================
    encoding = get_face_encoding(
        file_path
    )

    if encoding is None:

        return {

            "status": "Error",

            "error":
                "No face detected"
        }

    # =========================
    # 🔍 MATCH FACE
    # =========================
    stored_encoding = np.array(

        student["face_encoding"]
    )

    distance_score = np.linalg.norm(

        stored_encoding -

        np.array(encoding)
    )

    print(
        "🔍 FACE DISTANCE:",
        distance_score
    )

    if distance_score > FACE_THRESHOLD:

        return {

            "status": "Error",

            "error":
                "Face does not match"
        }

    # =========================
    # 📅 TODAY DATE
    # =========================
    today = datetime.now(
        timezone.utc
    ).strftime("%Y-%m-%d")

    # =========================
    # ❌ ALREADY PRESENT IN LAST 5 MINUTES?
    # =========================
    five_minutes_ago = datetime.now(timezone.utc) - timedelta(minutes=5)
    existing = (
        attendance_collection.find_one({

            "student_id": user_id,

            "created_at": {
                "$gte": five_minutes_ago
            }
        })
    )

    if existing:

        return {

            "status": "Error",

            "error":
                "Attendance already marked in the last 5 minutes"
        }

    # =========================
    # 🏫 FIND STUDENT CLASS
    # =========================
    class_data = classes_collection.find_one({

        "_id": ObjectId(session_class_id),
        "students.roll": student["roll"]
    })

    class_id = None
    class_name = None

    if class_data:

        class_id = str(
            class_data["_id"]
        )

        class_name = (
            class_data["class_name"]
        )
    else:
        return {
            "status": "Error",
            "error": "You are not enrolled in the class for this session"
        }

    # =========================
    # ✅ SAVE ATTENDANCE
    # =========================
    now_ist = datetime.now(
        timezone(timedelta(hours=5, minutes=30))
    )

    attendance_collection.insert_one({

        "student_id":
            user_id,

        "name":
            student["name"],

        "roll":
            student["roll"],

        "class_id":
            class_id,

        "class_name":
            class_name,

        "date":
            today,

        "status":
            "Present",

        "created_at":
            datetime.now(
                timezone.utc
            )
    })

    # =========================
    # 📄 APPEND TO CLASS CSV
    # =========================
    try:

        append_attendance_csv(

            student_id  = user_id,

            name        = student["name"],

            roll        = student["roll"],

            class_name  = class_name or "",

            date        = now_ist.strftime("%Y-%m-%d"),

            time        = now_ist.strftime("%H:%M:%S"),

            section     = class_data.get("section", "") if class_data else "",

            department  = class_data.get("department", "") if class_data else "",
        )

    except Exception as csv_err:

        # CSV write failure must never block the attendance response
        print(f"⚠️ CSV export error: {csv_err}")

    # =========================
    # ✅ UPDATE STUDENT STATUS
    # =========================
    students_collection.update_one(
        {
        "roll": student["roll"]
        },
                                   
        {
            "$set": {
                "attendance_status": "Present"
            }
        })
    # =========================
    # 🔄 UPDATE LAST ATTENDANCE
    # =========================
    registered_students_collection.update_one(

        {
            "_id":
                ObjectId(user_id)
        },

        {
            "$set": {

                "last_attendance":
                    today
            }
        }
    )

    return {

        "status": "Success",

        "message":
            "Attendance marked successfully"
    }


# =========================
# ❌ UNMARK ATTENDANCE
# =========================
def unmark_attendance(user_id):

    today = datetime.now(
        timezone.utc
    ).strftime("%Y-%m-%d")

    attendance_doc = (
        attendance_collection.find_one({

            "student_id":
                user_id,

            "date":
                today
        })
    )

    if attendance_doc:

        students_collection.update_one(

            {
                "roll":
                    attendance_doc["roll"]
            },

            {
                "$set": {
                    "attendance_status":
                        "Absent"
                }
            }
        )

    result = (
        attendance_collection.delete_one({

            "student_id":
                user_id,

            "date":
                today
        })
    )

    if result.deleted_count == 1:

        return {

            "status": "Success",

            "error":
                "Attendance not found"
        }

    return {

        "status": "Success",

        "message":
            "Attendance Unmarked"
    }
# =========================
# 📊 TODAY STATUS
# =========================
def get_today_status(user_id):
    latest_attendance = (
        attendance_collection.find_one(
            {"student_id": user_id},
            sort=[("created_at", -1)]
        )
    )

    remaining_minutes = 0
    remaining_seconds = 0
    if latest_attendance and "created_at" in latest_attendance:
        created_at = latest_attendance["created_at"]
        if created_at.tzinfo is None:
            created_at = created_at.replace(tzinfo=timezone.utc)
        
        now = datetime.now(timezone.utc)
        elapsed = (now - created_at).total_seconds()
        
        if elapsed < 300: # 5 minutes cooldown
            remaining_seconds = max(0, int(300 - elapsed))
            remaining_minutes = int(remaining_seconds // 60) + (1 if remaining_seconds % 60 > 0 else 0)

    if remaining_seconds > 0:
        return {
            "marked": True,
            "status": "Present",
            "remaining_minutes": remaining_minutes,
            "remaining_seconds": remaining_seconds
        }

    return {
        "marked": False,
        "status": "Absent",
        "remaining_minutes": 0,
        "remaining_seconds": 0
    }


# =========================
# 📜 WEEKLY HISTORY
# =========================
def get_weekly_history(user_id):
    from app.config.db import registered_students_collection
    from bson import ObjectId

    student = registered_students_collection.find_one({"_id": ObjectId(user_id)})
    roll = student.get("roll") if student else None

    query = {"student_id": user_id}
    if roll:
        query = {
            "$or": [
                {"student_id": user_id},
                {"roll": roll}
            ]
        }

    history = list(
        attendance_collection.find(query)
        .sort(
            "created_at",
            -1
        )
    )

    formatted = []

    for item in history:
        created_at_str = None
        if "created_at" in item:
            created_at_str = item["created_at"].isoformat()

        formatted.append({
            "date": item["date"],
            "status": item["status"],
            "time": created_at_str
        })

    return {
        "history": formatted
    }


# =========================
# 📊 CLASS ATTENDANCE REPORT
# =========================
def get_class_attendance_report(
    class_id
):

    # =========================
    # 🏫 FIND CLASS
    # =========================
    class_data = classes_collection.find_one({

        "_id":
            ObjectId(class_id)
    })

    if not class_data:

        return {

            "success": False,

            "error":
                "Class not found"
        }

    students = class_data.get(
        "students",
        []
    )

    report = []

    present_count = 0
    absent_count = 0
    na_count = 0

    # =========================
    # 📅 DATE RANGES
    # =========================
    today = datetime.now(timezone.utc)

    week_start = today - timedelta(days=7)

    month_start = today - timedelta(days=30)

    today_str = today.strftime("%Y-%m-%d")

    # =========================
    # 👨‍🎓 LOOP STUDENTS
    # =========================
    for student in students:

        roll = student.get("roll")

        name = student.get("name")

        registered_student = (
            registered_students_collection.find_one({
                "roll": roll
            })
        )

        # =========================
        # ❌ NOT REGISTERED
        # =========================
        if not registered_student:

            status = "N/A"

            weekly_count = 0

            monthly_count = 0

            na_count += 1

        else:

            # =========================
            # 📊 WEEKLY COUNT
            # =========================
            weekly_count = (
                attendance_collection.count_documents({

                    "roll": roll,

                    "created_at": {
                        "$gte": week_start
                    }
                })
            )

            # =========================
            # 📊 MONTHLY COUNT
            # =========================
            monthly_count = (
                attendance_collection.count_documents({

                    "roll": roll,

                    "created_at": {
                        "$gte": month_start
                    }
                })
            )

            # =========================
            # 📅 TODAY ATTENDANCE
            # =========================
            attendance = (
                attendance_collection.find_one({

                    "roll": roll,

                    "date": today_str
                })
            )

            if attendance:

                status = "Present"

                present_count += 1

            else:

                status = "Absent"

                absent_count += 1

        report.append({

            "name": name,

            "roll": roll,

            "attendance_status": status,

            "weekly_attendance": weekly_count,

            "monthly_attendance": monthly_count
        })

    return {

        "success": True,

        "class_name":
            class_data["class_name"],

        "present_students":
            present_count,

        "absent_students":
            absent_count,

        "na_students":
            na_count,

        "students":
            report
    }

# =========================
# 🎓 GET ATTENDED CLASSES
# =========================
def get_student_classes(user_id):
    pipeline = [
        {"$match": {"student_id": user_id, "status": "Present"}},
        {"$group": {"_id": "$class_name", "attended_count": {"$sum": 1}}},
        {"$sort": {"attended_count": -1}}
    ]
    results = list(attendance_collection.aggregate(pipeline))
    
    classes_list = []
    for item in results:
        # Use class_name string
        c_name = item["_id"] if item["_id"] else "Unknown Class"
        classes_list.append({
            "class_name": c_name,
            "attended_count": item["attended_count"]
        })
        
    return {
        "success": True,
        "classes": classes_list
    }



# =========================
# ✍ UPDATE MANUAL ATTENDANCE
# =========================
def update_manual_attendance(class_id: str, students: list):
    # Get current IST date
    now = datetime.now(timezone.utc)
    ist_time = now + timedelta(hours=5, minutes=30)
    today_str = ist_time.strftime("%Y-%m-%d")

    for student in students:
        roll = student.roll
        status = student.attendance_status
        name = student.name

        # Find student to get student_id if possible
        s_doc = registered_students_collection.find_one({"roll": roll})
        student_id = str(s_doc["_id"]) if s_doc else None

        # Upsert attendance record
        query = {
            "class_id": class_id,
            "date": today_str,
            "$or": [{"roll": roll}]
        }
        if student_id:
            query["$or"].append({"student_id": student_id})

        update_data = {
            "status": status,
            "name": name,
            "roll": roll,
            "class_id": class_id,
            "date": today_str
        }
        if student_id:
            update_data["student_id"] = student_id

        attendance_collection.update_one(
            query,
            {"$set": update_data},
            upsert=True
        )

    # Re-export CSV for today (optional but recommended to keep it synced)
    # The existing append_attendance_csv logic mostly appends, 
    # to perfectly sync manual overrides we'd recreate the file or just let DB be source of truth.
    # For now, we just update the DB as the source of truth.

    return {"message": "Attendance updated successfully"}
