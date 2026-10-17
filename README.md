# 🎥 Pasa La Peli - Movie Service

Microservicio de gestión de catálogo de películas, cartelera y salas/funciones. Las portadas se guardan en **almacenamiento local** (disco de la EC2, volumen Docker `movie-uploads`).

---

## 🚀 Tecnologías
- **Java 21**
- **Spring Boot 3.3.3**
- **Spring Data JPA / Hibernate**
- **MySQL 8.0**
- **Almacenamiento local de archivos (`LocalStorageService`)**
- **Docker & Dockerfile**

---

## ⚙️ Configuración (`application.yml`)
- **Puerto:** `8082`
- **Base de Datos:** `jdbc:mysql://localhost:3306/pasalapeli_db` (Usuario: `root`, Password: `root`)
- **Almacenamiento local de portadas:**
  - `storage.local.upload-dir`: directorio en disco (default `./uploads/peliculas`; en Docker `LOCAL_UPLOAD_DIR=/app/uploads` con el volumen `movie-uploads`)
  - Las imágenes se sirven como recurso estático en `/uploads/**`

---

## 📡 Endpoints Principales
- `GET /api/movies`: Lista películas (soporta filtro `?busqueda=nombre`).
- `GET /api/movies/{id}`: Detalle de película y sus funciones.
- `POST /api/movies`: Crea película (soporta multipart con archivo `imagen` guardado en disco local, o JSON).
- `PUT /api/movies/{id}`: Actualiza película y portada en el almacenamiento local.
- `DELETE /api/movies/{id}`: Elimina película y su archivo de portada en disco.
- `POST /api/movies/{id}/imagen`: Sube o reemplaza la portada en el almacenamiento local.
- `GET /api/movies/{id}/funciones`: Lista funciones programadas.
- `GET /api/funciones/{id}`: Detalle de función.
- `GET /api/funciones/{id}/disponibilidad`: Consulta cupos disponibles en tiempo real.
- `PUT /api/funciones/{id}/descontar?cantidad=N`: Descuenta entradas de forma atómica (bloqueo pesimista). Retorna **HTTP 409 Conflict** si no hay cupo.
- `POST /api/funciones`: Programa nueva función.

---

## 🛠️ Ejecución Local
```bash
mvn clean package -DskipTests
mvn spring-boot:run
```
O con Docker:
```bash
docker build -t movie-service .
docker run -p 8082:8082 movie-service
```

---

## ☁️ Despliegue CI/CD (GitHub Actions → EC2)

Este repo se despliega **solo a sí mismo** sobre una instancia **EC2 (Ubuntu 24.04)** que ya porta el stack completo. El orquestador vive en el repo [`pasalapeli-database`](https://github.com) (contiene el `docker-compose.yml` global en `/opt/pasalapeli/`).

### Workflow `.github/workflows/deploy.yml`
En cada `push` a `main`:
1. SSH al EC2 (acción `appleboy/ssh-action`).
2. `git pull` del código de `movie-service` en `/opt/pasalapeli/pasalapeli-movie-service`.
3. `docker compose up -d --build movie-service`.
4. Espera el estado `healthy` del contenedor vía `/actuator/health`.

### GitHub Secrets requeridos en este repo
| Secret | Descripción |
|---|---|
| `EC2_HOST` | IP pública del EC2 |
| `EC2_USER` | Usuario SSH (usualmente `ubuntu`) |
| `EC2_SSH_KEY` | Clave privada SSH (.pem) |

### Variables de entorno en producción (definidas en el `.env` del orquestador)
- `SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/pasalapeli_db?...`
- `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`
- `LOCAL_UPLOAD_DIR=/app/uploads` (opcional; default `./uploads/peliculas`). El volumen Docker `movie-uploads` persiste las portadas.

---

## 🔔 Mensajería asincrónica (productor)

Al crear una película, `PeliculaService` publica un evento **`PELICULA_CREADA`** en el topic **AWS SNS** `pasalapeli-events`, que consume el **[Notification Service](https://github.com)** (cola SQS) para avisar a los usuarios.

- El envío ocurre **después de confirmar la transacción** (`@TransactionalEventListener(AFTER_COMMIT)`), por lo que no se publica nada si la transacción revierte.
- El publisher es **best-effort**: si SNS no responde, la operación de negocio no falla (solo se registra el error).
- El atributo de mensaje `tipo` alimenta el FilterPolicy de la suscripción SNS→SQS.
- Clases: `event/EventoNotificacion.java`, `messaging/NotificacionEventPublisher.java`.
- Variables: `SNS_TOPIC_ARN` (salida de `scripts/setup-messaging.sh` del orquestador), `AWS_REGION` (default `us-east-1`). Credenciales via cadena por defecto (IMDS + instance profile en EC2).
