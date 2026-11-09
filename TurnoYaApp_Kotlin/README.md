# TurnoYa — App Android (Kotlin + Jetpack Compose)

Rediseño completo de TurnoYa, estilo app de domicilios: mapa interactivo de negocios cercanos, turnos virtuales en vivo, QR y panel para negocios. Usa los colores y logos de la marca (carpeta `branding/`).

## Abrir y ejecutar
1. Android Studio → **Open** → esta carpeta (`TurnoYaApp_Kotlin`).
2. Ejecuta la configuración `app` en un emulador o celular (Android 7.0+).

Desde la terminal: `gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.

## Servidor
La app trae por defecto la URL del túnel de Cloudflare (`DEFAULT_API_URL` en `app/build.gradle.kts`).
Si la URL cambia (pasa al reiniciar la VM), **no hace falta recompilar**: en la pantalla de ingreso toca el ⚙️ (o en Perfil → Servidor) y pega la nueva URL. En la VM se consulta con `turnoya-url`.

- Emulador + VM local: `http://10.0.2.2:8080`

## Qué incluye (requerimientos del documento)
| Requerimiento | Dónde |
|---|---|
| RF01 Registro cliente / dueño | `ui/auth` |
| RF02 Perfil de negocio con ubicación en mapa | `ui/owner/PanelScreens.kt` (editor con pin) |
| RF03 Sectores · RF04 Catálogo | `ui/owner/ManageBusinessScreen.kt` |
| RF05/RF06 QR único, póster imprimible, escaneo y enlace `turnoya://qr/...` | `ui/owner/QrTab.kt`, botón central de la barra |
| RF07/RF08 Invitar empleados, aceptar desde Perfil | pestaña Equipo · `ui/common/AccountScreens.kt` |
| RF09 Pedir turno · RF10 Fila y posición en vivo | `ui/client/BusinessScreen.kt`, `TurnScreens.kt` |
| RF11 Mapa interactivo (OpenStreetMap) + búsqueda y categorías | `ui/client/HomeScreen.kt`, `ui/map/OsmMap.kt` |
| RF12/RF13 Notificaciones (en la app y del sistema) | `notifications/Notifier.kt` |

## Datos de demostración
El servidor tiene negocios de ejemplo en Chapinero (Bogotá). Las cuentas y la contraseña están en
`turnoya_api/scripts/seed_demo.py`; para borrarlos usa `turnoya_api/scripts/seed_demo_cleanup.sql`.

## Estructura
```
data/          Modelos, API (Retrofit), sesión (DataStore)
ui/theme       Colores de marca, tipografía, formas
ui/components  Botones, tarjetas, categorías, logos
ui/map         Mapa OpenStreetMap para Compose
ui/client      Inicio con mapa, negocio, mis turnos, ticket en vivo
ui/owner       Panel, editor de negocio, gestión (fila, catálogo, sectores, equipo, QR)
ui/common      Avisos y perfil
```
