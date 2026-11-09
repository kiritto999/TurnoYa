from pydantic import BaseModel, Field
from app.schemas.common import ORMModel


class BusinessCreate(BaseModel):
    nombre: str = Field(min_length=2, max_length=150)
    logo_url: str | None = Field(default=None, max_length=255)
    categoria: str | None = Field(default=None, max_length=100)
    horario: str | None = Field(default=None, max_length=150)
    descripcion: str | None = Field(default=None, max_length=500)
    direccion: str | None = Field(default=None, max_length=255)
    telefono: str | None = Field(default=None, max_length=30)
    latitud: float | None = Field(default=None, ge=-90, le=90)
    longitud: float | None = Field(default=None, ge=-180, le=180)


class BusinessUpdate(BusinessCreate):
    pass


class BusinessStatus(BaseModel):
    estado: str = Field(pattern="^(activo|suspendido|inactivo)$")


class BusinessResponse(ORMModel):
    id_negocio: int
    id_dueno: int
    nombre: str
    logo_url: str | None
    categoria: str | None
    horario: str | None
    descripcion: str | None = None
    direccion: str | None = None
    telefono: str | None = None
    latitud: float | None = None
    longitud: float | None = None
    estado: str
    distancia_km: float | None = None
    turnos_en_espera: int | None = None
    mi_rol: str | None = None


class BusinessStats(BaseModel):
    en_espera: int
    llamados: int
    atendidos_hoy: int
    cancelados_hoy: int
    total_hoy: int
