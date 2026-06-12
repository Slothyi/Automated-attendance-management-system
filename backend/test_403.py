import urllib.request
import json

# login as admin
req = urllib.request.Request("http://127.0.0.1:8000/api/admin/login", 
    data=json.dumps({
        "name": "Admin Name",
        "email": "admin@example.com",
        "password": "password"
    }).encode('utf-8'),
    headers={"Content-Type": "application/json"}
)

try:
    with urllib.request.urlopen(req) as res:
        print("Login status:", res.status)
        data = json.loads(res.read().decode('utf-8'))
        token = data.get("token")
        print("Token:", token)
        
        # call manual update
        req2 = urllib.request.Request("http://127.0.0.1:8000/api/attendance/manual_update", 
            data=json.dumps({
                "class_id": "test_class_id",
                "students": [
                    {
                        "name": "Student A",
                        "roll": "1",
                        "attendance_status": "Present"
                    }
                ]
            }).encode('utf-8'),
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {token}"
            }
        )
        
        try:
            with urllib.request.urlopen(req2) as res2:
                print("Update status:", res2.status)
                print("Update text:", res2.read().decode('utf-8'))
        except urllib.error.HTTPError as e:
            print("Update error:", e.code, e.reason)
            print("Update error body:", e.read().decode('utf-8'))

except urllib.error.HTTPError as e:
    print("Login error:", e.code, e.reason)
    print("Login error body:", e.read().decode('utf-8'))
