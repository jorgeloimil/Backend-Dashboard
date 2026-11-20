# Backend-Dashboard

API del panel de casa (tablet junto al escritorio): **lista de la compra compartida**, **tiempo**, **agenda familiar** y **Fitcina** (qué toca entrenar hoy). El frontend vive en [Front-Dashboard](https://github.com/jorgeloimil/Front-Dashboard).

Mismo estilo que Fitcina: Spring Boot 3.5, Java 21, contrato OpenAPI como fuente de verdad (openapi-generator + patrón delegate), MapStruct, PostgreSQL con Flyway, y tests con JUnit/Mockito/Cucumber (H2 en memoria).

## Arrancar en local

Necesitas JDK 21 y un PostgreSQL (el más rápido, con Docker):

```bash
docker run -d --name panel-db -p 5432:5432 \
  -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=home_dashboard postgres:16-alpine
./mvnw spring-boot:run      # http://localhost:8080  ·  Swagger UI en /swagger-ui.html
```

```bash
./mvnw verify               # unitarios + Cucumber, sin PostgreSQL
```

## Endpoints

| Ruta | Qué hace |
|------|----------|
| `GET/POST /api/compra/articulos` | Lista y alta de artículos (clasifica el pasillo solo; no duplica) |
| `PATCH/DELETE /api/compra/articulos/{id}` | Marcar comprado, editar, quitar |
| `DELETE /api/compra/articulos/comprados` | Limpia los comprados |
| `GET /api/tiempo` | Tiempo actual, 12 horas y 6 días ([Open-Meteo](https://open-meteo.com), gratis y sin clave) |
| `GET /api/agenda?dias=7` | Eventos de los calendarios iCal configurados, con repeticiones |
| `GET /api/fitcina/hoy` | Rutinas de [Fitcina](https://github.com/jorgeloimil/Fitcina-Backend) que tocan hoy, su estado (pendiente / en curso / hecha), racha y sesiones de la semana. Siempre 200: si Fitcina falla lo dice `estado` |
| `GET /actuator/health` | Healthcheck |

El contrato está en `src/main/resources/swagger-file/home-dashboard-1.0.0.yml`. Para cambiar la API: editar el contrato → `./mvnw generate-sources` → el compilador señala los delegates que lo incumplen.

## Configuración (variables de entorno)

Todo tiene valor por defecto para desarrollo; en el NUC se pasarán desde el docker-compose.

| Variable | Descripción | Por defecto |
|----------|-------------|-------------|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | PostgreSQL | `jdbc:postgresql://localhost:5432/home_dashboard` / `postgres` / `postgres` |
| `PORT` | Puerto HTTP | `8080` |
| `PANEL_ZONA` | Zona horaria de casa | `Europe/Madrid` |
| `TIEMPO_UBICACION` / `TIEMPO_LATITUD` / `TIEMPO_LONGITUD` | Dónde vives | Madrid (`40.4168`, `-3.7038`) |
| `TIEMPO_CACHE` / `AGENDA_CACHE` | Cada cuánto se vuelve a pedir | `10m` / `5m` |
| `AGENDA_CALENDARIOS_0_NOMBRE` / `_URL` / `_COLOR` | Un calendario iCal (sube el número para más: `_1_`, `_2_`…) | ninguno |

### Conectar Fitcina

El panel llama a la API de Fitcina **por la red interna del docker-compose** (no sale a internet) y solo lee: `/api/routines`, `/api/sessions` y `/api/sessions/stats`. Fitcina no tiene claves de servicio, así que el panel entra con tu email y contraseña de Fitcina, guarda el JWT en memoria y entra de nuevo si caduca.

| Variable | Descripción | Por defecto |
|----------|-------------|-------------|
| `FITCINA_URL` | URL de Fitcina vista desde este contenedor | `http://fitcina-backend:8080` |
| `FITCINA_EMAIL` / `FITCINA_PASSWORD` | Cuenta de Fitcina cuya rutina se muestra | vacío: la tarjeta sale «sin configurar» |
| `FITCINA_CACHE` | Cuánto se reutiliza la última lectura | `2m` |
| `FITCINA_REINTENTO` | Espera tras un fallo antes de volver a llamar a Fitcina (protege su límite de intentos de login) | `1m` |

"Hoy toca" sale de los `diasSemana` de cada rutina (`Lunes,Miércoles,Viernes`, sin importar tildes ni mayúsculas) y el estado, de las sesiones de hoy en `PANEL_ZONA`. Si Fitcina no responde: se sirve la última copia de hoy como `DESACTUALIZADO`, o `NO_DISPONIBLE` si no la hay. La contraseña va en el `.env` del NUC, nunca en el repo.

### Conectar Google Calendar

En Google Calendar → Configuración del calendario → *Integrar el calendario* → **Dirección secreta en formato iCal**:

```bash
AGENDA_CALENDARIOS_0_NOMBRE=Familia
AGENDA_CALENDARIOS_0_URL=https://calendar.google.com/calendar/ical/.../basic.ics
AGENDA_CALENDARIOS_0_COLOR=#FF5A36
```

Esa URL es un secreto (quien la tenga lee el calendario): va en `.env`, nunca en el repo. iCloud (`webcal://…`) y Outlook también sirven.

## Seguridad

El MVP **no tiene login**: está pensado para publicarse solo dentro de la red de casa y de la tailnet de Tailscale. No lo expongas a internet.

## Estructura

```
src/main/java/com/homedashboard
├── api          # *ApiDelegateImpl: implementan el contrato
├── config       # PanelProperties (panel.*), reloj, cliente HTTP
├── exceptions   # GlobalExceptionHandler (ProblemDetail, RFC 7807)
├── mappers      # MapStruct
├── modelo       # entidades JPA
├── repository
└── services     # compra, tiempo (Open-Meteo con caché) y agenda (iCal con ical4j)
```

Los DTO y las interfaces `*Api` se generan en `target/` en cada build: no se editan ni se versionan.

## Convención de commits

`<tipo>: <descripción en español>` — `feat:`, `fix:`, `refactor:`, `docs:`, `test:`, `chore:`.
