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

# =========================
# 📍 COLLEGE LOCATION
# =========================
COLLEGE_LAT = 22.575698
COLLEGE_LNG = 88.427682

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
    file: UploadFile
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
    # 📡 VERIFY ATTENDANCE SESSION
    # =========================
    if not verify_attendance_session(
        session_code,
        student["class_id"],
        session_uuid
    ):

        return {

            "status": "Error",

            "error":
                "No active attendance session"
        }

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
    # ❌ ALREADY PRESENT?
    # =========================
    existing = (
        attendance_collection.find_one({

            "student_id": user_id,

            "date": today
        })
    )

    if existing:

        return {

            "status": "Error",

            "error":
                "Attendance already marked"
        }

    # =========================
    # 🏫 FIND STUDENT CLASS
    # =========================
    class_data = classes_collection.find_one({

        "students.roll":
            student["roll"]
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

    # =========================
    # ✅ SAVE ATTENDANCE
    # =========================
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

    today = datetime.now(
        timezone.utc
    ).strftime("%Y-%m-%d")

    attendance = (
        attendance_collection.find_one({

            "student_id":
                user_id,

            "date":
                today
        })
    )

    if attendance:

        return {

            "marked": True,

            "status": "Present"
        }

    return {

        "marked": False,

        "status": "Absent"
    }


# =========================
# 📜 WEEKLY HISTORY
# =========================
def get_weekly_history(user_id):

    history = list(

        attendance_collection.find({

            "student_id":
                user_id
        })

        .sort(
            "created_at",
            -1
        )

        .limit(7)
    )

    formatted = []

    for item in history:

        formatted.append({

            "date":
                item["date"],

            "status":
                item["status"]
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

