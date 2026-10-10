# alquiler-eventos-api-gateway

API Gateway (Spring Cloud Gateway) del sistema de alquiler de equipos para eventos
(FIUNI - Sistemas Web y Distribuidos). Es la **unica puerta de entrada**: enruta las
peticiones a `ms-alquiler` y `ms-compras` descubriendolos por nombre en **Eureka**
(`lb://nombre-del-servicio`) y ofrece un **Swagger UI unico** con la documentacion
de ambos microservicios.

## Stack

| Tecnologia | Version |
|---|---|
| Java | 21 |
| Spring Boot | 3.4.5 |
| Spring Cloud (Gateway + Eureka Client) | 2024.0.1 |
| SpringDoc OpenAPI (WebFlux UI) | 2.8.5 |
| Maven | 3.9+ |

## Orden de arranque

1. `alquiler-eventos-eureka` (puerto 8761)
2. `ms-alquiler` (8081) y `ms-compras` (8082)
3. `api-gateway` (8080) con `mvn spring-boot:run`

## URLs

| Que | URL |
|---|---|
| Swagger UI (pruebas de todo el sistema) | <http://localhost:8080/swagger-ui.html> |
| Consola de Eureka | <http://localhost:8761> |

## Rutas (application.yml)

| Ruta en el gateway | Destino (Eureka) |
|---|---|
| `/api/clientes/**`, `/api/ordenes-alquiler/**` | `ms-alquiler` |
| `/api/proveedores/**`, `/api/ordenes-compra/**` | `ms-compras` |
| `/alquiler/v3/api-docs` -> `/api/v3/api-docs` | OpenAPI de `ms-alquiler` |
| `/compras/v3/api-docs` -> `/api/v3/api-docs` | OpenAPI de `ms-compras` |

## Variables de entorno (todo es configurable)

| Variable | Defecto | Descripcion |
|---|---|---|
| `SERVER_PORT` | `8080` | Puerto del gateway |
| `APP_NAME` | `api-gateway` | Nombre en Eureka |
| `API_CONTEXT_PATH` | `/api` | Context path de los microservicios |
| `ALQUILER_SERVICE_ID` | `ms-alquiler` | Nombre del servicio de alquiler en Eureka |
| `COMPRAS_SERVICE_ID` | `ms-compras` | Nombre del servicio de compras en Eureka |
| `EUREKA_DEFAULT_ZONE` | `http://localhost:8761/eureka/` | URL de Eureka |
| `EUREKA_ENABLED` | `true` | Habilita el registro/descubrimiento |
| `EUREKA_PREFER_IP` | `true` | Registrar por IP en vez de hostname |
| `LOG_DIR` | `./logs` | Carpeta del archivo de log |
| `LOG_LEVEL_ROOT` | `INFO` | Nivel de log general |
| `LOG_LEVEL_GATEWAY` | `INFO` | Nivel de log de Spring Cloud Gateway |

## Requisito en cada microservicio

El `server` del OpenAPI de cada microservicio debe apuntar al gateway
(`app.openapi.server-url`, por defecto `http://localhost:8080/api`) para que
"Try it out" pase por el gateway.
