# Sistema de Gestión — Asilo de Ancianos Cabeza de Algodón

Proyecto final de **Análisis y Diseño de Sistemas II** (8.º semestre), Universidad Mariano Gálvez de Guatemala, Centro Universitario de Mazatenango. Estudiante: Brandon Joaquín García Suy (carné 3090-23-12053).

## Qué es esto

Sistema de gestión para un asilo de ancianos real: control de internos, historial médico centralizado, referencias y citas médicas, laboratorio, farmacia, y el módulo financiero (cuentas por cobrar/pagar, donaciones, gastos, reportes). Analizado y diseñado en las Fases 1-5 del curso; a partir de la Fase 6 se pasa a la construcción técnica.

## Stack y arquitectura

Monolito modular, un solo repositorio con dos proyectos hermanos:

```
/gestion-asilo-frontend   React (Vite) + Tailwind CSS + Axios
/gestion-asilo-backend    Spring Boot 4 (Java 21) + Spring Data JPA + Spring Security + JWT (jjwt) + PostgreSQL
```

- Frontend y backend se comunican por REST sobre JSON.
- Autenticación con JWT; autorización por rol (RBAC) — ver sección de roles abajo.
- Base de datos PostgreSQL, esquema generado por Hibernate a partir de las entidades JPA (`ddl-auto=update` en desarrollo).

## Estructura de carpetas

**Backend** (`backend/src/main/java/app/cabezadealgodon/com/gestionasilobackend/`):
`controllers`, `services`, `repository`, `models`, `dto`, `exception`, `config` (Spring Security, CORS), `security` (filtro JWT, `JwtUtil`, `UserDetailsService`).

Patrón de capas: `Controller → Service → Repository`. El controller nunca habla directo con el repository ni contiene lógica de negocio; los DTO son la única forma en que datos entran/salen de la API (nunca se exponen las entidades JPA directamente). Ver skill `arquitectura-backend` para el detalle y un ejemplo completo.

**Frontend** (`frontend/src/`):
`assets`, `components`, `context` (estado global, p. ej. sesión/usuario autenticado), `hooks`, `layouts`, `pages`, `services` (llamadas Axios a la API).

## Roles del sistema (RBAC)

7 actores, cada uno con su propio menú y permisos (ver skill `diseno-visual-asilo`, sección de navegación):

| Actor | Rol |
|---|---|
| ACT-01 | Médico General |
| ACT-02 | Administrador de la Fundación |
| ACT-03 | Médico Especialista |
| ACT-04 | Administrador del Asilo |
| ACT-05 | Enfermería |
| ACT-06 | Laboratorio |
| ACT-07 | Farmacia |

**Regla de seguridad no negociable (RNF-03):** el Administrador del Asilo (ACT-04) jamás debe poder leer datos clínicos — diagnósticos, psicopatologías, medicamentos. Su vista de "Internos" solo incluye datos personales, familiar responsable y estado de cuenta. Esto se aplica tanto en el frontend (qué se renderiza) como en el backend (qué devuelve el endpoint según el rol del token) — no basta con ocultarlo en la UI.

## Variables de entorno

- Backend: `backend/src/main/resources/application.properties` (no versionado, contiene credenciales de PostgreSQL y `jwt.secret`). Existe `application-example.properties` versionado como plantilla.
- Frontend: `frontend/.env` (no versionado, `VITE_API_URL` apuntando a la API). Existe `.env.example` versionado como plantilla.
- Docker: `.env` en la raíz (no versionado, `POSTGRES_PASSWORD`). `docker-compose.yml` sí se versiona, pero lee las credenciales de ese `.env`; nunca escribir una contraseña literal en él. Existe `.env.example` como plantilla.

Ninguno de estos archivos debe llegar a git. Tener la regla en `.gitignore` no basta si el archivo ya estaba rastreado: `.gitignore` solo aplica a archivos que git aún no sigue. Antes de dar por seguro un archivo de credenciales, comprobarlo con `git ls-files --error-unmatch <ruta>` — si responde con la ruta, está versionado.

## Convenciones de trabajo

- Ramas: `caracteristica/YS-<numero>-<descripcion-corta>`, usando el código de la historia de Jira (proyecto **YS**, 8 épicas, 28+ historias). Ej.: `caracteristica/YS-38-expiracion-token`. Se crean siempre a partir de `develop`, nunca de `main`.
- **Flujo de ramas (git-flow).** `develop` es la rama de integración: ahí se fusiona cada rama de característica terminada, siempre con `--no-ff`, para que la historia de Jira quede visible como una fusión propia y no se disuelva en commits sueltos. `main` se reserva para las entregas: al cerrar un sprint se fusiona `develop` en `main` y se etiqueta la versión (`git tag -a v0.1.0 -m "..."`). Esa etiqueta es lo que justifica el modelo — sin ella `main` se queda congelada y se pierde la correspondencia entre el historial de git y los sprints de Jira, que es justo lo que el catedrático revisa.
- **Pruebas del backend:** `./mvnw test` desde `gestion-asilo-backend`. Levantan un PostgreSQL propio con Testcontainers, así que requieren Docker encendido, pero no tocan la base de desarrollo ni necesitan que esté corriendo.
- Commits en español, en modo imperativo (ej. "Agrega validación de DPI en registro de interno").
- **Git es exclusivamente manual:** Claude nunca ejecuta `git checkout -b`, `git commit`, `git merge` ni `git push` (ni ninguna otra operación de git que cree, modifique o suba historial) en este proyecto, aunque el usuario lo haya aprobado antes. En su lugar, indica textualmente qué crear o subir — por ejemplo "Crea la rama `caracteristica/YS-40-...`" o "Sube esto con el commit: '...'" — y el usuario lo ejecuta él mismo.
- **Claude avisa cuándo fusionar y cuándo ramificar.** No espera a que el usuario pregunte: cuando una historia queda terminada, o cuando la rama actual empieza a acumular subtareas de más de una historia, Claude lo señala y entrega los comandos completos (fusionar a `develop`, borrar la rama vieja local y remota, crear la siguiente desde `develop`). También avisa cuando toca cerrar un sprint: fusionar `develop` en `main` y etiquetar la entrega. El usuario decide y los ejecuta.
- Trazabilidad: cada historia de usuario (HU) corresponde a un requisito funcional (RF-01 a RF-21) documentado en "Diseño proyecto Asilo.pdf". Al implementar una HU, revisar su RF asociado y sus criterios de aceptación en Jira antes de codear.
- Toda pantalla nueva debe respetar los tokens de diseño del manual visual — ver skill `diseno-visual-asilo`. No usar colores, tamaños de fuente o espaciados que no estén en esa paleta/escala.

## Documentación de referencia (fuente de verdad)

- **Jira** (proyecto YS): backlog, épicas, historias, criterios de aceptación, sprints y subtareas técnicas.
- **Notion** ("Sistema Asilo Cabeza de Algodón"): checklist maestro de fases del proyecto, historias de usuario, product backlog, definición de módulos.
- **Figma** ("Sistema Asilo Cabeza de Algodón — UI Sprint 1", archivo `Gzjti6A7pPPGwESzZLVR5u`): diseño de pantallas y sistema de diseño (tokens, componentes).
- PDFs en la carpeta del proyecto: `Descripción del proyecto.pdf` (enunciado del catedrático), `Diseño proyecto Asilo.pdf` (RF/RNF, casos de uso, arquitectura), `Manual de Diseño Visual - Asilo Cabeza de Algodón.pdf` (sistema visual completo — resumido en la skill `diseno-visual-asilo`).

## Skills del proyecto

- **`diseno-visual-asilo`** — colores, tipografía, espaciado, sombras, iconografía y componentes del Manual de Diseño Visual. Consultar siempre al escribir componentes de UI o clases de Tailwind.
- **`arquitectura-backend`** — convenciones de capas y estructura para el backend Spring Boot.

## Requerimientos
Los requerimientos del sistema se encuentran en /docs/requerimientos-del-sistema.md y solo cambian por revisión formal, registrada en su control de cambios (sección 3 del documento). Claude no los modifica por su cuenta.
Los requerimientos estan transformados en historias que se encuentran en JIRA.
