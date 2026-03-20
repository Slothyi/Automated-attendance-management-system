from fastapi import APIRouter, File, UploadFile, Form
from pydantic import BaseModel
from app.controllers.auth_controller import register_user, login_user

router = APIRouter()


# ✅ Request model for login
class LoginRequest(BaseModel):
    email: str
    password: str


# ✅ REGISTER (multipart/form-data)
@router.post("/register")
def register(
    name: str = Form(...),
    roll: str = Form(...),
    email: str = Form(...),
    password: str = Form(...),
    file: UploadFile = File(...)
):
    return register_user(name, roll, email, password, file)


# ✅ LOGIN (JSON body)
@router.post("/login")
def login(data: LoginRequest):
    return login_user(data.email, data.password)