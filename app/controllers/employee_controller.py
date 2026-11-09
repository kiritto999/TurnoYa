from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.core.security import get_current_user
from app.models import Usuario
from app.schemas.employee import EmployeeInviteCreate, EmployeePermissionsUpdate, EmployeeResponse, InvitationResponse
from app.services.employee_service import EmployeeService

router = APIRouter(tags=["Empleados"])


def serialize_membership(row):
    membership, nombre, email = row
    return {**{k: getattr(membership, k) for k in ("id_empleado_negocio", "id_usuario", "id_negocio", "id_sector", "permisos", "estado_invitacion", "fecha_invitacion")}, "nombre": nombre, "email": email}


@router.post("/businesses/{business_id}/employees/invitations", status_code=201)
def invite_employee(business_id: int, data: EmployeeInviteCreate, db: Session = Depends(get_db), user: Usuario = Depends(get_current_user)):
    membership, token = EmployeeService(db).invite(business_id, user, data.email, data.id_sector, data.permisos)
    return {"message": "Invitación creada", "id_empleado_negocio": membership.id_empleado_negocio, "invitation_token": token}


@router.get("/businesses/{business_id}/employees", response_model=list[EmployeeResponse])
def list_employees(business_id: int, db: Session = Depends(get_db), user: Usuario = Depends(get_current_user)):
    return [serialize_membership(row) for row in EmployeeService(db).list(business_id, user)]


@router.post("/employee-invitations/{token}/accept", response_model=EmployeeResponse)
def accept_invitation(token: str, db: Session = Depends(get_db), user: Usuario = Depends(get_current_user)):
    membership = EmployeeService(db).accept(token, user)
    return membership


@router.patch("/employees/{membership_id}/permissions", response_model=EmployeeResponse)
def update_permissions(membership_id: int, data: EmployeePermissionsUpdate, db: Session = Depends(get_db), user: Usuario = Depends(get_current_user)):
    return EmployeeService(db).permissions(membership_id, user, data.permisos, data.id_sector)


@router.get("/employee-invitations/mine", response_model=list[InvitationResponse])
def my_invitations(db: Session = Depends(get_db), user: Usuario = Depends(get_current_user)):
    return EmployeeService(db).my_invitations(user)


@router.post("/employee-invitations/by-id/{membership_id}/accept", response_model=EmployeeResponse)
def accept_by_id(membership_id: int, db: Session = Depends(get_db), user: Usuario = Depends(get_current_user)):
    return EmployeeService(db).respond(membership_id, user, True)


@router.post("/employee-invitations/by-id/{membership_id}/reject", response_model=EmployeeResponse)
def reject_by_id(membership_id: int, db: Session = Depends(get_db), user: Usuario = Depends(get_current_user)):
    return EmployeeService(db).respond(membership_id, user, False)


@router.delete("/employees/{membership_id}", status_code=204)
def remove_employee(membership_id: int, db: Session = Depends(get_db), user: Usuario = Depends(get_current_user)):
    EmployeeService(db).remove(membership_id, user)
