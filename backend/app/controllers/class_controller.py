from bson import ObjectId

from app.config.db import (
    classes_collection,
    students_collection,
    registered_students_collection
)


# =========================
# 🏫 CREATE CLASS
# =========================
def create_class(
    class_name,
    section,
    department,
    year,
    semester,
    admin_id
):

    # =========================
    # ✅ CHECK DUPLICATE CLASS
    # =========================
    existing_class = classes_collection.find_one({

        "class_name": class_name,

        "section": section,

        "department": department,

        "year": year,

        "semester": semester
    })

    if existing_class:

        return {

            "error": "Class already exists"
        }

    # =========================
    # ✅ CREATE CLASS OBJECT
    # =========================
    new_class = {

        "class_name": class_name,

        "section": section,

        "department": department,

        "year": year,

        "semester": semester,

        "created_by": admin_id,

        "students": []
    }

    # =========================
    # ✅ INSERT CLASS
    # =========================
    result = classes_collection.insert_one(
        new_class
    )

    return {

        "message":
            "Class created successfully",

        "class_id":
            str(result.inserted_id)
    }


# =========================
# 👨‍🎓 ADD STUDENTS
# =========================
def add_student_to_class(
    class_id,
    students
):

    # =========================
    # ✅ FIND CLASS
    # =========================
    existing_class = classes_collection.find_one({

        "_id": ObjectId(class_id)
    })

    if not existing_class:

        return {

            "error": "Class not found"
        }

    student_list = []

    # =========================
    # ✅ LOOP STUDENTS
    # =========================
    for student in students:

        # =========================
        # ✅ DUPLICATE CHECK
        # =========================
        existing_student = students_collection.find_one({

            "roll": student.roll
        })

        if existing_student:

            continue

        # =========================
        # ✅ STUDENT OBJECT
        # =========================
        student_data = {

            "name": student.name,

            "roll": student.roll,

            "attendance_status": "N/A"
        }

        student_list.append(student_data)

        # =========================
        # ✅ SAVE IN STUDENTS COLLECTION
        # =========================
        students_collection.insert_one({

            "name": student.name,

            "roll": student.roll,

            "attendance_status": "N/A",

            "class_id": class_id,

            "class_name":
                existing_class["class_name"],

            "section":
                existing_class["section"],

            "department":
                existing_class["department"],

            "year":
                existing_class["year"],

            "semester":
                existing_class["semester"]
        })

    # =========================
    # ✅ UPDATE CLASS COLLECTION
    # =========================
    if student_list:

        classes_collection.update_one(

            {
                "_id": ObjectId(class_id)
            },

            {
                "$push": {

                    "students": {

                        "$each": student_list
                    }
                }
            }
        )

    return {

        "message":
            "Students added successfully"
    }


# =========================
# 📋 GET ALL CLASSES
# =========================
def get_all_classes():

    classes = classes_collection.find()

    class_list = []

    for c in classes:

        students = c.get(
            "students",
            []
        )

        present_count = 0

        for s in students:

            if s.get(
                "attendance_status"
            ) == "Present":

                present_count += 1

        class_data = {

            "class_id":
                str(c["_id"]),

            "class_name":
                c["class_name"],

            "section":
                c["section"],

            "department":
                c["department"],

            "year":
                c["year"],

            "semester":
                c["semester"],

            "student_count":
                len(students),

            "present_count":
                present_count
        }

        class_list.append(class_data)

    return {

        "classes":
            class_list
    }


# =========================
# 👨‍🎓 GET CLASS STUDENTS
# =========================
def get_class_students(class_id):

    # =========================
    # ✅ FIND CLASS
    # =========================
    existing_class = classes_collection.find_one({

        "_id": ObjectId(class_id)
    })

    if not existing_class:

        return {

            "error": "Class not found"
        }

    # =========================
    # ✅ GET STUDENTS
    # =========================
    students = students_collection.find({

        "class_id": class_id
    })

    student_list = []

    present_count = 0

    absent_count = 0

    na_count = 0

    # =========================
    # ✅ LOOP
    # =========================
    for student in students:

        # =========================
        # ✅ CHECK REGISTERED
        # =========================
        registered = (

            registered_students_collection.find_one({

                "roll": student["roll"]
            })
        )

        # =========================
        # ✅ STATUS
        # =========================
        if registered:

            status = student.get(

                "attendance_status",

                "Absent"
            )

            # convert old N/A users
            if status == "N/A":

                status = "Absent"

        else:

            status = "N/A"

        # =========================
        # ✅ COUNTS
        # =========================
        if status == "Present":

            present_count += 1

        elif status == "Absent":

            absent_count += 1

        else:

            na_count += 1

        # =========================
        # ✅ APPEND
        # =========================
        student_list.append({

            "name":
                student["name"],

            "roll":
                student["roll"],

            "attendance_status":
                status
        })

    return {

        "class_name":
            existing_class["class_name"],

        "class_id":
            class_id,

        "section":
            existing_class["section"],

        "department":
            existing_class["department"],

        "year":
            existing_class["year"],

        "semester":
            existing_class["semester"],

        "present_students":
            present_count,

        "absent_students":
            absent_count,

        "na_students":
            na_count,

        "students":
            student_list
    }


# =========================
# 🔍 CHECK STUDENT EXISTS
# =========================
def student_exists(
    name,
    roll
):
    existing_student = students_collection.find_one({

        "name": {

            "$regex":
                f"^{name}$",

            "$options": "i"
        },

        "roll": roll
    })

    return existing_student is not None