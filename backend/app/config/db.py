from pymongo import ASCENDING, MongoClient
from pymongo.errors import OperationFailure
import os
from dotenv import load_dotenv

# =========================
# ✅ LOAD ENV
# =========================
load_dotenv()

# =========================
# ✅ CONNECT MONGO
# =========================
client = MongoClient(

    os.getenv("MONGO_URI")
)

# =========================
# ✅ DATABASE
# =========================
db = client["AttendanceDB"]

# =========================
# 👨‍💼 ADMINS
# =========================
admins_collection = db["admins"]

# =========================
# 👨‍🎓 AUTHORIZED STUDENTS
# =========================
students_collection = db["students"]

# =========================
# ✅ REGISTERED STUDENTS
# =========================
registered_students_collection = db[
    "registered_students"
]

# =========================
# 🏫 CLASSES
# =========================
classes_collection = db["classes"]

# =========================
# 📊 ATTENDANCE
# =========================
attendance_collection = db["attendance"]

# =========================
# 📡 ATTENDANCE SESSIONS
# =========================
attendance_sessions_collection = db[
    "attendance_sessions"
]
# =========================
# 🧑‍🎓 STUDENT GROUPS
# =========================
student_groups_collection = db["student_groups"]

# =========================
# 📧 EMAIL VERIFICATIONS
# =========================
email_verifications_collection = db["email_verifications"]

# =========================
# 🔑 PASSWORD RESETS
# =========================
password_resets_collection = db["password_resets"]

# =========================
# ✅ INDEXES
# =========================

def ensure_students_indexes():

    indexes = students_collection.index_information()

    roll_index = indexes.get("roll_1")

    if (
        roll_index
        and roll_index.get("unique")
        and roll_index.get("key") == [("roll", 1)]
    ):

        try:

            students_collection.drop_index("roll_1")

        except OperationFailure:

            pass

    students_collection.create_index(

        [
            ("roll", ASCENDING),
            ("class_id", ASCENDING)
        ],

        unique=True,

        name="roll_class_id_unique"
    )


ensure_students_indexes()

registered_students_collection.create_index(

    "email",

    unique=True,

    sparse=True
)

registered_students_collection.create_index(

    "roll",

    unique=True
)

# =========================
# 🧹 CLEAN UP BAD ADMIN INDEXES
# =========================
def cleanup_admin_indexes():

    try:

        existing = admins_collection.index_information()

        # Drop the wrong (roll, class_id) index if it still exists
        if "roll_1_class_id_1" in existing:

            admins_collection.drop_index("roll_1_class_id_1")

            print("[DB] Dropped stale admin index: roll_1_class_id_1")

    except OperationFailure:

        pass


cleanup_admin_indexes()

# =========================
# ✅ ADMIN: only email must be unique
# =========================
admins_collection.create_index(
    "email",
    unique=True
)

print("✅ MongoDB Connected")
