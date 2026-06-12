import urllib.request
import urllib.error
import json

req = urllib.request.Request("http://127.0.0.1:8000/api/attendance/manual_update", 
    data=json.dumps({
        "class_id": "test_class_id",
        "students": []
    }).encode('utf-8'),
    headers={"Content-Type": "application/json"}
)

try:
    with urllib.request.urlopen(req) as res:
        print("Status:", res.status)
        print("Body:", res.read().decode('utf-8'))
except urllib.error.HTTPError as e:
    print("Error:", e.code, e.reason)
    print("Error body:", e.read().decode('utf-8'))
