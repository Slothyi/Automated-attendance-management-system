from fastapi import FastAPI
from app.routes import auth_routes, attendance_routes
from fastapi.security import HTTPBearer

app = FastAPI()

security = HTTPBearer()

app.include_router(auth_routes.router, prefix="/api/auth")
app.include_router(attendance_routes.router, prefix="/api/attendance")