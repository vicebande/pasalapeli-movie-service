# 🎥 Pasa La Peli - Movie Service

Microservicio de gestión de catálogo de películas, cartelera y salas/funciones con integración a **Amazon S3** para almacenamiento multimedia.

Parte del ecosistema Cloud Native **Pasa La Peli** para **Desarrollo Cloud Native I (DSY1107) - Duoc UC**.

---

## 🚀 Tecnologías
- **Java 21**
- **Spring Boot 3.3.3**
- **Spring Data JPA / Hibernate**
- **MySQL 8.0**
- **AWS SDK for Java v2 (Amazon S3 Client)**
- **Docker & Dockerfile**

---

## ⚙️ Configuración (`application.yml`)
- **Puerto:** `8082`
- **Base de Datos:** `jdbc:mysql://localhost:3306/pasalapeli_db` (Usuario: `root`, Password: `root`)
- **Amazon S3:**
  - `aws.s3.enabled`: `true` / `false` (si está en false, usa el almacenamiento local en `./uploads/peliculas`)
  - `aws.s3.bucket-name`: `pasalapeli-portadas`
  - `aws.s3.region`: `us-east-1`
  - `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY`

---

## 📡 Endpoints Principales
- `GET /api/movies`: Lista películas (soporta filtro `?busqueda=nombre`).
- `GET /api/movies/{id}`: Detalle de película y sus funciones.
- `POST /api/movies`: Crea película (soporta multipart con archivo `imagen` hacia S3 o JSON).
- `PUT /api/movies/{id}`: Actualiza película y portada en S3.
- `DELETE /api/movies/{id}`: Elimina película y objeto asociado en Amazon S3.
- `POST /api/movies/{id}/imagen`: Sube o reemplaza portada en Amazon S3.
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
