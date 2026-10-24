# Sistema de Gestión — Asilo de Ancianos Cabeza de Algodón

Sistema web para la gestión de un asilo de ancianos: internos e historial médico, referencias y citas con especialistas, atención médica, laboratorio, farmacia y finanzas.

Proyecto final de **Análisis y Diseño de Sistemas II**, Universidad Mariano Gálvez de Guatemala, Centro Universitario de Mazatenango. Autor: Brandon Joaquín García Suy (3090-23-12053).

| Parte | Tecnología |
|---|---|
| Frontend | React 19 + Vite + Tailwind CSS + Axios |
| Backend | Spring Boot 4 (Java 21) + Spring Data JPA + Spring Security + JWT |
| Base de datos | PostgreSQL 15 (en Docker) |
| Correo de prueba | Mailpit (en Docker) |

## Versiones

- `main` contiene las entregas cerradas de cada sprint, etiquetadas (`v0.1.0`, `v0.1.1`, …).
- `develop` contiene lo último que ya se integró y todavía no se entregó.

Los módulos que aparecen en gris en el menú todavía no están construidos.

## Requisitos

- [Git](https://git-scm.com/)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/), encendido. Levanta la base de datos y el servidor de correo, y lo usan las pruebas del backend.
- JDK 21. Maven no hace falta: el proyecto trae su propio `mvnw`.
- Node.js 20.19 o superior, o 22.12 o superior (lo exige Vite).

Puertos que deben estar libres: `5433` (PostgreSQL), `1025` y `8025` (Mailpit), `8080` (backend) y `5173` (frontend).

## Puesta en marcha

Los comandos están escritos para Git Bash (viene con Git para Windows) o una terminal de Linux o macOS. En PowerShell, cambie `cp` por `Copy-Item` y `./mvnw` por `.\mvnw.cmd`.

### 1. Clonar el repositorio

```bash
git clone https://github.com/Hoakin26/Sistema-Gestion-Asilo.git
cd Sistema-Gestion-Asilo
git checkout develop   # o la etiqueta de la entrega a revisar, por ejemplo v0.1.2
```

### 2. Base de datos y correo de prueba

```bash
cp .env.example .env
```

Abra `.env` y escriba una contraseña en `POSTGRES_PASSWORD`. Después:

```bash
docker compose up -d
```

Esto levanta PostgreSQL en el puerto `5433` y Mailpit en `8025`. Mailpit recibe todos los correos que envía el sistema y no los reenvía a nadie.

### 3. Backend

```bash
cd gestion-asilo-backend
cp src/main/resources/application-example.properties src/main/resources/application.properties
```

Abra `application.properties` y cambie tres valores:

| Propiedad | Qué poner |
|---|---|
| `spring.datasource.password` | La misma contraseña que puso en `POSTGRES_PASSWORD`. |
| `jwt.secret` | Un texto aleatorio de **al menos 32 caracteres**. Puede generarlo con `openssl rand -base64 48`. |
| `app.admin-inicial.password` | La contraseña con la que entrará el primer administrador. |

Arranque el backend:

```bash
./mvnw spring-boot:run
```

La primera vez descarga las dependencias y tarda unos minutos. Está listo cuando el log dice `Started GestionAsiloBackendApplication`. En ese primer arranque, Flyway crea las tablas (`src/main/resources/db/migration`) y el backend agrega los 7 roles, el catálogo de especialidades y el primer administrador.

Deje esta terminal abierta.

Si el arranque falla con `Unresolved compilation problems`, el editor (la extensión de Java de VS Code) compiló clases con errores en `target/classes` y el backend las está usando. Arranque limpiando antes lo compilado:

```bash
./mvnw clean spring-boot:run
```

### 4. Frontend

En otra terminal, desde la raíz del repositorio:

```bash
cd gestion-asilo-frontend
cp .env.example .env
npm install
npm run dev
```

Abra http://localhost:5173.

### 5. Primer inicio de sesión

| Correo | Contraseña |
|---|---|
| `admin@asilo.gt` | La que puso en `app.admin-inicial.password` |

Si dejó esa propiedad en blanco, el backend generó una contraseña y la mostró **una sola vez** en el log del primer arranque. Busque la línea `Administrador inicial creado`.

El sistema pide cambiar la contraseña antes de dejar hacer cualquier otra cosa. Lo mismo pasa con todo usuario que recibe una contraseña asignada por otra persona.

## Roles

Cada rol ve su propio menú. El Administrador del Asilo crea las cuentas de los demás desde **Usuarios y roles**.

| Rol | Qué puede hacer hoy |
|---|---|
| Administrador del Asilo | Crear, editar y desactivar usuarios, y restablecer contraseñas. Ver los internos **sin datos clínicos**. |
| Médico General | Registrar y actualizar internos con su ficha médica. Crear solicitudes de referencia a un especialista. |
| Administrador de la Fundación | Ver las solicitudes entrantes. Registrar especialistas con sus horarios. Asignar fecha, hora y especialista a cada solicitud. |
| Médico Especialista | Ver su agenda del día, iniciar la visita desde la cita y consultar los antecedentes del interno mientras la visita está activa. |
| Enfermería | Consultar los internos y su ficha médica. |
| Laboratorio y Farmacia | Por ahora solo el Dashboard: sus módulos todavía no están construidos. |

**Regla de seguridad (RNF-03).** El Administrador del Asilo nunca debe poder leer datos clínicos: diagnósticos, psicopatologías ni medicamentos. Esto lo aplica el backend, no solo la pantalla, así que también se puede comprobar en las respuestas de la API (pestaña Red del navegador o Postman).

## Recorrido de prueba de punta a punta

Este recorrido pasa por todos los módulos construidos, en el orden en que se usan:

1. **Administrador del Asilo.** En *Usuarios y roles*, cree un usuario de cada uno de estos roles: Médico General, Administrador de la Fundación y Médico Especialista.
2. **Médico General.** En *Internos*, registre un interno con un familiar responsable que tenga correo.
3. **Médico General.** En *Solicitudes de referencia*, cree una solicitud para ese interno a una especialidad. Al familiar le llega un aviso por correo: véalo en Mailpit, http://localhost:8025.
4. **Administrador de la Fundación.** En *Médicos y disponibilidad*, registre el perfil del especialista: la cuenta creada en el paso 1, la misma especialidad de la solicitud, la duración de cada cita y sus horarios. Después, en *Solicitudes entrantes*, abra la solicitud y asígnele fecha, hora y especialista.
5. **Médico Especialista.** En *Mi agenda del día*, inicie la visita desde la cita. Dentro de la visita se ven los antecedentes del interno.

Para completar el paso 5 el mismo día, el horario del especialista debe incluir el día de hoy y la cita debe quedar para más tarde. El sistema no acepta citas en horas que ya pasaron, y una visita solo se puede iniciar desde el día de su cita.

Los criterios de aceptación de cada historia están en Jira, proyecto YS.

## Pruebas automatizadas

```bash
cd gestion-asilo-backend
./mvnw test
```

Requieren Docker encendido: cada ejecución levanta su propio PostgreSQL con Testcontainers y lo descarta al terminar. No tocan la base de desarrollo ni necesitan que esté corriendo.

Revisión estática del frontend:

```bash
cd gestion-asilo-frontend
npm run lint
```

## Despliegue en producción

El sistema se publica en un servidor con Docker, con los tres contenedores de [`docker-compose.prod.yml`](docker-compose.prod.yml): Caddy sirve la aplicación y obtiene el certificado HTTPS, el backend corre con el perfil `prod` y PostgreSQL queda sin puerto hacia afuera. La arquitectura, los respaldos y la reversión están en la [Fase de diseño](docs/fase-diseno.md) (2.5 y 2.6).

Requisitos del servidor: Docker con Compose, los puertos 80 y 443 abiertos, y un subdominio que apunte a su IP pública.

```bash
git clone https://github.com/Hoakin26/Sistema-Gestion-Asilo.git
cd Sistema-Gestion-Asilo
git checkout <etiqueta>                   # la última entrega, por ejemplo v0.2.1
cp .env.prod.example .env.prod            # y complete los valores (ver los comentarios)
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

Prueba de humo: `https://<dominio>/api/actuator/health` responde `{"status":"UP"}`, y el inicio de sesión funciona con el administrador inicial de `.env.prod`. Si su contraseña quedó en blanco, se genera una al azar y aparece una sola vez en el registro:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml logs backend
```

Para desplegar una versión nueva: respalde la base, cambie a la etiqueta nueva (`git fetch --tags && git checkout <etiqueta>`) y repita el `up -d --build`.

`.env.prod` existe solo en el servidor y no se versiona. Use secretos nuevos, distintos de los de desarrollo.

## Problemas frecuentes

**`docker compose` dice `Falta POSTGRES_PASSWORD`.** No existe el `.env` de la raíz, o `POSTGRES_PASSWORD` está vacía. Vea el paso 2.

**El backend no arranca y el log menciona `WeakKeyException`.** El `jwt.secret` tiene menos de 32 caracteres.

**El backend no arranca y el log dice `password authentication failed`.** `spring.datasource.password` no coincide con la contraseña de la base. Ojo: PostgreSQL toma `POSTGRES_PASSWORD` solo la primera vez que crea su volumen, así que cambiar el `.env` después no cambia la contraseña. Para empezar de cero, ejecute `docker compose down -v` y luego `docker compose up -d`. **Esto borra todos los datos de la base.**

**Perdí la contraseña del administrador inicial.** El administrador solo se crea cuando no hay ningún usuario en la base. En un ambiente de prueba, escriba la contraseña en `app.admin-inicial.password`, ejecute `docker compose down -v` y `docker compose up -d`, y vuelva a arrancar el backend. **Esto borra todos los datos de la base.**

**El frontend carga, pero el inicio de sesión falla con un error de red.** Falta el `.env` del frontend, o se creó con `npm run dev` ya corriendo: Vite solo lee el `.env` al arrancar, así que reinícielo. También revise que el backend siga en marcha en el puerto `8080`.

**El navegador muestra un error de CORS.** El backend solo acepta peticiones desde `http://localhost:5173`. Si ese puerto estaba ocupado, Vite abre el frontend en otro (`5174`, por ejemplo). Libere el `5173`, o agregue `app.cors.origen-frontend=http://localhost:5174` en `application.properties` y reinicie el backend.

**El correo al familiar no llega.** Es lo esperado: el sistema lo envía a Mailpit y no a un buzón real. Véalo en http://localhost:8025.

## Estructura del repositorio

```
.
├── docker-compose.yml         PostgreSQL y Mailpit (desarrollo)
├── .env.example               Plantilla de credenciales de la base de datos
├── docker-compose.prod.yml    Producción: Caddy, backend y PostgreSQL
├── .env.prod.example          Plantilla de las variables de producción
├── docs/                      Requerimientos, trazabilidad, diseño y seguridad
├── gestion-asilo-backend/     API REST en Spring Boot
│   └── src/main/java/.../     controllers, services, repository, models, dto,
│                              exception, config, security
└── gestion-asilo-frontend/    Aplicación React
    └── src/                   pages, components, layouts, context, hooks,
                               services, constants, utils
```

Ningún archivo con credenciales se versiona: `.env`, `.env.prod`, `gestion-asilo-frontend/.env` y `application.properties` se crean a partir de sus plantillas.

## Documentación

- [Requerimientos del sistema](docs/requerimientos-del-sistema.md): RF-01 a RF-22 y requerimientos no funcionales.
- [Trazabilidad de requerimientos](docs/trazabilidad-requerimientos.md): de cada requerimiento a sus historias.
- [Fase de diseño](docs/fase-diseno.md)
- [Seguridad](docs/seguridad.md): contraseñas, JWT, roles y decisiones de seguridad.
- Jira, proyecto YS: backlog, historias, criterios de aceptación y sprints.

## Flujo de trabajo

- Cada historia se desarrolla en una rama `caracteristica/YS-<número>-<descripción>`, creada desde `develop`.
- Al terminarla, la rama se fusiona en `develop` con `--no-ff`.
- Al cerrar un sprint, `develop` se fusiona en `main` y se etiqueta la entrega.
- Los commits van en español y en modo imperativo, con el código de la subtarea de Jira.
