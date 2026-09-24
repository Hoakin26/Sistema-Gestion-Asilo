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

- **Algoritmo de firma: HS256** (HMAC-SHA256), fijado explícitamente en el código desde YS-48, vía la librería `jjwt` (`io.jsonwebtoken`, ya en el `pom.xml`). Es un algoritmo simétrico: la misma clave (`jwt.secret` en `application.properties`, no versionada) firma y valida el token. Se eligió sobre RS256 (asimétrico, clave pública/privada) porque el backend es el único emisor y validador de tokens — no hay un tercero que necesite verificar el token sin conocer el secreto, así que la complejidad adicional de un par de claves no aporta beneficio real aquí.
- El token incluye como *claims* mínimos: el correo del usuario (subject) y el **nombre del rol** (`RolNombre`, ver sección 3). El rol viaja dentro del token firmado — no se consulta al frontend qué rol dice tener el usuario, porque un token firmado no puede alterarse sin invalidar la firma.
- `jwt.expiration` (28800000 ms = 8 h, un turno; ver sección 10) limita la ventana de uso de un token robado.
- Esto es lo que hace cumplible la regla **RNF-03**: cada endpoint puede confiar en la identidad que trae el token validado por [JwtAuthenticationFilter.java](../gestion-asilo-backend/src/main/java/app/cabezadealgodon/com/gestionasilobackend/security/JwtAuthenticationFilter.java) para decidir qué datos devolver — nunca en lo que el cliente afirme o en lo que la UI oculte.

La clave HMAC debe medir al menos 256 bits para HS256; debe generarse al azar, nunca escribirse a mano (ver sección 10). Si se acorta, la aplicación falla al arrancar en vez de firmar con una clave débil.

## 3. Modelo de roles (RBAC) — decisiones de esta subtarea (YS-40)

- **`RolNombre`** es un `enum` de Java con los 7 roles del sistema (ACT-01 a ACT-07 de CLAUDE.md: Médico General, Administrador de la Fundación, Médico Especialista, Administrador del Asilo, Enfermería, Laboratorio, Farmacia). El criterio de aceptación de YS-37 en Jira solo listaba 6 (omitía Enfermería); se completó a 7 usando CLAUDE.md como fuente de verdad del diseño de roles, para no tener que migrar el esquema más adelante.
- **`Rol` es una entidad JPA propia** (tabla `rol`), no solo un enum embebido directamente en `Usuario`, porque así lo pide la subtarea YS-40 y porque deja espacio para atributos futuros del rol (p. ej. una descripción visible en el frontend) sin tocar la tabla `usuario`.
- El campo `nombre` de `Rol` usa `@Enumerated(EnumType.STRING)` y no `EnumType.ORDINAL`. `ORDINAL` guarda un número (0, 1, 2...) que depende del orden de declaración del enum en el código — si alguien reordena o inserta un valor en medio del enum, los roles ya guardados en la base de datos cambiarían de significado silenciosamente. `STRING` guarda el nombre literal (`"MEDICO_GENERAL"`), es legible directamente en la base de datos y es inmune a ese reordenamiento.
- Los 7 roles se insertan automáticamente al arrancar la aplicación (`config/RolSeeder.java`, un `CommandLineRunner` que solo actúa si la tabla `rol` está vacía) porque son un catálogo fijo del sistema, no datos que un usuario cree desde la UI — no existe (ni existirá) un endpoint para crear/eliminar roles.
- `Usuario.rol` es una relación `@ManyToOne` obligatoria (`optional = false`, `nullable = false`): todo usuario tiene exactamente un rol, según el criterio de aceptación "Asignar un único rol a cada usuario".

## 4. Correo único: defensa en profundidad

El criterio de aceptación "Validar que el correo no esté duplicado" se resuelve en tres capas, no solo una:

1. **Base de datos:** `Usuario.correo` tiene `unique = true` a nivel de columna. Aunque el service tenga un error o se inserte un registro por otra vía, la base de datos rechaza el duplicado.
2. **Service:** valida antes de guardar y lanza `CorreoDuplicadoException`, que se traduce a un 409 con mensaje legible en vez de un error crudo de SQL. Al editar, la comprobación excluye al propio usuario — de lo contrario nadie podría guardar cambios conservando su correo actual.
3. **Manejador de excepciones:** captura además `DataIntegrityViolationException` como red de seguridad. La capa 2 tiene una ventana de carrera (dos peticiones simultáneas con el mismo correo pueden pasar ambas la comprobación antes de que cualquiera inserte), y ahí la restricción de la base de datos es lo único que queda. Sin este manejador, ese caso saldría como 500.

**Normalización.** El service pasa el correo a minúsculas y le recorta los espacios antes de comparar y guardar. La restricción `unique` de PostgreSQL distingue mayúsculas, así que sin esto `Ana@asilo.gt` y `ana@asilo.gt` convivirían como usuarios distintos aunque sean el mismo buzón, y la validación de unicidad sería burlable con solo cambiar una letra de caja.

Un correo con espacios *alrededor* (`" ana@asilo.gt "`) se rechaza con 400 antes de llegar al service, porque `@Email` se evalúa antes que la normalización. Es una decisión consciente: recortar espacios de todo texto entrante también recortaría las contraseñas, alterando en silencio la credencial que el usuario eligió. El formulario del frontend (YS-43) es el lugar correcto para limpiar ese espacio.

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
- **Advertencia — estos endpoints todavía no están restringidos por rol.** Según HU-29 solo el Administrador del Asilo debería poder usarlos; hoy cualquier usuario autenticado puede llamarlos. Esa restricción (`@PreAuthorize`) llega en YS-52/YS-53.

## 6. Configuración de Spring Security (YS-46)

[SecurityConfig.java](../gestion-asilo-backend/src/main/java/app/cabezadealgodon/com/gestionasilobackend/config/SecurityConfig.java) define la cadena de filtros. Cuatro decisiones y su porqué:

- **Sesión *stateless*.** El servidor no guarda estado de sesión: cada petición se autentica por sí sola. Es lo que exige un esquema con JWT y además permite escalar a varias instancias sin sesiones compartidas.
- **CSRF desactivado.** Es contraintuitivo desactivar una protección, pero CSRF defiende de un ataque muy concreto: que un sitio malicioso haga que *el navegador de la víctima* envíe una petición aprovechando la cookie de sesión que ya trae. Esta API no autentica con cookies sino con la cabecera `Authorization`, que un sitio externo no puede fijar — el ataque no aplica. Dejarla activa solo bloqueaba las escrituras: mientras estuvo encendida, `POST`, `PUT` y `PATCH` devolvían 401 porque el `CsrfFilter` corre *antes* del filtro de autenticación y rechazaba la petición cuando el usuario todavía era anónimo.
- **CORS restringido al origen del frontend** (`http://localhost:5173` en desarrollo, configurable con `app.cors.origen-frontend`). Se listan explícitamente los orígenes, métodos y cabeceras permitidos en vez de abrir a `*`, para que ninguna otra página pueda consumir la API desde el navegador de un usuario autenticado.
- **Todo requiere autenticación salvo `/api/auth/**`**, que queda público porque ahí vivirá el login (YS-47): un endpoint de login protegido por autenticación sería imposible de usar.

La autenticación por HTTP Basic que sirvió de puente en YS-46 quedó **eliminada** al entrar el filtro JWT (YS-47): dejarla habilitada mantendría abierta una segunda puerta de entrada que ya nadie usa, y toda puerta que no se vigila termina siendo la que se olvida cerrar.

Como ya no hay un mecanismo de login integrado de Spring (ni Basic ni formulario), una petición sin autenticar no tendría a dónde ser redirigida y Spring respondería 403. Por eso [EntryPointNoAutenticado.java](../gestion-asilo-backend/src/main/java/app/cabezadealgodon/com/gestionasilobackend/security/EntryPointNoAutenticado.java) devuelve un 401 con el mismo formato de error que el resto de la API — 401 ("no sé quién eres") y 403 ("sé quién eres, pero no puedes") significan cosas distintas para el frontend.

### Autenticación contra la base de datos

[UsuarioDetailsService.java](../gestion-asilo-backend/src/main/java/app/cabezadealgodon/com/gestionasilobackend/security/UsuarioDetailsService.java) carga el usuario por correo y lo traduce a lo que Spring Security entiende:

- El rol se expone como autoridad `ROLE_ADMINISTRADOR_ASILO`, `ROLE_ENFERMERIA`, etc. El prefijo `ROLE_` no es decorativo: `hasRole('ENFERMERIA')` lo antepone automáticamente, y sin él las reglas de YS-52 no coincidirían con nada.
- **`activo = false` marca la cuenta como deshabilitada**, así que desactivar a alguien no es solo un cambio cosmético en un listado: le corta el acceso al sistema de inmediato. Verificado — un usuario desactivado recibe 401 aunque su contraseña sea correcta.
- Cuando el correo no existe, el mensaje de error es "Credenciales inválidas", nunca "ese correo no está registrado". Un mensaje que distingue ambos casos permite a un atacante averiguar qué correos son válidos (enumeración de usuarios) y luego concentrar el ataque de contraseñas en esas cuentas.

### El administrador inicial

[AdminInicialSeeder.java](../gestion-asilo-backend/src/main/java/app/cabezadealgodon/com/gestionasilobackend/config/AdminInicialSeeder.java) crea un Administrador del Asilo la primera vez que arranca el sistema con la tabla `usuario` vacía. Resuelve un bloqueo circular: crear usuarios exige estar autenticado, pero sin ningún usuario en la base no hay con qué autenticarse.

Correo por defecto: `admin@asilo.gt`, configurable con `app.admin-inicial.correo` en `application.properties` (archivo no versionado). La contraseña **no tiene valor por defecto en el código** (YS-69, sección 11): se toma de `app.admin-inicial.password` o, si no está configurada, se genera al azar y se muestra una sola vez en el log del primer arranque. En ambos casos debe cambiarse de inmediato con `PUT /api/auth/password`. El seeder corre con `@Order(2)`, después del de roles, porque necesita que el rol `ADMINISTRADOR_ASILO` ya exista.

### Verificación realizada

Probado contra la aplicación corriendo y la base de datos real:

| Prueba | Resultado |
|---|---|
| `GET /api/usuarios` sin credenciales | 401 |
| `GET /api/usuarios` como administrador | 200 |
| `POST /api/usuarios` (crear enfermera) | 201, sin hash en la respuesta |
| Autenticarse con la contraseña de la usuaria recién creada | 200 (BCrypt cifra y verifica bien) |
| `PUT /api/usuarios/2` (cambiar rol) | 200 |
| `PATCH /api/usuarios/2/estado` (desactivar) | 200 |
| Autenticarse con esa usuaria ya desactivada | 401 |
| `GET /api/usuarios/999` | 404 |
| `POST` con correo duplicado | 500 — pendiente de YS-42 |

## 7. Validaciones y manejo de errores (YS-42)

**Validación de entrada con Bean Validation.** Los DTO declaran sus reglas (`@NotBlank`, `@Email`, `@NotNull`, `@Size`) y el controller las activa con `@Valid`. Al declararlas en el DTO, ninguna petición llega al service con datos incompletos: la validación es parte del contrato del endpoint, no una comprobación que alguien pueda olvidar escribir.

Dos detalles con intención:

- **La contraseña acepta entre 8 y 64 caracteres.** El máximo no es arbitrario: BCrypt ignora todo lo que exceda **72 bytes**, así que sin un tope el sistema aceptaría una contraseña larguísima y silenciosamente solo usaría su principio. El límite se fijó en 64 caracteres y no en 72 porque el corte de BCrypt es en bytes, y las tildes y la ñ ocupan dos bytes cada una.
- **`CambioEstadoRequest.activo` es `Boolean` y no `boolean`.** Con el primitivo, un `PATCH` cuyo cuerpo no traiga el campo lo recibiría como `false` y **desactivaría al usuario en silencio**. Con el objeto envuelto más `@NotNull`, esa petición se rechaza con 400.

**Cuerpo de error uniforme.** [ManejadorGlobalExcepciones.java](../gestion-asilo-backend/src/main/java/app/cabezadealgodon/com/gestionasilobackend/exception/ManejadorGlobalExcepciones.java) traduce todas las excepciones a un mismo formato JSON, para que el frontend tenga una sola forma que interpretar:

| Situación | Código |
|---|---|
| Campos inválidos o faltantes | 400, con el detalle campo por campo |
| JSON malformado o valor de rol inexistente | 400 |
| Usuario o rol que no existe | 404 |
| Correo ya registrado | 409 |
| Violación de una restricción de la base de datos | 409 |

Dos criterios de seguridad en ese manejador:

- **No hay un manejador genérico de `Exception`.** Un `@ExceptionHandler(Exception.class)` que devuelva `ex.getMessage()` es una vía típica de fuga de información: expone rutas de clases, consultas SQL y detalles internos ante cualquier fallo inesperado. Sin él, Spring responde un 500 genérico y el detalle queda en el log del servidor, que es donde pertenece.
- **El error de integridad devuelve un mensaje genérico**, no el de la excepción, que incluye el nombre de la tabla y de la restricción violada.

### Verificación realizada

| Prueba | Resultado |
|---|---|
| `POST` con todos los campos vacíos | 400 con los 4 errores de campo |
| `POST` con correo mal formado y contraseña corta | 400 con ambos errores |
| `POST` con correo ya registrado | 409 |
| `POST` con el mismo correo en mayúsculas | 409 (la normalización lo detecta) |
| `POST` con un rol inexistente (`JARDINERO`) | 400 |
| `PATCH .../estado` con cuerpo `{}` | 400, no desactiva |
| `PUT` conservando el correo propio | 200 |
| `PUT` tomando el correo de otro usuario | 409 |
| `GET` de un id inexistente | 404 |
| `POST` válido con correo en mayúsculas | 201, guardado en minúsculas |

Con esto quedan cubiertos los seis criterios de aceptación de HU-29.

## 8. Login y sesión con JWT (YS-47)

`POST /api/auth/login` recibe correo y contraseña y devuelve el token junto con el nombre y el rol del usuario (el frontend necesita el rol para armar su menú en YS-55). A partir de ahí, cada petición lleva el token en `Authorization: Bearer <token>`.

**Cómo se validan las credenciales.** [AuthService.java](../gestion-asilo-backend/src/main/java/app/cabezadealgodon/com/gestionasilobackend/services/AuthService.java) delega en el `AuthenticationManager` de Spring en vez de comparar contraseñas a mano. Eso importa: el `AuthenticationManager` usa el `PasswordEncoder` configurado, compara los hashes en tiempo constante (una comparación normal de cadenas se puede medir para adivinar el hash carácter por carácter) y aplica las comprobaciones de cuenta deshabilitada. El correo se pasa a minúsculas antes de autenticar, porque así se guardó al registrarlo.

**El mismo mensaje para todos los fallos.** Contraseña incorrecta, correo inexistente y cuenta desactivada devuelven exactamente `401 Credenciales inválidas`. Distinguirlos convertiría el login en un buscador de correos válidos, y el caso de la cuenta desactivada es el más delicado: Spring lanza `DisabledException` *antes* de comprobar la contraseña, así que un mensaje específico revelaría que la cuenta existe con solo acertar el correo. El costo es que un empleado desactivado no sabe por qué no entra — se asume que el administrador se lo comunica.

**Por qué el filtro relee el usuario de la base de datos.** El token ya trae el rol firmado, así que el filtro podría confiar en ese claim y ahorrarse una consulta. No lo hace: carga el usuario con `UsuarioDetailsService` en cada petición. La razón es que un token vive 24 horas, y sin esa relectura, desactivar a alguien o quitarle un permiso no surtiría efecto hasta que su token expirara — un empleado despedido conservaría acceso el resto del día. Es el precio de una consulta por petición a cambio de que "desactivar" signifique de verdad "fuera ahora". Verificado: un token emitido y todavía vigente deja de funcionar en cuanto el administrador desactiva a ese usuario.

### Verificación realizada

| Prueba | Resultado |
|---|---|
| Login correcto (correo escrito en mayúsculas) | 200 con token, expiración a 24 h |
| Login con contraseña incorrecta | 401 "Credenciales inválidas" |
| Login con correo inexistente | 401, mensaje idéntico |
| Login de una usuaria desactivada | 401, mensaje idéntico |
| Login con campos vacíos | 400 con el detalle por campo |
| `GET /api/usuarios` con token válido | 200 |
| `GET /api/usuarios` sin token | 401 con el formato de error de la API |
| `GET /api/usuarios` con el token alterado en un carácter | 401 |
| Acceso con HTTP Basic | 401 — la puerta anterior quedó cerrada |
| Token válido usado después de que el admin desactivara a su dueño | 401 |

## 9. Cerrar sesión (YS-68)

`POST /api/auth/logout` requiere token válido y responde 204. Es el único endpoint de `/api/auth` que no es público: cierra la sesión de quien la pide, así que hay que saber quién es.

**El problema de fondo.** Un JWT es válido por sí mismo: el servidor no guarda sesiones, así que no hay nada que "borrar" al cerrar sesión. Si el frontend simplemente elimina el token de su almacenamiento, ese token sigue siendo aceptado por el backend durante las 24 horas que le quedan de vida — y quien lo hubiera copiado antes (de la consola del navegador, de un log, de un equipo compartido del asilo) podría seguir usándolo.

**La solución elegida: una marca de tiempo, no una lista negra.** `Usuario.fechaInvalidacionSesiones` guarda el instante del último cierre de sesión, y el filtro rechaza todo token emitido antes de esa marca. Se descartó la alternativa habitual —guardar los tokens revocados en una tabla— porque esa tabla crece con cada cierre de sesión, obliga a purgar periódicamente los registros ya expirados y agrega una consulta por petición. La marca de tiempo es una sola columna, no crece, y se lee en la consulta que el filtro ya hacía.

Como efecto secundario deseable, cerrar sesión invalida los tokens de **todos** los dispositivos de esa persona, no solo el del navegador actual. Para un asilo donde el personal comparte equipos, es el comportamiento correcto: quien cierra sesión espera quedar fuera, no seguir con la sesión abierta en la computadora de enfermería.

**Por qué el token lleva un claim propio de emisión.** El claim estándar `iat` del JWT solo tiene precisión de segundos. Si alguien cierra sesión y vuelve a entrar dentro del mismo segundo, el token nuevo tendría el mismo `iat` que la marca de invalidación y sería imposible distinguir si se emitió antes o después — con el resultado de que el usuario recibiría un token muerto al nacer y no podría entrar. Por eso [JwtService.java](../gestion-asilo-backend/src/main/java/app/cabezadealgodon/com/gestionasilobackend/security/JwtService.java) agrega el claim `emision` con milisegundos, que es el que se compara. El `iat` estándar se mantiene por convención y para poder inspeccionar el token con herramientas externas.

Un detalle de implementación: el filtro necesitaba la entidad `Usuario` completa (no solo el rol y el estado) para leer esa marca, así que `UsuarioDetailsService` ahora devuelve [UsuarioAutenticado.java](../gestion-asilo-backend/src/main/java/app/cabezadealgodon/com/gestionasilobackend/security/UsuarioAutenticado.java), un `UserDetails` que envuelve la entidad. Eso evita una segunda consulta y, más adelante, le dará a los controllers acceso directo al usuario autenticado para aplicar RNF-03.

### Verificación realizada

| Prueba | Resultado |
|---|---|
| `POST /api/auth/logout` sin token | 401 |
| `POST /api/auth/logout` con token válido | 204 |
| El mismo token después de cerrar sesión | 401 |
| Token de un segundo dispositivo tras cerrar sesión en el primero | 401 |
| Iniciar sesión inmediatamente después de cerrar (5 intentos seguidos) | 200 en los 5 |
| Token anterior al cierre, ya habiendo vuelto a iniciar sesión | 401 |
| Cerrar sesión dos veces con el mismo token | 204 y luego 401 |

## 10. Expiración del token y manejo de secretos (YS-48)

**Contraseñas con BCrypt: ya estaba resuelto.** El título de la subtarea incluye cifrar contraseñas, pero eso quedó hecho en YS-41 (sección 1): el codificador delegante guarda cada hash con el prefijo `{bcrypt}`. Aquí se verificó directamente en la base de datos: los usuarios registrados tienen hashes `{bcrypt}$2a$10$…` de 68 caracteres — prefijo del codificador, versión `2a` del algoritmo y factor de costo 10 (2¹⁰ = 1 024 rondas). Ningún hash carece del prefijo.

**Expiración: 8 horas, un turno.** `jwt.expiration` bajó de 86 400 000 ms (24 h) a 28 800 000 ms (8 h). El criterio es el uso real del sistema: el personal del asilo trabaja por turnos, y enfermería opera en tabletas que se mueven entre dormitorios. Con 24 horas, una tableta olvidada mantenía una sesión válida sobre datos clínicos durante un día completo, a lo largo de tres turnos y al alcance de cualquiera. Ocho horas cubren una jornada completa sin obligar a volver a ingresar a mitad de ella, y acotan a un turno la ventana de uso de un token robado.

La expiración no reemplaza al cierre de sesión (sección 9): quien termina su turno debe cerrar sesión, y eso invalida el token de inmediato. Las 8 horas son la red para cuando eso no ocurre.

No hay tokens de renovación (*refresh tokens*): al vencer, el usuario vuelve a iniciar sesión. Para un sistema de uso interno por turnos es suficiente, y evita almacenar y revocar un segundo tipo de credencial de larga duración.

**Algoritmo fijo en el código.** `generarToken` llamaba a `signWith(clave)` sin indicar algoritmo, y en ese caso jjwt lo elige según el largo de la clave: HS256 desde 256 bits, HS384 desde 384 y HS512 desde 512. Al rotar el secreto por uno más largo, los tokens pasaron a firmarse con HS512 sin que nadie lo decidiera, y esta documentación quedó diciendo HS256. Ahora se llama `signWith(clave, Jwts.SIG.HS256)`: el algoritmo es una decisión explícita y no un efecto secundario del largo del secreto. Una clave de menos de 256 bits sigue haciendo fallar el arranque.

Al validar no hace falta restringir el algoritmo del encabezado. La clave es simétrica, así que cualquier variante HS exige conocer el secreto para producir una firma válida, y jjwt rechaza por defecto los tokens sin firma (`alg: none`).

### Rotación de secretos

Durante esta subtarea se descubrió que `application.properties` estaba versionado desde el commit `90bdd22`, aunque figuraba en `.gitignore`: esa regla no tiene efecto sobre archivos que git ya rastrea. Con él habían llegado al repositorio remoto la contraseña de PostgreSQL y el `jwt.secret` (este último desde `2894d9a`). El repositorio es privado, así que no hubo exposición pública, pero ambas credenciales se dieron por comprometidas:

- `application.properties` se retiró del control de versiones con `git rm --cached`; `application-example.properties` queda como plantilla.
- `docker-compose.yml` dejó de contener la contraseña literal: la lee de un `.env` no versionado, con `.env.example` como plantilla, y se niega a arrancar si falta.
- Se generó un `jwt.secret` aleatorio nuevo. Consecuencia buscada: todo token firmado con el secreto anterior dejó de ser válido.
- Se cambió la contraseña de PostgreSQL con `ALTER USER`. Cambiarla solo en el `.env` no habría bastado: la imagen de Postgres lee `POSTGRES_PASSWORD` únicamente al inicializar un volumen vacío.
- **No se reescribió el historial.** Purgar los commits con `git-filter-repo` cambiaría el identificador de todos ellos y rompería la trazabilidad entre git y las historias de Jira. Con las credenciales rotadas, las que quedan en el historial ya no abren nada. Si el repositorio se hace público, conviene reevaluarlo.

### Verificación realizada

Los tokens de prueba se fabricaron firmándolos directamente con HMAC, para controlar cada campo. Los que comparan vigente contra vencido son idénticos salvo `exp` —mismo `iat`, misma `emision`—, así que el rechazo solo puede deberse a la expiración y no al cierre de sesión de la sección 9. Las pruebas de PostgreSQL se hicieron por la IP del contenedor: por `127.0.0.1` la imagen no pide contraseña y la prueba no discriminaría.

| Prueba | Resultado |
|---|---|
| Token real del login: algoritmo del encabezado | `HS256`, fijado (antes de este cambio salía `HS512`) |
| Token real del login: `exp` − `iat` | 28 800 s = 8 h exactas |
| Token real contra `GET /api/usuarios` | 200 |
| Token vigente, vence en 10 min (control) | 200 |
| El mismo token, vencido hace 1 min | 401 |
| El mismo token, vencido hace 1 s | 401 (sin tolerancia de reloj) |
| Token `HS512` firmado con el secreto actual | 200: los emitidos antes de fijar el algoritmo siguen valiendo hasta vencer |
| Token firmado con el `jwt.secret` filtrado | 401 |
| Token con el contenido alterado después de firmar | 401 |
| Contraseña de PostgreSQL filtrada | Rechazada (una clave inventada, usada como control, también) |
| Contraseña de PostgreSQL nueva | Aceptada; la aplicación arranca y conecta |
| Hashes de la tabla `usuario` | Todos con prefijo `{bcrypt}$2a$10$` |
| Secretos nuevos en archivos versionados, historial de git y log de arranque | Ausentes |

## 11. Cambio de contraseña (YS-69)

**El problema.** El administrador inicial se creaba con una contraseña fija escrita en el código, y no había forma de cambiarla: la edición de usuarios no incluye la contraseña, y configurar otra en `app.admin-inicial.password` no afecta a un administrador que ya existe, porque el seeder solo actúa con la tabla vacía. Cualquier instalación que no la sobrescribiera quedaba abierta para quien leyera el repositorio.

**El endpoint.** `PUT /api/auth/password` permite a un usuario autenticado cambiar su propia contraseña. Recibe `passwordActual` y `passwordNueva`:

- Pide la contraseña actual aunque la petición ya traiga un token válido. Sin ese requisito, quien encontrara una sesión abierta —una tableta olvidada en un dormitorio— podría cambiarla y quedarse con la cuenta.
- La nueva sigue las reglas de YS-42: entre 8 y 64 caracteres, guardada con BCrypt.
- Debe ser distinta de la actual. Si no, el sistema registraría un cambio que no ocurrió, y la operación serviría solo para cerrar las sesiones de otros dispositivos.

**Errores con 400, nunca con 401.** Una contraseña actual incorrecta responde 400 con el error en el campo `passwordActual`, en el mismo formato que las validaciones (sección 7). No reutiliza la `AuthenticationException` de Spring —que el manejador global traduce a 401— porque el interceptor del frontend trata todo 401 como sesión terminada: quien solo se equivocó al escribir quedaría fuera del sistema.

**Sesiones.** El cambio actualiza `fechaInvalidacionSesiones` (el mecanismo de la sección 9), así que todo token emitido antes deja de valer, incluidos los de otros dispositivos. La respuesta trae un token nuevo, emitido después de esa marca, para que quien hizo el cambio siga dentro.

La marca se trunca a milisegundos antes de guardarse. PostgreSQL guarda microsegundos, y al redondearlos la marca podría pasar al milisegundo siguiente; como el filtro compara en milisegundos, el token recién emitido quedaría con fecha anterior a la marca y sería rechazado en cuanto se usara.

**Sin contraseña por defecto en el código.** `AdminInicialSeeder` ya no tiene un valor fijo. Si `app.admin-inicial.password` está configurada, usa esa; si no, genera una de 16 caracteres con `SecureRandom` y la muestra una sola vez en el log del primer arranque. El alfabeto excluye los caracteres que se confunden al copiarlos (`0`/`O`, `1`/`l`/`I`); aun así, 16 caracteres entre 56 posibles dan unos 93 bits de entropía.

El riesgo que queda: esa contraseña permanece escrita en el log. Por eso el aviso pide cambiarla de inmediato, y en una instalación real conviene configurarla antes del primer arranque.

### Verificación realizada

Las pruebas del endpoint se hicieron con un usuario creado para la ocasión y desactivado al terminar (la API no permite borrar usuarios). Las del seeder, con una segunda instancia apuntando a una base de datos temporal vacía, eliminada después: así no se tocó el administrador real.

| Prueba | Resultado |
|---|---|
| Cambio sin token | 401 |
| Contraseña actual incorrecta | 400, error en `passwordActual`; la sesión sigue abierta |
| Nueva igual a la actual | 400, error en `passwordNueva` |
| Nueva de 5 caracteres | 400: «La contraseña debe tener entre 8 y 64 caracteres» |
| Sin `passwordActual` | 400 |
| Cambio correcto | 200 con token nuevo, aceptado de inmediato |
| Token usado para el cambio / token de otro dispositivo | 401 / 401 |
| Login con la contraseña vieja / con la nueva | 401 / 200 |
| 5 cambios seguidos, usando cada token al instante | 5 de 5: el nuevo 200, el anterior 401 |
| Hash guardado | Prefijo `{bcrypt}$2a$10$` |
| Seeder con base vacía y sin `app.admin-inicial.password` | Genera 16 caracteres del alfabeto sin ambiguos y los muestra una sola vez en el log |
| Login con la contraseña generada / con `Admin123*` | 200 / 401 |
| Cambio de la generada con el endpoint | 200; la generada deja de servir |

## 12. Restablecer contraseñas desde administración (YS-71)

**El problema.** El cambio de la sección 11 exige la contraseña actual, así que no sirve para quien la olvidó: esa persona quedaba fuera del sistema sin ninguna vía de regreso, y la pantalla de login prometía una ayuda que el administrador no podía dar.

**El endpoint.** `PUT /api/usuarios/{id}/password` asigna una contraseña temporal a otro usuario. Recibe solo la contraseña nueva, con las reglas de YS-42 (entre 8 y 64 caracteres, guardada con BCrypt), y responde 404 si el id no existe.

**Nadie puede restablecerse a sí mismo.** Este endpoint no pide la contraseña anterior —se usa justamente cuando nadie la sabe—, así que permitirlo sobre la cuenta propia abriría el agujero que la sección 11 cierra: quien encontrara una sesión abierta, como una tableta olvidada en un dormitorio, se apropiaría de la cuenta sin conocer la contraseña. El intento responde 403 indicando la vía correcta, que es el cambio de contraseña propio.

**Por qué 403 y no 400 ni 401.** El frontend trata todo 401 como sesión terminada (YS-70), así que devolverlo aquí expulsaría del sistema a quien solo se equivocó de destinatario. Un 400 sugeriría que los datos enviados están mal, y no lo están. El 403 dice lo que realmente ocurre: la petición es válida y la sesión también, pero esa operación no corresponde sobre ese usuario.

**Sesiones.** Restablecer invalida todas las sesiones del usuario afectado, con el mismo `fechaInvalidacionSesiones` truncado a milisegundos de la sección 11, y no toca las de quien la restablece.

**Pendiente: autorización por rol.** Hoy cualquier usuario autenticado puede llamar a este endpoint, igual que a los demás de `/api/usuarios`; la restricción llega en YS-52/YS-53. Hasta entonces, cualquiera con sesión válida puede restablecer la contraseña de otra persona, incluida la del administrador. Es la misma limitación que ya tenían crear y editar usuarios, pero con contraseñas de por medio conviene decirla con todas las letras: **el sistema no debe usarse con datos reales hasta cerrar HU-31.**

**Lo que queda abierto.** El administrador conoce la contraseña temporal hasta que el usuario la cambie, y nada lo obliga a hacerlo: dos personas comparten una credencial, lo que rompe la trazabilidad de quién hizo cada registro. Para resolverlo se abrió HU-32 (YS-72), que marcará al usuario como obligado a cambiarla en su primer ingreso. La pantalla llegó con YS-73: el administrador restablece desde la lista de usuarios, el sistema sugiere una contraseña sin caracteres ambiguos y la muestra una sola vez para que pueda dictarla o anotarla. La acción no aparece en su propia fila, porque el backend la rechazaría.

### Verificación realizada

Sobre una instancia aparte con una base de datos temporal vacía, eliminada al terminar: así las pruebas no dependieron de la contraseña del administrador real ni dejaron usuarios de prueba en los datos.

| Prueba | Resultado |
|---|---|
| Restablecer sin token | 401 |
| Id inexistente | 404 |
| Contraseña de 5 caracteres | 400, error en `password` |
| Restablecer la propia | 403, y la sesión sigue abierta |
| Restablecer la de otra persona | 200 |
| Sus dos sesiones abiertas | 401 las dos |
| Sesión de quien restableció | Intacta (200) |
| Su contraseña anterior / la temporal | 401 / 200 |
| Ella cambia la temporal por una propia (sección 11) | 200; entra con la suya y la temporal deja de servir |
| Hash guardado | Prefijo `{bcrypt}$2a$10$` |

## 13. Pruebas automatizadas de autenticación (YS-51)

Son las primeras pruebas automatizadas del proyecto. Cubren los tres criterios de la subtarea —login correcto, credenciales inválidas y expiración del token— más los casos sin los cuales esas comprobaciones no significarían gran cosa.

**Contra PostgreSQL de verdad, no una base en memoria.** `PruebaConBaseDeDatos` levanta un contenedor con Testcontainers y le pasa la conexión a Spring. Se eligió así por dos razones: una base en memoria se comporta distinto del motor real, y apuntar a la base de desarrollo obligaría a tenerla encendida y ensuciaría sus datos. El contenedor se arranca una sola vez para toda la ejecución y se destruye al terminar.

El secreto de firma y la contraseña del administrador inicial se fijan en `src/test/resources/application.properties`. Ese archivo sí se versiona: solo firma tokens de una base efímera, y tener valores predecibles es lo que permite que las pruebas se ejecuten en cualquier máquina sin configuración previa.

**La prueba más valiosa es la de los tres mensajes iguales.** Comprueba que contraseña incorrecta, correo inexistente y usuario desactivado devuelvan exactamente el mismo texto (sección 5). Si alguien más adelante hace más específico alguno de esos mensajes creyendo que ayuda al usuario, esta prueba falla y explica por qué: distinguirlos permitiría averiguar qué correos están registrados.

**Los tokens se fabrican, no se esperan.** Para probar la expiración no se espera a que venza uno real: se firma con el mismo secreto y fecha de vencimiento pasada, y se compara contra otro idéntico salvo por ese campo. Así la prueba tarda milisegundos y el rechazo no puede deberse a ninguna otra causa.

Se ejecutan con `./mvnw test` desde `gestion-asilo-backend`, con Docker encendido.

### Verificación realizada

| Prueba | Resultado |
|---|---|
| Login correcto | 200 con token, tipo, correo y rol |
| Correo escrito en mayúsculas | 200 |
| Contraseña incorrecta, correo inexistente y usuario desactivado | 401, y los tres mensajes idénticos |
| Login sin credenciales | 400 con el error en cada campo |
| Token válido / sin token | 200 / 401 |
| Token vencido, contra uno idéntico sin vencer | 401 / 200 |
| Token con el rol alterado tras la firma | 401 |
| Token firmado con otro secreto | 401 |
| Cierre de sesión y reingreso | 204, el token viejo 401 y el nuevo 200 |
| Arranque del contexto de Spring | Levanta con todas sus piezas |

## 14. Autorización por rol (YS-52, YS-53 y YS-82)

Hasta aquí el sistema solo distinguía entre autenticado y no autenticado: cualquiera de los siete roles con un token válido podía listar, crear, editar y desactivar usuarios. HU-31 cierra eso.

### Quién administra usuarios

El Administrador del Asilo (ACT-04), y nadie más. La matriz de roles del diseño asigna "Usuarios y roles" únicamente a su menú. Conviene decirlo explícitamente porque el nombre del Administrador de la Fundación (ACT-02) invita a suponer lo contrario: ese actor atiende solicitudes de referencia y agenda médica, no cuentas del sistema.

| Rol | Módulos que le corresponden |
|---|---|
| Médico General (ACT-01) | Dashboard, Internos, Historial médico, Solicitudes de referencia |
| Administrador de la Fundación (ACT-02) | Dashboard, Solicitudes entrantes, Agenda y asignación, Médicos y disponibilidad |
| Médico Especialista (ACT-03) | Dashboard, Mi agenda del día, Visitas médicas, Historial médico, Órdenes de laboratorio, Recetas |
| **Administrador del Asilo (ACT-04)** | Dashboard, **Usuarios y roles**, Internos (vista administrativa), Cuentas por cobrar, Cuentas por pagar, Donaciones, Gastos, Reportes, Configuración |
| Enfermería (ACT-05) | Dashboard, Internos, Solicitudes asignadas, Citas |
| Laboratorio (ACT-06) | Dashboard, Órdenes pendientes, Registro de resultados |
| Farmacia (ACT-07) | Dashboard, Recetas pendientes, Despacho de medicamentos |

Cambiar la propia contraseña y cerrar sesión son comunes a los siete roles, así que siguen exigiendo solo autenticación.

### Estado actual de los endpoints

| Endpoint | Quién puede |
|---|---|
| `POST /api/auth/login` | Cualquiera, sin token |
| `POST /api/auth/logout` | Cualquier rol autenticado |
| `PUT /api/auth/password` | Cualquier rol autenticado |
| `GET`, `POST`, `PUT`, `PATCH` en `/api/usuarios/**` | Solo ADMINISTRADOR_ASILO |

### Por qué la regla va anotada y no en una lista de rutas

`SecurityConfig` mantiene `anyRequest().authenticated()` como piso, y el permiso concreto se declara con `@EnableMethodSecurity` sobre el controlador. La alternativa —enumerar rutas y roles en la configuración— separa la regla del endpoint que protege: al agregar una ruta hay que acordarse de ir a otro archivo, y si no se hace, el endpoint queda abierto sin que nada lo señale.

Por lo mismo la anotación va **en la clase y no en cada método**. Anotando método por método, olvidar uno lo deja accesible a los siete roles, y ese descuido no produce ningún síntoma visible hasta que alguien lo aprovecha. En la clase, un endpoint nuevo nace protegido y hay que quitarle la restricción a propósito para abrirlo.

La regla vive en una anotación propia, `@SoloAdministradorAsilo`, en vez de repetir la expresión `hasRole(...)`. Así el controlador se lee como un permiso y no como una expresión, y el día que cambie quién administra usuarios es un solo archivo el que cambia.

El nombre del rol viaja como texto dentro de la anotación porque Java solo admite constantes ahí: no hay forma de que el compilador lo ate al enum `RolNombre`. Quien lo renombre debe corregir la anotación, y son las pruebas de acceso por rol las que detectan el olvido.

### Qué destaparon las pruebas existentes

Al aplicar la restricción, tres pruebas de YS-51 empezaron a fallar con 403. No era un defecto: usaban `GET /api/usuarios` como sonda de "este token sirve" y entraban con el usuario de enfermería, que desde ahora no tiene nada que hacer ahí. Se corrigieron para que entren con un Administrador del Asilo, porque lo que les toca comprobar es la autenticación y no la autorización. Aflojar la restricción para que pasaran habría desactivado justo lo que esta historia vino a cerrar.

`AutorizacionTest` comprueba lo contrario y también el contrapeso: que cerrar sesión le siga sirviendo a un rol sin acceso a usuarios. Sin esa comprobación, restringir de más pasaría inadvertido y dejaría a seis de los siete roles sin poder salir del sistema.

### El barrido de los siete roles (YS-82)

Comprobar dos roles no alcanzaba. `AutorizacionTest` recorre ahora los siete con `@EnumSource(RolNombre.class)`, y la expectativa se deriva del propio enum —solo `ADMINISTRADOR_ASILO` entra—, de modo que **un rol nuevo entra solo a la prueba y nace esperando que se le niegue el acceso**. Si la expectativa estuviera en una lista aparte, un rol nuevo quedaría sin cubrir y el descuido no daría ningún síntoma.

Es la única red que tiene esta historia: como el nombre del rol viaja como texto dentro de la anotación, renombrar un valor de `RolNombre` no rompe la compilación y podría dejar la restricción sin efecto en silencio.

El barrido tiene tres partes, y las dos últimas son el contrapeso: además de comprobar que seis roles reciben 403 en la administración de usuarios, comprueba que los siete pueden cerrar sesión y cambiar su propia contraseña. Restringir de más es tan defecto como restringir de menos, y sin esas dos comprobaciones dejaría a seis roles sin poder salir del sistema sin que nada lo advirtiera.

Los usuarios de prueba se rehacen en cada llamada y no una vez al inicio, porque hay pruebas que cambian la contraseña y otras que cierran la sesión: compartiéndolos, el resultado dependería del orden en que JUnit las ejecute.

### Verificación realizada

| Prueba | Resultado |
|---|---|
| Los siete roles piden la lista de usuarios | 200 solo ADMINISTRADOR_ASILO; 403 los otros seis |
| Los siete roles cierran sesión | 204 los siete |
| Los siete roles cambian su propia contraseña | 200 los siete |
| La misma ruta sin token | 401, no 403 |
| Enfermería intenta crear un usuario | 403 |
| Suite completa del backend | 35 pruebas, todas pasan |

## 15. Registro de accesos denegados (YS-54)

Antes de esta subtarea, un rol sin permiso recibía un 403 con el cuerpo vacío y nada quedaba anotado. Las dos mitades del problema son la misma: el sistema no le decía nada a nadie, ni al usuario ni a quien administra.

### Qué se anota y qué no

Cada intento denegado deja una línea de nivel WARN con **quién** (el correo del token), **con qué rol**, **qué intentó** (método y ruta) y **desde qué dirección**. Ejemplo real de las pruebas:

```
WARN  Acceso denegado: enfermera.autorizacion@asilo.gt con [ROLE_ENFERMERIA]
      intentó GET /api/usuarios desde 127.0.0.1
```

Es WARN y no ERROR porque el sistema hizo exactamente lo que debía: merece atención si se repite, no figurar entre las fallas del servicio. Un 403 aislado suele ser alguien que se equivocó de enlace; el mismo 403 repetido es otra cosa, y sin registro no hay forma de distinguirlos, porque el usuario rechazado no reporta nada.

**No se anota el token ni el cuerpo de la petición.** Sería lo más cómodo para diagnosticar, y es justamente por eso que conviene decidirlo de antemano: el archivo de log terminaría guardando credenciales y datos de internos sin ninguna de las protecciones que sí tiene la base de datos.

La dirección sale de la conexión y no de la cabecera `X-Forwarded-For`, que el propio cliente puede escribir: quien estuviera probando dónde puede entrar elegiría qué origen queda anotado. Si el sistema llega a quedar detrás de un proxy inverso habrá que leer esa cabecera, pero confiando solo en la que agregue el proxy.

Se registra en el log de la aplicación y no en una tabla de auditoría. Es la lectura literal de la subtarea y no toca el esquema; si más adelante se quiere un historial consultable que sobreviva a los reinicios, es una subtarea aparte con su entidad y su política de purga.

### Por qué hay dos manejadores

`ManejadorAccesoDenegado` atiende las denegaciones de la cadena de filtros y el `@ExceptionHandler` del manejador global atiende las de `@PreAuthorize`. No es redundancia: la anotación sobre un controlador lanza la excepción dentro del DispatcherServlet, donde Spring MVC la resuelve antes de que llegue al filtro, así que hoy el camino activo es el segundo. El primero es la red para el día en que se agregue una regla por ruta en `SecurityConfig`, que si no saldría como un 403 sin cuerpo sin que nada lo advierta.

Los dos producen la misma respuesta y registran por el mismo componente, así que cuál actúe es indistinto para quien recibe el 403.

### El mensaje

> No tiene permiso para realizar esta acción. Si cree que es un error, consúltelo con el Administrador del Asilo.

No nombra el rol que haría falta, y hay una prueba que lo verifica. Decirlo le iría describiendo el mapa de permisos a quien prueba dónde puede entrar, y al usuario legítimo no le sirve de nada: lo que necesita es a quién acudir, no qué rol le falta.

### Verificación realizada

| Prueba | Resultado |
|---|---|
| El 403 trae `mensaje` y `ruta` en el formato de error de la API | Pasa |
| El mensaje no contiene el nombre del rol necesario | Pasa |
| Cada intento denegado deja su línea WARN con correo, rol, método, ruta y origen | Verificado en la salida de las pruebas |
| Suite completa del backend | 17 pruebas, todas pasan |

## 16. Qué falta

De HU-31 ([YS-39](https://joaquingsuy93.atlassian.net/browse/YS-39)):

- **[YS-81](https://joaquingsuy93.atlassian.net/browse/YS-81):** mostrar en el frontend solo los módulos del rol. Hoy los siete roles ven el menú del Administrador del Asilo, así que seis de ellos ven puertas que el backend ya les cerró. Es lo único que falta para cerrar HU-31.

De HU-32 ([YS-72](https://joaquingsuy93.atlassian.net/browse/YS-72)):

- Forzar el cambio de la contraseña temporal en el primer ingreso, para que el administrador deje de conocerla.
