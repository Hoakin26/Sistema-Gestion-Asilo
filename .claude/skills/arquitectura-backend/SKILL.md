---
name: arquitectura-backend
description: Convenciones de capas y estructura de paquetes para el backend Spring Boot del Asilo Cabeza de Algodón. Usar al crear cualquier endpoint, entidad o servicio nuevo.
---

# Arquitectura del backend

Monolito modular en Spring Boot 4 / Java 21, paquete raíz `app.cabezadealgodon.com.gestionasilobackend`.

## Flujo de capas (obligatorio, en este orden)

```
Controller  →  Service  →  Repository  →  (PostgreSQL)
     ↑             ↑
   DTO          Model/Entity
```

- **Controller** (`controllers/`): solo recibe la petición HTTP, valida el DTO de entrada (`@Valid`), llama al service correspondiente y devuelve un DTO de salida con el código HTTP adecuado. Nunca contiene lógica de negocio ni accede al repository directamente.
- **Service** (`services/`): contiene toda la lógica de negocio (reglas, validaciones de dominio, orquestación entre repositorios). Recibe y devuelve DTOs o entidades según el caso; es donde se aplican las reglas de negocio de las historias de usuario (ej. "no se puede registrar un interno sin al menos un familiar responsable").
- **Repository** (`repository/`): interfaces `JpaRepository<Entidad, Long>`, sin lógica — solo consultas (`findBy...`, `@Query` cuando el derivado no alcanza).
- **Model/Entity** (`models/`): clases `@Entity` que mapean tablas. Nunca se exponen directamente en las respuestas de la API — siempre se traducen a DTO.
- **DTO** (`dto/`): un DTO de entrada (`...Request`) y uno de salida (`...Response`) por recurso cuando difieren. Es la única forma en que los datos cruzan la frontera de la API.
- **Exception** (`exception/`): excepciones de negocio propias (ej. `RecursoNoEncontradoException`, `AccesoNoAutorizadoException`) más un `@RestControllerAdvice` central que las traduce a respuestas HTTP consistentes (400/404/403/409) con un cuerpo de error uniforme.
- **Config** (`config/`): `SecurityConfig` (cadena de filtros, reglas de autorización por rol y por ruta), configuración de CORS, beans generales.
- **Security** (`security/`): filtro JWT (`JwtAuthenticationFilter`), utilidad de firma/validación de tokens (`JwtUtil` o `JwtService`), implementación de `UserDetailsService` que carga el usuario y su rol desde la base de datos.

## Reglas de autorización por rol

Toda ruta protegida debe declarar explícitamente qué rol(es) puede acceder, ya sea con `@PreAuthorize("hasRole('...')")` a nivel de método o con reglas en `SecurityConfig`. No basta con que el frontend oculte el menú: el backend debe rechazar la petición si el rol del token no corresponde. Esto es especialmente crítico para la restricción RNF-03 (el Administrador del Asilo no puede leer datos clínicos) — el endpoint de internos debe devolver un DTO distinto (o campos nulos/omitidos) según el rol del solicitante, nunca confiar en que el frontend no los muestre.

## Nombrado

- Entidades en singular, en español o inglés consistente con el resto del código ya escrito (revisar `models/` existente antes de decidir).
- Endpoints REST en plural, en español (consistente con el resto del dominio: entidades, Jira, Notion): `/api/internos`, `/api/usuarios`, `/api/solicitudes-referencia`.
- Cada nuevo endpoint debe poder trazarse a una historia de Jira (comentar el número de HU en el método del controller o del service ayuda a la trazabilidad para la defensa del proyecto).

## Antes de crear un endpoint nuevo

1. Ubicar la historia de usuario y su(s) criterio(s) de aceptación en Jira.
2. Definir el DTO de entrada y de salida primero (qué necesita el frontend, qué debe validarse).
3. Escribir el método de service con la lógica de negocio y sus validaciones.
4. Exponer el controller, delegando todo a services.
5. Si el endpoint expone datos clínicos, confirmar contra la tabla de roles del CLAUDE.md que el rol solicitante tiene permitido verlos.