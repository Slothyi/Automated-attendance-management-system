from jose import jwt
import os
from datetime import timezone,datetime, timedelta

SECRET = os.getenv("JWT_SECRET")

def create_token(data: dict):
    payload = data.copy()
    payload["exp"] = datetime.now(timezone.utc) + timedelta(hours=6)
    return jwt.encode(payload, SECRET, algorithm="HS256")

def verify_token(token: str):
    return jwt.decode(token, SECRET, algorithms=["HS256"])