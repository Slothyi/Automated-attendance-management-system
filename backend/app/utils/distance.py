import math

def calculate_distance(lat1, lon1, lat2, lon2):
    R = 6371

    dLat = math.radians(lat2 - lat1)
    dLon = math.radians(lon2 - lon1)

    a = (math.sin(dLat/2)**2 +
         math.cos(math.radians(lat1)) *
         math.cos(math.radians(lat2)) *
         math.sin(dLon/2)**2)

    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))

    return R * c

def is_within_college(lat, lon):
    college_lat = 23.526515   # ⚠️ change this
    college_lon = 87.742507   # ⚠️ change this

    distance = calculate_distance(lat, lon, college_lat, college_lon)

    return distance <= 0.2   # 200 meters