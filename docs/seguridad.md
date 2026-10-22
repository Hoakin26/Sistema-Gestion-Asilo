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

## 14. Autorización por rol (YS-52, YS-53, YS-81 y YS-82)

Hasta aquí el sistema solo distinguía entre autenticado y no autenticado: cualquiera de los siete roles con un token válido podía listar, crear, editar y desactivar usuarios. HU-31 cierra eso.

### Quién administra usuarios

El Administrador del Asilo (ACT-04), y nadie más. La matriz de roles del diseño asigna "Usuarios y roles" únicamente a su menú. Conviene decirlo explícitamente porque el nombre del Administrador de la Fundación (ACT-02) invita a suponer lo contrario: ese actor atiende solicitudes de referencia y agenda médica, no cuentas del sistema.

| Rol | Módulos que le corresponden |
|---|---|
| Médico General (ACT-01) | Dashboard, Internos, Historial médico, Solicitudes de referencia |
| Administrador de la Fundación (ACT-02) | Dashboard, Solicitudes entrantes, Agenda y asignación, Médicos y disponibilidad |
| Médico Especialista (ACT-03) | Dashboard, Mi agenda del día, Visitas médicas, Órdenes de laboratorio, Recetas (sin "Historial médico" desde HU-02: ver sección 28) |
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

### El menú por rol en el frontend (YS-81)

El backend ya negaba, pero los siete roles seguían viendo el menú del Administrador del Asilo: seis de ellos tenían a la vista puertas que el sistema ya les había cerrado.

**Lo que no le corresponde al rol no aparece; lo que le corresponde pero aún no está construido sigue en gris.** Son dos mensajes distintos y conviene que no se confundan: el gris dice "llega en una fase posterior" y la ausencia dice "no es de su rol". Un médico no tiene por qué enterarse de que existe un módulo de donaciones.

La matriz vive en `constants/modulos.js`, como un catálogo de módulos más un mapa de rol a módulos. Un catálogo y un mapa en vez de siete listas completas: agregar un módulo o cambiarle el ícono es un solo cambio y no siete que se desincronizan.

**El guardia de las rutas lee esa misma matriz.** Es la parte que importa: ocultar una opción no protege nada si la URL escrita a mano abre igual. `puedeEntrar(rol, ruta)` se resuelve con los mismos datos que arman el menú, así que lo que se muestra y lo que se deja entrar no pueden discrepar. Sigue sin ser la protección real —esa es el backend (YS-53), que responde 403 aunque este guardia se equivoque—, pero evita que el usuario llegue a una pantalla que no le va a responder.

Un rol que este archivo no contemple recibe solo el Dashboard, nunca el menú completo: si algún día llega uno nuevo, el error debe ser de menos y no de más.

Quien escribe a mano una dirección que no le toca ve una pantalla que se lo explica, dentro del esqueleto de la aplicación y con su menú al lado, en vez de una redirección callada que lo dejaría creyendo que el enlace falló. El mensaje no nombra el rol necesario, igual que la respuesta del backend.

**Hizo falta un Dashboard.** Al restringir por rol, seis de los siete se quedaban sin ningún lugar donde aterrizar: el login y la ruta comodín llevaban a `/usuarios`, que ahora es de un solo rol. `/dashboard` es la única ruta que los siete tienen en común según la matriz del diseño, así que es la pantalla inicial de todos. Por ahora no muestra indicadores, porque los datos que resumiría todavía no existen; muestra los módulos construidos del rol, o un aviso de qué sí puede hacer mientras tanto.

### Verificación realizada

| Prueba | Resultado |
|---|---|
| Los siete roles piden la lista de usuarios | 200 solo ADMINISTRADOR_ASILO; 403 los otros seis |
| Los siete roles cierran sesión | 204 los siete |
| Los siete roles cambian su propia contraseña | 200 los siete |
| La misma ruta sin token | 401, no 403 |
| Enfermería intenta crear un usuario | 403 |
| Suite completa del backend | 35 pruebas, todas pasan |
| El menú de cada uno de los siete roles coincide con la matriz del diseño | Verificado sobre `modulosDeRol` |
| Un rol desconocido | Solo Dashboard, sin acceso a `/usuarios` |

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

## 16. Contraseña temporal obligatoria (YS-74)

Cierra el hueco que YS-71 dejó anotado en la sección 12: el administrador quedaba conociendo la contraseña con la que otra persona usa el sistema, y nada obligaba a cambiarla. Mientras eso siga ocurriendo, el historial no puede responder quién hizo cada registro, que es lo que sostiene la trazabilidad de los datos clínicos y financieros.

El diseño completo, con las alternativas descartadas, está en `hu-32-contrasena-temporal.md`.

### Los tres caminos que marcan, y el único que desmarca

Una contraseña que su dueño no eligió puede entrar al sistema por tres puertas, y las tres dejan marcado al usuario:

| Camino | Quién fija la contraseña |
|---|---|
| `UsuarioService.crear` | El administrador, al dar de alta |
| `UsuarioService.restablecerPassword` | El administrador, desde la lista de usuarios |
| `AdminInicialSeeder` | El sistema, al sembrar el primer administrador |

Los criterios de YS-72 solo nombran el segundo. Se incluyeron los otros dos porque producen exactamente el mismo problema, y forzar el cambio tras un restablecimiento pero no tras un alta dejaría el hueco abierto por el camino más transitado. El tercero es además el más expuesto: esa contraseña queda escrita en el log de arranque.

La apaga un solo lugar, `AuthService.cambiarPassword`, y puede hacerlo porque es el único que exige la contraseña anterior: solo su dueño llega hasta ahí. La regla de YS-69 que rechaza una contraseña nueva igual a la actual —escrita entonces por otro motivo— es la que impide "cumplir" el cambio dejando puesta la temporal.

### La columna, y por qué lleva su valor por defecto en la base

`debe_cambiar_password` es `NOT NULL`, y la tabla `usuario` ya tenía filas. Con `ddl-auto=update`, Hibernate emite un `ALTER TABLE ... ADD COLUMN ... NOT NULL` que PostgreSQL rechaza si no se le da un valor por defecto. Por eso el campo declara `columnDefinition = "boolean not null default false"`: las filas existentes se rellenan solas y nacen sin la marca, que es lo correcto, porque ninguna de esas contraseñas la conoce el administrador.

### El bloqueo

Mientras la marca esté puesta, la API responde **403 con `codigo: "CAMBIO_PASSWORD_REQUERIDO"`** a todo salvo el cambio de contraseña —la salida del callejón— y el cierre de sesión. El `codigo` es un campo nuevo y opcional de `ErrorResponse`: existe para que el frontend no tenga que reconocer la situación por el texto del mensaje, que ata su comportamiento a una frase en español que cualquiera puede reescribir sin saber que algo depende de ella.

La regla vive en el backend y no en la pantalla, por lo mismo que en HU-31: si solo la aplicara el frontend, bastaría con escribir otra dirección para seguir trabajando con la credencial que el administrador conoce.

La marca se lee del usuario que el filtro JWT acaba de cargar de la base, no de un claim del token. Así, restablecer la contraseña de alguien que está trabajando surte efecto en su siguiente petición y no cuando expire su token.

### Una trampa de Spring Boot que vale la pena recordar

`CambioPasswordObligatorioFilter` **no** se anota con `@Component`. Spring Boot registra automáticamente todo bean de tipo `Filter` en el contenedor de servlets, de modo que el filtro correría dos veces: una fuera de la cadena de seguridad y otra dentro. Como `OncePerRequestFilter` salta la segunda, la única que se ejecutaría sería la de afuera —donde el usuario todavía no está autenticado— y la comprobación siempre daría "no hay nada que bloquear". El bloqueo quedaría desactivado sin que nada lo advirtiera. Lo construye `SecurityConfig` directamente.

`JwtAuthenticationFilter` sí es `@Component` y está hoy en esa misma situación. Funciona, pero no por diseño; conviene revisarlo aparte.

### Verificación realizada

Contra una instancia desechable con su propia base, para no tocar los datos de desarrollo, más la migración comprobada sobre la base real con siete usuarios existentes.

| Prueba | Resultado |
|---|---|
| Migración sobre la tabla real con 7 filas | Columna creada `not null default false`, los 7 en `false`, sin errores |
| Login del administrador recién sembrado | `debeCambiarPassword: true` |
| Con la marca puesta, `GET /api/usuarios` | 403 con `codigo: CAMBIO_PASSWORD_REQUERIDO` |
| La misma ruta sin token | 401, no 403 |
| Cambiar la temporal por ella misma | 400 |
| Cambiarla por una propia | 200 y `debeCambiarPassword: false` |
| `GET /api/usuarios` con el token nuevo | 200 |
| Usuario dado de alta por el administrador, al entrar | Nace marcado |
| Cerrar sesión estando bloqueado | 204 |
| Suite completa del backend | 35 pruebas, todas pasan |

Las pruebas automatizadas de este flujo son [YS-76](https://joaquingsuy93.atlassian.net/browse/YS-76); lo de arriba es verificación manual.

## 17. La ventana de cambio de contraseña (YS-75)

Es la mitad visible de HU-32, y de paso salda una deuda: `PUT /api/auth/password` existía desde YS-69 pero **no tenía ninguna interfaz**. Hasta ahora nadie podía cambiar su contraseña desde la aplicación, y el Dashboard llegó a ofrecer una opción del menú que no existía.

### Por qué es una ventana flotante y no una pantalla

El primer intento fue una pantalla propia en `/cambiar-password`. Se descartó tras probarla: la versión obligatoria era una tarjeta centrada sobre el fondo vacío, **casi idéntica al login**, y el usuario no percibía que hubiera cambiado de sitio. Quedaba la sensación de que el inicio de sesión no había funcionado.

Como ventana flotante sobre la aplicación, el `<dialog>` nativo comunica justo lo necesario: el fondo visible y atenuado dice "está adentro", y el fondo inerte dice "pero hay algo pendiente". Además la pone en el mismo idioma que el resto de los diálogos del sistema —crear, editar, confirmar—, que es donde el usuario ya espera encontrar una tarea acotada.

Con eso desaparecieron la ruta `/cambiar-password` y el archivo de constantes que había hecho falta para exceptuarla del guardia de módulos.

### Una sola ventana para los dos casos

`CambiarPasswordModal` atiende el cambio obligatorio y el voluntario. Son el mismo formulario, contra el mismo endpoint, con las mismas reglas; separarlos garantizaría que con el tiempo uno corrija un detalle que el otro no.

| | Obligatorio | Voluntario |
|---|---|---|
| Cómo se abre | Sola, al detectarse la marca | Desde el menú de usuario |
| Se puede descartar | No: sin X, sin Escape, sin clic en el fondo | Sí |
| Botones | *Cerrar sesión* y *Cambiar contraseña* | *Descartar* y *Cambiar contraseña* |
| Fondo | El Dashboard, inerte | La pantalla donde estaba |

El modo forzado lo aporta el `prop` `forzado` de `Modal`, que quita las tres salidas. Entonces la única salida la pone el contenido: **una ventana sin botón de salir deja al usuario encerrado**, y por eso la obligatoria lleva *Cerrar sesión* en lugar de *Descartar*.

El aviso dice el motivo y no solo la orden. "Debe cambiarla" a secas se lee como un trámite; saber que otra persona conoce esa contraseña es lo que hace entender por qué no se puede saltar (RNF-04).

### Los tres guardias de `RutaPrivada`, y por qué en ese orden

1. **¿Hay sesión?** Si no, al login. Si se comprobara el permiso primero, alguien sin sesión vería "no tiene permiso" en lugar de que se le pida iniciarla.
2. **¿Falta cambiar la contraseña?** Si sí, al Dashboard, que es la única pantalla que no consulta nada al backend; encima se abre la ventana obligatoria. Va antes del permiso porque un usuario marcado que escribiera una dirección ajena a su rol vería "no tiene permiso" en vez de lo que de verdad le toca hacer.
3. **¿El rol tiene acceso?** Con la matriz de `modulos.js`.

### El destino recordado era de quien salía, no de quien entraba

Un defecto que apareció al probar YS-75 y que venía de YS-70. `RutaPrivada` anotaba la última pantalla para devolver ahí al usuario tras volver a entrar, lo cual sirve cuando la sesión **se cae sola**. Pero también se disparaba al cerrar sesión a propósito, y ese destino quedaba guardado en la ruta del login: **se lo aplicaba al siguiente que entrara, aunque fuera otra persona**. Salir desde "Cambiar contraseña" mandaba ahí al administrador que entraba después, sin que él hubiera pedido nada.

Ahora el destino solo se recuerda cuando la salida no fue voluntaria. `AuthProvider` lo distingue con `salioPorSuCuenta`, del mismo modo que ya distinguía si correspondía explicar el fin de la sesión.

### La marca puede encenderse a mitad de sesión

Ocurre cada vez que un administrador restablece la contraseña de alguien que está trabajando. Para ese caso no alcanza con lo que informa el login: el interceptor de Axios reconoce el `codigo` del 403 y avisa al proveedor de sesión, que enciende la marca y hace que la ventana se abra sola.

Se reconoce por el código y no por el texto del mensaje, que es una frase en español que cualquiera puede reescribir sin saber que algo depende de ella.

### Guardar el token nuevo no es opcional

El backend invalida todos los tokens anteriores al cambiar la contraseña, incluido el que viajó en esa misma petición, y entrega uno nuevo. Si el frontend no lo reemplazara, el usuario quedaría con la sesión caída justo después de cumplir con lo que se le pidió. `AuthProvider` lo guarda en el mismo paso, junto con la marca ya apagada que trae la respuesta.

### Dos cosas que se avisan en lugar de preguntarse

**El cierre de las otras sesiones.** Se evaluó ofrecerlo como casilla opcional y se descartó: es una pregunta cuya respuesta correcta es siempre la misma. Quien cambia su contraseña porque sospecha que alguien más la conoce anula el cambio si deja vivas las otras sesiones; quien la cambia por rutina solo gana no tener que volver a entrar en la tablet. El riesgo es asimétrico, el personal no es experto en tecnología (RNF-04), y en el caso obligatorio habría que ignorar la elección de todos modos. La ventana lo informa como un hecho.

**La confirmación de la contraseña nueva.** Sin segundo campo, igual que en el restablecimiento de YS-73. El campo tiene el ojo para revelar lo escrito, que resuelve el mismo problema sin pedir que se escriba dos veces; y si aun así quedara mal, el administrador puede restablecerla.

### Verificación realizada

| Prueba | Resultado |
|---|---|
| `npm run build` | Pasa |
| `npm run lint` | Solo el aviso preexistente de `UsuariosPage.jsx:73` |
| Sin referencias huérfanas tras quitar la ruta y su archivo de constantes | Verificado |

**El resultado visual no está verificado automáticamente**: el proyecto no tiene pruebas de interfaz, y la extensión de navegador no pudo abrir pestañas en esta sesión. La comprobación es manual.

## 18. Pruebas del flujo de contraseña temporal (YS-76)

`CambioPasswordObligatorioTest` cubre HU-32 de punta a punta, con tokens reales obtenidos del login y contra PostgreSQL en Testcontainers, igual que el resto.

### Por qué los dos usuarios de la prueba son Administradores del Asilo

Es la decisión que hace que estas pruebas sirvan. El bloqueo se comprueba contra `GET /api/usuarios`, que desde YS-53 está reservado a ese rol. Si el usuario bloqueado fuera de enfermería, **su 403 podría venir del guardia de roles** y la prueba quedaría verde sin haber comprobado una sola línea de HU-32. Siendo administrador, el único 403 posible es el del cambio pendiente — y aun así se verifica el `codigo` del cuerpo.

### La prueba que más importa

El circuito entero en un solo caso: el administrador restablece, el usuario entra, choca contra el bloqueo, cambia la contraseña y vuelve a trabajar con el token nuevo. Comprobar cada pieza por separado dejaría pasar el escenario en que todas funcionan y aun así el usuario queda encerrado, que es el riesgo propio de esta historia.

### Se comprobó que las pruebas fallan cuando deben

Una suite que pasa a la primera no demuestra nada por sí sola: puede estar pasando en vacío. Se comentó a propósito la línea de `AuthService.cambiarPassword` que apaga la marca —el error más caro posible, porque dejaría a **todos** los usuarios encerrados para siempre en la ventana obligatoria— y se volvió a ejecutar:

```
CambioPasswordObligatorioTest.elCircuitoCompleto:121
  JSON path "$.debeCambiarPassword" expected:<false> but was:<true>
```

La línea se restauró y se verificó contra git que el archivo quedara idéntico al commit. Las otras seis pruebas siguieron pasando, lo cual es correcto: en ellas la marca ya estaba apagada de antemano, así que quitar esa línea no cambia nada. La única que podía detectarlo es la que la enciende primero.

### Verificación realizada

| Caso | Resultado |
|---|---|
| Restablecer deja marcado al usuario y el login lo informa | Pasa |
| Con la marca puesta, un endpoint de su propio rol responde 403 con `codigo` | Pasa |
| Cerrar sesión sigue disponible estando bloqueado | Pasa |
| Circuito completo: restablecer, entrar, chocar, cambiar, trabajar | Pasa |
| Repetir la contraseña temporal no cumple el cambio y la marca sigue puesta | Pasa |
| Cambiar la contraseña por decisión propia no deja marca | Pasa |
| El alta de usuario también deja marcado | Pasa |
| Suite completa del backend | 42 pruebas, todas pasan |

Con esto **HU-32 queda cerrada**.

## 19. El expediente del interno y el RNF-03 (HU-01)

El Administrador del Asilo entra al expediente del interno —datos personales y familiar responsable— pero jamás a sus datos clínicos. Es la única restricción del sistema que protege información de salud de una persona, y por eso se aplica en tres capas que no dependen unas de otras.

### Capa 1: la entidad no tiene camino hacia lo clínico (YS-57)

`Interno` no tiene un campo `fichaMedica`: la relación la posee `FichaMedica`, que apunta al interno. Para leer un dato clínico hay que pedirlo explícitamente al `FichaMedicaRepository`. Así no existe un campo que alguien pueda olvidar excluir de un DTO, y `InternoResponse`, que también reciben el Administrador del Asilo y enfermería, no puede contener nada clínico por descuido.

### Capa 2: cada endpoint declara quién lo usa (YS-58)

| Endpoint | Anotación | Roles |
|---|---|---|
| `GET /api/internos`, `GET /api/internos/{id}` | `@PersonalConAccesoAInternos` | Médico General, Médico Especialista, Enfermería, Administrador del Asilo |
| `GET /api/internos/{id}/ficha-medica` | `@PersonalClinico` | Médico General, Médico Especialista, Enfermería |
| `POST`, `PUT /api/internos/{id}`, `PUT /api/internos/{id}/ficha-medica` | `@SoloMedicoGeneral` | Médico General |

El controlador no lleva una anotación de clase a propósito: una sola regla para todo serviría de más a unos endpoints o de menos a otros.

### Capa 3: la pantalla ni siquiera pide la ficha (YS-61)

Para el Administrador del Asilo, el perfil del interno no llama a `/ficha-medica` y no monta la sección: no se le oculta, simplemente no existe para él, igual que los módulos que no son de su rol (sección 14). La lista de roles clínicos del frontend (`leeDatosClinicos` en `constants/roles.js`) es copia de la de `@PersonalClinico`; si alguna vez discreparan, la autoridad sigue siendo el backend, que respondería 403.

A la inversa, la cuota mensual es un dato financiero: el formulario del Médico General no la muestra (RNF-02) y el perfil solo la enseña al Administrador del Asilo. Como el `PUT` del expediente reemplaza todos los campos, al editar el formulario reenvía la cuota guardada para no borrarla; la vía propia del Administrador del Asilo para fijarla llega con YS-132.

### Verificación realizada

| Caso | Resultado |
|---|---|
| El Administrador del Asilo recibe 403 al pedir la ficha (`InternoApiTest`) | Pasa |
| Ninguno de los seis roles distintos del Médico General registra, edita ni escribe la ficha: 18 combinaciones (`InternoRecorridoTest`) | Pasa |
| Editar el expediente no toca la ficha médica | Pasa |
| En el navegador, con la cuenta del Administrador del Asilo: el perfil no muestra la ficha y en la red solo aparece `GET /api/internos/{id}`, nunca `/ficha-medica` | Comprobado a mano |
| Con la cuenta de enfermería: la ficha se ve, sin botones de edición | Comprobado a mano |
| Suite completa del backend | 89 pruebas, todas pasan |

## 20. La solicitud de referencia (HU-03)

### El médico sale del token, no del formulario

`SolicitudReferenciaRequest` no tiene campo para el médico que refiere: `SolicitudReferenciaService` lo toma del usuario autenticado. Si viniera en el cuerpo, cualquier Médico General podría crear solicitudes a nombre de otro, y el registro de quién refirió a quién dejaría de valer.

### Cada dato elegido se comprueba contra la base

El DTO valida el formato; el servicio valida el sentido. El interno tiene que existir y estar activo, la especialidad tiene que estar activa y el acompañante tiene que ser un usuario activo **de Enfermería** (RF-03). Si algo no cumple, la respuesta es 400 con el error bajo el nombre de su campo (`CampoInvalidoException`), igual que una validación de formato, para que la pantalla lo pinte junto a él.

### La lista de acompañantes no expone más de lo necesario

El Médico General necesita elegir a un enfermero(a), no administrar usuarios. Por eso `GET /api/enfermeros` es un endpoint propio que devuelve solo id y nombre (`ResumenPersona`), y no un filtro de `/api/usuarios`, que es del Administrador del Asilo y trae correos, roles y estados. El mismo criterio sigue la solicitud: nombra al interno, al médico y al enfermero solo por id y nombre.

### Quién usa cada endpoint

| Endpoint | Anotación | Roles |
|---|---|---|
| `POST /api/solicitudes-referencia` | `@SoloMedicoGeneral` | Médico General |
| `GET /api/especialidades` | `@MedicoGeneralOFundacion` | Médico General, Administrador de la Fundación |
| `GET /api/enfermeros` | `@SoloMedicoGeneral` | Médico General |
| `GET /api/solicitudes-referencia` (HU-05) | `@MedicoGeneralOFundacion` | Médico General, Administrador de la Fundación |

`SolicitudReferenciaTest` barre los seis roles restantes contra la creación y los cinco restantes contra las especialidades: todos reciben 403.

### El correlativo no se repite ni con solicitudes simultáneas

El número (`REF-2026-0001`, reiniciado cada año) sale de una fila por año en `contador_solicitud`, que el servicio bloquea con `select … for update` antes de leerla. Calcular "el máximo más uno" habría dejado que dos solicitudes simultáneas leyeran el mismo máximo. Se comprobó que la prueba lo detecta: sin el bloqueo, de cinco solicitudes creadas a la vez una choca contra la restricción única del número y responde 409.

## 21. El aviso por correo al familiar (HU-04)

### Lo que dice el correo, y lo que no

El aviso lleva lo que pide HU-04 —el número de la solicitud, la especialidad, el estado y la fecha— y **no lleva el motivo de la referencia**. El motivo es un dato clínico (RNF-03), y el correo es un canal que el asilo deja de controlar en cuanto lo envía: puede reenviarse, quedar en un teléfono compartido o leerse en otra pantalla. Quien necesite el detalle lo pide en el asilo. `NotificacionFamiliarTest` comprueba que el motivo no aparece.

Todo lo que viene de la base pasa por `HtmlUtils.htmlEscape` antes de entrar al HTML del correo: un nombre con `<` no puede romperlo ni inyectar nada.

### La consulta de avisos no expone el correo del familiar

`GET .../notificaciones` la usan el Médico General y la Fundación, y devuelve solo el estado, los intentos y las fechas. El correo al que se envió y el detalle técnico del error quedan en la base (`destinatario`, `ultimo_error`) y en el registro del servidor, no en la API.

### Un correo caído no afecta a la solicitud

El aviso se registra en la misma transacción que la solicitud, y se envía después de confirmarla, en otro hilo. Si el servidor de correo no responde (espera máxima de 5 segundos), el aviso queda `FALLIDA` y la solicitud no se entera. Se comprobó con el servidor real: con Mailpit detenido, la solicitud se creó con 201 y el aviso quedó fallido; al levantarlo, el reintento manual lo envió.

### Credenciales del servidor de correo

Ninguna en el código ni en el repositorio. Por defecto el backend apunta a Mailpit en `localhost:1025`, que no pide credenciales ni reenvía nada. Para un servidor real se definen `spring.mail.*` en `application.properties` (no versionado) o como variables de entorno; `application-example.properties` muestra cómo.

## 22. Especialistas y asignación de cita (HU-06)

### Quién usa cada endpoint

| Endpoint | Anotación | Roles |
|---|---|---|
| `/api/especialistas/**` (listar, cuentas disponibles, registrar, editar, disponibilidad) | `@SoloAdministradorFundacion`, en la clase | Administrador de la Fundación |
| `POST /api/solicitudes-referencia/{id}/cita` | `@SoloAdministradorFundacion` | Administrador de la Fundación |
| `GET /api/solicitudes-referencia/{id}/cita` | `@MedicoGeneralOFundacion` | Médico General, Administrador de la Fundación |

`EspecialistaTest` y `CitaTest` barren los seis roles restantes contra el listado y el registro de especialistas y contra la asignación: todos reciben 403. El resto de `/api/especialistas` queda cubierto por la misma anotación de la clase. La pantalla muestra "Asignar cita" solo a la Fundación (`asignaCitas` en `roles.js`), pero eso es comodidad; la autoridad es el backend.

### El servidor valida lo que la pantalla ya filtró

La pantalla solo ofrece como botones las horas libres que le dio el servidor, pero el servidor no confía en eso: la cita tiene que ser futura, caer exactamente en una franja del horario del especialista, con un especialista activo y de la misma especialidad que la solicitud. Las horas que se ofrecen y las que se aceptan salen de la misma clase (`Agenda`), así que la pantalla no puede ofrecer una hora que la asignación después rechace.

### Dos asignaciones a la misma hora no quedan las dos

Asignar bloquea la fila del especialista (`PESSIMISTIC_WRITE`) hasta terminar la transacción: dos asignaciones para el mismo especialista se hacen una tras otra, y la segunda ya ve la cita de la primera y recibe 409. Debajo quedan dos restricciones únicas: una cita por solicitud y una por especialista y hora. `CitaTest` lanza dos asignaciones simultáneas a la misma hora y comprueba que una recibe 201 y la otra 409.

### El correo de la cita no lleva datos clínicos

El segundo aviso al familiar lleva el especialista, la especialidad, la fecha, la hora y el estado. Como el primero, no lleva el motivo de la referencia (RNF-03), y todo lo que viene de la base pasa por `HtmlUtils.htmlEscape`.

### Un orden arbitrario ya no rompe la bandeja

`sort` viene de la URL. Con las consultas `@Query` de la búsqueda (YS-166), un campo inexistente llegaba a Hibernate y respondía 500. Ahora cada listado acepta solo una lista blanca de campos (`OrdenNoPermitidoException`): la bandeja, `fechaCreacion` y `numero`; los internos, `nombreCompleto`, `fechaIngreso` y `dpi`. Cualquier otro campo es 400.

### Las restricciones de los enum siguen al código

Hibernate crea, para cada columna enum, una restricción CHECK con los valores de ese momento, y con `ddl-auto=update` no la vuelve a tocar. Al agregar `CITA_ASIGNADA`, la base de desarrollo siguió aceptando solo `SOLICITUD_CREADA`, y asignar una cita respondía 500 al registrar el aviso. Las pruebas no lo veían porque crean el esquema desde cero. `RestriccionesDeEnum` rehace esas restricciones al arrancar, con los valores que el enum tiene hoy; `RestriccionesDeEnumTest` recrea la restricción vieja y comprueba que queda al día.

## 23. La visita médica (HU-07)

### Quién usa cada endpoint

| Endpoint | Anotación | Roles |
|---|---|---|
| `GET /api/citas/mi-agenda`, `POST /api/citas/{id}/visita`, `GET /api/visitas`, `GET /api/visitas/{id}` | `@SoloMedicoEspecialista`, en la clase | Médico Especialista |
| `GET /api/especialidades/tarifas`, `PUT /api/especialidades/{id}/tarifa` | `@SoloAdministradorFundacion` | Administrador de la Fundación |

La visita es un dato clínico (RNF-03). `VisitaMedicaTest` barre los seis roles restantes, el Administrador del Asilo entre ellos, contra los cuatro endpoints de la visita: todos reciben 403. Contra los de la tarifa barre los seis que no son la Fundación.

### El rol no basta: la cita tiene que ser de quien la pide

Todos los especialistas tienen el mismo rol, así que `@SoloMedicoEspecialista` no distingue a uno de otro. El servicio compara la cuenta del token con la del especialista de la cita: solo quien la tiene asignada inicia la visita, y solo él abre después su detalle. Otro especialista recibe 403 con un mensaje que lo dice (`OperacionNoPermitidaException`), y la solicitud no cambia. La agenda y la lista de visitas se arman a partir de la cuenta del token, nunca de un id que llegue en la petición.

### Una cita, una visita

El servicio rechaza con 409 una cita cuya visita ya se inició, y debajo queda la restricción única de `visita_medica.cita_id` para dos inicios simultáneos. La visita se guarda con `saveAndFlush`, para que ese choque salga dentro de la petición y no al confirmar la transacción.

### Lo que el especialista no ve

`VisitaMedicaResponse` no lleva el costo de la consulta, que es un dato de la cuenta del familiar y de la fundación, no de quien atiende. La tarifa tiene su propio DTO (`EspecialidadTarifaResponse`) y no se agregó a `EspecialidadResponse`, porque esa también la recibe el Médico General al referir.

## 24. Los antecedentes durante la consulta (HU-08)

### Dos permisos distintos para dos lecturas distintas

La ficha médica (alergias, padecimientos, medicamentos habituales) la lee el especialista siempre, con `@PersonalClinico` (YS-58). Los antecedentes, en cambio, se abren **solo al especialista de la visita y solo mientras está activa**, que es lo que pide HU-08. `GET /api/visitas/{id}/historial` comprueba las dos cosas en el servicio:

- Otro especialista recibe 403, aunque tenga el mismo rol.
- Con la visita finalizada, también 403: el acceso termina con la consulta.

El rol lo filtra antes `@SoloMedicoEspecialista`, así que el Administrador del Asilo no llega ni a esa comprobación (RNF-03). `VisitaMedicaTest` cubre los tres casos y suma el endpoint al barrido de los seis roles.

### Los antecedentes se consultan, no se copian

`HistorialMedicoService` arma los antecedentes a partir de las visitas registradas, en el momento en que se piden. No hay una tabla de historial que pueda quedar desactualizada o contradecir a la visita de la que salió (sección 3.4 del diseño). El servicio no decide quién puede verlos: eso lo hace quien lo llama, para que HU-02 lo reutilice con sus propios permisos.

## 25. El diagnóstico y el cierre de la visita (HU-09)

### Quién escribe y cuándo

`PUT /api/visitas/{id}/diagnostico` y `PATCH /api/visitas/{id}/finalizar` están bajo `@SoloMedicoEspecialista`, como el resto de la visita. Además, el servicio exige que la visita sea de quien la pide (403 para otro especialista) y que siga activa: una visita finalizada responde 409 y ya no acepta cambios. `VisitaMedicaTest` lo comprueba y suma los dos endpoints al barrido de los seis roles, con el Administrador del Asilo entre ellos (RNF-03).

### Un cierre, un cargo

Finalizar lee la visita con bloqueo (`PESSIMISTIC_WRITE`) hasta el final de la transacción. Dos "Finalizar" simultáneos se hacen uno tras otro, y el segundo ya la encuentra finalizada y recibe 409, así que la consulta no se cobra dos veces. El cargo se registra dentro de la misma transacción que el cierre (`CargoService` exige una transacción en curso): si algo falla, no queda una visita finalizada sin cargo ni un cargo de una visita abierta.

### Ningún cargo nace vacío

Decidido el 10/10: sin tarifa de consulta, la visita no se finaliza. El diagnóstico se guarda aparte y no se pierde, y el mensaje le dice al especialista que la Fundación debe definir la tarifa. Si la definen después de iniciar la visita, el cierre toma esa. Así la cuenta del familiar (HU-18) nunca suma un cargo sin monto.

## 26. La receta (HU-10)

### Quién escribe la receta y quién la lee

`POST`, `PUT` y `GET /api/visitas/{id}/receta` están bajo `@SoloMedicoEspecialista`, y el servicio exige además que la visita sea de quien la pide (403 para otro especialista). Registrar y corregir exigen también que la visita siga activa (409 si está finalizada); consultar, no: la receta de una visita finalizada se sigue pudiendo leer. `VisitaMedicaTest` suma los tres endpoints al barrido de los seis roles, con el Administrador del Asilo entre ellos (RNF-03).

`RecetaResponse` no lleva el diagnóstico de la visita. Lo va a recibir también la farmacia (HU-15), que para despachar necesita los medicamentos, las dosis y las cantidades, no la razón clínica (mínimo privilegio, sección 5 del diseño).

### Lo que la farmacia ve no cambia a sus espaldas

La receta se corrige completa y solo mientras está pendiente de despacho: en cuanto la farmacia entrega algo (HU-16), el cambio se rechaza con 409. Además, la receta ya lleva la columna `version` (bloqueo optimista de JPA) que pide el diseño para HU-16: cuando exista el despacho, dos escrituras simultáneas sobre la misma receta, de la farmacia o del especialista, no se pisarán; HU-16 agrega el manejo de ese choque como 409. Hoy dos correcciones del mismo especialista ya se hacen una tras otra, porque cada una bloquea la visita.

## 27. Los exámenes de laboratorio (HU-11)

### Quién ordena y quién ve el costo

`GET /api/examenes-laboratorio` y `POST` y `GET /api/visitas/{id}/ordenes-laboratorio` están bajo `@SoloMedicoEspecialista`, y el servicio exige además que la visita sea de quien la pide (403 para otro especialista). Ordenar exige también que la visita siga activa (409 si está finalizada); consultar las órdenes, no. `VisitaMedicaTest` suma los tres endpoints al barrido de los seis roles, con el Administrador del Asilo entre ellos (RNF-03): el nombre de un examen ya dice algo de lo que se sospecha del paciente.

Cada orden guarda el costo que tenía el examen en el catálogo al ordenarlo (RF-08: "registrando el costo para el asilo"). Si el precio del catálogo cambia después, las órdenes ya hechas conservan el suyo. El costo no viaja en las respuestas: el especialista ordena por criterio clínico, y quien lo usa es el laboratorio, que lo confirma al cargar el resultado y lo pasa a la cuenta del familiar (HU-14).

### Un examen, una orden, una sola vez

Cada examen crea su propia orden en `PENDIENTE`, porque el laboratorio avanza cada uno por separado (DE-02). Un examen repetido en la misma petición se rechaza en su fila (400), y uno que ya tiene una orden vigente en la visita, con 409 y su nombre. Ordenar bloquea la visita (`PESSIMISTIC_WRITE`), igual que la receta y el cierre: dos "Ordenar" simultáneos con el mismo examen se hacen uno tras otro, y el segundo recibe 409, así que el examen no se cobra dos veces. Todas las órdenes de una petición se guardan en la misma transacción: si una fila falla, no se crea ninguna.

### El catálogo

Decidido el 13/10: por ahora, una lista base de 16 exámenes frecuentes en adultos mayores con su costo de referencia en quetzales, que agrega `ExamenLaboratorioSeeder`. El seeder solo agrega los nombres que falten, así que un precio corregido a mano en la base no se pisa al reiniciar. Un examen marcado como inactivo deja de aparecer en el catálogo y ya no se puede ordenar (400), sin perder las órdenes que lo usaron.

## 28. El historial médico centralizado (HU-02)

### Quién lo consulta

`GET /api/internos/{id}/historial` está bajo `@SoloMedicoGeneral`, como lo pedía el diseño. Para cada rol que queda fuera hay una razón:

- **Administrador del Asilo:** el RNF-03. El historial es todo dato clínico: diagnósticos, medicamentos y exámenes.
- **Médico Especialista:** HU-08. Ve los antecedentes del paciente solo durante su visita activa, por `GET /api/visitas/{id}/historial`. Un historial abierto fuera de la visita le daría justo lo que ese criterio le niega. Por la misma razón, "Historial médico" salió de su menú (decidido el 14/10).
- **Enfermería:** sigue leyendo la ficha médica (alergias, padecimientos, medicamentos habituales), que es lo que necesita para administrar. El historial de consultas no.

`VisitaMedicaTest` barre los seis roles restantes, con el Administrador del Asilo entre ellos: todos reciben 403.

### Lo mismo que los antecedentes, sin la exclusión

`HistorialMedicoService` arma las dos cosas a partir de lo registrado en las visitas, sin una copia aparte que pueda contradecirlas (sección 24). Los antecedentes de HU-08 dejan fuera la visita en curso, que el especialista está registrando. El historial no deja fuera nada, porque el Médico General no está registrando ninguna: la consulta en curso aparece marcada como tal. Un interno que no existe responde 404, y uno sin consultas, con las listas vacías.

Los padecimientos y los medicamentos habituales no se duplican en el historial: la pantalla pide aparte la ficha médica, con el permiso que ya tenía (`@PersonalClinico`).

### En la pantalla

El guardia de rutas del frontend resuelve los permisos con la misma matriz del menú. Como "Historial médico" ya no está en el menú del especialista ni en el de enfermería, si escriben a mano `/historial-medico/15` ven "No tiene permiso para ver esta página" sin que la pantalla llegue a pedir nada. La autoridad sigue siendo el backend: aunque el guardia se equivocara, el endpoint respondería 403.

## 29. Un error inesperado no cierra la sesión (YS-172)

### El defecto

Encontrado el 13/10 al verificar HU-11 en el navegador. Una clase que el IDE compiló con errores hizo fallar al servidor al ordenar exámenes, y en vez de un mensaje de error la pantalla volvió al login con "Su sesión terminó". Lo escrito en el formulario se perdió.

La cadena era esta:

1. Ningún manejador atendía la falla, así que salía del controlador sin traducir y el contenedor la reenviaba internamente a `/error`.
2. Ese reenvío no pasa por el filtro JWT: como todo `OncePerRequestFilter`, se salta los reenvíos de error. Llegaba sin usuario, y `anyRequest().authenticated()` lo rechazaba con 401.
3. El frontend trata todo 401 como sesión terminada: borra el token y saca al usuario.

Al reproducirlo con una prueba apareció lo mismo en dos casos más, con la sesión válida. Una dirección que no existe (debía ser 404) y un método que no corresponde (405) también salían como 401, porque Spring los resuelve con el mismo reenvío.

### La corrección

- **`ManejadorGlobalExcepciones.manejarNoPrevisto`.** Lo que ningún otro método atiende sale como 500, con el formato de error de la API y un mensaje genérico, sin el detalle de la excepción. La traza completa queda en el log del servidor. Atiende también los `Error` de Java, como el de una clase compilada con errores, porque Spring MVC los entrega envueltos en una excepción. Las excepciones de Spring que ya traen su estado lo conservan: 404 para una dirección que no existe, y 405 con su cabecera `Allow`.
- **`SecurityConfig` deja pasar el reenvío interno a `/error`** (`dispatcherTypeMatchers(DispatcherType.ERROR)`), no la ruta: una petición que alguien escriba a `/error` sigue exigiendo token, porque no es un reenvío. Así, lo que falle fuera de Spring MVC sale con su estado real y no como 401. Por ejemplo, el filtro JWT cuando la base de datos no responde: con la regla anterior, una caída momentánea de la base habría sacado del sistema a cualquiera que hiciera una petición en ese momento.
- **`AtributosDeError`** da al reenvío el mismo formato de error que al resto de la API, para que el frontend encuentre el mensaje también ahí.

El 401 queda para lo que significa: sin token, token inválido o vencido, o sesión cerrada.

### Cómo se prueba

`ErrorInesperadoTest` corre contra un servidor de verdad, en un puerto al azar. El reenvío a `/error` lo hace el contenedor, y MockMvc no lo reproduce: con MockMvc, estas mismas peticiones respondían bien y la prueba no habría visto el defecto.

Un controlador que solo existe en la prueba falla de tres maneras: una excepción, un `Error` de Java y una respuesta de error armada por el contenedor. Para cada una se comprueba el estado, que el mensaje no lleve el detalle interno, que la traza sí quede en el log y que el mismo token siga funcionando en la petición siguiente. También se comprueba que sin token, o con uno inventado, la respuesta siga siendo 401. Antes de la corrección, la prueba fallaba en cuatro de sus cinco casos, todos con 401.

## 30. La bandeja del laboratorio (HU-12)

### Quién la usa y qué ve

`GET /api/ordenes-laboratorio` y `PATCH /api/ordenes-laboratorio/{id}/iniciar` están bajo `@SoloLaboratorio`, puesto sobre todo el controlador. Los especialistas ya ven sus órdenes por la visita (HU-11), y ningún otro rol tiene por qué saber qué exámenes se le piden a un paciente: el nombre de un examen ya dice algo de lo que se sospecha. `VisitaMedicaTest` barre los seis roles restantes en los dos endpoints, con el Administrador del Asilo entre ellos (RNF-03), y todos reciben 403. El rol se revisa antes de buscar la orden, así que un 403 no revela si una orden existe.

Cada orden lleva el paciente, el examen, las indicaciones y el médico que la solicitó, que es lo que pide HU-12. No lleva el diagnóstico ni el costo (mínimo privilegio, `fase-diseno.md` 4.9). El paciente y el médico van como `ResumenPersona`, con el id y el nombre: el DPI sirve para buscar, pero la respuesta no lo devuelve.

### Tomar una orden

DE-02 ya está en `EstadoOrden`, como DE-01 en `EstadoSolicitud`. La orden solo cambia de estado por `OrdenLaboratorio.cambiarEstado`, que rechaza con 409 cualquier paso que el diagrama no contemple. Tomarla la pasa de Pendiente a En Proceso y anota al laboratorista. Tomarla otra vez es 409.

Si dos personas la toman a la vez, la columna `version` que la orden ya tenía desde HU-11 hace que la segunda falle al guardar en vez de pisar a la primera. Ese choque salía como 500; ahora `ManejadorGlobalExcepciones` lo traduce a 409 para cualquier registro con bloqueo optimista, y la receta (HU-16) también se beneficia.

### La búsqueda

Por nombre, sin mayúsculas ni tildes, con el mismo `PatronBusqueda` de las otras listas, o por DPI. Los comodines que escriba el usuario cuentan como texto. El orden por defecto va de la más antigua a la más reciente, con desempate por id: las órdenes de una misma visita comparten la fecha, y sin desempate una orden podía repetirse o perderse al cambiar de página.

## 31. El resultado de laboratorio (HU-13)

### Quién lo carga y quién lo ve

`POST /api/ordenes-laboratorio/{id}/resultado` queda bajo el `@SoloLaboratorio` del controlador. La descarga del archivo, `GET /api/ordenes-laboratorio/{id}/archivo`, lleva su propia anotación, `@LaboratorioOMedico`: decidido el 19/10, el resultado lo ven los médicos además del laboratorio. Esa es la primera barrera; la segunda está en `LaboratorioService`:

- **El Laboratorio y el Médico General** descargan el de cualquier orden: uno lo cargó y el otro lo consulta en el historial centralizado (HU-02).
- **Un especialista**, el de las órdenes que dio él y, mientras tiene una visita activa con el mismo paciente, el de los antecedentes de ese paciente, con la misma regla que HU-08. Fuera de eso, 403.
- **El Administrador del Asilo, Enfermería, la Fundación y Farmacia** reciben 403 antes de que se busque la orden (RNF-03: un resultado de laboratorio es un dato clínico).

El texto del resultado viaja en las mismas respuestas que ya llevaban la orden: la visita del especialista (HU-11), los antecedentes (HU-08) y el historial (HU-02). Ninguna de ellas la recibe un rol que no sea médico.

### El archivo

- **El tipo se reconoce por el contenido.** `TipoArchivoResultado` compara los primeros bytes con las firmas de PDF, PNG y JPEG. La extensión y el tipo que declara el navegador los escribe quien sube el archivo, así que un ejecutable renombrado a `.pdf` se rechaza.
- **10 MB por archivo, en todos los entornos.** Spring Boot usa 1 MB si no se le dice otra cosa, y el `application.properties` de desarrollo no se versiona; `ArchivosConfig` lee las mismas propiedades que `application-prod.properties`, con 10 MB por defecto. El servicio vuelve a revisar el tamaño, y lo que el servidor rechaza antes de llegar a él sale como 413 en el campo `archivo`.
- **Se guarda fuera de la aplicación y con un nombre propio.** `AlmacenArchivos` escribe en una carpeta del disco (en producción, el volumen `asilo_archivos`, que se respalda con la base) con un nombre aleatorio. El nombre original solo se muestra y se usa al descargar; nunca decide dónde se guarda, así que un `../../` no sale de la carpeta.
- **Se descarga, no se abre dentro del sistema.** La respuesta va como `attachment`, y Spring Security agrega `X-Content-Type-Options: nosniff`: el navegador no lo interpreta como parte de la página.
- **Sin archivos huérfanos.** El archivo se guarda antes que la orden; si la orden no se puede guardar (por ejemplo, porque otra persona cargó el resultado al mismo tiempo y el bloqueo optimista lo detuvo), el archivo se borra.

### Un resultado, una vez

La orden pasa a Resultados Cargados por `cambiarEstado`, que solo lo permite desde Pendiente o En Proceso: una orden que ya tiene resultado, o que se anuló, responde 409. El resultado no se corrige después de cargado; HU-13 no lo pide y el especialista ya pudo haberlo leído.

## 32. El costo del laboratorio (HU-14)

Desde RF-08 v1.1, el costo de cada examen lo registra el laboratorio al cargar el resultado, con el costo de referencia del catálogo como sugerencia.

- **Va en la misma petición que el resultado** (`POST .../resultado`) y en la misma transacción: se guarda en la orden y nace el `CargoServicio` de laboratorio de la visita, por `CargoService`, el mismo camino que el cargo de la consulta (HU-09). Si algo falla, no queda ni el resultado ni el cargo.
- **Se cobra una sola vez.** No hay una regla aparte para eso: el resultado solo se carga sobre una orden pendiente o en proceso, y la segunda carga es un 409 antes de llegar al cargo.
- **Mayor que cero, y como lo escribe una persona.** Hasta 8 enteros y 2 decimales, con la coma aceptada como punto; ni notación científica ("1e3") ni signos. Cada caso tiene su mensaje en el campo `costo`.
- **Quién ve el costo.** Decidido el 19/10: solo el laboratorio, y solo en el formulario del resultado y en su detalle, por `GET .../costo` (`@SoloLaboratorio`). La bandeja sigue sin costos, como decidió HU-12, y `OrdenLaboratorioResponse` tampoco los lleva, porque también la recibe el especialista, que ordena por criterio clínico. `VisitaMedicaTest` barre los otros seis roles en `GET .../costo`.

## 33. La bandeja de la farmacia (HU-15)

### Quién la usa y qué ve

`GET /api/recetas` está bajo `@SoloFarmacia`, puesto sobre todo el controlador, como la bandeja del laboratorio. Los especialistas ya ven sus recetas por la visita (HU-10), y ningún otro rol tiene por qué saber qué medicamentos se le recetan a un paciente: el nombre de un medicamento dice tanto como un diagnóstico (RNF-03). `VisitaMedicaTest` barre los seis roles restantes, con el Administrador del Asilo entre ellos, y todos reciben 403.

Cada receta lleva el paciente, el médico que la emitió, la fecha, el estado y los medicamentos con su dosis, cantidad y forma de aplicación, que es lo que la farmacia necesita para despachar. No lleva el diagnóstico de la visita: la farmacia despacha la receta, no la consulta (`fase-diseno.md` 4.9). El paciente y el médico van como `ResumenPersona`, sin el DPI, que sirve para buscar pero no se devuelve. `RecetaResponse` es el mismo DTO que recibe el especialista; ganó esos dos campos y nada más.

### La búsqueda y el filtro

Por nombre, sin mayúsculas ni tildes, o por DPI, con el mismo `PatronBusqueda` de las otras bandejas: un comodín que escriba el usuario cuenta como texto, y la prueba lo comprueba con "%". El filtro solo acepta los estados de DE-03; cualquier otro, como "DESPACHADA", que es como lo llama HU-16 (hallazgo 1 del diseño), es 400. El orden por defecto va de la más antigua a la más reciente, con desempate por id.

Los medicamentos no se traen en la misma consulta que la página: con una colección, Hibernate tendría que traer todas las recetas para cortar la página en memoria. Llegan por lotes (`@BatchSize`), en una consulta más para toda la página.

## 34. El despacho de la farmacia (HU-16)

### Quién despacha

`POST /api/recetas/{id}/despacho` hereda `@SoloFarmacia` del controlador. `VisitaMedicaTest` barre los otros seis roles, con el Administrador del Asilo entre ellos, y todos reciben 403; el rol se revisa antes de buscar la receta, así que un 403 no revela si existe. Cada despacho anota a quien lo hizo y cuándo, y cada medicamento entregado, su fecha.

### Nada se entrega dos veces

Es el criterio de HU-16 ("que no se despache dos veces la misma receta") y se aplica en tres niveles:

- **La receta.** El estado solo cambia por `Receta.cambiarEstado`, que respeta DE-03 como la orden respeta DE-02. Una receta Entregada ya no admite despachos: 409.
- **Cada medicamento.** Una receta incompleta sí se vuelve a despachar (el diagrama del análisis la completa "cuando llega inventario"), pero solo con lo que sigue pendiente: un medicamento ya entregado es 409, y la petición se rechaza entera, sin entregar a medias el resto.
- **Dos personas a la vez.** La receta se lee con `OPTIMISTIC_FORCE_INCREMENT`: su versión sube en cada despacho aunque solo cambien los medicamentos, así que dos despachos simultáneos, o un despacho y una corrección del especialista, chocan siempre y el segundo recibe 409 en vez de entregar dos veces. Sin ese bloqueo, marcar un medicamento no tocaba la fila de la receta y el choque podía pasar inadvertido.

### Lo que no se entrega

Un medicamento que no va en la lista queda pendiente; así se marca uno sin existencias (CU-005, E.1). Si en el primer despacho no se entrega ninguno, la receta queda en Medicamento Agotado, sin fecha de despacho, y sigue en la bandeja de la farmacia. Un despacho posterior sin entregas no cambiaría nada y se rechaza (400). HU-17 cobra solo lo entregado (CU-005, E.2).

## 35. El costo de la farmacia (HU-17)

- **Va en la misma petición que el despacho** y en la misma transacción: cada medicamento entregado guarda su costo y genera su `CargoServicio` de farmacia por `CargoService`, el camino del cargo de la consulta (HU-09) y del laboratorio (HU-14). Si algo falla, no queda ni la entrega ni el cargo.
- **Un cargo por medicamento entregado**, como el laboratorio tiene uno por examen. Lo pendiente no se cobra (CU-005, E.2), y como nada se entrega dos veces (sección 34), nada se cobra dos veces.
- **Las mismas reglas que el costo del laboratorio.** Mayor que cero, hasta 8 enteros y 2 decimales, con la coma aceptada como punto, sin notación científica ni signos. Las dos validaciones viven en un solo lugar (`Montos`), con los mismos mensajes, y un error sale en la fila del medicamento.
- **Quién ve el costo.** Solo la farmacia, al despachar y en el detalle de la receta, por `GET /api/recetas/{id}/costos` (`@SoloFarmacia`). `RecetaResponse` no lo lleva porque también la recibe el especialista, que receta por criterio clínico, como se decidió para el laboratorio el 19/10. `VisitaMedicaTest` barre los otros seis roles en `GET .../costos`. El cargo del familiar dice "farmacia" y el monto, nunca el nombre del medicamento (`fase-diseno.md` 4.9).

## 36. La cuenta del familiar (HU-18)

### Quién la ve

`GET /api/cuentas`, `POST /api/cuentas/consolidar` y `GET /api/internos/{id}/estado-cuenta` están bajo `@SoloAdministradorAsilo`, puesto sobre todo `CuentaFamiliarController`, y `PUT /api/internos/{id}/cuota` también. Es el que cobra. Ningún rol clínico tiene por qué saber lo que paga una familia, y la fundación cobra al asilo, no a cada familia (eso es RF-13). `VisitaMedicaTest` barre los otros seis roles en los cuatro endpoints, y todos reciben 403.

### Lo que dice el estado de cuenta, y lo que no

Es la vista financiera de un dato clínico: cada cargo salió de una consulta, un examen o un medicamento. Por eso va por tipo de servicio y nunca por nombre (`fase-diseno.md` 4.9): una línea que dijera "Consulta de Psiquiatría" o el nombre de un antipsicótico revelaría el padecimiento del interno sin mostrar un diagnóstico, y el Administrador del Asilo no puede ver datos clínicos (RNF-03). Cada cargo lleva el tipo, la fecha, el número de solicitud y los montos; la cuenta, el familiar a cargo y los totales. La prueba lo comprueba buscando en la respuesta el examen, el medicamento (uno que delata un padecimiento), el diagnóstico y la especialidad: no aparece ninguno.

### Consolidar no duplica

La cuenta es una por interno y mes, con una restricción única en la base. Consolidar otra vez el mismo mes recalcula la misma cuenta con los cargos que tenga en ese momento, y cada cargo apunta a una sola cuenta. Mientras no haya pagos recalcular no le quita nada a nadie; lo que pasa al reconsolidar una cuenta que ya tiene pagos lo decidió HU-34 (sección 38). La cuenta guarda el familiar y la cuota con que se cobró, para que un cambio posterior no reescriba lo ya cobrado.

### La cuota tiene un solo dueño

Desde HU-18 la cuota la fija el Administrador del Asilo. El `PUT /api/internos/{id}` del Médico General ya no la toca. Su formulario devolvía la cuota que tenía al abrirse, así que guardarlo después de que el administrador la cambiara la regresaba al valor viejo sin que nadie lo notara. La prueba lo cubre: el Médico General edita con otra cuota y la del administrador se mantiene.

## 37. Los descuentos de la fundación (HU-19)

### Quién los define y quién los ve

`PUT /api/politicas-descuento/{tipoServicio}` está bajo `@SoloAdministradorFundacion`: el descuento lo da la fundación, y es ella quien decide cuánto. `GET /api/politicas-descuento` está bajo `@AsiloOFundacion`, porque el Administrador del Asilo los ve aplicados en cada cuenta y tiene que poder explicárselos al familiar, pero no los cambia. `VisitaMedicaTest` comprueba que los otros seis roles, el Administrador del Asilo incluido, reciben 403 al definir, y que los cinco roles que no administran reciben 403 al consultar. Cada versión anota quién la definió.

### Un solo lugar donde se resta

El descuento solo lo calcula `CalculadoraDescuento`, con la regla del tipo (`DescuentoPorcentaje` o `DescuentoMontoFijo`, patrón Estrategia). Lo usan el cargo al nacer (`CargoService`) y la consolidación del mes (`CuentaFamiliarService`), así que las dos llegan siempre al mismo resultado. Una regla nueva es una clase nueva y la calculadora no cambia (principio abierto/cerrado).

### El neto nunca es negativo

Un descuento mayor que el costo se limita al costo: el cargo queda en cero y el descuento guardado es el que de verdad se aplicó, para que original menos descuento cuadre con el neto en el estado de cuenta. Un porcentaje no pasa de 100 ni un valor baja de cero (400). La cuota base del asilo no lleva descuento: CU-006 lo aplica "a los servicios médicos".

### Por fecha y versionado

Decidido el 20/10. Cada política rige desde su fecha hasta que empieza la siguiente del mismo servicio, y a cada cargo le toca la vigente el día del servicio. Cambiar un descuento agrega una versión nueva en vez de reescribir la anterior: lo que rigió antes queda escrito, y un mes ya consolidado solo cambia si se vuelve a consolidar con una versión que ya regía esas fechas. Una cuenta con pagos no vuelve a calcular los descuentos de lo que ya cobró (HU-34, sección 38).

## 38. Los pagos del familiar (HU-34)

### Quién cobra

`POST /api/cuentas/{id}/pagos`, `GET /api/cuentas/{id}/pagos` y `POST /api/pagos/{id}/anulacion` están en `CuentaFamiliarController`, bajo el mismo `@SoloAdministradorAsilo` de la sección 36. `VisitaMedicaTest` barre los otros seis roles en los tres y todos reciben 403. La prueba manda cuerpos válidos, porque la validación corre antes que la seguridad del método: con un cuerpo inválido respondería 400 sin llegar a comprobar el rol. El historial no lleva nada clínico: monto, fecha, forma de pago, referencia y quién lo registró.

### El pago no pasa del saldo

Que el monto sea mayor que cero y tenga dos decimales lo revisa `PagoRequest`; que no pase del saldo, `PagoService`, con el saldo leído en la misma transacción. Una cuenta ya pagada completa no recibe más pagos (409). La cuenta tiene `@Version`: dos pagos simultáneos que juntos pasarían del saldo no se suman los dos, el segundo recibe 409 y tiene que volver a leer el saldo. El saldo y el estado no tienen setter: solo cambian con `registrarPago`, `anularPago` y la consolidación, así que nadie los pone a mano.

### Anular no borra

Decidido el 20/10. Un pago mal registrado se anula con un motivo obligatorio, y queda en el historial tachado, con quién lo anuló y cuándo. Su monto vuelve al saldo y el estado se recalcula sin él. Borrarlo dejaría un hueco en lo cobrado sin explicación, justo lo que un control de caja tiene que poder rastrear. Un pago anulado no se anula dos veces (409).

### Lo cobrado se congela

Decidido el 20/10. Al reconsolidar una cuenta con pagos, los cargos que ya estaban en ella conservan su monto y su descuento, y la cuota y el familiar se quedan como estaban. Solo se suman los cargos que llegaron después. Así un cambio de cuota o un descuento nuevo no reescribe lo que el familiar ya pagó; si entra un cargo nuevo, el saldo crece y una cuenta cancelada vuelve a Pago Parcial.

### La mora la pone el calendario

Decidido el 20/10. La cuenta vence al final del mes siguiente a su período: la de octubre, el 30 de noviembre. Pasada esa fecha, con saldo, está En Mora; un pago parcial no la saca y el del total sí. El estado no se cambia a mano. `CuentaMensual.estadoAl` lo calcula con el saldo, lo pagado y la fecha, y cada lectura lo recalcula con la fecha del día, porque la mora llega sin que nadie toque la cuenta. `CuentaMensualTest` cubre los bordes: el día en que vence, el siguiente, el cambio de año y febrero bisiesto.

## 39. Las cuentas por pagar a la fundación (HU-20)

### Quién las ve

`GET /api/cuentas-por-pagar`, el historial y el registro de pagos de cada deuda, y `POST /api/pagos-fundacion/{id}/anulacion` están en `CuentaPorPagarController`, bajo `@SoloAdministradorAsilo`: es el que lleva las finanzas del asilo y el que le paga a la fundación. `VisitaMedicaTest` barre los otros seis roles en los cuatro endpoints y todos reciben 403, con cuerpos válidos por la misma razón de la sección 38. El Administrador de la Fundación tampoco entra; así lo pide la historia (YS-141).

### Lo que dice la deuda, y lo que no

La deuda sale de los mismos cargos que la cuenta del familiar, pero agrupados por mes y tipo de servicio: cuántos servicios hubo y cuánto suman. No lleva pacientes, ni exámenes, ni medicamentos (RNF-03). La prueba lo comprueba buscando en la respuesta el examen, el medicamento, el diagnóstico y el nombre del interno: no aparece ninguno.

### Lo adeudado no se puede editar

Decidido el 21/10: lo adeudado es siempre la suma del neto de los cargos, y no se guarda. Nadie puede escribir a mano cuánto se le debe a la fundación; solo cambia cuando cambian los cargos. Se guarda lo pagado, en una fila por mes y servicio que nace con el primer pago, con restricción única y `@Version`: dos pagos simultáneos no pasan juntos del saldo, y dos primeros pagos simultáneos tampoco crean dos filas (el segundo recibe 409). Los pagos se anulan con motivo y no se borran, como los del familiar.

### Si lo adeudado baja después de pagar

Si la fundación define después un descuento que ya regía ese mes, al reconsolidar la cuenta del familiar el neto de los cargos baja, y con él lo adeudado. El saldo nunca queda negativo: la deuda queda Pagada con lo pagado a la vista, y la diferencia se corrige anulando el pago y registrando el correcto.

## 40. Las citas pendientes de pago (HU-21)

### Quién las ve

`GET /api/servicios-pendientes` está en `ServicioPendienteController`, bajo `@SoloAdministradorAsilo`, como la cuenta del familiar de la que sale. `VisitaMedicaTest` barre los otros seis roles y todos reciben 403. En la pantalla es una pestaña de "Cuentas por cobrar", así que no agrega una entrada al menú: el menú de cada rol sigue siendo el del diseño.

### Lo que dice cada cita, y lo que no

Es otra vista financiera de datos clínicos: cada cita es una visita médica. Por eso, como el estado de cuenta (sección 36), va por tipo de servicio y nunca por nombre: "1 consulta · 2 exámenes · 1 medicamento", con sus montos, y sin la especialidad, los exámenes, los medicamentos ni el diagnóstico (RNF-03). La prueba busca en la respuesta el examen, el medicamento, el diagnóstico y la especialidad, y no aparece ninguno. El número de solicitud sí va, como en el estado de cuenta, para ubicar la cita sin decir de qué fue.

### Lo pagado no se inventa

Decidido el 21/10: el familiar paga la cuenta del mes entera, y lo pagado se reparte entre sus citas en proporción. Nada de eso se guarda: sale cada vez de lo pagado de la cuenta y del neto de cada cargo, así que el listado no puede contradecir a la cuenta. Una cita sale del listado solo cuando sus cuentas quedan pagadas completas, como dice RF-22 ("los servicios quedan pendientes hasta cubrir el total"). La búsqueda por paciente escapa los comodines de `LIKE`, como las bandejas del laboratorio y la farmacia.

## 41. Las donaciones (HU-22)

### Quién las registra

`POST /api/donaciones`, el histórico y la anulación están en `DonacionController`, bajo `@SoloAdministradorAsilo`: las donaciones son ingresos del asilo, y es él quien lleva sus finanzas. Su prueba, `DonacionTest`, barre los otros seis roles en los tres endpoints y todos reciben 403, con cuerpos válidos por la razón de la sección 38.

### Un ingreso no se borra

Como los pagos (sección 38), una donación no se edita ni se borra: si se registró mal, se anula con un motivo obligatorio y queda en el histórico tachada, con quién la anuló y cuándo. Deja de contar en los totales del histórico y, cuando exista, en el reporte de entradas (RF-20b). Así lo que se recibió y lo que se corrigió quedan escritos.

### El monto se valida en los dos lados

Mayor que cero, con hasta dos decimales y hasta diez enteros (`numeric(12,2)`, como pide el diseño para los montos), y la fecha no puede estar en el futuro. El formulario repite las mismas reglas para avisar antes de enviar, pero la que cuenta es la del backend (`DonacionRequest`).

## 42. Los gastos operativos (HU-23)

### Quién los registra

`POST /api/gastos-operativos`, el histórico y la anulación están en `GastoOperativoController`, bajo `@SoloAdministradorAsilo`, como las donaciones: son egresos del asilo, y él lleva sus finanzas. `GastoOperativoTest` barre los otros seis roles en los tres endpoints y todos reciben 403, con cuerpos válidos por la razón de la sección 38.

### Las mismas reglas que las donaciones

Un gasto no se edita ni se borra: se anula con un motivo obligatorio, queda en el histórico tachado y deja de contar en los totales (sección 41). El monto se valida igual, mayor que cero con hasta dos decimales, y la fecha no puede estar en el futuro. La regla propia de los gastos depende de dos campos y la revisa el servicio: con el tipo Otro, la descripción es obligatoria, porque es lo único que dice en qué se gastó. El formulario la repite para avisar antes de enviar.

## 43. El reporte de costos por cita (HU-24)

### Quién lo genera

`GET /api/reportes/costos-por-cita` está en `ReporteController` con `@SoloAdministradorAsilo` en el método, no en la clase: el controlador va a reunir los cinco reportes, y los clínicos (HU-25, HU-28) serán de otros roles. `VisitaMedicaTest` barre los otros seis roles y todos reciben 403.

### Por tipo de servicio y nunca por nombre

RF-17 pide desglosar los exámenes y los medicamentos de cada cita, y el Administrador del Asilo no puede ver datos clínicos (RNF-03): el nombre de un medicamento lo es. Por eso cada cita dice cuántos exámenes y cuántos medicamentos tuvo y cuánto costaron, sin decir cuáles, como el estado de cuenta (sección 36). La prueba busca en la respuesta el examen, el medicamento, el diagnóstico y la especialidad: no aparece ninguno.

### El PDF no sale del navegador

"Exportar a PDF" usa la impresión del navegador ("Guardar como PDF"), con una hoja de estilos que deja solo el reporte. El PDF se arma en la computadora de quien lo pide con los datos que ya tenía en pantalla: no hay un servicio externo de conversión ni una copia del reporte guardada en el servidor.

## 44. El reporte de análisis médico (HU-25)

### Solo los médicos, y el especialista solo en su visita

Es el reporte con más información clínica del sistema: la ficha, el motivo de reclusión, los diagnósticos, los medicamentos y los resultados de laboratorio. `GET /api/reportes/analisis-medico` lleva `@MedicoGeneralOEspecialista`; enfermería, el laboratorio, la farmacia y los dos administradores reciben 403 (RNF-03), aunque HU-25 diga "como Médico" sin más. El Médico General lo genera de cualquier paciente, como su historial centralizado (HU-02).

Decidido el 21/10: el especialista, solo del paciente de una visita suya que siga activa, la misma regla de los antecedentes (HU-08). El servicio lo comprueba con las visitas activas del especialista, y si no hay una de ese paciente responde 403. Así el reporte no abre una puerta a fichas que el especialista no podría ver desde su visita. Las pruebas cubren al especialista con su visita activa (200), con otro paciente, con la visita de otro especialista y con la visita ya finalizada (403 en los tres).

### En el menú, y lo que sale al PDF

Los dos médicos tienen "Reportes" en su menú (decidido el 21/10; la matriz del diseño no lo traía), y la lista de reportes de esa pantalla depende del rol. Esa lista solo decide qué tarjetas se ven; quien lo exige es el backend. El PDF se arma en el navegador, como el de costos (sección 43), y la pantalla advierte que el reporte tiene datos clínicos y que se comparta solo con personal médico.

## 45. El reporte de cobros (HU-26)

### Quién lo genera

`GET /api/reportes/cobros` está en el mismo `ReporteController`, con `@SoloAdministradorAsilo` en el método, como el de costos (sección 43); la lógica va en su propio `ReporteCobrosService`. `VisitaMedicaTest` barre los otros seis roles (403) y la petición sin token (401).

### Los gastos médicos, por tipo y nunca por nombre

RF-19 pide el detalle de los gastos médicos de cada cobro, y el Administrador del Asilo no puede ver datos clínicos (RNF-03). Cada cuenta dice cuántas consultas, cuántos exámenes y cuántos medicamentos tuvo y cuánto costaron, sin decir cuáles, como el estado de cuenta (sección 36) y el reporte de costos. La prueba busca en la respuesta el examen, el medicamento y el diagnóstico de la visita: no aparece ninguno.

### Lo cobrado sale de los pagos

El total cobrado es la suma de los pagos registrados (HU-34) cuya fecha cae en el rango (decidido el 22/10), sin los anulados: un pago anulado no se cobró, y su anulación queda en el historial de la cuenta, no en este reporte. La prueba registra un pago, lo anula y comprueba que no aparece ni suma. El PDF se arma en el navegador, como el de costos.

## 46. Los reportes financieros (HU-27)

### Dos reportes, los dos del Administrador del Asilo

Decidido el 22/10: HU-27 son dos reportes y no uno consolidado, como los separó la revisión v1.1 de los requerimientos (RF-20a y RF-20b). `GET /api/reportes/pagos-fundacion` y `GET /api/reportes/entradas` están en `ReporteController` con `@SoloAdministradorAsilo` en cada método, y la lógica va en `ReporteFinancieroService`. `VisitaMedicaTest` barre los otros seis roles contra los dos (403) y los pide sin token (401).

### Qué muestra cada uno

El de pagos a la fundación dice de cada pago solo el concepto (consulta, laboratorio o farmacia), el mes de la deuda y el monto, como las cuentas por pagar (sección 39): ni pacientes ni servicios. El de entradas nombra al paciente de cada cobro, que es un dato personal que el Administrador del Asilo ya ve en su vista de internos, y nada clínico. En los dos, lo anulado no aparece ni suma: un pago o una donación anulada no se movió. Las pruebas anulan uno de cada uno y comprueban que su id no está en la respuesta. El PDF se arma en el navegador, como el de costos (sección 43).

## 47. Qué falta

Nada pendiente de HU-30, HU-31 ni HU-32. Lo que queda abierto, sin historia asignada todavía:

- **Caducidad de la contraseña temporal.** Una temporal que nadie cambia en tres meses sigue siendo una credencial compartida.
- **Pruebas de interfaz.** El frontend no tiene ninguna automatización; todo lo visual se comprueba a mano.
