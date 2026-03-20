# from fastapi import APIRouter, File, UploadFile, Form, Depends
# from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
# from app.controllers.attendance_controller import (
#     mark_attendance,
#     unmark_attendance,
#     get_today_status
# )
# from app.utils.jwt_handler import verify_token

# router = APIRouter()

# security = HTTPBearer()

# # ✅ MARK ATTENDANCE
# @router.post("/mark")
# def mark(
#     lat: float = Form(...),
#     lng: float = Form(...),
#     file: UploadFile = File(...),
#     credentials: HTTPAuthorizationCredentials = Depends(security)
# ):
#     token = credentials.credentials
#     user = verify_token(token)

#     return mark_attendance(user["id"], lat, lng, file)


# # ✅ UNMARK ATTENDANCE
# @router.post("/unmark")
# def unmark(
#     credentials: HTTPAuthorizationCredentials = Depends(security)
# ):
#     token = credentials.credentials
#     user = verify_token(token)

#     return unmark_attendance(user["id"])


# # ✅ TODAY STATUS
# @router.get("/status")
# def status(
#     credentials: HTTPAuthorizationCredentials = Depends(security)
# ):
#     token = credentials.credentials
#     user = verify_token(token)

#     return get_today_status(user["id"])


from fastapi import APIRouter, File, UploadFile, Form, Header
from app.controllers.attendance_controller import (
    mark_attendance,
    unmark_attendance,
    get_today_status
)
from app.utils.jwt_handler import verify_token

router = APIRouter()


@router.post("/mark")
def mark(
    lat: float = Form(...),
    lng: float = Form(...),
    file: UploadFile = File(...),
    authorization: str = Header(...)
):
    try:
        token = authorization.split(" ")[1]
    except:
        return {"error": "Invalid token"}

    user = verify_token(token)
    return mark_attendance(user["id"], lat, lng, file)


@router.post("/unmark")
def unmark(authorization: str = Header(...)):
    try:
        token = authorization.split(" ")[1]
    except:
        return {"error": "Invalid token"}

    user = verify_token(token)
    return unmark_attendance(user["id"])


@router.get("/status")
def status(authorization: str = Header(...)):
    try:
        token = authorization.split(" ")[1]
    except:
        return {"error": "Invalid token"}

    user = verify_token(token)
    return get_today_status(user["id"])