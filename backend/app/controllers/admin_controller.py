from app.config.db import admins_collection
from passlib.context import CryptContext
from app.utils.jwt_handler import create_token
from pymongo.errors import DuplicateKeyError
import re

pwd_context = CryptContext(
    schemes=["bcrypt"],
    deprecated="auto"
)


# =========================
# 👨‍💼 ADMIN LOGIN
# =========================
def admin_login(
    name,
    email,
    password
):

    normalized_name = str(name or "").strip().upper()

    normalized_email = str(email or "").strip().lower()

    if not normalized_name or not normalized_email or not password:

        return {
            "error": "Please fill all fields"
        }

    if "@" not in normalized_email or "." not in normalized_email:

        return {
            "error": "Invalid email address"
        }

    # ✅ FIND ADMIN
    admin = admins_collection.find_one({

        "name": normalized_name,

        "email": normalized_email
    })

    if not admin:

        admin = admins_collection.find_one({

            "name": {
                "$regex": f"^{re.escape(str(name or '').strip())}$",
                "$options": "i"
            },

            "email": {
                "$regex": f"^{re.escape(str(email or '').strip())}$",
                "$options": "i"
            }
        })

    # ❌ ADMIN NOT FOUND
    if not admin:

        return {
            "error": "Admin not found"
        }

    # ❌ WRONG PASSWORD
    if not pwd_context.verify(
        password,
        admin["password"]
    ):

        return {
            "error": "Wrong password"
        }

    try:

        admins_collection.update_one(
            {
                "_id": admin["_id"]
            },
            {
                "$set": {
                    "name": normalized_name,
                    "email": normalized_email
                }
            }
        )

    except DuplicateKeyError:

        return {
            "error": "Email already used by another admin"
        }

    # ✅ CREATE JWT TOKEN
    token = create_token({

        "id": str(admin["_id"]),

        "role": "admin"
    })

    # ✅ SUCCESS RESPONSE
    return {

        "token": token,

        "name": normalized_name,

        "email": normalized_email,

        "admin_id": str(admin["_id"])
    }
