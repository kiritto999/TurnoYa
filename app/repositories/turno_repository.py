from sqlalchemy import func, select, text
from sqlalchemy.orm import Session
from app.models import Turno, Negocio, Sector, CatalogoItem

class TurnRepository:
    def __init__(self, db: Session):
        self.db = db

    def next_number(self, business_id: int, sector_id: int | None) -> int:
        lock_name = f"turnoya:turno:{business_id}:{sector_id or 0}"
        locked = self.db.scalar(text("SELECT GET_LOCK(:lock_name, 5)"), {"lock_name": lock_name})
        if locked != 1:
            raise TimeoutError("No se pudo adquirir el bloqueo para generar el número de turno")
        self._lock_name = lock_name
        current = self.db.scalar(
            select(func.coalesce(func.max(Turno.numero_turno), 0)).where(
                Turno.id_negocio == business_id,
                Turno.id_sector == sector_id,
            )
        ) or 0
        return int(current) + 1

    def release_lock(self):
        if getattr(self, "_lock_name", None):
            self.db.execute(text("SELECT RELEASE_LOCK(:lock_name)"), {"lock_name": self._lock_name})
            self._lock_name = None

    def get(self, turn_id: int):
        return self.db.get(Turno, turn_id)

    def queue(self, business_id: int, sector_id: int | None = None):
        stmt = select(Turno).where(
            Turno.id_negocio == business_id,
            Turno.estado.in_(["pendiente", "llamado", "atendiendo"]),
        )
        if sector_id is not None:
            stmt = stmt.where(Turno.id_sector == sector_id)
        return list(self.db.scalars(stmt.order_by(Turno.numero_turno)))

    def customer_turns(self, user_id: int):
        stmt = (
            select(
                Turno,
                Negocio.nombre.label("nombre_negocio"),
                Sector.nombre.label("nombre_sector"),
                CatalogoItem.precio.label("precio")
            )
            .join(Negocio, Turno.id_negocio == Negocio.id_negocio)
            .outerjoin(Sector, Turno.id_sector == Sector.id_sector)
            .outerjoin(CatalogoItem, Turno.id_item == CatalogoItem.id_item)
            .where(Turno.id_cliente == user_id)
            .order_by(Turno.fecha_solicitud.desc())
        )
        
        results = self.db.execute(stmt).all()
        turnos_list = []
        for row in results:
            turno = row.Turno
            turno.nombre_negocio = row.nombre_negocio
            turno.nombre_sector = row.nombre_sector if row.nombre_sector else "General / Sin Sector"
            turno.precio = float(row.precio) if row.precio is not None else 0.0
            turnos_list.append(turno)
            
        return turnos_list