from sqlalchemy import func, select, text
from sqlalchemy.orm import Session
from app.models import CatalogoItem, Negocio, Sector, Turno, Usuario

ACTIVE_STATES = ("pendiente", "llamado", "atendiendo")
DEFAULT_MINUTES_PER_TURN = 5


class TurnRepository:
    def __init__(self, db: Session):
        self.db = db

    def next_number(self, business_id: int, sector_id: int | None) -> int:
        """Siguiente número de turno sin duplicados bajo concurrencia (RNF06).

        Bloquea la fila del negocio (SELECT ... FOR UPDATE) hasta el commit/rollback de la
        transacción, así dos solicitudes simultáneas se serializan. El MAX también es una
        lectura con bloqueo para ver el último valor confirmado y no una instantánea vieja.
        """
        self.db.execute(select(Negocio.id_negocio).where(Negocio.id_negocio == business_id).with_for_update())
        current = self.db.scalar(
            select(func.coalesce(func.max(Turno.numero_turno), 0)).where(
                Turno.id_negocio == business_id,
                Turno.id_sector.is_(None) if sector_id is None else Turno.id_sector == sector_id,
            ).with_for_update()
        ) or 0
        return int(current) + 1

    def release_lock(self):
        """Compatibilidad: el bloqueo de fila se libera solo con commit/rollback."""
        return None

    def get(self, turn_id: int):
        return self.db.get(Turno, turn_id)

    def _detailed(self):
        return (
            select(
                Turno,
                Negocio.nombre.label("nombre_negocio"),
                Sector.nombre.label("nombre_sector"),
                CatalogoItem.nombre.label("nombre_item"),
                CatalogoItem.precio.label("precio"),
                CatalogoItem.tiempo_estimado.label("tiempo_item"),
                Usuario.nombre.label("nombre_cliente"),
            )
            .join(Negocio, Turno.id_negocio == Negocio.id_negocio)
            .join(Usuario, Turno.id_cliente == Usuario.id_usuario)
            .outerjoin(Sector, Turno.id_sector == Sector.id_sector)
            .outerjoin(CatalogoItem, Turno.id_item == CatalogoItem.id_item)
        )

    @staticmethod
    def _fill(row):
        turno = row.Turno
        turno.nombre_negocio = row.nombre_negocio
        turno.nombre_sector = row.nombre_sector or "General"
        turno.nombre_item = row.nombre_item
        turno.precio = float(row.precio) if row.precio is not None else None
        turno.nombre_cliente = row.nombre_cliente
        turno.tiempo_item = row.tiempo_item
        turno.personas_adelante = None
        turno.tiempo_espera_min = None
        return turno

    def _queue_turns(self, business_id: int, sector_id: int | None, filter_sector: bool):
        stmt = self._detailed().where(Turno.id_negocio == business_id, Turno.estado.in_(ACTIVE_STATES))
        if filter_sector:
            stmt = stmt.where(Turno.id_sector.is_(None) if sector_id is None else Turno.id_sector == sector_id)
        rows = self.db.execute(stmt.order_by(Turno.fecha_solicitud, Turno.id_turno)).all()
        return [self._fill(r) for r in rows]

    @staticmethod
    def _apply_positions(turns):
        """Calcula personas adelante y minutos estimados de espera sobre una cola ordenada por llegada."""
        minutes = 0
        ahead = 0
        for t in turns:
            t.personas_adelante = ahead
            t.tiempo_espera_min = minutes
            if t.estado == "pendiente":
                ahead += 1
            minutes += t.tiempo_item or DEFAULT_MINUTES_PER_TURN
        return turns

    def queue(self, business_id: int, sector_id: int | None = None):
        turns = self._queue_turns(business_id, sector_id, filter_sector=sector_id is not None)
        if sector_id is not None:
            return self._apply_positions(turns)
        # Sin filtro: la posición se calcula dentro de cada sector, el orden global es por llegada
        by_sector: dict = {}
        for t in turns:
            by_sector.setdefault(t.id_sector, []).append(t)
        for group in by_sector.values():
            self._apply_positions(group)
        return turns

    def _positions_for(self, business_id: int, sector_id: int | None) -> dict:
        queue = self._apply_positions(self._queue_turns(business_id, sector_id, True))
        return {t.id_turno: t for t in queue}

    def with_position(self, turn: Turno):
        row = self.db.execute(self._detailed().where(Turno.id_turno == turn.id_turno)).first()
        turn = self._fill(row)
        if turn.estado in ACTIVE_STATES:
            ref = self._positions_for(turn.id_negocio, turn.id_sector).get(turn.id_turno)
            if ref is not None:
                turn.personas_adelante = ref.personas_adelante
                turn.tiempo_espera_min = ref.tiempo_espera_min
        return turn

    def customer_turns(self, user_id: int):
        rows = self.db.execute(
            self._detailed().where(Turno.id_cliente == user_id).order_by(Turno.fecha_solicitud.desc())
        ).all()
        turns = [self._fill(r) for r in rows]
        cache: dict = {}
        for turn in turns:
            if turn.estado not in ACTIVE_STATES:
                continue
            key = (turn.id_negocio, turn.id_sector)
            if key not in cache:
                cache[key] = self._positions_for(*key)
            ref = cache[key].get(turn.id_turno)
            if ref is not None:
                turn.personas_adelante = ref.personas_adelante
                turn.tiempo_espera_min = ref.tiempo_espera_min
        return turns
