from fastapi import APIRouter
from pydantic import BaseModel
from typing import List

from app.controllers.class_controller import (

    create_class,

    add_student_to_class,

    get_all_classes,

    get_class_students
)

# =========================
# ✅ ROUTER
# =========================
router = APIRouter()


# =========================
# 🏫 CREATE CLASS MODEL
# =========================
class CreateClassRequest(BaseModel):

    class_name: str

    section: str

    department: str

    year: str

    semester: str 
    
    admin_id: str


# =========================
# 👨‍🎓 STUDENT MODEL
# =========================
class StudentData(BaseModel):

    name: str

    roll: str


# =========================
# 👨‍🎓 ADD STUDENTS MODEL
# =========================
class AddStudentRequest(BaseModel):

    class_id: str

    students: List[StudentData]


# =========================
# 🏫 CREATE CLASS
# =========================
@router.post("/create")
def create_new_class(data: CreateClassRequest):

    return create_class(

        class_name=data.class_name,

        section=data.section,

        department=data.department,

        year=data.year,
        
        semester=data.semester,

        admin_id=data.admin_id
    )


# =========================
# 👨‍🎓 ADD STUDENTS
# =========================
@router.post("/add-students")
def add_students(data: AddStudentRequest):

    return add_student_to_class(

        class_id=data.class_id,

        students=data.students
    )


# =========================
# 📋 GET ALL CLASSES
# =========================
@router.get("/all")
def get_classes():

    return get_all_classes()


# =========================
# 👨‍🎓 GET CLASS STUDENTS
# =========================
@router.get("/students/{class_id}")
def get_students(class_id: str):

    return get_class_students(class_id)