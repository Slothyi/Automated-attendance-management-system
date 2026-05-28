from app.config.db import (
    attendance_collection,
    students_collection,
    classes_collection,
    registered_students_collection
)

from app.utils.distance import (
    calculate_distance
)

from app.utils.face_recognition_utils import (
    get_face_encoding,
    compare_faces
)

from datetime import (
    datetime,
    timezone,
    timedelta
)

from bson import ObjectId

import numpy as np
import cv2
import os


# =========================
# 📍 COLLEGE LOCATION
# =========================
COLLEGE_LAT = 23.526479
COLLEGE_LNG = 87.742496


# =========================
# ⏱️ 1 HOUR RULE
# =========================
def can_mark_attendance(last_attendance):

    if not last_attendance:
        return True

    if last_attendance.tzinfo is None:

        last_attendance = (
            last_attendance.replace(
                tzinfo=timezone.utc
            )
        )

    now = datetime.now(timezone.utc)

    return (

        now - last_attendance

    ) >= timedelta(hours=1)


# =========================
# ✅ MARK ATTENDANCE
# =========================
def mark_attendance(
    user_id,
    lat,
    lng,
    file
):

    try:

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
        # 🧠 FETCH USER
        # =========================
        user = (
            registered_students_collection
            .find_one({

                "_id": ObjectId(user_id)
            })
        )

        if not user:

            return {

                "status": "Error",

                "error":
                    "User not found"
            }

        # =========================
        # ⏳ COOLDOWN CHECK
        # =========================
        last = user.get(
            "last_attendance"
        )

        if not can_mark_attendance(last):

            now = datetime.now(
                timezone.utc
            )

            if (
                last and
                last.tzinfo is None
            ):

                last = last.replace(
                    tzinfo=timezone.utc
                )

            remaining = (
                timedelta(hours=1)
                - (now - last)
            )

            minutes = int(

                remaining.total_seconds()
                / 60
            )

            return {

                "status": "Error",

                "error":
                    f"Wait {minutes} minutes before marking again"
            }

        # =========================
        # 📸 READ FILE
        # =========================
        file_bytes = file.file.read()

        if (
            not file_bytes or
            len(file_bytes) == 0
        ):

            return {

                "status": "Error",

                "error":
                    "Empty image received"
            }

        # =========================
        # 📂 SAVE IMAGE
        # =========================
        os.makedirs(

            "uploads",

            exist_ok=True
        )

        file_path = (

            "uploads/"

            f"selfie_"
            f"{datetime.now().timestamp()}.jpg"
        )

        with open(file_path, "wb") as f:

            f.write(file_bytes)

        # =========================
        # 🖼️ VERIFY IMAGE
        # =========================
        image = cv2.imread(file_path)

        if image is None:

            return {

                "status": "Error",

                "error":
                    "Corrupted image"
            }

        # =========================
        # 🤖 UNKNOWN FACE
        # =========================
        unknown_encoding = (
            get_face_encoding(
                file_path
            )
        )

        if unknown_encoding is None:

            return {

                "status": "Error",

                "error":
                    "No valid face detected"
            }

        # =========================
        # 🧠 KNOWN FACE
        # =========================
        known_encoding = np.array(

            user["face_encoding"]
        )

        # =========================
        # 🤖 FACE MATCH
        # =========================
        match = compare_faces(

            known_encoding,
            unknown_encoding
        )

        if not match:

            return {

                "status": "Error",

                "error":
                    "Face does not match"
            }

        # =========================
        # 🕒 CURRENT TIME
        # =========================
        now = datetime.now(
            timezone.utc
        )

        # =========================
        # 💾 SAVE ATTENDANCE
        # =========================
        attendance = {

            "user_id": user_id,

            "status": "Present",

            "selfie_url": file_path,

            "latitude": lat,

            "longitude": lng,

            "timestamp": now
        }

        attendance_collection.insert_one(
            attendance
        )

        # =========================
        # 🔄 UPDATE REGISTERED USER
        # =========================
        registered_students_collection.update_one(

            {
                "_id": ObjectId(user_id)
            },

            {
                "$set": {

                    "last_attendance":
                        now
                }
            }
        )

        # =========================
        # 🔄 UPDATE AUTHORIZED STUDENT
        # =========================
        students_collection.update_one(

            {
                "roll": user["roll"]
            },

            {
                "$set": {

                    "attendance_status":
                        "Present"
                }
            }
        )

        # =========================
        # 🔄 UPDATE CLASS STATUS
        # =========================
        classes_collection.update_one(

            {
                "_id": ObjectId(
                    user["class_id"]
                ),

                "students.roll":
                    user["roll"]
            },

            {
                "$set": {

                    "students.$.attendance_status":
                        "Present"
                }
            }
        )

        # =========================
        # ✅ SUCCESS
        # =========================
        return {

            "status": "Present",

            "message":
                "Attendance marked successfully"
        }

    except Exception as e:

        print("❌ ERROR:", str(e))

        return {

            "status": "Error",

            "error":
                str(e)
        }

# =========================
# ❌ UNMARK ATTENDANCE
# =========================
def unmark_attendance(user_id):

    try:

        # =========================
        # 🧠 FETCH USER
        # =========================
        user = (
            registered_students_collection
            .find_one({

                "_id": ObjectId(user_id)
            })
        )

        if not user:

            return {

                "status": "Error",

                "error":
                    "User not found"
            }

        # =========================
        # 🗑️ DELETE ATTENDANCE
        # =========================
        result = (
            attendance_collection
            .find_one_and_delete(

                {
                    "user_id": user_id
                },

                sort=[("timestamp", -1)]
            )
        )

        if not result:

            return {

                "status": "Error",

                "error":
                    "No attendance found"
            }

        # =========================
        # 🔄 RESET USER
        # =========================
        registered_students_collection.update_one(

            {
                "_id": ObjectId(user_id)
            },

            {
                "$set": {

                    "last_attendance":
                        None
                }
            }
        )

        # =========================
        # 🔄 RESET STUDENT STATUS
        # =========================
        students_collection.update_one(

            {
                "roll": user["roll"]
            },

            {
                "$set": {

                    "attendance_status":
                        "Absent"
                }
            }
        )

        # =========================
        # 🔄 RESET CLASS STATUS
        # =========================
        classes_collection.update_one(

            {
                "_id": ObjectId(
                    user["class_id"]
                ),

                "students.roll":
                    user["roll"]
            },

            {
                "$set": {

                    "students.$.attendance_status":
                        "Absent"
                }
            }
        )

        return {

            "status": "Success",

            "message":
                "Attendance unmarked successfully"
        }

    except Exception as e:

        print("❌ ERROR:", str(e))

        return {

            "status": "Error",

            "error":
                str(e)
        }


# =========================
# 📊 TODAY STATUS
# =========================
def get_today_status(user_id):

    try:

        user = (
            registered_students_collection
            .find_one({

                "_id": ObjectId(user_id)
            })
        )

        if not user:

            return {

                "status": "Error",

                "error":
                    "User not found"
            }

        last = user.get(
            "last_attendance"
        )

        if not last:

            return {

                "status":
                    "Absent"
            }

        now = datetime.now(
            timezone.utc
        )

        if last.tzinfo is None:

            last = last.replace(
                tzinfo=timezone.utc
            )

        diff = now - last

        if diff < timedelta(hours=1):

            remaining = (
                timedelta(hours=1)
                - diff
            )

            minutes = int(

                remaining.total_seconds()
                / 60
            )

            return {

                "status":
                    "Present",

                "remaining_minutes":
                    minutes
            }

        return {

            "status":
                "Ready"
        }

    except Exception as e:

        print("❌ ERROR:", str(e))

        return {

            "status": "Error",

            "error":
                str(e)
        }


# =========================
# 📜 WEEKLY HISTORY
# =========================
def get_weekly_history(user_id):

    try:

        now = datetime.now(
            timezone.utc
        )

        week_ago = (
            now - timedelta(days=7)
        )

        records = (
            attendance_collection.find({

                "user_id": user_id,

                "timestamp": {
                    "$gte": week_ago
                }

            }).sort(
                "timestamp",
                -1
            )
        )

        history = []

        for r in records:

            history.append({

                "date":
                    r["timestamp"].strftime(
                        "%Y-%m-%d"
                    ),

                "status":
                    r["status"]
            })

        return {

            "status": "Success",

            "history":
                history
        }

    except Exception as e:

        print("❌ ERROR:", str(e))

        return {

            "status": "Error",

            "error":
                str(e)
        }

# =========================
# 📊 CLASS ATTENDANCE REPORT
# =========================
def get_class_attendance_report(class_id):

    from datetime import datetime, timedelta

    # =========================
    # ✅ FIND CLASS
    # =========================
    existing_class = classes_collection.find_one({

        "_id": ObjectId(class_id)
    })

    if not existing_class:

        return {
            "error": "Class not found"
        }

    students = students_collection.find({

        "class_id": class_id
    })

    report = []

    today = datetime.utcnow()

    week_ago = today - timedelta(days=7)

    month_ago = today - timedelta(days=30)

    for student in students:

        roll = student["roll"]

        # =========================
        # ✅ REGISTERED CHECK
        # =========================
        registered = (
            registered_students_collection.find_one({

                "roll": roll
            })
        )

        if not registered:

            continue

        user_id = str(registered["_id"])

        # =========================
        # ✅ WEEKLY RECORDS
        # =========================
        weekly_records = list(

            attendance_collection.find({

                "user_id": user_id,

                "timestamp": {
                    "$gte": week_ago
                }
            })
        )

        # =========================
        # ✅ MONTHLY RECORDS
        # =========================
        monthly_records = list(

            attendance_collection.find({

                "user_id": user_id,

                "timestamp": {
                    "$gte": month_ago
                }
            })
        )

        weekly_present = len(weekly_records)

        monthly_present = len(monthly_records)

        weekly_percentage = min(
            int((weekly_present / 7) * 100),
            100
        )

        monthly_percentage = min(
            int((monthly_present / 30) * 100),
            100
        )

        report.append({

            "name":
                student["name"],

            "roll":
                roll,

            "weekly_present":
                weekly_present,

            "monthly_present":
                monthly_present,

            "weekly_percentage":
                weekly_percentage,

            "monthly_percentage":
                monthly_percentage
        })

    return {

        "class_name":
            existing_class["class_name"],

        "report":
            report
    }