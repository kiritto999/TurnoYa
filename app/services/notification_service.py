from sqlalchemy.orm import Session
from fastapi import HTTPException
from sqlalchemy import select
from app.models import EmpleadoNegocio, Negocio, Notificacion, Usuario
from app.repositories.notification_repository import NotificationRepository

TURN_MESSAGES = {
    "llamado": "¡Es tu turno! Acércate al punto de atención",
    "atendiendo": "Te están atendiendo ahora",
    "atendido": "Tu turno fue atendido. ¡Gracias por usar TurnoYa!",
    "cancelado": "Tu turno fue cancelado",
    "ausente": "Tu turno se marcó como ausente",
    "pendiente": "Tu turno volvió a la fila de espera",
}


class NotificationService:
    def __init__(self, db: Session):
        self.db = db
        self.repo = NotificationRepository(db)

    def create(self, user_id: int, tipo: str, mensaje: str, turn_id: int | None = None, business_id: int | None = None):
        n = Notificacion(id_usuario=user_id, id_turno=turn_id, id_negocio=business_id, tipo=tipo, mensaje=mensaje[:255], leido=False)
        self.db.add(n)
        return n

    def list(self, user_id: int):
        return self.repo.list_for_user(user_id)

    def mark_read(self, notification_id: int, user: Usuario):
        n = self.repo.get(notification_id)
        if not n or n.id_usuario != user.id_usuario:
            raise HTTPException(status_code=404, detail="Notificación no encontrada")
        n.leido = True
        self.db.commit()
        self.db.refresh(n)
        return n

    def mark_all_read(self, user: Usuario):
        for n in self.repo.list_for_user(user.id_usuario):
            n.leido = True
        self.db.commit()

    def notify_new_turn(self, business: Negocio, turn_id: int, turn_number: int):
        ids = {business.id_dueno}
        employees = self.db.scalars(select(EmpleadoNegocio).where(
            EmpleadoNegocio.id_negocio == business.id_negocio,
            EmpleadoNegocio.estado_invitacion == "aceptada",
        ))
        ids.update(e.id_usuario for e in employees)
        for uid in ids:
            self.create(uid, "nuevo_turno", f"{business.nombre}: nuevo turno #{turn_number} en la fila", turn_id, business.id_negocio)

    def notify_turn_change(self, client_id: int, business_id: int, turn_id: int, estado: str, numero: int | None = None):
        business = self.db.get(Negocio, business_id)
        prefix = f"{business.nombre} · Turno #{numero}: " if business and numero else ""
        self.create(client_id, "cambio_turno", prefix + TURN_MESSAGES.get(estado, f"Estado: {estado}"), turn_id, business_id)

    def notify_invitation(self, user_id: int, business: Negocio):
        self.create(user_id, "invitacion", f"{business.nombre} te invitó a unirte como empleado", None, business.id_negocio)
