from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.core.security import get_current_user
from app.models import Usuario
from app.schemas.notification import NotificationResponse
from app.services.notification_service import NotificationService

router = APIRouter(prefix="/notifications", tags=["Notificaciones"])


@router.get("", response_model=list[NotificationResponse])
def notifications(db: Session = Depends(get_db), user: Usuario = Depends(get_current_user)):
    return NotificationService(db).list(user.id_usuario)


@router.get("/unread-count")
def unread_count(db: Session = Depends(get_db), user: Usuario = Depends(get_current_user)):
    return {"no_leidas": sum(1 for n in NotificationService(db).list(user.id_usuario) if not n.leido)}


@router.post("/read-all", status_code=204)
def mark_all_read(db: Session = Depends(get_db), user: Usuario = Depends(get_current_user)):
    NotificationService(db).mark_all_read(user)


@router.patch("/{notification_id}/read", response_model=NotificationResponse)
def mark_read(notification_id: int, db: Session = Depends(get_db), user: Usuario = Depends(get_current_user)):
    return NotificationService(db).mark_read(notification_id, user)
