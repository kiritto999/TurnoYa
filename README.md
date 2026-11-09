# TurnoYa

Plataforma de turnos virtuales para pequeños negocios: los clientes encuentran negocios en un mapa, piden turno (también escaneando un QR) y siguen su posición en vivo; los negocios gestionan su fila, catálogo, sectores y empleados.

| Carpeta | Qué es |
|---|---|
| `/` (raíz) | API REST en FastAPI + MySQL |
| `TurnoYaApp_Kotlin/` | App Android en Kotlin + Jetpack Compose ([ver README](TurnoYaApp_Kotlin/README.md)) |
| `migrations/` | Cambios de base de datos para instalaciones existentes |
| `scripts/` | Superadmin, prueba de humo y datos de demostración |

## Usuarios de prueba

Cuentas de demostración creadas con `scripts/seed_demo.py` (6 negocios de ejemplo en Chapinero, Bogotá).
**Contraseña de todas: `Demo2026!`**

| Rol | Correo | Para probar |
|---|---|---|
| Dueño | `demo.dueno@turnoya-demo.com` | Panel, crear/editar negocios, fila, catálogo, sectores, equipo y QR |
| Cliente | `demo.cliente@turnoya-demo.com` | Mapa, pedir turno, seguimiento en vivo, notificaciones |
| Empleado | `demo.empleado@turnoya-demo.com` | Atender la fila de "Barbería El Clásico" |
| Clientes en fila | `demo.fila1@turnoya-demo.com` … `demo.fila5@turnoya-demo.com` | Turnos de ejemplo ya creados |

> Son cuentas solo para pruebas. Para borrarlas con sus negocios: `mysql < scripts/seed_demo_cleanup.sql`.
> Para volver a crearlas: `python scripts/seed_demo.py http://127.0.0.1:8000`.

## Servidor
La app trae la URL del servidor en `TurnoYaApp_Kotlin/app/build.gradle.kts` (`DEFAULT_API_URL`) y se puede cambiar dentro de la app (⚙️ en el ingreso, o Perfil → Servidor). Documentación interactiva de la API: `<URL del servidor>/docs`.

---

# API (FastAPI + MySQL)

API REST para el proyecto TurnoYa. Está organizada por capas: controllers (HTTP), services (reglas de negocio), repositories (acceso a datos), models (SQLAlchemy), schemas (Pydantic) y core (configuración, BD y seguridad).

## 1. Base de datos

Ejecuta el archivo `turnoya_schema.sql` en MySQL. El esquema crea `turnoya_db`.

La API está configurada para MySQL local **sin contraseña** para el usuario `root`:

`mysql+pymysql://root:@localhost:3306/turnoya_db`

Si tu usuario MySQL no es `root`, cambia `DB_USER` en `.env`.

## 2. Instalación

```bash
python -m venv .venv
# Windows
.venv\Scripts\activate
# Linux/macOS
source .venv/bin/activate
pip install -r requirements.txt
copy .env.example .env  # Windows
# cp .env.example .env # Linux/macOS
uvicorn app.main:app --reload
```

Swagger: http://127.0.0.1:8000/docs

## 3. Autenticación

El login devuelve JWT. Envía:

`Authorization: Bearer <token>`

La contraseña del usuario de TurnoYa **sí se almacena hasheada** (Argon2). Esto es independiente de la contraseña del servidor MySQL, que en tu caso está vacía.

## 4. Roles

- `cliente`
- `dueño`
- `empleado`
- `superadmin`

## 5. Endpoints principales

### Auth / RF01
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`

### Negocios / RF02, RF11, RF14
- `POST /api/v1/businesses`
- `GET /api/v1/businesses?nombre=&categoria=&lat=&lng=&radio_km=` — con `lat`/`lng` ordena por cercanía y devuelve `distancia_km`; siempre incluye `turnos_en_espera`
- `GET /api/v1/businesses/categories`
- `GET /api/v1/businesses/mine` — negocios del dueño o donde el usuario es empleado (`mi_rol`)
- `GET /api/v1/businesses/{id}`
- `GET /api/v1/businesses/{id}/stats` — en espera, atendidos y cancelados de hoy
- `PUT /api/v1/businesses/{id}`
- `PATCH /api/v1/businesses/{id}/status`

### Sectores / RF03
- `POST /api/v1/businesses/{id}/sectors`
- `GET /api/v1/businesses/{id}/sectors`
- `GET /api/v1/sectors/{id}`
- `PUT /api/v1/sectors/{id}`
- `DELETE /api/v1/sectors/{id}`

### Catálogo / RF04
- `POST /api/v1/businesses/{id}/catalog`
- `GET /api/v1/businesses/{id}/catalog`
- `GET /api/v1/catalog/{id}`
- `PUT /api/v1/catalog/{id}`
- `DELETE /api/v1/catalog/{id}`

### Empleados / RF07, RF08
- `POST /api/v1/businesses/{id}/employees/invitations`
- `GET /api/v1/businesses/{id}/employees`
- `POST /api/v1/employee-invitations/{token}/accept`
- `GET /api/v1/employee-invitations/mine` — invitaciones pendientes del usuario
- `POST /api/v1/employee-invitations/by-id/{id}/accept` · `.../reject`
- `PATCH /api/v1/employees/{id}/permissions`
- `DELETE /api/v1/employees/{id}`

### QR / RF05, RF06
- `POST /api/v1/businesses/{id}/qr`
- `GET /api/v1/businesses/{id}/qr`
- `GET /api/v1/qr/{code}`
- `GET /api/v1/qr/{code}/image`

### Turnos / RF09, RF10
- `POST /api/v1/businesses/{id}/turns`
- `GET /api/v1/businesses/{id}/queue` — incluye `personas_adelante` y `tiempo_espera_min` (los clientes no ven datos de otros clientes)
- `POST /api/v1/businesses/{id}/queue/next?sector_id=` — llama al siguiente turno
- `GET /api/v1/turns/my`
- `GET /api/v1/turns/{id}`
- `PATCH /api/v1/turns/{id}/status`

### Notificaciones / RF12, RF13
- `GET /api/v1/notifications`
- `GET /api/v1/notifications/unread-count`
- `POST /api/v1/notifications/read-all`
- `PATCH /api/v1/notifications/{id}/read`

## 6. Lo que queda preparado para ampliar

- Mapa: implementado. `negocio` tiene `descripcion`, `direccion`, `telefono`, `latitud` y `longitud` (bases existentes: ejecutar `migrations/001_ubicacion_negocio.sql`).
- QR: el contenido es `turnoya://qr/{codigo}`, que la app abre directamente.
- Un cliente no puede tener dos turnos activos en el mismo negocio. El número de turno se genera con bloqueo de fila (`SELECT ... FOR UPDATE`), probado con 8 solicitudes simultáneas.
- Push: el esquema guarda notificaciones. Para push real hay que conectar FCM, OneSignal, APNs u otro proveedor.

## 7. Prueba rápida

Registra un dueño, crea su negocio y luego usa Swagger para probar sectores, catálogo, QR y turnos.

- `python scripts/smoke_test.py` recorre todo el flujo (borra los datos con `scripts/smoke_cleanup.sql`).
- `python scripts/seed_demo.py` carga negocios de demostración en Bogotá (borrar con `scripts/seed_demo_cleanup.sql`).

## 8. Crear el superadministrador

Como RF14 es una función administrativa, no se habilita desde el registro público. Después de instalar dependencias:

```bash
python scripts/create_superadmin.py
```

## 9. Notificaciones

La app Android consulta las notificaciones guardadas y las muestra como notificaciones del sistema (cada 20 s con la app abierta y cada 15 min en segundo plano). Para push instantáneo con la app cerrada haría falta integrar FCM y guardar tokens de dispositivo.
