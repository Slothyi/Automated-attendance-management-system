# import face_recognition
# import numpy as np

# def get_face_encoding(image_path):
#     image = face_recognition.load_image_file(image_path)
#     encodings = face_recognition.face_encodings(image)

#     if len(encodings) == 0:
#         return None

#     return encodings[0]


# def compare_faces(known_encoding, unknown_encoding):
#     results = face_recognition.compare_faces(
#         [known_encoding],
#         unknown_encoding,
#         tolerance=0.5
#     )

#     return results[0]

import face_recognition
import cv2
import numpy as np

def get_face_encoding(image_path):
    # ✅ Read image using OpenCV
    image = cv2.imread(image_path)

    if image is None:
        return None

    # ✅ Convert BGR → RGB
    rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)

    # ✅ Resize (VERY IMPORTANT)
    small = cv2.resize(rgb, (0, 0), fx=0.5, fy=0.5)

    # ✅ Detect face locations
    face_locations = face_recognition.face_locations(small)

    if len(face_locations) == 0:
        return None

    # ✅ Get encodings
    encodings = face_recognition.face_encodings(small, face_locations)

    return encodings[0]


def compare_faces(known_encoding, unknown_encoding):
    results = face_recognition.compare_faces(
        [known_encoding],
        unknown_encoding,
        tolerance=0.6   # 🔥 relaxed (important)
    )

    return results[0]