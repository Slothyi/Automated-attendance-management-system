from fastapi import APIRouter
from pydantic import BaseModel

from app.controllers.admin_controller import admin_login

# ✅ ROUTER
router = APIRouter()


# =========================
# 👨‍💼 ADMIN LOGIN MODEL
# =========================
class AdminLoginRequest(BaseModel):

    name: str

    email: str

    password: str


# =========================
# 🔐 ADMIN LOGIN ROUTE
# =========================
@router.post("/login")
def login(data: AdminLoginRequest):

    return admin_login(

        data.name,

        data.email,

        data.password
    )