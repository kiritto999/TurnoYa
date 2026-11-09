from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.core.security import get_current_user, require_roles
from app.models import Usuario
from app.schemas.business import BusinessCreate, BusinessResponse, BusinessStats, BusinessStatus, BusinessUpdate
from app.services.business_service import BusinessService

router = APIRouter(prefix="/businesses", tags=["Negocios"])


@router.post("", response_model=BusinessResponse, status_code=201)
def create_business(data: BusinessCreate, db: Session = Depends(get_db), user: Usuario = Depends(require_roles("dueño"))):
    return BusinessService(db).create(user, data)


@router.get("", response_model=list[BusinessResponse])
def list_businesses(
    nombre: str | None = Query(None),
    categoria: str | None = Query(None),
    lat: float | None = Query(None, ge=-90, le=90, description="Latitud del usuario para ordenar por cercanía"),
    lng: float | None = Query(None, ge=-180, le=180, description="Longitud del usuario"),
    radio_km: float | None = Query(None, gt=0, description="Radio máximo en km (requiere lat/lng)"),
    db: Session = Depends(get_db),
):
    return BusinessService(db).repo.list_active(nombre, categoria, lat, lng, radio_km)


@router.get("/categories", response_model=list[str])
def categories(db: Session = Depends(get_db)):
    return BusinessService(db).repo.categories()


@router.get("/mine", response_model=list[BusinessResponse])
def my_businesses(db: Session = Depends(get_db), user: Usuario = Depends(get_current_user)):
    """Negocios donde el usuario es dueño o empleado aceptado."""
    return BusinessService(db).repo.list_for_user(user.id_usuario)


@router.get("/admin/all", response_model=list[BusinessResponse])
def all_businesses(db: Session = Depends(get_db), user: Usuario = Depends(require_roles("superadmin"))):
    return BusinessService(db).repo.list_all()


@router.get("/{business_id}", response_model=BusinessResponse)
def get_business(business_id: int, db: Session = Depends(get_db)):
    service = BusinessService(db)
    return service.repo._decorate([service.ensure_exists(business_id)])[0]


@router.get("/{business_id}/stats", response_model=BusinessStats)
def business_stats(business_id: int, db: Session = Depends(get_db), user: Usuario = Depends(get_current_user)):
    return BusinessService(db).stats(business_id, user)


@router.put("/{business_id}", response_model=BusinessResponse)
def update_business(business_id: int, data: BusinessUpdate, db: Session = Depends(get_db), user: Usuario = Depends(get_current_user)):
    return BusinessService(db).update(business_id, user, data)


@router.patch("/{business_id}/status", response_model=BusinessResponse)
def set_business_status(business_id: int, data: BusinessStatus, db: Session = Depends(get_db), user: Usuario = Depends(require_roles("superadmin"))):
    return BusinessService(db).set_status(business_id, data.estado)
