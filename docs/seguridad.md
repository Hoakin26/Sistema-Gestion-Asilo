# Seguridad — módulo de usuarios y autenticación

Este documento explica las decisiones de seguridad tomadas al construir el modelo de datos de usuarios (HU-29, [YS-37](https://joaquingsuy93.atlassian.net/browse/YS-37)) y las que se tomarán en las subtareas siguientes del mismo módulo (login JWT, CRUD de usuarios). Se actualiza a medida que avanzan las historias de la Épica 8 (Autenticación y gestión de usuarios).

## 1. Contraseñas: por qué nunca se guardan en texto plano

La entidad `Usuario` ([Usuario.java](../gestion-asilo-backend/src/main/java/app/cabezadealgodon/com/gestionasilobackend/models/Usuario.java)) tiene un campo `passwordHash`, nunca `password`. Guardar contraseñas en texto plano significa que cualquiera con acceso a la base de datos (un respaldo filtrado, una inyección SQL, un empleado malicioso) obtiene las contraseñas reales de todo el personal del asilo — y como la mayoría de personas reutiliza contraseñas, ese daño se extiende fuera del sistema.

**Algoritmo elegido: BCrypt** (`BCryptPasswordEncoder` de Spring Security, ya incluido vía `spring-boot-starter-security` en el `pom.xml`).

- BCrypt es un algoritmo de *hashing lento y con costo ajustable*, diseñado específicamente para contraseñas — a diferencia de SHA-256 o MD5, que son rápidos por diseño (útiles para verificar integridad de archivos) y por eso son inseguros para contraseñas: permiten miles de millones de intentos por segundo en un ataque de fuerza bruta con GPU.
- Incluye **salt automático** por contraseña: dos usuarios con la misma contraseña producen hashes distintos, lo que evita ataques de tablas precalculadas (rainbow tables).
- Tiene un **factor de costo** (work factor, por defecto 10 en Spring) que controla cuántas rondas de hashing se aplican. A mayor costo, más lento es calcular el hash — lo cual es indeseable para el login legítimo (retraso imperceptible) pero muy costoso para un atacante que prueba millones de contraseñas.
- El hasheo real (`passwordEncoder.encode(passwordPlano)`) ocurre en el **service** al registrar o cambiar la contraseña de un usuario (YS-41/YS-42), nunca en el controller ni en la entidad — la entidad solo almacena el resultado.

No se usó Argon2 (más moderno, ganador del Password Hashing Competition 2015) porque BCrypt viene integrado sin dependencias adicionales en Spring Security, es ampliamente auditado, y es más que suficiente para el volumen de usuarios de este sistema (personal de un asilo, no una plataforma masiva).

## 2. Autenticación: JWT

- **Algoritmo de firma: HS256** (HMAC-SHA256), vía la librería `jjwt` (`io.jsonwebtoken`, ya en el `pom.xml`). Es un algoritmo simétrico: la misma clave (`jwt.secret` en `application.properties`, no versionada) firma y valida el token. Se eligió sobre RS256 (asimétrico, clave pública/privada) porque el backend es el único emisor y validador de tokens — no hay un tercero que necesite verificar el token sin conocer el secreto, así que la complejidad adicional de un par de claves no aporta beneficio real aquí.
- El token incluye como *claims* mínimos: el correo del usuario (subject) y el **nombre del rol** (`RolNombre`, ver sección 3). El rol viaja dentro del token firmado — no se consulta al frontend qué rol dice tener el usuario, porque un token firmado no puede alterarse sin invalidar la firma.
- `jwt.expiration` (actualmente 86400000 ms = 24 h) limita la ventana de uso de un token robado.
- Esto es lo que hace cumplible la regla **RNF-03**: cada endpoint puede confiar en el rol que viene en el token validado por el filtro JWT (`security/JwtAuthenticationFilter`, a implementar en la historia de login) para decidir qué datos devolver — nunca en lo que el cliente afirme o en lo que la UI oculte.

## 3. Modelo de roles (RBAC) — decisiones de esta subtarea (YS-40)

- **`RolNombre`** es un `enum` de Java con los 7 roles del sistema (ACT-01 a ACT-07 de CLAUDE.md: Médico General, Administrador de la Fundación, Médico Especialista, Administrador del Asilo, Enfermería, Laboratorio, Farmacia). El criterio de aceptación de YS-37 en Jira solo listaba 6 (omitía Enfermería); se completó a 7 usando CLAUDE.md como fuente de verdad del diseño de roles, para no tener que migrar el esquema más adelante.
- **`Rol` es una entidad JPA propia** (tabla `rol`), no solo un enum embebido directamente en `Usuario`, porque así lo pide la subtarea YS-40 y porque deja espacio para atributos futuros del rol (p. ej. una descripción visible en el frontend) sin tocar la tabla `usuario`.
- El campo `nombre` de `Rol` usa `@Enumerated(EnumType.STRING)` y no `EnumType.ORDINAL`. `ORDINAL` guarda un número (0, 1, 2...) que depende del orden de declaración del enum en el código — si alguien reordena o inserta un valor en medio del enum, los roles ya guardados en la base de datos cambiarían de significado silenciosamente. `STRING` guarda el nombre literal (`"MEDICO_GENERAL"`), es legible directamente en la base de datos y es inmune a ese reordenamiento.
- Los 7 roles se insertan automáticamente al arrancar la aplicación (`config/RolSeeder.java`, un `CommandLineRunner` que solo actúa si la tabla `rol` está vacía) porque son un catálogo fijo del sistema, no datos que un usuario cree desde la UI — no existe (ni existirá) un endpoint para crear/eliminar roles.
- `Usuario.rol` es una relación `@ManyToOne` obligatoria (`optional = false`, `nullable = false`): todo usuario tiene exactamente un rol, según el criterio de aceptación "Asignar un único rol a cada usuario".

## 4. Correo único: defensa en profundidad

El criterio de aceptación "Validar que el correo no esté duplicado" se resuelve en dos capas, no solo una:

1. **Base de datos:** `Usuario.correo` tiene `unique = true` a nivel de columna. Aunque el service tenga un error o se inserte un registro por otra vía, la base de datos rechaza el duplicado.
2. **Service (YS-42, pendiente):** una validación explícita antes de guardar, que devuelve un error de negocio legible (409 Conflict con mensaje claro) en vez de dejar que el usuario vea un error crudo de restricción SQL.

## 5. Qué falta (subtareas siguientes de HU-29)

- YS-41: repositorios (`UsuarioRepository`), `PasswordEncoder` (bean `BCryptPasswordEncoder`), service y controller del CRUD de usuarios.
- YS-42: validaciones de negocio (correo duplicado, campos obligatorios) con mensajes de error consistentes vía `@RestControllerAdvice`.
- Historias de login (Épica 8, fuera de YS-37): `JwtUtil`/`JwtService` para firmar y validar tokens, `JwtAuthenticationFilter`, `UserDetailsService` que carga `Usuario` por correo y expone su `Rol` como `GrantedAuthority`.
