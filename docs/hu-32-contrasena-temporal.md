# HU-32 · Contraseña temporal obligatoria (YS-72)

Documento de diseño previo a la implementación. Recoge el estado del código al 8 de octubre de 2026, la solución acordada y las decisiones tomadas. No describe código ya escrito: a esta fecha HU-32 no tiene ninguna línea implementada.

Contrastado contra las subtareas [YS-74](https://joaquingsuy93.atlassian.net/browse/YS-74), [YS-75](https://joaquingsuy93.atlassian.net/browse/YS-75) y [YS-76](https://joaquingsuy93.atlassian.net/browse/YS-76) de Jira. **Lo que finalmente se construyó está en `seguridad.md`, secciones 16 y 17**; difiere de este documento en la interfaz, que acabó siendo una ventana flotante y no una pantalla propia.

## 1. Qué problema cierra

Lo dejó abierto YS-71, y está anotado en `seguridad.md` §12:

> El administrador conoce la contraseña temporal hasta que el usuario la cambie, y nada lo obliga a hacerlo: dos personas comparten una credencial, lo que rompe la trazabilidad de quién hizo cada registro.

Eso no es solo higiene. El sistema guarda historial clínico y movimientos de dinero, y la defensa del proyecto se apoya en poder decir quién hizo cada cosa. Mientras dos personas puedan entrar con la misma credencial, esa afirmación no se sostiene.

## 2. Dónde se fija hoy una contraseña que su dueño no eligió

Son tres lugares. Los criterios de aceptación de YS-72 solo nombran el segundo, pero los tres producen exactamente el mismo problema:

| Lugar | Quién la fija | Historia | ¿Lo pide Jira? |
|---|---|---|---|
| `UsuarioService.crear` | El administrador, al registrar al usuario | YS-41 / YS-42 | No |
| `UsuarioService.restablecerPassword` | El administrador, desde la lista de usuarios | YS-71 | Sí |
| `AdminInicialSeeder` | El propio sistema, en el primer arranque | YS-45 | No |

El tercero es el más incómodo y el que más conviene no olvidar: cuando no se configura una contraseña, el seeder genera una al azar y **la escribe en el log de arranque**. Cualquiera con acceso a ese archivo la conoce.

**Decisión tomada:** los tres marcan al usuario. Forzar el cambio después de un restablecimiento pero no después de un alta dejaría el hueco abierto por el camino más transitado, y es el mismo defecto con otro nombre.

Hay un único lugar donde la contraseña la elige su dueño: `AuthService.cambiarPassword`, que exige la contraseña actual. Ese es, por lo tanto, el único que puede apagar la marca.

## 3. El cambio en cada capa

### Entidad

Un campo nuevo en `Usuario`:

```java
@Column(name = "debe_cambiar_password", nullable = false)
private boolean debeCambiarPassword = false;
```

**Cuidado con la migración.** El esquema se genera con `ddl-auto=update` y la tabla `usuario` ya tiene filas. Hibernate va a emitir un `ALTER TABLE ... ADD COLUMN ... NOT NULL` sin valor por defecto, y PostgreSQL lo rechaza cuando existen filas. Hay que darle a la columna un valor por defecto en la base para que las filas existentes se rellenen solas:

```java
@Column(name = "debe_cambiar_password", nullable = false,
        columnDefinition = "boolean not null default false")
```

Es la primera vez en el proyecto que se agrega una columna obligatoria a una tabla con datos, así que conviene probarlo contra una copia de la base de desarrollo antes que contra la de verdad.

Decisión implícita: los usuarios que ya existen nacen con la marca apagada. Ninguno de ellos tiene contraseña temporal conocida por el administrador, salvo quizá el admin inicial, y obligarlos a cambiarla sería castigar a todos por el estado de uno.

### DTO

`LoginResponse` gana un campo, para que el frontend sepa a dónde llevar al usuario apenas entra:

```java
public record LoginResponse(
        String token, String tipo, String correo,
        String nombreCompleto, RolNombre rol,
        boolean debeCambiarPassword) { }
```

Hace falta además que el rechazo del backend sea distinguible (ver "Dónde se hace cumplir"). Se propone agregar un campo opcional a `ErrorResponse`:

```java
String codigo   // p. ej. "CAMBIO_PASSWORD_REQUERIDO"; nulo en el resto de los errores
```

`ErrorResponse` ya lleva `@JsonInclude(NON_NULL)`, así que el campo no aparece en ninguna respuesta que no lo use y nada de lo existente cambia de forma. La alternativa —que el frontend reconozca el caso por el texto del mensaje— ata el comportamiento a una cadena en español que cualquiera puede reescribir sin darse cuenta de que algo depende de ella.

### Service

- `crear`, `restablecerPassword` y el seeder **encienden** la marca.
- `cambiarPassword` la **apaga**, y es el único que puede hacerlo, porque es el único que exige la contraseña actual.
- `cambiarPassword` ya rechaza una contraseña nueva igual a la actual (YS-69). Esa regla, que se escribió por otro motivo, es justo la que impide que alguien "cumpla" el cambio dejando puesta la contraseña que el administrador le dictó.

### Dónde se hace cumplir

**En el backend, no en el frontend.** Es la misma lección de HU-31: si la obligación vive solo en la pantalla, basta con escribir otra dirección para saltársela, y el usuario seguiría operando con una credencial que su administrador conoce. YS-74 lo dice con todas las letras en su enfoque.

El lugar natural es `JwtAuthenticationFilter`, que ya carga al usuario desde la base en cada petición para comprobar `fechaInvalidacionSesiones`. Con el usuario en la mano, comprobar una marca más no cuesta una consulta extra.

Mientras la marca esté encendida, la API responde **403 con `codigo: "CAMBIO_PASSWORD_REQUERIDO"`** a todo, con tres excepciones:

| Ruta | Por qué se permite |
|---|---|
| `PUT /api/auth/password` | Es la salida del callejón; bloquearla dejaría al usuario encerrado |
| `POST /api/auth/logout` | Para que pueda irse sin cambiarla, si prefiere volver después |
| `POST /api/auth/login` | Es pública; además es la que le informa de la marca |

Se descartó la alternativa de emitir un token restringido —uno que solo sirviera para cambiar la contraseña—. Es más estricta, pero mete una segunda clase de token en el sistema y obliga a razonar sobre qué pasa cuando uno de esos llega a un endpoint que no lo espera. La marca en la base es un solo hecho, en un solo lugar, y el filtro ya la tiene a mano.

### Frontend

Al recibir `debeCambiarPassword: true`, la sesión se inicia pero la navegación queda fija en una pantalla de cambio obligatorio, sin menú lateral: ofrecerle módulos que la API va a rechazar solo genera confusión. La pantalla debe decir **por qué** se le pide, no solo pedirlo — "esta contraseña la conoce quien se la asignó" es una razón que el personal no técnico entiende (RNF-04).

Conviene además que el interceptor de Axios reconozca el código y lleve a esa pantalla, para el caso en que la marca se encienda mientras hay una sesión abierta: ocurre cada vez que un administrador restablece la contraseña de alguien que está trabajando.

**La misma pantalla resuelve una deuda anterior.** `PUT /api/auth/password` existe desde YS-69, pero **no tiene ninguna interfaz**: `authService.js` solo expone iniciar y cerrar sesión. Hoy nadie puede cambiar su contraseña desde la aplicación. YS-75 cubre las dos cosas con el mismo formulario: el cambio obligatorio y el voluntario, este último desde una entrada nueva en el menú de usuario.

**Defecto conocido que esto corrige.** `DashboardPage` le dice hoy al usuario que "puede cambiar su contraseña desde el menú de usuario, arriba a la derecha". Esa entrada no existe: el menú solo tiene "Cerrar sesión". El texto se escribió en YS-81 dando por supuesta una opción que nunca se construyó. Queda corregido al implementar YS-75; si YS-75 se pospusiera, hay que corregir ese texto igual.

## 4. Decisiones tomadas

1. **El admin inicial nace con la marca encendida.** Su contraseña aparece en el log de arranque, y una credencial de administrador escrita en un archivo de texto es peor riesgo que el contrario —que alguien despliegue, no la cambie, pierda el log y se quede sin entrar—. Ese caso se recupera volviendo a sembrar con `app.admin-inicial.password`.
2. **El alta de usuario también marca**, aunque Jira no lo pida (ver sección 2).

Queda pendiente, fuera del alcance de esta historia:

- **Caducidad de la contraseña temporal.** Una temporal que nadie cambia en tres meses sigue siendo una credencial compartida. Podría abrirse como historia aparte.
- **El usuario desactivado.** Si se restablece la contraseña de alguien desactivado, la marca queda encendida y se le aplicará cuando lo reactiven. No parece requerir trato especial.

## 5. Lo que no cubre esta historia

No es autenticación en dos pasos ni política de expiración periódica de contraseñas. HU-32 cierra un hueco concreto: que nadie opere el sistema con una credencial que otra persona eligió y conoce.

## 6. Subtareas

Las de Jira, en orden de ejecución. Una por commit.

| Subtarea | Alcance |
|---|---|
| [YS-74](https://joaquingsuy93.atlassian.net/browse/YS-74) · **[Backend] Marcar y exigir el cambio** | Campo en la entidad con su valor por defecto, encendido en los tres lugares de la sección 2, apagado en `cambiarPassword`, campo nuevo en `LoginResponse`, bloqueo en el filtro con `codigo` en `ErrorResponse` |
| [YS-75](https://joaquingsuy93.atlassian.net/browse/YS-75) · **[Frontend] Pantalla de cambio obligatorio** | Formulario que sirve al cambio obligatorio y al voluntario, entrada en el menú de usuario, interceptor que reconoce el código, errores del backend junto a su campo, token nuevo guardado al terminar |
| [YS-76](https://joaquingsuy93.atlassian.net/browse/YS-76) · **[Pruebas] Probar el flujo** | Los cinco casos de la subtarea, incluido que las sesiones anteriores al restablecimiento queden invalidadas |

## 7. Cómo se verificará

Las pruebas van sobre `AutorizacionTest` y `AutenticacionTest`, con Testcontainers, igual que el resto. El caso que no debe faltar es el del circuito entero recorrido de punta a punta: el administrador restablece, el usuario entra, choca contra el bloqueo en cualquier otro endpoint, cambia la contraseña y recién entonces la API le responde con normalidad. Comprobar cada pieza por separado dejaría pasar el escenario en que todas funcionan y aun así el usuario queda encerrado.
