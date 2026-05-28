from fastapi import FastAPI
from fastapi.security import HTTPBearer

# ✅ ROUTES
from app.routes import (
    auth_routes,
    attendance_routes,
    admin_routes,
    class_routes
)

app = FastAPI()

security = HTTPBearer()

# =========================
# 🔐 AUTH ROUTES
# =========================
app.include_router(
    auth_routes.router,
    prefix="/api/auth",
    tags=["Auth"]
)

# =========================
# 📸 ATTENDANCE ROUTES
# =========================
app.include_router(
    attendance_routes.router,
    prefix="/api/attendance",
    tags=["Attendance"]
)

# =========================
# 👨‍💼 ADMIN ROUTES
# =========================
app.include_router(
    admin_routes.router,
    prefix="/api/admin",
    tags=["Admin"]
)

# =========================
# 🏫 CLASS ROUTES
# =========================
app.include_router(
    class_routes.router,
    prefix="/api/class",
    tags=["Class"]
)

# =========================
# 🏠 ROOT
# =========================
@app.get("/")
def home():

    return {
        "message": "Attendance Management API Running"
    }