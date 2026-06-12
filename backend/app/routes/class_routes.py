from fastapi import APIRouter, Query
from pydantic import BaseModel
from typing import List

from app.config.db import (
    
    student_groups_collection
)

from app.controllers.class_controller import (

    create_class,

    add_student_to_class,

    get_all_classes,

    get_class_students,
    
    get_student_groups,
    
    get_student_group,
    
    get_class_calendar
)

# =========================
# ✅ ROUTER
# =========================
router = APIRouter()


# =========================
# 🏫 CREATE CLASS MODEL
# =========================
class CreateClassRequest(BaseModel):
    course_name: str
    course_code: str
    semester: str
    section: str
    year: str
    academic_session: str
    department: str
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
        course_name=data.course_name,
        course_code=data.course_code,
        semester=data.semester,
        section=data.section,
        year=data.year,
        academic_session=data.academic_session,
        department=data.department,
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
def get_classes(admin_id: str = Query(default="")):

    return get_all_classes(
        admin_id=admin_id if admin_id else None
    )

# =========================
# 👨‍🎓 GET STUDENT GROUPS
# =========================
@router.get("/student-groups")
def student_groups():

    return get_student_groups()


# =========================
# 👨‍🎓 GET STUDENT GROUP
# =========================
@router.get("/student-group/{group_name}")
def student_group(group_name: str):

    return get_student_group(
        group_name
    )
    
# =========================
# 👨‍🎓 GET CLASS STUDENTS
# =========================
@router.get("/students/{class_id}")
def get_students(class_id: str):

    return get_class_students(class_id)

# =========================
# 📅 GET CLASS CALENDAR
# =========================
@router.get("/calendar")
def calendar():

    return get_class_calendar()

# =========================
# 🗑 DELETE CLASS
# =========================
@router.delete("/{class_id}")
def delete_class_endpoint(class_id: str):
    from app.controllers.class_controller import delete_class
    return delete_class(class_id)
