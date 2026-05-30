from datetime import (
    datetime,
    timedelta,
    timezone
)

import random
import string
import uuid

from app.config.db import (
    attendance_sessions_collection,
    students_collection
)


# =========================
# 🔑 GENERATE SESSION CODE
# =========================
def generate_session_code():

    return ''.join(

        random.choices(

            string.ascii_uppercase +

            string.digits,

            k=6
        )
    )


# =========================
# ▶ START SESSION
# =========================
def start_attendance_session(
    class_id
):

    # =========================
    # ❌ DISABLE OLD SESSIONS
    # =========================
    attendance_sessions_collection.update_many(

        {
            "class_id": class_id,

            "active": True
        },

        {
            "$set": {
                "active": False
            }
        }
    )


    # =========================
    # 🔄 RESET ATTENDANCE STATUS
    # =========================
    students_collection.update_many(

    {
        "class_id": class_id
    },

    {
        "$set": {
            "attendance_status": "Absent"
        }
    }
    )   
    session_code = generate_session_code()

    session_uuid = str(uuid.uuid4())
    
    bluetooth_name = (
        f"ATTENDANCE_{session_code}"
    )

    attendance_sessions_collection.insert_one({

        "class_id": class_id,

        "session_code": session_code,

        "session_uuid": session_uuid,
        
        "bluetooth_name":
            bluetooth_name,

        "active": True,

        "created_at":
            datetime.now(timezone.utc),

        "expires_at":

            datetime.now(
                timezone.utc
            ) + timedelta(minutes=10)
    })

    return {

        "success": True,

        "message":
            "Attendance session started",

        "session_code":
            session_code,
            
        "session_uuid":
            session_uuid,

        "bluetooth_name":
            bluetooth_name
    }


# =========================
# ⏹ STOP SESSION
# =========================
def stop_attendance_session(
    class_id
):

    attendance_sessions_collection.update_many(

        {
            "class_id": class_id,

            "active": True
        },

        {
            "$set": {
                "active": False
            }
        }
    )

    return {

        "success": True,

        "message":
            "Attendance session stopped"
    }


# =========================
# ✅ VERIFY SESSION
# =========================
def verify_attendance_session(
    session_code,
    class_id,
    session_uuid
):

    session = (
        attendance_sessions_collection
        .find_one({

            "session_code":
                session_code,
                
            "session_uuid":
                session_uuid,

            "class_id":
                class_id,

            "active": True
        })
    )

    if not session:
        return False
    
    expires_at = session["expires_at"]
    if expires_at.tzinfo is None:
        expires_at = expires_at.replace(tzinfo=timezone.utc)
        
    if datetime.now(timezone.utc) > expires_at:
        return False

    return True


# =========================
# 📡 GET ACTIVE SESSION
# =========================
def get_active_session(class_id):

    session = (
        attendance_sessions_collection
        .find_one({

            "class_id": class_id,

            "active": True
        })
    )

    if not session:

        return {

            "success": False,

            "message":
                "No active session"
        }

    return {

        "success": True,

        "session_code":
            session["session_code"],

        "session_uuid":
            session["session_uuid"],

        "bluetooth_name":
            session["bluetooth_name"]
    }