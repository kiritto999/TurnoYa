import math
from sqlalchemy import func, select
from sqlalchemy.orm import Session
from app.models import EmpleadoNegocio, Negocio, Turno

ACTIVE_TURN_STATES = ("pendiente", "llamado", "atendiendo")


def haversine_km(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    r = 6371.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp, dl = math.radians(lat2 - lat1), math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(math.sqrt(a))


class BusinessRepository:
    def __init__(self, db: Session):
        self.db = db

    def get(self, business_id: int):
        return self.db.get(Negocio, business_id)

    def waiting_counts(self, business_ids: list[int]) -> dict[int, int]:
        if not business_ids:
            return {}
        rows = self.db.execute(
            select(Turno.id_negocio, func.count())
            .where(Turno.id_negocio.in_(business_ids), Turno.estado.in_(ACTIVE_TURN_STATES))
            .group_by(Turno.id_negocio)
        ).all()
        return {bid: int(c) for bid, c in rows}

    def _decorate(self, businesses: list[Negocio], lat: float | None = None, lng: float | None = None):
        counts = self.waiting_counts([b.id_negocio for b in businesses])
        for b in businesses:
            b.turnos_en_espera = counts.get(b.id_negocio, 0)
            b.distancia_km = None
            if lat is not None and lng is not None and b.latitud is not None and b.longitud is not None:
                b.distancia_km = round(haversine_km(lat, lng, float(b.latitud), float(b.longitud)), 2)
        return businesses

    def list_active(self, nombre: str | None = None, categoria: str | None = None,
                    lat: float | None = None, lng: float | None = None, radio_km: float | None = None):
        stmt = select(Negocio).where(Negocio.estado == "activo")
        if nombre:
            stmt = stmt.where(Negocio.nombre.ilike(f"%{nombre}%"))
        if categoria:
            stmt = stmt.where(Negocio.categoria.ilike(f"%{categoria}%"))
        businesses = self._decorate(list(self.db.scalars(stmt.order_by(Negocio.nombre))), lat, lng)
        if lat is not None and lng is not None:
            if radio_km:
                businesses = [b for b in businesses if b.distancia_km is None or b.distancia_km <= radio_km]
            businesses.sort(key=lambda b: (b.distancia_km is None, b.distancia_km or 0))
        return businesses

    def list_all(self):
        return self._decorate(list(self.db.scalars(select(Negocio).order_by(Negocio.nombre))))

    def list_for_user(self, user_id: int):
        owned = list(self.db.scalars(select(Negocio).where(Negocio.id_dueno == user_id).order_by(Negocio.nombre)))
        for b in owned:
            b.mi_rol = "dueño"
        staff = list(self.db.scalars(
            select(Negocio)
            .join(EmpleadoNegocio, EmpleadoNegocio.id_negocio == Negocio.id_negocio)
            .where(EmpleadoNegocio.id_usuario == user_id, EmpleadoNegocio.estado_invitacion == "aceptada")
            .order_by(Negocio.nombre)
        ))
        owned_ids = {b.id_negocio for b in owned}
        for b in staff:
            b.mi_rol = "empleado"
        return self._decorate(owned + [b for b in staff if b.id_negocio not in owned_ids])

    def categories(self):
        rows = self.db.scalars(
            select(Negocio.categoria).where(Negocio.estado == "activo", Negocio.categoria.is_not(None)).distinct()
        )
        return sorted({c.strip() for c in rows if c and c.strip()}, key=str.lower)
