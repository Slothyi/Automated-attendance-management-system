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

    get_class_attendance_report,
    
    update_manual_attendance
)

from pydantic import BaseModel
from typing import List

class StudentItemPayload(BaseModel):
    name: str
    roll: str
    attendance_status: str

class ManualAttendancePayload(BaseModel):
    class_id: str
    students: List[StudentItemPayload]


from app.utils.jwt_handler import verify_token
from bson import ObjectId
from app.config.db import registered_students_collection

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

    # =========================
    # ✅ SINGLE ACTIVE SESSION CHECK
    # =========================
    try:
        student_doc = registered_students_collection.find_one({"_id": ObjectId(user["id"])})
        if not student_doc or student_doc.get("active_token") != token:
            raise HTTPException(
                status_code=401,
                detail="Session expired. Another device has logged in."
            )
    except Exception as e:
        if isinstance(e, HTTPException):
            raise e
        raise HTTPException(
            status_code=401,
            detail="Session expired. Another device has logged in."
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

    classroom_beacon: str = Form(default=None),

    otp_code: str = Form(default=None),

    user: dict = Depends(get_current_user)

):

    try:

        return mark_attendance(

            user["id"],

            lat,

            lng,
            
            session_code,
            
            session_uuid,
            
            file,

            classroom_beacon,

            otp_code
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
# =========================
# ✍ MANUAL ATTENDANCE OVERRIDE
# =========================
@router.post("/manual_update")
def manual_update(
    payload: ManualAttendancePayload,
    user: dict = Depends(get_current_user)
):
    return update_manual_attendance(payload.class_id, payload.students)
