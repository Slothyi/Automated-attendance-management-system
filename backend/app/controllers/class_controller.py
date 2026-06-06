from bson import ObjectId

from datetime import datetime, timedelta, timezone

from pymongo.errors import DuplicateKeyError

from app.config.db import (
    classes_collection,
    students_collection,
    registered_students_collection,
    student_groups_collection
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
    # ✅ CREATE CLASS OBJECT
    # =========================
    now = datetime.now(timezone.utc)

    new_class = {

        "class_name": class_name,

        "section": section,

        "department": department,

        "year": year,

        "semester": semester,

        "created_by": admin_id,

        "students": [],

        "created_at": now,

        "created_date": now.strftime("%Y-%m-%d"),

        "expires_at": now + timedelta(hours=12)
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

    print("================================")
    print("CLASS ID RECEIVED:", repr(class_id))
    print("================================")

    class_id = str(class_id or "").strip()

    if not class_id:

        print("❌ CLASS ID IS EMPTY")

        return {
            "message": "Class ID is empty. Select a class first",
            "error": "Class ID is empty"
        }

    if not ObjectId.is_valid(class_id):

        return {
            "message": "Invalid class selected. Select the class again",
            "error": "Invalid class ID"
        }

    # =========================
    # ✅ FIND CLASS
    # =========================
    existing_class = classes_collection.find_one({

        "_id": ObjectId(class_id)

    })

    if not existing_class:

        return {

            "message": "Class not found. Select the class again",
            "error": "Class not found"
        }

    # =========================
    # STUDENT LIST
    # =========================
    student_list = []

    skipped_duplicates = 0

    skipped_invalid = 0

    existing_class_rolls = {

        str(student.get("roll", "")).strip()

        for student in existing_class.get("students", [])
    }

    # =========================
    # ✅ LOOP STUDENTS
    # =========================
    for student in students:

        student_name = student.name.strip()

        student_roll = student.roll.strip()

        if not student_name or not student_roll:

            skipped_invalid += 1

            continue

        if student_roll in existing_class_rolls:

            skipped_duplicates += 1

            continue

        # =========================
        # ✅ DUPLICATE CHECK
        # =========================
        existing_student = students_collection.find_one({

            "roll": student_roll,

            "class_id": class_id
        })

        if existing_student:

            skipped_duplicates += 1

            existing_class_rolls.add(student_roll)

            continue

        # =========================
        # ✅ STUDENT OBJECT
        # =========================
        student_data = {

            "name": student_name,

            "roll": student_roll,

            "attendance_status": "N/A"
        }

        # =========================
        # ✅ SAVE IN STUDENTS COLLECTION
        # =========================
        student_saved = False

        try:

            students_collection.insert_one({

                "name": student_name,

                "roll": student_roll,

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

            student_saved = True

        except DuplicateKeyError:

            existing_student = students_collection.find_one({

                "roll": student_roll,

                "class_id": class_id
            })

            if not existing_student:

                raise

            skipped_duplicates += 1

            existing_class_rolls.add(student_roll)

        if not student_saved:

            continue

        student_list.append(student_data)

        existing_class_rolls.add(student_roll)

    # =========================
    # ✅ UPDATE CLASS COLLECTION
    # =========================
    if student_list:

        classes_collection.update_one(

            {
                "_id": ObjectId(class_id)
            },

            {
                "$addToSet": {

                    "students": {

                        "$each": student_list
                    }
                }
            }
        )

        # =========================
        # ✅ LOAD ALL STUDENTS OF CLASS
        # =========================
        all_students = list(
            students_collection.find({

                "class_id": class_id
            })
        )

        group_students = []

        for s in all_students:

            group_students.append({

                "name": s["name"],

                "roll": s["roll"]
            })

        # =========================
        # ✅ CREATE / UPDATE GROUP
        # =========================
        group_name = (
            f"{existing_class['class_name']} | "
            f"{existing_class['department']} | "
            f"(Y){existing_class['year']} | "
            f"(S){existing_class['semester']} | "
            f"{existing_class['section']}"
        )


        print("GROUP CREATED:", group_name)
        print("TOTAL STUDENTS:", len(group_students))

        create_student_group(
            group_name,
            group_students
        )

    added_count = len(student_list)

    if added_count == 0:

        if skipped_duplicates:

            if skipped_duplicates == 1:

                message = "Student already present in this class"

            else:

                message = (
                    "No new students added. Selected students are already "
                    "present in this class"
                )

        elif skipped_invalid:

            message = "No valid students to add"

        else:

            message = "No students to add"

    elif skipped_duplicates:

        message = (
            f"Added {added_count} student"
            f"{'' if added_count == 1 else 's'}. "
            f"Skipped {skipped_duplicates} already present"
        )

    else:

        message = (
            "Student added successfully"
            if added_count == 1
            else "Students added successfully"
        )

    return {

        "message":
            message
    }

def create_student_group(
    group_name,
    students
):

    student_groups_collection.update_one(

        {
            "group_name": group_name
        },

        {
            "$set": {

                "group_name": group_name,

                "students": students
            }
        },

        upsert=True
    )

    print("GROUP SAVED TO DATABASE:", group_name)

# =========================
# 📚 GET STUDENT GROUPS
# =========================
def get_student_groups():
    groups = []
    for group in student_groups_collection.find():
        # Ignore empty groups
        if len(group.get("students", [])) == 0:
            continue
        groups.append({
            "group_id": str(group["_id"]),
            "group_name": group["group_name"]
        })
    return {
        "groups": groups
    }

# =========================
# 👨‍🎓 GET GROUP STUDENTS
# =========================
def get_student_group(group_name):
    group = student_groups_collection.find_one({"group_name": group_name})
    if not group:
        return {"error": "Group not found"}
    return {"students": group.get("students", [])}

# =========================
# 📋 GET ALL CLASSES
# =========================
def get_all_classes(admin_id: str = None):
    query = {"expires_at": {"$gt": datetime.now(timezone.utc)}}
    if admin_id:
        query["created_by"] = admin_id
    classes = classes_collection.find(query)
    class_list = []
    for c in classes:
        class_students = list(students_collection.find({"class_id": str(c["_id"])}))
        present_count = sum(1 for s in class_students if s.get("attendance_status") == "Present")
        class_list.append({
            "class_id": str(c["_id"]),
            "class_name": c.get("class_name", ""),
            "section": c.get("section", ""),
            "department": c.get("department", ""),
            "year": c.get("year", ""),
            "semester": c.get("semester", ""),
            "student_count": len(class_students),
            "present_count": present_count,
            "created_at": c.get("created_at"),
            "expires_at": c.get("expires_at")
        })
    return {"classes": class_list}

# =========================
# 📅 GET CLASS CALENDAR
# =========================
def get_class_calendar():
    classes = list(classes_collection.find())
    result = []
    for c in classes:
        created_at = c.get("created_at")
        result.append({
            "class_id": str(c["_id"]),
            "class_name": c.get("class_name", ""),
            "created_at": created_at.strftime("%Y-%m-%d") if created_at else ""
        })
    return {"classes": result}

# =========================
# 👨‍🎓 GET CLASS STUDENTS
# =========================
def get_class_students(class_id):
    # =========================
    # ✅ FIND CLASS
    # =========================
    existing_class = classes_collection.find_one({"_id": ObjectId(class_id)})

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
    normalized_name = str(name).strip()
    normalized_roll = str(roll).strip()
    
    existing_student = students_collection.find_one({

        "name": {

            "$regex":
                f"^{normalized_name}$",

            "$options": "i"
        },

        "roll": normalized_roll
    })

    return existing_student is not None
