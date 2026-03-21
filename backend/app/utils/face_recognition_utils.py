import face_recognition
import numpy as np
import cv2


# =========================
# 📸 GET FACE ENCODING (IMPROVED)
# =========================
def get_face_encoding(image_path):
    image = cv2.imread(image_path)

    if image is None:
        return None

    # 🔄 Resize (VERY IMPORTANT for detection)
    image = cv2.resize(image, (0, 0), fx=0.5, fy=0.5)

    # 🔄 Convert to RGB
    rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)

    # 🔍 Detect face
    face_locations = face_recognition.face_locations(rgb, model="hog")

    if len(face_locations) == 0:
        print("❌ No face detected")
        return None

    if len(face_locations) > 1:
        print("⚠️ Multiple faces detected")
        return None

    encodings = face_recognition.face_encodings(rgb, face_locations)

    return encodings[0]
# =========================
# 🤖 MULTI-SAMPLE ENCODING (NEW)
# =========================
def get_average_encoding(image_paths):
    encodings = []

    for path in image_paths:
        enc = get_face_encoding(path)
        if enc is not None:
            encodings.append(enc)

    if len(encodings) == 0:
        return None

    return np.mean(encodings, axis=0)


# =========================
# 🤖 FACE MATCH (IMPROVED)
# =========================
def compare_faces(known_encoding, unknown_encoding, threshold=0.5):
    distance = np.linalg.norm(known_encoding - unknown_encoding)

    print("🔍 FACE DISTANCE:", distance)

    return distance < threshold