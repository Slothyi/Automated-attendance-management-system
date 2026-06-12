import os
import sys

# Add the app directory to sys.path so we can import from app
sys.path.append(os.path.join(os.path.dirname(__file__), 'backend'))

from app.config.db import attendance_collection, classes_collection
from bson import ObjectId

def fix_unknown_classes():
    # Find all attendance records missing class_name
    bad_records = list(attendance_collection.find({"class_name": {"$exists": False}}))
    print(f"Found {len(bad_records)} records missing class_name")
    
    updated_count = 0
    for record in bad_records:
        class_id = record.get("class_id")
        if class_id:
            class_doc = classes_collection.find_one({"_id": ObjectId(class_id)})
            if class_doc:
                class_name = class_doc.get("class_name", "Unknown Class")
                attendance_collection.update_one(
                    {"_id": record["_id"]},
                    {"$set": {"class_name": class_name}}
                )
                updated_count += 1
                
    print(f"Successfully updated {updated_count} records.")

if __name__ == "__main__":
    fix_unknown_classes()
