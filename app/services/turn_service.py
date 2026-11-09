from datetime import datetime
from fastapi import HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session
from app.models import CatalogoItem, Turno, Usuario
from app.repositories.turno_repository import ACTIVE_STATES, TurnRepository
from app.services.business_service import BusinessService
from app.services.notification_service import NotificationService


class TurnService:
    def __init__(self, db: Session):
        self.db = db
        self.repo = TurnRepository(db)
        self.business = BusinessService(db)
        self.notifications = NotificationService(db)

    def create(self, business_id: int, client: Usuario, data):
        business = self.business.ensure_exists(business_id)
        if business.estado != "activo":
            raise HTTPException(status_code=409, detail="El negocio no está disponible")
        if data.id_sector is not None:
            self.business.ensure_sector(business_id, data.id_sector)
        if data.id_item is not None:
            item = self.db.get(CatalogoItem, data.id_item)
            if not item or item.id_negocio != business_id or item.estado != "activo":
                raise HTTPException(status_code=400, detail="Ítem de catálogo inválido")
        existing = self.db.scalar(select(Turno).where(
            Turno.id_cliente == client.id_usuario,
            Turno.id_negocio == business_id,
            Turno.estado.in_(ACTIVE_STATES),
        ))
        if existing:
            raise HTTPException(status_code=409, detail=f"Ya tienes un turno activo (#{existing.numero_turno}) en este negocio")
        number = self.repo.next_number(business_id, data.id_sector)
        try:
            turn = Turno(
                id_cliente=client.id_usuario,
                id_negocio=business_id,
                id_sector=data.id_sector,
                id_item=data.id_item,
                numero_turno=number,
                motivo=data.motivo,
                estado="pendiente",
                fecha_solicitud=datetime.utcnow(),
            )
            self.db.add(turn)
            self.db.flush()
            self.notifications.notify_new_turn(business, turn.id_turno, turn.numero_turno)
            self.db.commit()
            self.db.refresh(turn)
        except Exception:
            self.db.rollback()
            raise
        finally:
            self.repo.release_lock()
        return self.repo.with_position(turn)

    def get(self, turn_id: int, user: Usuario):
        turn = self.db.get(Turno, turn_id)
        if not turn:
            raise HTTPException(status_code=404, detail="Turno no encontrado")
        allowed = turn.id_cliente == user.id_usuario or user.rol == "superadmin"
        if not allowed and user.rol in ("dueño", "empleado"):
            self.business.ensure_staff(turn.id_negocio, user)
            allowed = True
        if not allowed:
            raise HTTPException(status_code=403, detail="No tiene acceso a este turno")
        return turn

    def get_detailed(self, turn_id: int, user: Usuario):
        return self.repo.with_position(self.get(turn_id, user))

    def queue(self, business_id: int, user: Usuario, sector_id: int | None):
        if user.rol == "cliente":
            self.business.ensure_exists(business_id)
        else:
            self.business.ensure_staff(business_id, user)
        turns = self.repo.queue(business_id, sector_id)
        if user.rol == "cliente":
            # Un cliente ve la fila, pero sin datos personales de otros clientes
            for t in turns:
                if t.id_cliente != user.id_usuario:
                    t.nombre_cliente = None
                    t.motivo = None
        return turns

    def my_turns(self, user_id: int):
        return self.repo.customer_turns(user_id)

    def change_status(self, turn_id: int, user: Usuario, estado: str):
        turn = self.get(turn_id, user)
        if turn.id_cliente == user.id_usuario and user.rol == "cliente":
            if estado != "cancelado":
                raise HTTPException(status_code=403, detail="El cliente solo puede cancelar su propio turno")
            if turn.estado not in ("pendiente", "llamado"):
                raise HTTPException(status_code=409, detail="Este turno ya no se puede cancelar")
        turn.estado = estado
        if estado == "atendido":
            turn.fecha_atencion = datetime.utcnow()
        self.notifications.notify_turn_change(turn.id_cliente, turn.id_negocio, turn.id_turno, estado, turn.numero_turno)
        self.db.commit()
        self.db.refresh(turn)
        return self.repo.with_position(turn)

    def call_next(self, business_id: int, user: Usuario, sector_id: int | None):
        """Marca como 'llamado' el siguiente turno pendiente (por orden de llegada)."""
        self.business.ensure_staff(business_id, user)
        stmt = select(Turno).where(Turno.id_negocio == business_id, Turno.estado == "pendiente")
        if sector_id is not None:
            stmt = stmt.where(Turno.id_sector == sector_id)
        turn = self.db.scalar(stmt.order_by(Turno.fecha_solicitud, Turno.id_turno).limit(1))
        if not turn:
            raise HTTPException(status_code=404, detail="No hay turnos en espera")
        return self.change_status(turn.id_turno, user, "llamado")
