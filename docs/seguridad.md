# Seguridad — módulo de usuarios y autenticación

Este documento explica las decisiones de seguridad tomadas al construir el modelo de datos de usuarios (HU-29, [YS-37](https://joaquingsuy93.atlassian.net/browse/YS-37)) y las que se tomarán en las subtareas siguientes del mismo módulo (login JWT, CRUD de usuarios). Se actualiza a medida que avanzan las historias de la Épica 8 (Autenticación y gestión de usuarios).

## 1. Contraseñas: por qué nunca se guardan en texto plano

La entidad `Usuario` ([Usuario.java](../gestion-asilo-backend/src/main/java/app/cabezadealgodon/com/gestionasilobackend/models/Usuario.java)) tiene un campo `passwordHash`, nunca `password`. Guardar contraseñas en texto plano significa que cualquiera con acceso a la base de datos (un respaldo filtrado, una inyección SQL, un empleado malicioso) obtiene las contraseñas reales de todo el personal del asilo — y como la mayoría de personas reutiliza contraseñas, ese daño se extiende fuera del sistema.

**Algoritmo elegido: BCrypt**, configurado como bean en [PasswordEncoderConfig.java](../gestion-asilo-backend/src/main/java/app/cabezadealgodon/com/gestionasilobackend/config/PasswordEncoderConfig.java) (Spring Security ya viene vía `spring-boot-starter-security` en el `pom.xml`).

- BCrypt es un algoritmo de *hashing lento y con costo ajustable*, diseñado específicamente para contraseñas — a diferencia de SHA-256 o MD5, que son rápidos por diseño (útiles para verificar integridad de archivos) y por eso son inseguros para contraseñas: permiten miles de millones de intentos por segundo en un ataque de fuerza bruta con GPU.
- Incluye **salt automático** por contraseña: dos usuarios con la misma contraseña producen hashes distintos, lo que evita ataques de tablas precalculadas (rainbow tables).
- Tiene un **factor de costo** (work factor, por defecto 10 en Spring) que controla cuántas rondas de hashing se aplican. A mayor costo, más lento es calcular el hash — lo cual es indeseable para el login legítimo (retraso imperceptible) pero muy costoso para un atacante que prueba millones de contraseñas.
- El hasheo real (`passwordEncoder.encode(passwordPlano)`) ocurre en el **service** al registrar o cambiar la contraseña de un usuario (YS-41/YS-42), nunca en el controller ni en la entidad — la entidad solo almacena el resultado.

No se usó Argon2 (más moderno, ganador del Password Hashing Competition 2015) porque BCrypt viene integrado sin dependencias adicionales en Spring Security, es ampliamente auditado, y es más que suficiente para el volumen de usuarios de este sistema (personal de un asilo, no una plataforma masiva).

**Por qué el bean es `PasswordEncoderFactories.createDelegatingPasswordEncoder()` y no `new BCryptPasswordEncoder()`:** el encoder delegado sigue cifrando con BCrypt (es su algoritmo por defecto), pero guarda el hash con un prefijo que identifica el algoritmo usado, así:

```
{bcrypt}$2a$10$N9qo8uLOickgx2ZMRZoMy...
```

Ese prefijo hace posible **migrar de algoritmo sin invalidar las contraseñas existentes**: si mañana se decide pasar a Argon2, los usuarios nuevos se guardan como `{argon2}...` y los viejos siguen validándose como `{bcrypt}...`, sin obligar a todo el personal del asilo a restablecer su contraseña. Con un `BCryptPasswordEncoder` "pelado" el hash no lleva prefijo y esa migración obligaría a un cambio masivo de contraseñas. Es también la configuración que Spring Security recomienda por defecto.

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

## 5. La API de usuarios (YS-41) — decisiones de seguridad

Endpoints expuestos en `/api/usuarios` ([UsuarioController.java](../gestion-asilo-backend/src/main/java/app/cabezadealgodon/com/gestionasilobackend/controllers/UsuarioController.java)):

| Método | Ruta | Qué hace |
|---|---|---|
| `GET` | `/api/usuarios` | Lista todos los usuarios |
| `GET` | `/api/usuarios/{id}` | Obtiene un usuario |
| `POST` | `/api/usuarios` | Crea un usuario (201) |
| `PUT` | `/api/usuarios/{id}` | Edita nombre, correo y rol |
| `PATCH` | `/api/usuarios/{id}/estado` | Activa o desactiva |

- **La contraseña entra pero nunca sale.** `UsuarioRequest` recibe `password` en texto plano (sobre HTTPS en producción), el service lo convierte inmediatamente a hash con el `PasswordEncoder` y `UsuarioResponse` **no tiene campo de contraseña ni de hash**. Este es el motivo concreto por el que la arquitectura prohíbe exponer entidades JPA directamente: devolver la entidad `Usuario` en un `@RestController` filtraría el `passwordHash` en cada respuesta JSON.
- **No existe `DELETE`.** Los usuarios se desactivan (`PATCH .../estado` con `{"activo": false}`), no se borran. Un usuario que ya registró consultas médicas, recetas o movimientos financieros no puede desaparecer sin romper la trazabilidad de esos registros — y la trazabilidad de quién hizo qué es en sí misma un control de seguridad.
- **El endpoint de edición no cambia la contraseña.** `UsuarioUpdateRequest` solo lleva nombre, correo y rol. Cambiar contraseña es una operación distinta, con reglas propias (confirmar la contraseña actual), y mezclarla con la edición del perfil facilitaría un secuestro de cuenta por parte de quien administra usuarios.
- **Advertencia — estos endpoints todavía no están restringidos por rol.** Según HU-29 solo el Administrador del Asilo debería poder usarlos; esa restricción (`@PreAuthorize`) llega en YS-52/YS-53 junto con el filtro JWT. Mientras tanto quedan bajo la protección por defecto de Spring Boot, que cierra el paso pero **no** distingue roles.

### Por qué YS-46 (configurar Spring Security) es prerrequisito para probar esto

Con la configuración por defecto de Spring Boot, los endpoints de escritura de YS-41 **no se pueden ejercitar todavía**. Comprobado contra la aplicación corriendo:

| Operación | Resultado hoy |
|---|---|
| `GET /api/usuarios` | 200, funciona con HTTP Basic |
| `POST`, `PUT`, `PATCH` | 401, bloqueados |

Dos causas, ambas resueltas por [YS-46](https://joaquingsuy93.atlassian.net/browse/YS-46):

1. **Protección CSRF activa.** Spring Security la habilita por defecto y el `CsrfFilter` corre *antes* del filtro de autenticación, así que una petición sin token CSRF se rechaza cuando el usuario todavía es anónimo — por eso devuelve 401 y no el 403 que uno esperaría. CSRF protege contra peticiones forjadas desde el navegador aprovechando cookies de sesión; una API *stateless* que autentica con un token JWT en la cabecera `Authorization` no usa cookies para autenticar, así que el ataque no aplica y la protección solo estorba. `SecurityConfig` la desactivará explícitamente.
2. **El usuario por defecto queda inutilizable.** Al declarar un bean `PasswordEncoder`, Spring Boot deja de prefijar con `{noop}` la contraseña generada que imprime al arrancar, y el encoder delegado no puede interpretar un hash sin prefijo — el login falla incluso con la contraseña correcta de la consola.

Mientras YS-46 no exista, para probar las lecturas en Postman se puede arrancar con una credencial temporal de desarrollo:

```
./mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--spring.security.user.password={noop}dev123"
```

y autenticar con usuario `user` / contraseña `dev123`. Es solo un apoyo de desarrollo: no debe quedar en `application.properties` ni usarse fuera de la máquina local.

## 6. Qué falta

De HU-29 ([YS-37](https://joaquingsuy93.atlassian.net/browse/YS-37)):

- **YS-42:** validaciones de negocio — correo duplicado (hoy un correo repetido revienta contra la restricción `unique` de la base de datos y devuelve 500 en vez de un 409 legible) y campos obligatorios con Bean Validation (`@NotBlank`, `@Email`, `@NotNull`) más un `@RestControllerAdvice` central que unifique el cuerpo de los errores.

De HU-30 ([YS-38](https://joaquingsuy93.atlassian.net/browse/YS-38)) y HU-31 ([YS-39](https://joaquingsuy93.atlassian.net/browse/YS-39)):

- **YS-46:** `SecurityConfig` — cadena de filtros, sesión *stateless*, CSRF desactivado (innecesario y contraproducente en una API con JWT, donde no se usan cookies de sesión) y CORS para el frontend.
- **YS-47:** endpoint de login que valida credenciales y emite el JWT.
- **YS-48:** expiración del token.
- **YS-52/YS-53:** `@PreAuthorize` por endpoint y protección de los endpoints ya existentes según rol.
- **YS-54:** registro (log) de intentos de acceso denegado.
