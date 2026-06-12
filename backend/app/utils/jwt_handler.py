from jose import jwt, JWTError
import os
from datetime import timezone,datetime, timedelta

SECRET = os.getenv("JWT_SECRET")

def create_token(data: dict):
    payload = data.copy()
    payload["exp"] = datetime.now(timezone.utc) + timedelta(hours=5)
    return jwt.encode(payload, SECRET, algorithm="HS256")

def verify_token(token: str):
    try:
        return jwt.decode(token, SECRET, algorithms=["HS256"])
    except JWTError:
        return None