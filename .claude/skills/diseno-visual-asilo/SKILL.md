---
name: diseno-visual-asilo
description: Tokens y reglas del Manual de Diseño Visual del Asilo Cabeza de Algodón (colores, tipografía, espaciado, sombras, iconografía, componentes). Usar siempre al construir o revisar cualquier pantalla, componente Tailwind/React o estilo del frontend.
---

# Diseño visual — Asilo Cabeza de Algodón

Fuente: "Manual de Diseño Visual - Asilo Cabeza de Algodón.pdf" v1.0. Todos los valores de color fueron verificados contra WCAG 2.1 nivel AA (contraste mínimo 4.5:1 texto normal, 3:1 elementos gráficos/bordes), por el RNF-04 (usabilidad, personal no experto en tecnología). No inventar colores, tamaños ni espaciados fuera de esta paleta.

## Colores de marca

| Token | Hex | Uso |
|---|---|---|
| Primario | `#2563EB` | Barra de navegación, menú lateral sobre fondo claro, botones primarios |
| Primario oscuro | `#1E40AF` | Hover/active, sobre fondos tenues como `#DBEAFE` |
| Primario tenue | `#DBEAFE` | Solo fondos, **nunca texto** |
| Superficie | `#FFFFFF` | Tarjetas, tablas, modales |

## Neutros

| Token | Hex | Uso |
|---|---|---|
| Fondo | `#F8FAFC` | Fondo de la aplicación |
| Texto principal | `#1E293B` | 14.63:1 sobre blanco |
| Texto secundario | `#64748B` | 4.76:1 — íconos, texto auxiliar |
| Divisor | `#E2E8F0` | **Solo decorativo** — separadores de tabla, líneas. Nunca como borde de campo de formulario (no alcanza 3:1); para bordes de controles interactivos usar `#64748B`. |

## Colores semánticos (valores definitivos, ya corregidos a AA)

Cada estado se representa con **color + ícono + texto simultáneamente** — nunca solo color (≈8% de daltonismo rojo-verde en hombres no distinguiría Éxito de Error).

| Rol | Texto/ícono/borde | Fondo tenue | Ícono lucide-react típico |
|---|---|---|---|
| Éxito | `#15803D` | `#DCFCE7` | `CircleCheck` |
| Advertencia | `#B45309` | `#FEF3C7` | `Clock` |
| Error | `#B91C1C` | `#FEE2E2` | `TriangleAlert` |
| Información | `#0369A1` | `#E0F2FE` | `CalendarCheck` |
| Neutral | `#475569` | `#F1F5F9` | `CircleSlash` |
| En proceso | `#6D28D9` | `#EDE9FE` | `Activity` / `FlaskConical` / `PackageOpen` / `CircleDollarSign` (según contexto) |

Estos son los únicos 6 colores de estado del sistema. El componente `StatusBadge` es la única fuente de verdad para pintar estados — no redefinir colores de estado por módulo (un "Pendiente" debe verse igual en médico, laboratorio, farmacia y finanzas).

Mapa completo de estados por máquina de estados (usar exactamente estos, no inventar otros):

- **DE-01 · Solicitud de referencia médica**: Pendiente de Asignación (Advertencia), Cita Programada (Información), En Atención (En proceso), Atendida (Éxito), Cancelada (Neutral).
- **DE-02 · Orden de laboratorio**: Pendiente (Advertencia), En Proceso (En proceso), Resultados Cargados (Éxito), Anulada (Neutral).
- **DE-03 · Receta médica**: Pendiente de Despacho (Advertencia), Parcialmente Entregada (En proceso), Entregada (Éxito), Medicamento Agotado (Error).
- **DE-04 · Cuenta mensual del familiar**: Pendiente de Pago (Advertencia), Pago Parcial (En proceso), Completamente Cancelada (Éxito), En Mora (Error).

## Tipografía

Fuente: **Inter** (Google Fonts). Familia de respaldo: `system-ui, -apple-system, "Segoe UI", Roboto, Arial, sans-serif`.

| Elemento | Tamaño | Peso | Interlineado | Uso |
|---|---|---|---|---|
| H1 | 30px | 700 | 1.2 | Título de página |
| H2 | 24px | 600 | 1.3 | Secciones |
| H3 | 20px | 600 | 1.4 | Subtítulos y títulos de tarjeta |
| Texto base | 16px | 400 | 1.5 | Contenido general y celdas de tabla |
| Texto destacado | 16px | 500 | 1.5 | Información importante |
| Botones | 16px | 500 | 1.0 | Acciones |
| Encabezado de tabla | 14px | 600 | 1.4 | Cabeceras de columna |
| Auxiliar | 14px | 400 | 1.4 | Textos de ayuda y errores de campo |

**Regla dura: 14px es el tamaño mínimo absoluto en toda la interfaz.** Nunca usar 12px, ni para acomodar más columnas en una tabla — si una tabla no cabe, se reduce el número de columnas visibles o se habilita scroll horizontal, nunca se reduce la letra.

Toda columna numérica (montos, saldos, dosis, cantidades, números de solicitud, fechas en tablas) debe llevar `class="tabular-nums slashed-zero"` en Tailwind, para alinear dígitos y evitar confundir `0` con `O`.

## Espaciado, radios, sombras, foco

- Escala de espaciado (múltiplos de 4px, sin valores intermedios): **4, 8, 12, 16, 24, 32, 48, 64**.
- Radios de borde: 4px (badges, campos de formulario), 8px (tarjetas, botones), 12px (modales), completo/`rounded-full` (avatares).
- Sombras:
  - Tarjeta: `0 1px 2px rgba(15,23,42,0.06)`
  - Menú desplegable: `0 4px 12px rgba(15,23,42,0.10)`
  - Modal: `0 12px 32px rgba(15,23,42,0.16)`
- Foco: todo elemento interactivo debe tener `outline: 2px solid #2563EB; outline-offset: 2px`. **Nunca usar `outline: none` sin un reemplazo visible equivalente** — es requisito de accesibilidad y necesario para operar el sistema por teclado.

## Responsividad

| Breakpoint | Ancho | Contexto |
|---|---|---|
| sm | 640px | Teléfono (uso excepcional) |
| **md** | **768px** | **Tableta — rondas de enfermería y evaluación médica en dormitorios. No es opcional.** |
| lg | 1024px | Laptop — oficinas y laboratorio |
| xl | 1280px | Escritorio — módulo financiero y reportes |

Área táctil mínima **44×44px** en botones, campos y filas de tabla accionables — obligatorio en vistas de tableta (personal de enfermería opera de pie y en movimiento).

## Iconografía

Librería: **lucide-react** (MIT, >1500 íconos, incluye vocabulario clínico/financiero: `Stethoscope`, `Pill`, `FlaskConical`, `Syringe`, `HeartPulse`, `ClipboardList`, `Receipt`, `HandCoins`).

- 20px junto a texto de 16px.
- 24px en botones que contienen únicamente un ícono.
- Grosor de trazo uniforme: 2px.
- Todo ícono sin texto visible acompañante requiere `aria-label`.

## Biblioteca de componentes

Usar/crear estos componentes reutilizables en vez de maquetar cada pantalla desde cero:

- **Layout**: `Sidebar`, `Navbar`, `Footer`, `Breadcrumbs`, `PageHeader`
- **Navegación**: `Menu`, `Tabs`, `Pagination`
- **Acciones**: `Button` (primario, secundario, texto, peligro), `IconButton`, `Dropdown`
- **Formularios**: `FormField`, `Input`, `Select`, `Textarea`, `Checkbox`, `Radio`, `DatePicker`, `SearchAutocomplete`, `CurrencyInput`, `FileUpload`
- **Información**: `Card`, `Badge`, `StatusBadge`, `Table`, `DataTable`, `Timeline`, `EmptyState`, `Tooltip`
- **Retroalimentación**: `Alert`, `Toast`, `Modal`, `ConfirmationDialog`
- **Carga**: `Spinner`, `Skeleton`
- **Especializados**: `MedicalRecordCard`, `AppointmentCard`, `ReportCard`, `PatientHeader`, `AvailabilityCalendar`, `PrintableReportLayout`

## Reglas de identidad institucional

- Nombre oficial del cliente: **"Asilo de Ancianos Cabeza de Algodón"** (nunca "Nueva Vida").
- Logo: isotipo (corazón con mota de algodón calada, un solo `path` con `fill-rule="evenodd"`) + versión horizontal. Tamaño mínimo del isotipo: 16px. Área de resguardo: la mitad de su altura alrededor. No deformar, rotar, ni aplicar sombras/degradados. No recolorear el algodón por separado — es un hueco, no una forma.
- Usar el logotipo horizontal siempre que haya espacio; el isotipo solo cuando el ancho es limitado.

## Restricción crítica de acceso (aplica al diseño de pantallas, no solo a permisos de API)

Al maquetar cualquier vista de "Internos" para el rol **Administrador del Asilo (ACT-04)**, esa vista NUNCA debe incluir psicopatologías, diagnósticos ni medicamentos (RNF-03) — solo datos personales, familiar responsable y estado de cuenta. Si estás construyendo un componente compartido entre roles clínicos y administrativos, este debe recibir explícitamente qué campos mostrar según el rol, no ocultarlos solo con CSS.