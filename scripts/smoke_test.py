"""Prueba de humo de la API TurnoYa: recorre el flujo completo dueño -> cliente -> empleado.

Uso:  python scripts/smoke_test.py [BASE_URL]
Crea usuarios con correo @smoke-turnoya.com; borralos luego con scripts/smoke_cleanup.sql
"""
import sys
import time
import httpx

BASE = (sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8000").rstrip("/") + "/api/v1"
tag = str(int(time.time()))
c = httpx.Client(timeout=20)


def check(resp, code=200):
    if resp.status_code != code:
        raise SystemExit(f"FALLO {resp.request.method} {resp.request.url} -> {resp.status_code}: {resp.text}")
    return resp.json() if resp.content else None


def user(rol, name):
    email = f"{name}{tag}@smoke-turnoya.com"
    check(c.post(f"{BASE}/auth/register", json={"nombre": name.title(), "email": email, "password": "secreto123", "rol": rol}), 201)
    tok = check(c.post(f"{BASE}/auth/login", json={"email": email, "password": "secreto123"}))["access_token"]
    return email, {"Authorization": f"Bearer {tok}"}


_, owner = user("dueño", "dueno")
emp_email, emp = user("cliente", "empleado")
_, cli1 = user("cliente", "cliente")
_, cli2 = user("cliente", "clienteb")

biz = check(c.post(f"{BASE}/businesses", headers=owner, json={
    "nombre": f"Barberia Smoke {tag}", "categoria": "Peluquerías", "descripcion": "Prueba",
    "direccion": "Cra 7 #72-41", "telefono": "3001234567", "latitud": 4.6560, "longitud": -74.0560}), 201)
bid = biz["id_negocio"]
sector = check(c.post(f"{BASE}/businesses/{bid}/sectors", headers=owner, json={"nombre": "Cortes"}), 201)
item = check(c.post(f"{BASE}/businesses/{bid}/catalog", headers=owner, json={
    "id_sector": sector["id_sector"], "nombre": "Corte clásico", "precio": 25000, "tiempo_estimado": 20}), 201)
qr = check(c.post(f"{BASE}/businesses/{bid}/qr", headers=owner, json={"id_sector": sector["id_sector"]}), 201)
assert qr["url_destino"].startswith("turnoya://qr/"), qr
assert check(c.get(f"{BASE}/qr/{qr['codigo_unico']}"))["id_negocio"] == bid

near = check(c.get(f"{BASE}/businesses", params={"lat": 4.65, "lng": -74.05, "radio_km": 5}))
mine = [b for b in near if b["id_negocio"] == bid]
assert mine and mine[0]["distancia_km"] is not None, near
print("cercanos OK, distancia", mine[0]["distancia_km"], "km")
assert "Peluquerías" in check(c.get(f"{BASE}/businesses/categories"))

t1 = check(c.post(f"{BASE}/businesses/{bid}/turns", headers=cli1, json={"id_sector": sector["id_sector"], "id_item": item["id_item"]}), 201)
assert check(c.post(f"{BASE}/businesses/{bid}/turns", headers=cli1, json={"id_sector": sector["id_sector"]}), 409) is not None
t2 = check(c.post(f"{BASE}/businesses/{bid}/turns", headers=cli2, json={"id_sector": sector["id_sector"], "motivo": "Barba"}), 201)
assert t1["personas_adelante"] == 0 and t2["personas_adelante"] == 1 and t2["tiempo_espera_min"] == 20, (t1, t2)
print("posiciones OK:", t1["numero_turno"], t2["numero_turno"], "espera", t2["tiempo_espera_min"], "min")

inv = check(c.post(f"{BASE}/businesses/{bid}/employees/invitations", headers=owner, json={"email": emp_email}), 201)
pend = check(c.get(f"{BASE}/employee-invitations/mine", headers=emp))
assert pend and pend[0]["nombre_negocio"].startswith("Barberia"), pend
check(c.post(f"{BASE}/employee-invitations/by-id/{inv['id_empleado_negocio']}/accept", headers=emp))
assert check(c.get(f"{BASE}/businesses/mine", headers=emp))[0]["mi_rol"] == "empleado"
assert check(c.get(f"{BASE}/businesses/mine", headers=owner))[0]["mi_rol"] == "dueño"

q = check(c.get(f"{BASE}/businesses/{bid}/queue", headers=emp))
assert [t["nombre_cliente"] for t in q] == ["Cliente", "Clienteb"], q
called = check(c.post(f"{BASE}/businesses/{bid}/queue/next", headers=emp))
assert called["id_turno"] == t1["id_turno"] and called["estado"] == "llamado"
check(c.patch(f"{BASE}/turns/{t1['id_turno']}/status", headers=emp, json={"estado": "atendido"}))
my2 = check(c.get(f"{BASE}/turns/my", headers=cli2))
assert my2[0]["personas_adelante"] == 0, my2
public_q = check(c.get(f"{BASE}/businesses/{bid}/queue", headers=cli1))
assert all(t["nombre_cliente"] is None for t in public_q), public_q
check(c.patch(f"{BASE}/turns/{t2['id_turno']}/status", headers=cli2, json={"estado": "cancelado"}))

stats = check(c.get(f"{BASE}/businesses/{bid}/stats", headers=owner))
assert stats["atendidos_hoy"] == 1 and stats["cancelados_hoy"] == 1 and stats["total_hoy"] == 2, stats
notes = check(c.get(f"{BASE}/notifications", headers=cli1))
assert any("Es tu turno" in n["mensaje"] for n in notes), notes
assert check(c.get(f"{BASE}/notifications/unread-count", headers=cli1))["no_leidas"] >= 2
check(c.post(f"{BASE}/notifications/read-all", headers=cli1), 204)
assert check(c.get(f"{BASE}/notifications/unread-count", headers=cli1))["no_leidas"] == 0
print("stats", stats)
# Concurrencia: 8 clientes piden turno a la vez -> numeros unicos y consecutivos
from concurrent.futures import ThreadPoolExecutor
clients = [user("cliente", f"conc{i}x")[1] for i in range(8)]
with ThreadPoolExecutor(8) as pool:
    res = list(pool.map(lambda h: c.post(f"{BASE}/businesses/{bid}/turns", headers=h, json={"id_sector": sector["id_sector"]}), clients))
nums = sorted(check(r, 201)["numero_turno"] for r in res)
assert nums == list(range(nums[0], nums[0] + 8)), nums
print("concurrencia OK:", nums)
print("SMOKE TEST OK")
