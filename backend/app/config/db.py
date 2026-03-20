from pymongo import MongoClient
import os
from dotenv import load_dotenv

load_dotenv()

client = MongoClient(os.getenv("MONGO_URI"))
db = client["attendance_ai"]

users_collection = db["users"]
attendance_collection = db["attendance"]