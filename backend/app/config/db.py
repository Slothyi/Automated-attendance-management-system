from pymongo import MongoClient
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
# ✅ INDEXES
# =========================

registered_students_collection.create_index(

    "email",

    unique=True,

    sparse=True
)

registered_students_collection.create_index(

    "roll",

    unique=True
)

admins_collection.create_index(

    "email",

    unique=True
)

print("✅ MongoDB Connected")