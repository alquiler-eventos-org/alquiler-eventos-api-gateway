# alquiler-eventos-api-gateway

API Gateway (Spring Cloud Gateway) del sistema de alquiler de equipos para eventos
(FIUNI - Sistemas Web y Distribuidos). Es la **unica puerta de entrada**: enruta las
peticiones a `ms-seguridad`, `ms-alquiler` y `ms-compras` descubriendolos por nombre en
**Eureka** (`lb://nombre-del-servicio`), **valida el JWT antes de rutear** y ofrece un
**Swagger UI unico** con la documentacion de los tres microservicios.

## Stack

| Tecnologia | Version |
|---|---|
| Java | 21 |
| Spring Boot | 3.4.5 |
| Spring Cloud (Gateway + Eureka Client) | 2024.0.1 |
| Spring Security + OAuth2 Resource Server | incluido en Spring Boot |
| SpringDoc OpenAPI (WebFlux UI) | 2.8.5 |
| Maven | 3.9+ |

## Orden de arranque

1. Redis (servidor compartido, solo necesario para la Entrega 3)
2. `alquiler-eventos-eureka` (puerto 8761)
3. `ms-seguridad` (8083), `ms-alquiler` (8081) y `ms-compras` (8082)
4. `api-gateway` (8080) con `mvn spring-boot:run`

## URLs

| Que | URL |
|---|---|
| Swagger UI (pruebas de todo el sistema) | <http://localhost:8080/swagger-ui.html> |
| Consola de Eureka | <http://localhost:8761> |

## Rutas (application.yml)

| Ruta en el gateway | Destino (Eureka) |
|---|---|
| `/api/auth/**`, `/api/usuarios/**`, `/api/roles/**` | `ms-seguridad` |
| `/api/clientes/**`, `/api/ordenes-alquiler/**` | `ms-alquiler` |
| `/api/proveedores/**`, `/api/ordenes-compra/**` | `ms-compras` |
| `/seguridad/v3/api-docs` -> `/api/v3/api-docs` | OpenAPI de `ms-seguridad` |
| `/alquiler/v3/api-docs` -> `/api/v3/api-docs` | OpenAPI de `ms-alquiler` |
| `/compras/v3/api-docs` -> `/api/v3/api-docs` | OpenAPI de `ms-compras` |

## Variables de entorno (todo es configurable)

| Variable | Defecto | Descripcion |
|---|---|---|
| `SERVER_PORT` | `8080` | Puerto del gateway |
| `APP_NAME` | `api-gateway` | Nombre en Eureka |
| `API_CONTEXT_PATH` | `/api` | Context path de los microservicios |
| `SEGURIDAD_SERVICE_ID` | `ms-seguridad` | Nombre del servicio de seguridad en Eureka |
| `ALQUILER_SERVICE_ID` | `ms-alquiler` | Nombre del servicio de alquiler en Eureka |
| `COMPRAS_SERVICE_ID` | `ms-compras` | Nombre del servicio de compras en Eureka |
| `EUREKA_DEFAULT_ZONE` | `http://localhost:8761/eureka/` | URL de Eureka |
| `EUREKA_ENABLED` | `true` | Habilita el registro/descubrimiento |
| `EUREKA_PREFER_IP` | **`false`** | `true` registra por IP de LAN (falla en local por firewall) |
| `LOG_DIR` | `./logs` | Carpeta del archivo de log |
| `LOG_LEVEL_ROOT` | `INFO` | Nivel de log general |
| `LOG_LEVEL_GATEWAY` | `INFO` | Nivel de log de Spring Cloud Gateway |

## Requisito en cada microservicio

El `server` del OpenAPI de cada microservicio debe apuntar al gateway
(`app.openapi.server-url`, por defecto `http://localhost:8080/api`) para que
"Try it out" pase por el gateway.

## Entrega 3: seguridad JWT en el Gateway

El gateway valida el JWT **antes de rutear** (`GatewaySecurityConfig`):

- Público: `/api/auth/login`, `/swagger-ui.html`, `/swagger-ui/**`,
  `/v3/api-docs/**`, los docs `/seguridad|compras|alquiler/v3/api-docs/**`,
  `/actuator/health` y `OPTIONS`.
- Todo lo demás exige `Authorization: Bearer <JWT>` válido (firma + `iss` +
  `exp`). Si falta, está vencido o alterado → **401**; si el rol no alcanza →
  **403**. Ambas respuestas usan el formato `ApiError` (`GatewaySecurityErrorHandler`).

| Variable | Defecto | Descripcion |
|---|---|---|
| `SECURITY_JWT_SECRET` | — | Base64 de 32+ bytes; **el mismo en todos los servicios** |
| `SECURITY_JWT_ISSUER` | `alquiler-eventos-seguridad` | Emisor esperado |
| `SECURITY_JWT_ALGORITHM` | `HS256` | `HS256`, `HS384` o `HS512` |

En Swagger UI, botón **Authorize** y pegar **solo el token** (sin `Bearer`).

> **Documentos compartidos del equipo** (viven en la carpeta del workspace, un
> nivel arriba, junto a todos los repos; no están dentro de cada repo):
> `README-ENTREGA-3.md` (plan y criterios de la Entrega 3),
> `GUIA-PRUEBAS-ENTREGA-3.md` (demo paso a paso),
> `GUIA-ADAPTAR-ENTREGA-3.md` (cómo replicar en `ms-alquiler`) y
> `cambios-para-tobi.md`.

