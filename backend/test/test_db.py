from pymongo import MongoClient
import os
from dotenv import load_dotenv

load_dotenv()

client = MongoClient(os.getenv("MONGO_URI"))

try:
    print("Connected:", client.list_database_names())
except Exception as e:
    print("Error:", e)