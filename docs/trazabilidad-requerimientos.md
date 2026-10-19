# Trazabilidad: requerimientos ↔ implementación

Contrasta lo construido y lo planificado contra `requerimientos-del-sistema.md`, que es la fuente de verdad del proyecto. Ese documento solo cambia por revisiones formales, registradas en su control de cambios: la versión 1.1 (15/10/2026) y la 1.2 (19/10/2026). Esta matriz está al día con la versión 1.2; el estado de implementación de cada RF es el del 14 de octubre de 2026.

Las historias de usuario de Jira no citan su RF; la correspondencia de abajo se derivó comparando la descripción de cada RF con la de cada historia. Las subtareas creadas el 8 de octubre sí nombran el RF que cubren.

## 1. Requerimientos funcionales

Desde la versión 1.1 son 24 RF: RF-20 y RF-21 se dividieron en dos cada uno y se agregó RF-22. La fecha de cada fila pendiente es la del plan día por día (`fase-diseno.md`, sección 1.5).

| RF | Requerimiento | Historias | Subtareas | Estado |
|---|---|---|---|---|
| RF-01 | Gestión de perfil del interno | HU-01 | YS-57 a YS-62 | **Implementado** (08/10): alta, edición y consulta del expediente, ficha médica con psicopatologías y medicamentos, validación en pantalla y en servidor. Vista administrativa sin datos clínicos para el Administrador del Asilo |
| RF-02 | Historial médico centralizado | HU-02 | YS-63 a YS-67 | **Implementado** (14/10): el Médico General consulta el historial de cada interno desde su perfil o desde el módulo "Historial médico". Arriba, los padecimientos, alergias y medicamentos habituales de la ficha; debajo, cada consulta con especialista, de la más reciente a la más antigua, con su motivo, diagnóstico, observaciones, medicamentos recetados y exámenes ordenados. Se arma de lo registrado en las visitas (RF-06), sin copia aparte — ver hallazgo 2 |
| RF-03 | Solicitudes de referencia médica | HU-03 | YS-83 a YS-86 | **Implementado** (09/10): el Médico General crea la solicitud con número correlativo, especialidad y enfermero(a) acompañante, desde su módulo o desde el perfil del interno. El listado llega con HU-05 |
| RF-04 | Notificación por correo al familiar | HU-04, HU-06 | YS-94 a YS-97, YS-91 | **Implementado** (09/10): el aviso al crear la solicitud, con su estado y especialidad (HU-04), y el aviso al asignar la cita, con el especialista, la fecha y la hora (HU-06, YS-91). Los dos salen en segundo plano después de confirmar, con reintento automático y manual |
| RF-05 | Módulo de asignación | HU-05, HU-06 | YS-87 a YS-93, YS-166 a YS-168 | **Implementado** (09/10): la Fundación visualiza las solicitudes en su bandeja, con búsqueda y orden por fecha (HU-05, YS-166); registra a los especialistas con su horario de atención (YS-167, YS-168), y desde el detalle de cada solicitud asigna especialista, fecha y una de las horas libres, con lo que la solicitud pasa a "Cita Programada" (HU-06) |
| RF-06 | Gestión de visita médica | HU-07, HU-09 | YS-98 a YS-101, YS-169, YS-105 a YS-107 | **Implementado** (13/10): el especialista ve su agenda del día con el motivo de cada referencia e inicia la visita desde la cita programada; la visita registra la fecha y la solicitud pasa a "En Atención" (HU-07). Registra el diagnóstico, obligatorio, y las observaciones, y al finalizar la solicitud pasa a "Atendida" y se genera el cargo de la consulta (HU-09) |
| RF-07 | Consulta de historial por especialista | HU-08 | YS-102 a YS-104 | **Implementado** (13/10): durante la visita activa, el especialista ve la ficha médica completa, las visitas anteriores del paciente con su diagnóstico (HU-09), los medicamentos que se le recetaron antes (HU-10) y los exámenes que se le ordenaron, con su estado (HU-11); con la visita finalizada el acceso a los antecedentes se cierra. Decidido el 09/10: HU-08 se construyó antes que la receta y los exámenes, que le fueron agregando su parte |
| RF-08 | Asignación de exámenes de laboratorio (v1.1: el costo lo registra el laboratorio) | HU-11, HU-14 | YS-111 a YS-113, YS-120 a YS-122 | **Implementado en parte** (13/10): en la visita activa, el especialista ordena uno o varios exámenes del catálogo, con indicaciones opcionales; cada uno crea una orden pendiente para el laboratorio (DE-02) que guarda el costo del catálogo (HU-11). Falta que el laboratorio confirme ese costo y lo pase a la cuenta del familiar (HU-14, Sprint 3) |
| RF-09 | Gestión de resultados de laboratorio | HU-12, HU-13 | YS-114 a YS-119 | **Implementado en parte** (19/10): el laboratorio consulta sus órdenes en una bandeja con el paciente, el examen, las indicaciones y el médico que lo solicitó; filtra por estado, busca al paciente por nombre o DPI y toma cada orden, que pasa a "En Proceso" (HU-12). Falta subir o guardar el resultado (HU-13, Sprint 3) |
| RF-10 | Receta y dosis de medicamentos | HU-10 | YS-108 a YS-110 | **Implementado** (13/10): durante la visita activa, el especialista registra la receta con cada medicamento, su dosis, cantidad y tiempo o forma de aplicación, todos obligatorios. La receta queda asociada a la visita y pendiente de despacho para la farmacia (HU-15), y se puede corregir mientras la farmacia no haya entregado nada |
| RF-11 | Despacho de farmacia | HU-15, HU-16, HU-17 | YS-123 a YS-131 | Sprint 3, 12–13/10 |
| RF-12 | Control de cuentas de familiares | HU-18, HU-19 | YS-132 a YS-138 | Sprint 3, 12–13/10. El costo de la cita ya existe: cada visita guarda la tarifa de consulta de su especialidad (YS-169, HU-07), y al finalizarla se registra su `CargoServicio`, con monto original, descuento y neto (HU-09). HU-19 calcula el descuento; HU-18 consolida los cargos en la cuenta mensual |
| RF-13 | Control de cuentas por pagar | HU-20 | YS-139 a YS-141 | Sprint 3, 13/10 |
| RF-14 | Gestión de citas pendientes | HU-21 | YS-142 a YS-144 | Sprint 3, 13/10 |
| RF-15 | Registro de donaciones | HU-22 | YS-145 a YS-147 | Sprint 3, 13/10 |
| RF-16 | Registro de gastos operativos | HU-23 | YS-148 a YS-150 | Sprint 3, 13/10 |
| RF-17 | Reporte estadístico de costos | HU-24 | YS-151 a YS-153 | Sprint 3, 14/10 |
| RF-18 | Reporte de análisis médicos | HU-25 | YS-154 a YS-156 | Sprint 3, 14/10 |
| RF-19 | Reporte de cobros a familiares | HU-26 | YS-157 a YS-159 | Sprint 3, 14/10 |
| RF-20a | Reporte de pagos a la fundación | HU-27 | YS-160 a YS-162 | Sprint 3, 14/10. HU-27 se dividió en criterios de aceptación separados para RF-20a y RF-20b (v1.1) |
| RF-20b | Reporte de entradas | HU-27 | YS-160 a YS-162 | Sprint 3, 14/10 |
| RF-21a | Reporte de exámenes por paciente | HU-28 | YS-163 a YS-165 | Sprint 3, 14/10. HU-28 se dividió en criterios de aceptación separados para RF-21a y RF-21b (v1.1) |
| RF-21b | Reporte de medicamentos por paciente | HU-28 | YS-163 a YS-165 | Sprint 3, 14/10 |
| RF-22 | Registro de pagos del familiar | HU-34 (YS-173) | Por crear | Sprint 3. Nuevo en v1.1; sin él, RF-12 y RF-14 no pueden cambiar el estado de los servicios pagados |

**Cobertura planificada: 24 de 24.** Implementados al 19/10: RF-01, RF-02, RF-03, RF-04, RF-05, RF-06, RF-07 y RF-10; en parte, RF-08 y RF-09. Si el tiempo aprieta, lo que se ajusta es el nivel de detalle de cada historia (`fase-diseno.md`, 1.7, Nivel 1), nunca la lista de requerimientos.

## 2. Requerimientos no funcionales

| RNF | Requerimiento | Cómo se cumple | Estado |
|---|---|---|---|
| RNF-01 | Arquitectura web, accesible desde Chrome, Edge y Safari | Aplicación React servida por navegador; adaptada a tableta y teléfono (HU-33) | Hecho. **Nunca se probó en Safari** — ver hallazgo 5 |
| RNF-02 | Siete perfiles estrictos; cada quien ve solo lo de su competencia | Los siete roles coinciden uno a uno con la lista del requerimiento. Restricción por endpoint con `@PreAuthorize` (HU-31), menú según el rol (YS-81) y barrido de los siete roles en las pruebas (YS-82) | Hecho |
| RNF-03 | Contraseñas como hash de un solo sentido (BCrypt); datos clínicos solo para los roles clínicos, nunca para el Administrador del Asilo (v1.2) | Contraseñas con BCrypt (HU-30). Datos clínicos separados de los personales en entidades distintas (YS-57), y el Administrador del Asilo excluido de la ficha médica (YS-58). Cambio obligatorio de contraseñas asignadas (HU-32). Los reportes y el estado de cuenta del Administrador del Asilo se desglosan por tipo de servicio, sin nombrar exámenes ni medicamentos (YS-134, YS-157) | Hecho para lo construido; cada subtarea nueva lo prueba |
| RNF-04 | Interfaz clara y legible para personal no experto | Sistema de diseño con tipografía mínima de 14 px, áreas táctiles de 44 px y mensajes que explican el motivo, no solo la orden | Hecho, transversal |
| RNF-05 | Disponibilidad del 99 % | Desplegado el 15/10/2026 en Oracle Cloud, con reinicio automático de contenedores, respaldo diario y monitoreo cada 5 minutos con UptimeRobot (`fase-diseno.md`, 2.5 y 2.6) | **En medición** desde el 15/10: el porcentaje sale del historial de UptimeRobot |
| RNF-06 | Historiales y reportes financieros en 3 segundos o menos, con 20 usuarios simultáneos y al menos 100 internos (v1.2) | Pruebas de desempeño y de carga PP-01 y PP-02 sobre el servidor desplegado | **Sin medir con carga.** Con un solo usuario, la ruta de salud responde en unos 216 ms — ver hallazgo 4 |

## 3. Hallazgos

### Corregido durante esta revisión

**1. RF-03: quién crea la referencia.** El requerimiento dice que la crea *el médico general*, *asignando un enfermero(a) acompañante*. El objetivo del Sprint 3 en `fase-diseno.md` y las subtareas YS-84 y YS-85 decían que también la creaba Enfermería. Se corrigieron los tres: la crea solo el Médico General, y el enfermero es un campo de la solicitud, no quien la origina. YS-86 ahora prueba explícitamente que Enfermería recibe 403 al intentarlo. De paso, YS-87 y YS-89 se alinearon con la tabla de API del diseño, que también le da lectura de la bandeja al Médico General.

**2. RF-02 dependía de una historia que estaba fuera del plan.** El requerimiento pide un historial que consolide *visitas, diagnósticos y padecimientos*, y las visitas y los diagnósticos los produce el RF-06 (HU-07, HU-09). El primer re-plan del 8 de octubre había sacado el RF-06 de la entrega, y con eso el historial solo habría podido mostrar la ficha. Al restaurar los 21 RF, HU-02 se movió para después de la visita médica (11/10): se construye cuando ya hay qué consolidar.

**3. La cobertura funcional.** Se confirmó que la entrega se evalúa contra los 21 RF. El primer re-plan cubría 4 y el plan original 11; el plan vigente cubre los 21. Todas las historias tienen sus subtareas en Jira desde el 8 de octubre.

### Abierto

**4. RNF-06 sin evidencia de carga.** El despliegue del 15/10 cerró la parte de RNF-05 que dependía de tener servidor y monitoreo. RNF-06 ahora fija sus condiciones (v1.2): falta cargar al menos 100 internos de prueba y medir con 20 usuarios simultáneos las consultas de historial y los reportes financieros (PP-01 y PP-02). Los reportes llegan en el Sprint 3, así que la prueba de carga va después de ellos.

**5. RNF-01 nombra Safari, y no se probó.** Todas las verificaciones visuales se hicieron en navegadores basados en Chromium. Safari en iPad es además el dispositivo previsto para las rondas de enfermería, así que es la prueba que más vale hacer antes de entregar. Está planificada para el 15/10.

### Coincide, con matices que conviene tener preparados

**6. "Contraseñas encriptadas". Resuelto en la v1.2.** RNF-03 ahora dice *hash con un algoritmo de un solo sentido (BCrypt)*, que es lo que el sistema hace: las contraseñas no se pueden descifrar ni siquiera teniendo acceso a la base, y al iniciar sesión se compara el hash de lo escrito con el guardado.

**7. El alta del interno va en dos pasos.** El RF-01 se cumple —se registran los datos, las psicopatologías y los medicamentos—, pero los datos clínicos se escriben por un endpoint propio después del alta, para que haya un solo camino hacia cada dato. La tabla de API de `fase-diseno.md` (4.9) ya lo refleja.

**8. La cuota mensual es opcional al registrar** (YS-59). Ningún RF la exige en el alta, y quien da de alta al interno es el Médico General, a quien la cuota no le compete. Hoy solo puede completarla él al actualizar el interno; la vía para el Administrador del Asilo, `PUT /api/internos/{id}/cuota`, se agregó a YS-132, junto con la consolidación de la cuenta que la usa (RF-12).

**9. Lo construido que no pide ningún RF.** Contraseña temporal obligatoria (HU-32), uso en tableta (HU-33), expiración de sesión y registro de accesos denegados. No contradicen nada: todos sirven a RNF-01, RNF-02, RNF-03 o RNF-04.

### Sobre el propio documento de requerimientos

**10.** Su encabezado nombra al cliente *Asilo "Nueva Vida"*, y el sistema usa *Asilo de Ancianos Cabeza de Algodón*. Es una decisión documentada (Notion, página "Estilo"): el nombre del documento de Análisis de Sistemas I se descartó por el del enunciado del proyecto. No es una desviación de los requerimientos, pero quien lea los dos documentos lo va a notar.

**11.** Las líneas sobrantes al final del archivo, que no eran requerimientos, se quitaron al publicar la versión 1.1.