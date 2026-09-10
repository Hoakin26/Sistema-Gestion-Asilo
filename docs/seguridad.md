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
- Esto es lo que hace cumplible la regla **RNF-03**: cada endpoint puede confiar en la identidad que trae el token validado por [JwtAuthenticationFilter.java](../gestion-asilo-backend/src/main/java/app/cabezadealgodon/com/gestionasilobackend/security/JwtAuthenticationFilter.java) para decidir qué datos devolver — nunca en lo que el cliente afirme o en lo que la UI oculte.

La clave HMAC debe medir al menos 256 bits para HS256; el `jwt.secret` actual tiene exactamente 32 caracteres. Si se acorta, la aplicación falla al arrancar en vez de firmar con una clave débil.

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

Credenciales por defecto: `admin@asilo.gt` / `Admin123*`, sobrescribibles con `app.admin-inicial.correo` y `app.admin-inicial.password` en `application.properties` (archivo no versionado). **Esa contraseña por defecto es pública en el código y debe cambiarse antes de cualquier uso real**; la aplicación lo advierte en el log al crear la cuenta. El seeder corre con `@Order(2)`, después del de roles, porque necesita que el rol `ADMINISTRADOR_ASILO` ya exista.

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

## 10. Qué falta

De HU-30 ([YS-38](https://joaquingsuy93.atlassian.net/browse/YS-38)) y HU-31 ([YS-39](https://joaquingsuy93.atlassian.net/browse/YS-39)):

- **YS-48:** expiración del token. La expiración ya se aplica (`jwt.expiration`, 24 h), porque emitir un token sin `exp` habría sido peor; queda revisar si 24 h es el plazo adecuado y probar el vencimiento.
- **YS-49/YS-50:** pantalla de login y manejo de sesión en el frontend (guardar el token, interceptor de Axios, botón de cerrar sesión que llame a `/api/auth/logout`).
- **YS-52/YS-53:** `@PreAuthorize` por endpoint y protección de los endpoints ya existentes según rol.
- **YS-54:** registro (log) de intentos de acceso denegado.
