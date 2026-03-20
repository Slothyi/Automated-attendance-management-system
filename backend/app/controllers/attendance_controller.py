from app.config.db import attendance_collection, users_collection
from app.utils.distance import calculate_distance
from app.utils.time_check import is_within_time
from app.utils.face_recognition_utils import get_face_encoding, compare_faces
from datetime import datetime
from bson import ObjectId
import numpy as np
import cv2

COLLEGE_LAT = 23.526515
COLLEGE_LNG = 87.742507


def mark_attendance(user_id, lat, lng, file):

    # ⏰ TIME CHECK
    if not is_within_time():
        return {"error": "Outside allowed time"}

    # 📍 LOCATION CHECK
    distance = calculate_distance(lat, lng, COLLEGE_LAT, COLLEGE_LNG)

    if distance > 0.2:
        return {"error": "Outside college area"}

    # 🔁 DUPLICATE CHECK
    today = datetime.now().strftime("%Y-%m-%d")

    existing = attendance_collection.find_one({
        "user_id": user_id,
        "date": today
    })

    if existing:
        return {"error": "Already marked"}

    # 🔥 READ FILE (SAFE)
    file_bytes = file.file.read()
    print("📸 FILE SIZE:", len(file_bytes))

    if len(file_bytes) == 0:
        return {"error": "Empty image received"}

    # 📸 SAVE FILE
    file_path = f"uploads/selfie_{datetime.now().timestamp()}.jpg"

    with open(file_path, "wb") as f:
        f.write(file_bytes)

    # 🔥 FIX: VERIFY IMAGE CAN BE READ
    image = cv2.imread(file_path)
    if image is None:
        return {"error": "Corrupted image"}

    # 🤖 FACE ENCODING
    unknown_encoding = get_face_encoding(file_path)

    if unknown_encoding is None:
        return {"error": "No face detected"}

    # 🧠 FETCH USER
    user = users_collection.find_one({"_id": ObjectId(user_id)})

    if not user:
        return {"error": "User not found"}

    known_encoding = np.array(user["face_encoding"])

    # 🤖 FACE MATCH
    match = compare_faces(known_encoding, unknown_encoding)

    if not match:
        return {"error": "Face does not match"}

    # 💾 SAVE ATTENDANCE
    attendance = {
        "user_id": user_id,
        "date": today,
        "status": "present",
        "selfie_url": file_path,
        "latitude": lat,
        "longitude": lng,
        "timestamp": datetime.now()
    }

    attendance_collection.insert_one(attendance)

    return {
        "message": "Attendance marked successfully",
        "status": "present"
    }


def unmark_attendance(user_id):

    today = datetime.now().strftime("%Y-%m-%d")

    result = attendance_collection.delete_one({
        "user_id": user_id,
        "date": today
    })

    if result.deleted_count == 0:
        return {"error": "No attendance found to delete"}

    return {"message": "Attendance unmarked successfully"}


def get_today_status(user_id):

    today = datetime.now().strftime("%Y-%m-%d")

    record = attendance_collection.find_one({
        "user_id": user_id,
        "date": today
    })

    if record:
        return {"status": "present"}
    else:
        return {"status": "absent"}