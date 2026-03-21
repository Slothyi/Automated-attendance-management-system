from app.config.db import users_collection
from passlib.context import CryptContext
from app.utils.jwt_handler import create_token
from app.utils.face_recognition_utils import get_face_encoding
import os
import numpy as np

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


# ============================
# 🔥 FACE DUPLICATE CHECK
# ============================
def is_duplicate_face(new_encoding):

    if new_encoding is None:
        return False

    new_encoding = np.array(new_encoding)

    users = users_collection.find()

    for user in users:
        stored_encoding = user.get("face_encoding")

        if not stored_encoding:
            continue

        stored_encoding = np.array(stored_encoding)

        # 🔥 Ensure same shape
        if stored_encoding.shape != new_encoding.shape:
            continue

        # 🔥 Euclidean distance
        distance = np.linalg.norm(stored_encoding - new_encoding)

        print("🔍 FACE DISTANCE:", distance)

        # 🔥 Threshold
        if distance < 0.5:
            return True

    return False


# ============================
# 📝 REGISTER USER
# ============================
def register_user(name, roll, email, password, file):

    # 🔁 EMAIL CHECK
    existing_user = users_collection.find_one({"email": email})
    if existing_user:
        return {"error": "User already exists"}

    # 🔁 ROLL CHECK (IMPORTANT)
    existing_roll = users_collection.find_one({"roll": roll})
    if existing_roll:
        return {"error": "Roll already registered"}

    hashed = pwd_context.hash(password)

    os.makedirs("uploads", exist_ok=True)

    file_path = f"uploads/{email}_register.jpg"

    with open(file_path, "wb") as f:
        f.write(file.file.read())

    # 🤖 FACE ENCODING
    encoding = get_face_encoding(file_path)

    if encoding is None:
        return {"error": "No face detected"}

    # 🔥 DUPLICATE FACE CHECK
    if is_duplicate_face(encoding):
        return {"error": "Face already registered with another account"}

    # ✅ SAVE USER
    user = {
        "name": name,
        "roll": roll,
        "email": email,
        "password": hashed,
        "face_encoding": encoding.tolist()
    }

    users_collection.insert_one(user)

    return {"message": "Registered successfully"}


# ============================
# 🔐 LOGIN USER
# ============================
def login_user(email, password):

    user = users_collection.find_one({"email": email})

    if not user:
        return {"error": "User not found"}

    if not pwd_context.verify(password, user["password"]):
        return {"error": "Wrong password"}

    token = create_token({"id": str(user["_id"])})

    return {
        "token": token,
        "name": user["name"]
    }