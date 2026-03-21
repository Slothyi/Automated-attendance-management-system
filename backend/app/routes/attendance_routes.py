from fastapi import APIRouter, File, UploadFile, Form, Header, HTTPException, Depends
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from app.controllers.attendance_controller import (
    mark_attendance,
    unmark_attendance,
    get_today_status,
    get_weekly_history
)

from app.utils.jwt_handler import verify_token

router = APIRouter()
security = HTTPBearer()

# =========================
# 🔐 TOKEN DEPENDENCY
# =========================
def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security)):
    print("HEADER RECEIVED:", credentials)

    token = credentials.credentials   # ✅ correct token extraction

    user = verify_token(token)

    if not user:
        raise HTTPException(status_code=401, detail="Invalid token")

    return user


# =========================
# ✅ MARK ATTENDANCE
# =========================
@router.post("/mark")
def mark(
    lat: float = Form(...),
    lng: float = Form(...),
    file: UploadFile = File(...),
    user: dict = Depends(get_current_user)   # ✅ CORRECT
):
    try:
        return mark_attendance(user["id"], lat, lng, file)

    except Exception as e:
        print("❌ ERROR:", str(e))
        raise HTTPException(status_code=500, detail="Internal server error")


# =========================
# ❌ UNMARK
# =========================
@router.post("/unmark")
def unmark(user: dict = Depends(get_current_user)):   # ✅ CORRECT
    try:
        return unmark_attendance(user["id"])

    except Exception as e:
        print("❌ ERROR:", str(e))
        raise HTTPException(status_code=500, detail="Internal server error")


# =========================
# 📊 STATUS
# =========================
@router.get("/status")
def status(user: dict = Depends(get_current_user)):   # ✅ CORRECT
    try:
        return get_today_status(user["id"])

    except Exception as e:
        print("❌ ERROR:", str(e))
        raise HTTPException(status_code=500, detail="Internal server error")
    

# =========================
# 📊 WEEKLY HISTORY
# =========================
@router.get("/history")
def history(user: dict = Depends(get_current_user)):
    return get_weekly_history(user["id"])