from bson import ObjectId

from datetime import datetime, timedelta, timezone

from pymongo.errors import DuplicateKeyError

from app.config.db import (
    classes_collection,
    students_collection,
    registered_students_collection,
    student_groups_collection,
    attendance_collection,
    attendance_sessions_collection
)


# =========================
# 🏫 CREATE CLASS
# =========================
def create_class(
    course_name,
    course_code,
    semester,
    section,
    year,
    academic_session,
    department,
    admin_id
):

    # =========================
    # ✅ CREATE CLASS OBJECT
    # =========================
    now = datetime.now(timezone.utc)

    new_class = {
        "class_name": course_name, # keeping key as class_name in DB to avoid massive refactoring, or better update the key to course_name? 
        "course_name": course_name,
        "course_code": course_code,
        "semester": semester,
        "section": section,
        "year": year,
        "academic_session": academic_session,
        "department": department,

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
    # Classes no longer expire
    query = {}
    if admin_id:
        query["created_by"] = admin_id
    classes = classes_collection.find(query)
    class_list = []
    
    # Get today's date in IST
    ist_time = datetime.now(timezone.utc) + timedelta(hours=5, minutes=30)
    today_str = ist_time.strftime("%Y-%m-%d")
    
    for c in classes:
        class_id_str = str(c["_id"])
        class_students = list(students_collection.find({"class_id": class_id_str}))
        
        # Calculate present count dynamically based on today's attendance records
        present_count = attendance_collection.count_documents({
            "class_id": class_id_str,
            "date": today_str,
            "status": "Present"
        })
        
        class_list.append({
            "class_id": class_id_str,
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
    """
    Returns calendar entries based on actual attendance sessions.
    A class only appears on a date if attendance was actually taken that day.
    """
    # Get all attendance sessions grouped by class_id + date
    sessions = list(attendance_sessions_collection.find({}))

    # Build a set of (class_id, date) pairs that had real attendance
    seen = set()
    result = []

    for session in sessions:
        class_id_str = str(session.get("class_id", ""))
        # Use the IST date stored on the session, fall back to created_at
        session_date = session.get("date")
        if not session_date:
            created_at = session.get("created_at")
            if created_at:
                ist_time = created_at + timedelta(hours=5, minutes=30)
                session_date = ist_time.strftime("%Y-%m-%d")
            else:
                continue

        key = (class_id_str, session_date)
        if key in seen:
            continue
        seen.add(key)

        # Fetch class metadata
        try:
            class_doc = classes_collection.find_one({"_id": ObjectId(class_id_str)})
        except Exception:
            continue

        if not class_doc:
            continue

        result.append({
            "class_id": class_id_str,
            "class_name": class_doc.get("class_name", ""),
            "created_at": session_date  # used by Android to filter by date
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

    # Find active session or most recent session of this class
    active_session = attendance_sessions_collection.find_one({
        "class_id": class_id,
        "active": True
    })
    if not active_session:
        active_session = attendance_sessions_collection.find_one(
            {"class_id": class_id},
            sort=[("created_at", -1)]
        )
    
    session_uuid = active_session.get("session_uuid") if active_session else None

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
            if session_uuid:
                attendance_record = attendance_collection.find_one({
                    "class_id": class_id,
                    "roll": student["roll"],
                    "session_uuid": session_uuid
                })
            else:
                ist_time = datetime.now(timezone.utc) + timedelta(hours=5, minutes=30)
                today_str = ist_time.strftime("%Y-%m-%d")
                attendance_record = attendance_collection.find_one({
                    "class_id": class_id,
                    "roll": student["roll"],
                    "date": today_str
                })
            
            if attendance_record:
                status = attendance_record.get("status", "Absent")
            else:
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

    # =========================
    # 🔢 SORT BY ROLL (ASCENDING)
    # =========================
    import re
    def roll_sort_key(s):
        r = str(s.get("roll", "")).strip()
        nums = re.findall(r'\d+', r)
        return (int(nums[0]) if nums else 0, r)

    student_list.sort(key=roll_sort_key)

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

# =========================
# 🗑 DELETE CLASS
# =========================
def delete_class(class_id: str):
    try:
        from bson.errors import InvalidId
        try:
            obj_id = ObjectId(class_id)
        except InvalidId:
            return {"success": False, "error": "Invalid Class ID"}
            
        result = classes_collection.delete_one({"_id": obj_id})
        if result.deleted_count > 0:
            students_collection.delete_many({"class_id": class_id})
            # Also purge historical attendance so it doesn't ghost in student views
            attendance_collection.delete_many({"class_id": class_id})
            return {"success": True, "message": "Class deleted successfully"}
        else:
            return {"success": False, "error": "Class not found"}
    except Exception as e:
        return {"success": False, "error": str(e)}
