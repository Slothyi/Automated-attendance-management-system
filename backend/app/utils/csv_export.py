import csv
import os
import re

# =========================
# 📁 OUTPUT DIRECTORY
# =========================
CSV_DIR = "attendance_csv"


# =========================
# 🧹 SANITIZE CLASS NAME → SAFE FILENAME
# Remove / \ : * ? " < > | and collapse spaces
# =========================
def _safe_filename(class_name: str) -> str:

    # Strip OS-reserved characters
    name = re.sub(r'[\\/:*?"<>|]', "", class_name)

    # Collapse multiple spaces / underscores
    name = re.sub(r"\s+", "_", name.strip())

    return name or "unknown_class"


# =========================
# 📝 APPEND ROW TO CLASS CSV
# Called immediately after a successful attendance insert.
# Thread-safe at the Python level (GIL + single process).
# =========================
CSV_HEADERS = [
    "name",
    "id",
    "roll",
    "class_name",
    "date",
    "time",
    "section",
    "department",
    "status",
]


def append_attendance_csv(
    *,
    student_id: str,
    name: str,
    roll: str,
    class_name: str,
    date: str,          # "YYYY-MM-DD"
    time: str,          # "HH:MM:SS"
    section: str,
    department: str,
):
    """
    Appends or updates one attendance record in attendance_csv/<class_name>.csv.
    If an 'Absent' row exists for this student today, it updates it to 'Present'.
    Otherwise, it appends a new 'Present' row.
    """
    os.makedirs(CSV_DIR, exist_ok=True)
    safe_name = _safe_filename(class_name or "unknown_class")
    file_path = os.path.join(CSV_DIR, f"{safe_name}.csv")

    file_exists = os.path.isfile(file_path)
    updated = False
    rows = []

    if file_exists:
        try:
            with open(file_path, mode="r", newline="", encoding="utf-8") as f:
                reader = csv.DictReader(f)
                for row in reader:
                    if row.get("roll") == roll and row.get("date") == date and row.get("status") == "Absent":
                        row["status"] = "Present"
                        row["time"] = time
                        if row.get("id") == "N/A" or not row.get("id"):
                            row["id"] = student_id
                        updated = True
                    rows.append(row)
        except Exception as e:
            print(f"⚠️ Error reading CSV for update: {e}")

    if updated:
        try:
            with open(file_path, mode="w", newline="", encoding="utf-8") as f:
                writer = csv.DictWriter(f, fieldnames=CSV_HEADERS)
                writer.writeheader()
                writer.writerows(rows)
            print(f"📄 CSV updated (Absent -> Present): {file_path}")
            return
        except Exception as e:
            print(f"⚠️ Error writing CSV update: {e}")

    # Fallback / Default: Append row
    with open(file_path, mode="a", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=CSV_HEADERS)
        if not file_exists:
            writer.writeheader()

        writer.writerow({
            "name":       name,
            "id":         student_id,
            "roll":       roll,
            "class_name": class_name or "",
            "date":       date,
            "time":       time,
            "section":    section,
            "department": department,
            "status":     "Present",
        })
    print(f"📄 CSV appended (New Present): {file_path}")


def initialize_class_csv(
    *,
    class_name: str,
    students: list,
    date: str,
    section: str,
    department: str,
):
    """
    Initializes all students of a class as 'Absent' in the CSV for a new session.
    Skips students who already have an entry for today.
    """
    os.makedirs(CSV_DIR, exist_ok=True)
    safe_name = _safe_filename(class_name or "unknown_class")
    file_path = os.path.join(CSV_DIR, f"{safe_name}.csv")

    file_exists = os.path.isfile(file_path)
    existing_rolls = set()

    if file_exists:
        try:
            with open(file_path, mode="r", newline="", encoding="utf-8") as f:
                reader = csv.DictReader(f)
                for row in reader:
                    if row.get("date") == date:
                        existing_rolls.add(row.get("roll"))
        except Exception as e:
            print(f"⚠️ Error checking duplicates in CSV: {e}")

    from app.config.db import registered_students_collection

    with open(file_path, mode="a", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=CSV_HEADERS)
        if not file_exists:
            writer.writeheader()

        for student in students:
            roll = student.get("roll")
            if not roll or roll in existing_rolls:
                continue

            reg_student = registered_students_collection.find_one({"roll": roll})
            student_id = str(reg_student["_id"]) if reg_student else "N/A"

            writer.writerow({
                "name":       student.get("name"),
                "id":         student_id,
                "roll":       roll,
                "class_name": class_name or "",
                "date":       date,
                "time":       "N/A",
                "section":    section,
                "department": department,
                "status":     "Absent",
            })
    print(f"📄 CSV initialized (Absent list): {file_path}")
