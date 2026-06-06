from fastapi import APIRouter, File, UploadFile, Form, Request, BackgroundTasks
from fastapi.responses import HTMLResponse
from pydantic import BaseModel
from app.controllers.auth_controller import (
    register_user,
    login_user,
    send_verification_email,
    verify_email,
    check_verification_status,
    reset_password,
    confirm_reset_password
)

router = APIRouter()


# ✅ SEND VERIFICATION EMAIL
@router.post("/send-verification-email")
def send_email_route(email: str, request: Request, background_tasks: BackgroundTasks):
    base_url = str(request.base_url)
    return send_verification_email(email, base_url, background_tasks)


# ✅ VERIFY EMAIL (HTML Response)
@router.get("/verify-email", response_class=HTMLResponse)
def verify_email_route(token: str):
    return verify_email(token)


# ✅ CHECK VERIFICATION STATUS
@router.get("/check-verification-status")
def check_status_route(email: str):
    return check_verification_status(email)



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


# ✅ Request model for reset password
class ResetPasswordRequest(BaseModel):
    email: str
    new_password: str


# ✅ RESET PASSWORD (Requests reset, sends email)
@router.post("/reset-password")
def reset_password_route(data: ResetPasswordRequest, request: Request, background_tasks: BackgroundTasks):
    base_url = str(request.base_url)
    return reset_password(data.email, data.new_password, base_url, background_tasks)

# ✅ CONFIRM RESET PASSWORD (HTML Response)
@router.get("/confirm-reset-password", response_class=HTMLResponse)
def confirm_reset_password_route(token: str):
    return confirm_reset_password(token)