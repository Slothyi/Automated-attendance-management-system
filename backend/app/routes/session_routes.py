from fastapi import APIRouter, Query

from app.controllers.session_controller import (

    start_attendance_session,

    stop_attendance_session,
    
    get_active_session
)



router = APIRouter()


# =========================
# ▶ START SESSION
# =========================
@router.post("/start/{class_id}")
def start_session(class_id: str, classroom_beacon: str = Query(default="CLASS_CSE_A")):

    return start_attendance_session(
        class_id,
        classroom_beacon
    )


# =========================
# ⏹ STOP SESSION
# =========================
@router.post("/stop/{class_id}")
def stop_session(class_id: str):

    return stop_attendance_session(
        class_id
    )
    

# =========================
# 📡 GET ACTIVE SESSION
# =========================
from fastapi import Header
from app.utils.jwt_handler import verify_token

@router.get("/active/{class_id}")
def active_session(class_id: str, authorization: str = Header(None)):

    student_id = None
    if authorization and authorization.startswith("Bearer "):
        token = authorization.split(" ")[1]
        try:
            user = verify_token(token)
            if user and user.get("role") == "student":
                student_id = user.get("id")
        except:
            pass

    return get_active_session(
        class_id, student_id
    )