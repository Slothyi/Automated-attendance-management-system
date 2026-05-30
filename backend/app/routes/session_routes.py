from fastapi import APIRouter

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
def start_session(class_id: str):

    return start_attendance_session(
        class_id
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
@router.get("/active/{class_id}")
def active_session(class_id: str):

    return get_active_session(
        class_id
    )