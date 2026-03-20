from datetime import datetime, timedelta, timezone

def can_mark_attendance(last_time):
    if last_time is None:
        return True

    # 🔥 FIX: convert naive → aware
    if last_time.tzinfo is None:
        last_time = last_time.replace(tzinfo=timezone.utc)

    now = datetime.now(timezone.utc)

    return now >= last_time + timedelta(hours=19)