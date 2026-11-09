"""Carga datos de demostración (negocios en Bogotá con ubicación, catálogo, sectores y turnos).

Uso:  python scripts/seed_demo.py [BASE_URL]
Es idempotente para los usuarios (si ya existen, solo inicia sesión).

Cuentas de demo (contraseña para todas: Demo2026!):
  - demo.dueno@turnoya-demo.com     (dueño de todos los negocios)
  - demo.cliente@turnoya-demo.com   (cliente)
  - demo.empleado@turnoya-demo.com  (empleado de "Barbería El Clásico")
Para borrarlo todo: scripts/seed_demo_cleanup.sql
"""
import sys
import httpx

BASE = (sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8000").rstrip("/") + "/api/v1"
PASSWORD = "Demo2026!"
c = httpx.Client(timeout=20)


def ok(r, *codes):
    if r.status_code not in (codes or (200, 201)):
        raise SystemExit(f"{r.request.method} {r.request.url} -> {r.status_code} {r.text}")
    return r.json() if r.content else None


def account(email, nombre, rol):
    r = c.post(f"{BASE}/auth/register", json={"nombre": nombre, "email": email, "password": PASSWORD, "rol": rol})
    if r.status_code not in (201, 409, 400):
        ok(r)
    tok = ok(c.post(f"{BASE}/auth/login", json={"email": email, "password": PASSWORD}))["access_token"]
    return {"Authorization": f"Bearer {tok}"}


owner = account("demo.dueno@turnoya-demo.com", "Laura Gómez", "dueño")
client = account("demo.cliente@turnoya-demo.com", "Andrés Ruiz", "cliente")
employee = account("demo.empleado@turnoya-demo.com", "Camila Torres", "cliente")
extra_clients = [account(f"demo.fila{i}@turnoya-demo.com", n, "cliente") for i, n in enumerate(
    ["Sofía Martínez", "Juan Pérez", "Valentina Rojas", "Mateo López", "Isabella Díaz"], 1)]

if ok(c.get(f"{BASE}/businesses/mine", headers=owner)):
    print("Los negocios demo ya existen; no se crean de nuevo.")
    sys.exit(0)

NEGOCIOS = [
    {
        "nombre": "Barbería El Clásico", "categoria": "Peluquerías", "horario": "Lun a Sáb 9:00 a.m. - 8:00 p.m.",
        "descripcion": "Cortes clásicos y modernos, barba con toalla caliente.", "direccion": "Cra 13 # 63-20, Chapinero",
        "telefono": "3104567890", "latitud": 4.6493, "longitud": -74.0634,
        "sectores": ["Cortes", "Barba"],
        "catalogo": [("Corte clásico", 25000, 25, "Cortes"), ("Corte + barba", 38000, 40, "Barba"), ("Arreglo de barba", 18000, 20, "Barba")],
        "fila": 3,
    },
    {
        "nombre": "Arepas La 57", "categoria": "Comida", "horario": "Todos los días 7:00 a.m. - 9:00 p.m.",
        "descripcion": "Arepas rellenas, jugos naturales y desayunos.", "direccion": "Calle 57 # 9-12",
        "telefono": "3015550101", "latitud": 4.6460, "longitud": -74.0640,
        "sectores": [], "catalogo": [("Arepa de choclo con queso", 9000, 8, None), ("Desayuno completo", 16000, 12, None)],
        "fila": 2,
    },
    {
        "nombre": "Mercado Fresco 60", "categoria": "Mercados", "horario": "Lun a Dom 7:00 a.m. - 10:00 p.m.",
        "descripcion": "Frutas, verduras y mercado del día. Pide turno para la carnicería.", "direccion": "Cra 7 # 60-35",
        "telefono": "6013334455", "latitud": 4.6489, "longitud": -74.0590,
        "sectores": ["Carnicería", "Cajas"], "catalogo": [("Pedido carnicería", None, 10, "Carnicería")],
        "fila": 1,
    },
    {
        "nombre": "Registraduría Chapinero", "categoria": "Registradurías", "horario": "Lun a Vie 8:00 a.m. - 4:00 p.m.",
        "descripcion": "Duplicados de cédula, registro civil y tarjeta de identidad.", "direccion": "Calle 53 # 13-40",
        "telefono": None, "latitud": 4.6421, "longitud": -74.0661,
        "sectores": ["Cédulas", "Registro civil"],
        "catalogo": [("Duplicado de cédula", 59900, 15, "Cédulas"), ("Registro civil de nacimiento", None, 20, "Registro civil")],
        "fila": 4,
    },
    {
        "nombre": "Clínica Dental Sonrisa", "categoria": "Salud", "horario": "Lun a Sáb 8:00 a.m. - 6:00 p.m.",
        "descripcion": "Odontología general y urgencias.", "direccion": "Calle 67 # 11-50",
        "telefono": "3207778899", "latitud": 4.6545, "longitud": -74.0609,
        "sectores": [], "catalogo": [("Valoración", 40000, 20, None), ("Limpieza", 90000, 40, None)],
        "fila": 0,
    },
    {
        "nombre": "Uñas & Spa Glam", "categoria": "Belleza", "horario": "Mar a Dom 10:00 a.m. - 7:00 p.m.",
        "descripcion": "Manicure, pedicure y spa de manos.", "direccion": "Cra 9 # 69-15",
        "telefono": "3112223344", "latitud": 4.6560, "longitud": -74.0573,
        "sectores": [], "catalogo": [("Manicure tradicional", 22000, 45, None), ("Semipermanente", 45000, 60, None)],
        "fila": 0,
    },
]

for n in NEGOCIOS:
    body = {k: n[k] for k in ("nombre", "categoria", "horario", "descripcion", "direccion", "telefono", "latitud", "longitud")}
    b = ok(c.post(f"{BASE}/businesses", headers=owner, json=body))
    sectors = {s: ok(c.post(f"{BASE}/businesses/{b['id_negocio']}/sectors", headers=owner, json={"nombre": s}))["id_sector"] for s in n["sectores"]}
    for nombre, precio, minutos, sector in n["catalogo"]:
        ok(c.post(f"{BASE}/businesses/{b['id_negocio']}/catalog", headers=owner, json={
            "nombre": nombre, "precio": precio, "tiempo_estimado": minutos, "id_sector": sectors.get(sector)}))
    ok(c.post(f"{BASE}/businesses/{b['id_negocio']}/qr", headers=owner, json={}))
    first_sector = next(iter(sectors.values()), None)
    for h in extra_clients[: n["fila"]]:
        ok(c.post(f"{BASE}/businesses/{b['id_negocio']}/turns", headers=h, json={"id_sector": first_sector}))
    if n["nombre"].startswith("Barbería"):
        inv = ok(c.post(f"{BASE}/businesses/{b['id_negocio']}/employees/invitations", headers=owner, json={"email": "demo.empleado@turnoya-demo.com"}))
        ok(c.post(f"{BASE}/employee-invitations/by-id/{inv['id_empleado_negocio']}/accept", headers=employee))
    print("creado:", b["nombre"])
print("Datos demo listos.")
