from app.config.db import users_collection
from passlib.context import CryptContext
from app.utils.jwt_handler import create_token
from app.utils.face_recognition_utils import get_face_encoding
import os

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

def register_user(name, roll, email, password, file):

    hashed = pwd_context.hash(password)

    file_path = f"uploads/{email}_register.jpg"

    with open(file_path, "wb") as f:
        f.write(file.file.read())

    encoding = get_face_encoding(file_path)

    if encoding is None:
        return {"error": "No face detected"}

    user = {
        "name": name,
        "roll": roll,
        "email": email,
        "password": hashed,
        "face_encoding": encoding.tolist()
    }

    users_collection.insert_one(user)

    return {"message": "Registered successfully"}


def login_user(email, password):
    user = users_collection.find_one({"email": email})

    if not user:
        return {"error": "User not found"}

    if not pwd_context.verify(password, user["password"]):
        return {"error": "Wrong password"}

    token = create_token({"id": str(user["_id"])})

    return {"token": token}