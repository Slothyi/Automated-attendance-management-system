from fastapi import (
    APIRouter,
    File,
    UploadFile,
    Form,
    HTTPException,
    Depends
)

from fastapi.security import (
    HTTPAuthorizationCredentials,
    HTTPBearer
)

from app.controllers.attendance_controller import (

    mark_attendance,

    unmark_attendance,

    get_today_status,

    get_weekly_history,

    get_class_attendance_report
)

from app.utils.jwt_handler import verify_token

# =========================
# ✅ ROUTER
# =========================
router = APIRouter()

# =========================
# 🔐 SECURITY
# =========================
security = HTTPBearer()


# =========================
# 🔐 TOKEN DEPENDENCY
# =========================
def get_current_user(

    credentials:
    HTTPAuthorizationCredentials = Depends(security)

):

    print(
        "HEADER RECEIVED:",
        credentials
    )

    # =========================
    # ✅ EXTRACT TOKEN
    # =========================
    token = credentials.credentials

    # =========================
    # ✅ VERIFY TOKEN
    # =========================
    user = verify_token(token)

    if not user:

        raise HTTPException(

            status_code=401,

            detail="Invalid token"
        )

    return user


# =========================
# ✅ MARK ATTENDANCE
# =========================
@router.post("/mark")
def mark(

    lat: float = Form(...),

    lng: float = Form(...),
    
    session_code: str = Form(...),

    session_uuid: str = Form(...),
    
    file: UploadFile = File(...),

    user: dict = Depends(get_current_user)

):

    try:

        return mark_attendance(

            user["id"],

            lat,

            lng,
            
            session_code,
            
            session_uuid,
            
            file
        )

    except Exception as e:

        print("❌ ERROR:", str(e))

        raise HTTPException(

            status_code=500,

            detail="Internal server error"
        )


# =========================
# ❌ UNMARK ATTENDANCE
# =========================
@router.post("/unmark")
def unmark(

    user: dict = Depends(get_current_user)

):

    try:

        return unmark_attendance(
            user["id"]
        )

    except Exception as e:

        print("❌ ERROR:", str(e))

        raise HTTPException(

            status_code=500,

            detail="Internal server error"
        )


# =========================
# 📊 TODAY STATUS
# =========================
@router.get("/status")
def status(

    user: dict = Depends(get_current_user)

):

    try:

        return get_today_status(
            user["id"]
        )

    except Exception as e:

        print("❌ ERROR:", str(e))

        raise HTTPException(

            status_code=500,

            detail="Internal server error"
        )


# =========================
# 📅 WEEKLY HISTORY
# =========================
@router.get("/history")
def history(

    user: dict = Depends(get_current_user)

):

    try:

        return get_weekly_history(
            user["id"]
        )

    except Exception as e:

        print("❌ ERROR:", str(e))

        raise HTTPException(

            status_code=500,

            detail="Internal server error"
        )

# =========================
# 🔑 ADMIN USER DEPENDENCY
# =========================    
def get_admin_user(

    credentials:
    HTTPAuthorizationCredentials = Depends(security)

):

    token = credentials.credentials

    user = verify_token(token)

    if not user:

        raise HTTPException(
            status_code=401,
            detail="Invalid token"
        )

    if user.get("role") != "admin":

        raise HTTPException(
            status_code=403,
            detail="Admin access required"
        )

    return user

# =========================
# 📊 CLASS ATTENDANCE REPORT
# =========================
@router.get("/class-report/{class_id}")
def class_report(

    class_id: str,

    user: dict = Depends(get_admin_user)

):

    try:

        return get_class_attendance_report(
            class_id
        )

    except Exception as e:

        print("❌ ERROR:", str(e))

        raise HTTPException(

            status_code=500,

            detail="Internal server error"
        )
    
# =========================
# 🎓 ATTENDED CLASSES
# =========================
@router.get("/classes")
def student_classes(
    user: dict = Depends(get_current_user)
):
    try:
        from app.controllers.attendance_controller import get_student_classes
        return get_student_classes(user["id"])
    except Exception as e:
        print("❌ ERROR:", str(e))
        raise HTTPException(
            status_code=500,
            detail="Internal server error"
        )