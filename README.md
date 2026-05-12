# Lectuaria Backend Java

Backend de Lectuaria construido con Java 17 + Spring Boot 3.

## Configuración del Entorno

### Requisitos Previos

- Java 17+
- Maven 3.8+
- PostgreSQL 12+

### Configuración Inicial

1. **Copiar el archivo de variables de entorno:**

   ```bash
   cp .env.example .env
   ```

2. **Editar `.env` con tus valores locales:**

   ```properties
   DB_HOST=localhost
   DB_PORT=5432
   DB_NAME=lectuaria
   DB_USER=postgres
   DB_PASSWORD=tu_contraseña
   JWT_SECRET=tu_clave_secreta
   SPRING_PROFILES_ACTIVE=dev
   ```

3. **Crear la base de datos PostgreSQL:**

   ```bash
   createdb lectuaria -U postgres
   ```

### Ejecutar el Proyecto

``` bash
# Simple - usa el perfil dev por defecto
mvn spring-boot:run

# Alternativa - especificar perfil explícitamente
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

El servidor estará disponible en `http://localhost:3000`

### Perfiles Disponibles

- **dev**: Desarrollo local con PostgreSQL
- **test**: Pruebas con H2 en memoria
- **prod**: Producción (requiere variables de entorno específicas)

## Estructura de Carpetas

```text
.
├── docs/
│   └── sprint-01-backend.md
├── src/
│   ├── main/java/com/lectuaria/backend/
│   │   ├── config/
│   │   ├── controller/
│   │   │   ├── auth/
│   │   │   ├── books/
│   │   │   ├── library/
│   │   │   └── zones/
│   │   ├── dto/
│   │   ├── exception/
│   │   ├── model/
│   │   ├── repository/
│   │   ├── service/
│   │   ├── specification/
│   │   └── validation/
│   ├── main/resources/
│   └── test/
└── pom.xml
```

## Dependencias Utilizadas

- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-validation`
- `spring-boot-starter-security`
- `spring-boot-starter-mail`
- `spring-security-crypto` (BCryptPasswordEncoder)
- `spring-boot-starter-test`

## Endpoints Backend

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `PUT /api/auth/me`
- `GET /api/books`
- `GET /api/books/{id}`
- `GET /api/books/isbn/{isbn}`
- `PUT /api/books/{bookId}/rating`
- `GET /api/books/{bookId}/rating`
- `GET /health`
