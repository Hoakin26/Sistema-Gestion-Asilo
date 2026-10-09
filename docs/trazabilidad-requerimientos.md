# Trazabilidad: requerimientos ↔ implementación

Contrasta lo construido y lo planificado contra `requerimientos-del-sistema.md`, que es la fuente de verdad del proyecto y **no se modifica**. Estado al 8 de octubre de 2026.

Las historias de usuario de Jira no citan su RF; la correspondencia de abajo se derivó comparando la descripción de cada RF con la de cada historia. Las subtareas creadas el 8 de octubre sí nombran el RF que cubren.

## 1. Requerimientos funcionales

La entrega se evalúa contra los 21 RF, y los 21 están en el plan de la entrega del viernes 16 de octubre (`fase-diseno.md`, sección 1.5). La fecha de cada fila es la del plan día por día.

| RF | Requerimiento | Historias | Subtareas | Estado |
|---|---|---|---|---|
| RF-01 | Gestión de perfil del interno | HU-01 | YS-57 a YS-62 | **Implementado** (08/10): alta, edición y consulta del expediente, ficha médica con psicopatologías y medicamentos, validación en pantalla y en servidor. Vista administrativa sin datos clínicos para el Administrador del Asilo |
| RF-02 | Historial médico centralizado | HU-02 | YS-63 a YS-67 | Sprint 2, 11/10, después de la visita médica — ver hallazgo 2 |
| RF-03 | Solicitudes de referencia médica | HU-03 | YS-83 a YS-86 | **Implementado** (09/10): el Médico General crea la solicitud con número correlativo, especialidad y enfermero(a) acompañante, desde su módulo o desde el perfil del interno. El listado llega con HU-05 |
| RF-04 | Notificación por correo al familiar | HU-04 | YS-94 a YS-97 | **Implementado en parte** (09/10): el aviso al crear la solicitud, con su estado y especialidad, envío asíncrono y reintento automático y manual. El aviso con el especialista asignado sale con HU-06 (YS-91), porque antes de la cita no hay especialista |
| RF-05 | Módulo de asignación | HU-05, HU-06 | YS-87 a YS-93 | Sprint 2, 09–10/10 |
| RF-06 | Gestión de visita médica | HU-07, HU-09 | YS-98 a YS-101, YS-105 a YS-107 | Sprint 2, 10–11/10 |
| RF-07 | Consulta de historial por especialista | HU-08 | YS-102 a YS-104 | Sprint 2, 10–11/10. El permiso ya existe: el especialista puede leer la ficha médica (`@PersonalClinico`, YS-58) |
| RF-08 | Asignación de exámenes de laboratorio | HU-11, HU-14 | YS-111 a YS-113, YS-120 a YS-122 | Sprint 3, 12/10 |
| RF-09 | Gestión de resultados de laboratorio | HU-12, HU-13 | YS-114 a YS-119 | Sprint 3, 12/10 |
| RF-10 | Receta y dosis de medicamentos | HU-10 | YS-108 a YS-110 | Sprint 2, 10–11/10 |
| RF-11 | Despacho de farmacia | HU-15, HU-16, HU-17 | YS-123 a YS-131 | Sprint 3, 12–13/10 |
| RF-12 | Control de cuentas de familiares | HU-18, HU-19 | YS-132 a YS-138 | Sprint 3, 12–13/10 |
| RF-13 | Control de cuentas por pagar | HU-20 | YS-139 a YS-141 | Sprint 3, 13/10 |
| RF-14 | Gestión de citas pendientes | HU-21 | YS-142 a YS-144 | Sprint 3, 13/10 |
| RF-15 | Registro de donaciones | HU-22 | YS-145 a YS-147 | Sprint 3, 13/10 |
| RF-16 | Registro de gastos operativos | HU-23 | YS-148 a YS-150 | Sprint 3, 13/10 |
| RF-17 | Reporte estadístico de costos | HU-24 | YS-151 a YS-153 | Sprint 3, 14/10 |
| RF-18 | Reporte de análisis médicos | HU-25 | YS-154 a YS-156 | Sprint 3, 14/10 |
| RF-19 | Reporte de cobros a familiares | HU-26 | YS-157 a YS-159 | Sprint 3, 14/10 |
| RF-20 | Reportes financieros consolidados | HU-27 | YS-160 a YS-162 | Sprint 3, 14/10 |
| RF-21 | Reportes médicos específicos | HU-28 | YS-163 a YS-165 | Sprint 3, 14/10 |

**Cobertura planificada para la entrega del 16/10: 21 de 21.** Implementados al 9/10: RF-01, RF-03 y la primera mitad del RF-04. Si el tiempo aprieta, lo que se ajusta es el nivel de detalle de cada historia (`fase-diseno.md`, 1.7, Nivel 1), nunca la lista de requerimientos.

## 2. Requerimientos no funcionales

| RNF | Requerimiento | Cómo se cumple | Estado |
|---|---|---|---|
| RNF-01 | Arquitectura web, accesible desde Chrome, Edge y Safari | Aplicación React servida por navegador; adaptada a tableta y teléfono (HU-33) | Hecho. **Nunca se probó en Safari** — ver hallazgo 5 |
| RNF-02 | Siete perfiles estrictos; cada quien ve solo lo de su competencia | Los siete roles coinciden uno a uno con la lista del requerimiento. Restricción por endpoint con `@PreAuthorize` (HU-31), menú según el rol (YS-81) y barrido de los siete roles en las pruebas (YS-82) | Hecho |
| RNF-03 | Contraseñas encriptadas; privacidad de los historiales médicos | Contraseñas con BCrypt (HU-30). Datos clínicos separados de los personales en entidades distintas (YS-57), y el Administrador del Asilo excluido de la ficha médica (YS-58). Cambio obligatorio de contraseñas asignadas (HU-32). Los reportes y el estado de cuenta del Administrador del Asilo se desglosan por tipo de servicio, sin nombrar exámenes ni medicamentos (YS-134, YS-157) | Hecho para lo construido; cada subtarea nueva lo prueba. Ver la nota de terminología en el hallazgo 6 |
| RNF-04 | Interfaz clara y legible para personal no experto | Sistema de diseño con tipografía mínima de 14 px, áreas táctiles de 44 px y mensajes que explican el motivo, no solo la orden | Hecho, transversal |
| RNF-05 | Disponibilidad del 99 % | Despliegue en servidor público con monitoreo de disponibilidad (`fase-diseno.md`, 2.5 y 2.6) | **Sin cubrir.** Planificado para el 15/10 — ver hallazgo 4 |
| RNF-06 | Respuesta en 3 a 5 segundos para historiales y reportes | Medición de tiempos de respuesta sobre el servidor desplegado | **Sin medir.** Planificado para el 15/10 — ver hallazgo 4 |

## 3. Hallazgos

### Corregido durante esta revisión

**1. RF-03: quién crea la referencia.** El requerimiento dice que la crea *el médico general*, *asignando un enfermero(a) acompañante*. El objetivo del Sprint 3 en `fase-diseno.md` y las subtareas YS-84 y YS-85 decían que también la creaba Enfermería. Se corrigieron los tres: la crea solo el Médico General, y el enfermero es un campo de la solicitud, no quien la origina. YS-86 ahora prueba explícitamente que Enfermería recibe 403 al intentarlo. De paso, YS-87 y YS-89 se alinearon con la tabla de API del diseño, que también le da lectura de la bandeja al Médico General.

**2. RF-02 dependía de una historia que estaba fuera del plan.** El requerimiento pide un historial que consolide *visitas, diagnósticos y padecimientos*, y las visitas y los diagnósticos los produce el RF-06 (HU-07, HU-09). El primer re-plan del 8 de octubre había sacado el RF-06 de la entrega, y con eso el historial solo habría podido mostrar la ficha. Al restaurar los 21 RF, HU-02 se movió para después de la visita médica (11/10): se construye cuando ya hay qué consolidar.

**3. La cobertura funcional.** Se confirmó que la entrega se evalúa contra los 21 RF. El primer re-plan cubría 4 y el plan original 11; el plan vigente cubre los 21. Todas las historias tienen sus subtareas en Jira desde el 8 de octubre.

### Abierto

**4. RNF-05 y RNF-06 sin evidencia.** Ninguno de los dos se puede demostrar sin desplegar. El plan les asigna el 15/10 —despliegue y medición de tiempos—, con el servidor preparado antes, durante el Sprint 2. Sigue siendo el riesgo más alto que queda abierto: si algo falla al desplegar, queda un solo día para resolverlo.

**5. RNF-01 nombra Safari, y no se probó.** Todas las verificaciones visuales se hicieron en navegadores basados en Chromium. Safari en iPad es además el dispositivo previsto para las rondas de enfermería, así que es la prueba que más vale hacer antes de entregar. Está planificada para el 15/10.

### Coincide, con matices que conviene tener preparados

**6. "Contraseñas encriptadas".** Se guardan con BCrypt, que es un *hash* y no un cifrado: no se pueden descifrar ni siquiera teniendo acceso a la base. Cumple la intención del requerimiento con una garantía más fuerte, pero si en la defensa se pregunta dónde se descifran, la respuesta es que nunca: al iniciar sesión se compara el hash de lo escrito con el guardado.

**7. El alta del interno va en dos pasos.** El RF-01 se cumple —se registran los datos, las psicopatologías y los medicamentos—, pero los datos clínicos se escriben por un endpoint propio después del alta, para que haya un solo camino hacia cada dato. La tabla de API de `fase-diseno.md` (4.9) ya lo refleja.

**8. La cuota mensual es opcional al registrar** (YS-59). Ningún RF la exige en el alta, y quien da de alta al interno es el Médico General, a quien la cuota no le compete. Hoy solo puede completarla él al actualizar el interno; la vía para el Administrador del Asilo, `PUT /api/internos/{id}/cuota`, se agregó a YS-132, junto con la consolidación de la cuenta que la usa (RF-12).

**9. Lo construido que no pide ningún RF.** Contraseña temporal obligatoria (HU-32), uso en tableta (HU-33), expiración de sesión y registro de accesos denegados. No contradicen nada: todos sirven a RNF-01, RNF-02, RNF-03 o RNF-04.

### Sobre el propio documento de requerimientos

**10.** Su encabezado nombra al cliente *Asilo "Nueva Vida"*, y el sistema usa *Asilo de Ancianos Cabeza de Algodón*. Es una decisión documentada (Notion, página "Estilo"): el nombre del documento de Análisis de Sistemas I se descartó por el del enunciado del proyecto. No es una desviación de los requerimientos, pero quien lea los dos documentos lo va a notar.

**11.** Las líneas 66 a 68 del archivo no son requerimientos: es texto que quedó de quien lo generó. Como el documento no se modifica, se deja tal cual y se señala aquí.
