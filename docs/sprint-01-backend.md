# Sprint 01 - Backend

## Resumen de Avances

### 1. Seguridad y Autenticación

- Se estandarizó el uso de `UserRole` (incluye `ADMIN`, `READER`, `LIBRARIAN`).
- Registro con hash de contraseña (`BCryptPasswordEncoder`).
- Login con emisión de JWT.
- Logout con invalidación de refresh token.
- Endpoints protegidos para ver y editar perfil.

### 2. Perfil de Usuario

- Perfil autenticado (`GET /api/auth/me`).
- Edición de perfil (`PUT /api/auth/me`) con validaciones de negocio.
- Control de restricciones por rol (reader vs bibliotecario).

### 3. Catálogo y Consulta de Libros

- Listado paginado de libros.
- Búsqueda por keywords.
- Filtros por género y autor.
- Consulta por ISBN.
- Top de libros populares y mejor valorados.
- DTOs de resumen y detalle con rating promedio y cantidad de valoraciones.

### 4. Publicación de Libros

- Flujo de publicación de libros con validaciones.
- Integración con APIs externas para enriquecer metadatos:
  - Google Books
  - Open Library

### 5. Bibliotecas

- Gestión de libros asociados a bibliotecas.
- Publicación de libros dentro de biblioteca.
- Validaciones de acceso para flujos de bibliotecario.

### 6. Zonas y Catálogos Base

- Endpoints para catálogo de géneros.
- Endpoints para zonas de residencia/operación.
