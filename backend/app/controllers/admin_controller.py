from app.config.db import admins_collection
from passlib.context import CryptContext
from app.utils.jwt_handler import create_token

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

    # ✅ FIND ADMIN
    admin = admins_collection.find_one({

        "name": name,

        "email": email
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

    # ✅ CREATE JWT TOKEN
    token = create_token({

        "id": str(admin["_id"]),

        "role": "admin"
    })

    # ✅ SUCCESS RESPONSE
    return {

        "token": token,

        "name": admin["name"],

        "email": admin["email"]
    }