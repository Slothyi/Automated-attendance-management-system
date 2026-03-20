from app.config.db import attendance_collection, users_collection
from app.utils.distance import calculate_distance
from app.utils.time_check import can_mark_attendance
from app.utils.face_recognition_utils import get_face_encoding, compare_faces

from datetime import datetime, timezone
from bson import ObjectId
import numpy as np
import cv2
import os


COLLEGE_LAT = 23.526515
COLLEGE_LNG = 87.742507


# =========================
# ✅ MARK ATTENDANCE
# =========================
def mark_attendance(user_id, lat, lng, file):
    try:
        # 📍 LOCATION CHECK
        distance = calculate_distance(lat, lng, COLLEGE_LAT, COLLEGE_LNG)

        if distance > 0.2:
            return {"error": "Outside college area"}

        # 🧠 FETCH USER
        user = users_collection.find_one({"_id": ObjectId(user_id)})

        if not user:
            return {"error": "User not found"}

        # ⏳ 19-HOUR RULE
        if not can_mark_attendance(user.get("last_attendance")):
            return {"error": "Wait 19 hours before marking again"}

        # 🔁 OPTIONAL: prevent rapid duplicate clicks
        recent = attendance_collection.find_one(
            {"user_id": user_id},
            sort=[("timestamp", -1)]
        )

        if recent:
            last_time = recent.get("timestamp")

            if last_time:
                if last_time.tzinfo is None:
                    last_time = last_time.replace(tzinfo=timezone.utc)

                if (datetime.now(timezone.utc) - last_time).seconds < 60:
                    return {"error": "Please wait before retrying"}

        # 📸 READ FILE
        file_bytes = file.file.read()

        if not file_bytes or len(file_bytes) == 0:
            return {"error": "Empty image received"}

        # 📂 SAVE FILE
        os.makedirs("uploads", exist_ok=True)
        file_path = f"uploads/selfie_{datetime.now().timestamp()}.jpg"

        with open(file_path, "wb") as f:
            f.write(file_bytes)

        # 🖼️ VERIFY IMAGE
        try:
            image = cv2.imread(file_path)
            if image is None:
                return {"error": "Corrupted image"}
        except Exception:
            return {"error": "Image processing failed"}

        # 🤖 FACE ENCODING
        unknown_encoding = get_face_encoding(file_path)

        if unknown_encoding is None:
            return {"error": "No face detected"}

        # 🧠 KNOWN FACE
        known_encoding = np.array(user["face_encoding"])

        # 🤖 FACE MATCH
        match = compare_faces(known_encoding, unknown_encoding)

        if not match:
            return {"error": "Face does not match"}

        # 💾 SAVE ATTENDANCE
        now = datetime.now(timezone.utc)

        attendance = {
            "user_id": user_id,
            "status": "present",
            "selfie_url": file_path,
            "latitude": lat,
            "longitude": lng,
            "timestamp": now
        }

        attendance_collection.insert_one(attendance)

        # 🔄 UPDATE USER (for 19-hour rule)
        users_collection.update_one(
            {"_id": ObjectId(user_id)},
            {"$set": {"last_attendance": now}}
        )

        return {
            "message": "Attendance marked successfully",
            "status": "present"
        }

    except Exception as e:
        print("❌ ERROR:", str(e))
        return {"error": "Internal server error"}


# =========================
# ❌ UNMARK ATTENDANCE
# =========================
def unmark_attendance(user_id):
    try:
        # 🔥 delete latest attendance
        result = attendance_collection.find_one_and_delete(
            {"user_id": user_id},
            sort=[("timestamp", -1)]
        )

        if not result:
            return {"error": "No attendance found to delete"}

        # 🔥 reset cooldown
        users_collection.update_one(
            {"_id": ObjectId(user_id)},
            {"$set": {"last_attendance": None}}
        )

        return {"message": "Attendance unmarked successfully"}

    except Exception as e:
        print("❌ ERROR:", str(e))
        return {"error": "Internal server error"}


# =========================
# 📊 STATUS
# =========================
def get_today_status(user_id):
    try:
        recent = attendance_collection.find_one(
            {"user_id": user_id},
            sort=[("timestamp", -1)]
        )

        if recent:
            return {"status": "present"}
        else:
            return {"status": "absent"}

    except Exception as e:
        print("❌ ERROR:", str(e))
        return {"error": "Internal server error"}