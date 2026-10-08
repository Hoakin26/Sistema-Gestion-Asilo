# Trazabilidad: requerimientos ↔ implementación

Contrasta lo construido y lo planificado contra `requerimientos-del-sistema.md`, que es la fuente de verdad del proyecto y **no se modifica**. Estado al 8 de octubre de 2026.

La correspondencia entre requerimientos e historias de usuario no está escrita en Jira —las historias no citan su RF—; la de abajo se derivó comparando la descripción de cada RF con la de cada historia.

## 1. Requerimientos funcionales

| RF | Requerimiento | Historias | Estado |
|---|---|---|---|
| RF-01 | Gestión de perfil del interno | HU-01 | **Sprint 2.** Backend terminado (YS-57, YS-58, YS-59); falta la interfaz (YS-60, YS-61) |
| RF-02 | Historial médico centralizado | HU-02 | **Sprint 2, solo parcial** — ver hallazgo 2 |
| RF-03 | Solicitudes de referencia médica | HU-03 | **Sprint 3** |
| RF-04 | Notificación por correo al familiar | HU-04 | Versión 1.1 |
| RF-05 | Módulo de asignación | HU-05, HU-06 | **Sprint 3** |
| RF-06 | Gestión de visita médica | HU-07, HU-09 | Versión 1.1 |
| RF-07 | Consulta de historial por especialista | HU-08 | Versión 1.1. El permiso ya existe: el especialista puede leer la ficha médica (`@PersonalClinico`, YS-58) |
| RF-08 | Asignación de exámenes de laboratorio | HU-11, HU-14 | Versión 1.1 |
| RF-09 | Gestión de resultados de laboratorio | HU-12, HU-13 | Versión 1.1 |
| RF-10 | Receta y dosis de medicamentos | HU-10 | Versión 1.1 |
| RF-11 | Despacho de farmacia | HU-15, HU-16, HU-17 | Versión 1.1 |
| RF-12 | Control de cuentas de familiares | HU-18, HU-19 | Versión 1.1 |
| RF-13 | Control de cuentas por pagar | HU-20 | Versión 1.1 |
| RF-14 | Gestión de citas pendientes | HU-21 | Versión 1.1 |
| RF-15 | Registro de donaciones | HU-22 | Versión 1.1 |
| RF-16 | Registro de gastos operativos | HU-23 | Versión 1.1 |
| RF-17 | Reporte estadístico de costos | HU-24 | Versión 1.1 |
| RF-18 | Reporte de análisis médicos | HU-25 | Versión 1.1 |
| RF-19 | Reporte de cobros a familiares | HU-26 | Versión 1.1 |
| RF-20 | Reportes financieros consolidados | HU-27 | Versión 1.1 |
| RF-21 | Reportes médicos específicos | HU-28 | Versión 1.1 |

**Cobertura en la entrega del 15/10: 4 de 21** (RF-01, RF-02 parcial, RF-03 y RF-05). El plan original cubría 11: también dejaba para después RF-04 y del RF-13 al RF-21.

## 2. Requerimientos no funcionales

| RNF | Requerimiento | Cómo se cumple | Estado |
|---|---|---|---|
| RNF-01 | Arquitectura web, accesible desde Chrome, Edge y Safari | Aplicación React servida por navegador; adaptada a tableta y teléfono (HU-33) | Hecho. **Nunca se probó en Safari** — ver hallazgo 5 |
| RNF-02 | Siete perfiles estrictos; cada quien ve solo lo de su competencia | Los siete roles coinciden uno a uno con la lista del requerimiento. Restricción por endpoint con `@PreAuthorize` (HU-31), menú según el rol (YS-81) y barrido de los siete roles en las pruebas (YS-82) | Hecho |
| RNF-03 | Contraseñas encriptadas; privacidad de los historiales médicos | Contraseñas con BCrypt (HU-30). Datos clínicos separados de los personales en entidades distintas (YS-57), y el Administrador del Asilo excluido de la ficha médica (YS-58). Cambio obligatorio de contraseñas asignadas (HU-32) | Hecho. Ver la nota de terminología en el hallazgo 6 |
| RNF-04 | Interfaz clara y legible para personal no experto | Sistema de diseño con tipografía mínima de 14 px, áreas táctiles de 44 px y mensajes que explican el motivo, no solo la orden | Hecho, transversal |
| RNF-05 | Disponibilidad del 99 % | — | **Sin cubrir.** El sistema no está desplegado todavía |
| RNF-06 | Respuesta en 3 a 5 segundos para historiales y reportes | — | **Sin medir.** Los reportes además pasaron a la versión 1.1 |

## 3. Hallazgos

### Corregido durante esta revisión

**1. RF-03: quién crea la referencia.** El requerimiento dice que la crea *el médico general*, *asignando un enfermero(a) acompañante*. El objetivo del Sprint 3 en `fase-diseno.md` y las subtareas YS-84 y YS-85 decían que también la creaba Enfermería. Se corrigieron los tres: la crea solo el Médico General, y el enfermero es un campo de la solicitud, no quien la origina. YS-86 ahora prueba explícitamente que Enfermería recibe 403 al intentarlo. De paso, YS-87 y YS-89 se alinearon con la tabla de API del diseño, que también le da lectura de la bandeja al Médico General.

### Abierto, requiere una decisión

**2. RF-02 no puede cumplirse entero en el plan actual.** El requerimiento pide un historial que consolide *visitas, diagnósticos y padecimientos*. Las visitas y los diagnósticos los produce el RF-06 (HU-07, HU-09), que el re-plan del 8 de octubre pasó a la versión 1.1. Con eso, el historial del Sprint 2 solo puede mostrar los padecimientos y medicamentos de la ficha. Es una dependencia que el re-plan pasó por alto. Las salidas son entregar el RF-02 como parcial y decirlo, o traer HU-07 y HU-09 al alcance, lo que agrega trabajo a un calendario que ya está justo.

**3. La cobertura funcional.** Si "el sistema debe coincidir con los requerimientos" significa que lo construido no los contradiga, el sistema hoy coincide. Si significa que **los 21 RF tienen que estar implementados en la entrega**, ni el plan actual (4 de 21) ni el original (11 de 21) lo logran, y la conversación con el catedrático es sobre la fecha, no sobre el alcance. Conviene confirmar cuál de las dos lecturas aplica antes de seguir.

**4. RNF-05 y RNF-06 sin evidencia.** Ninguno de los dos se puede demostrar sin desplegar. El despliegue está previsto en el Sprint 2 y es el riesgo más alto que queda abierto; la medición de tiempos de respuesta no tiene tarea asignada.

**5. RNF-01 nombra Safari, y no se probó.** Todas las verificaciones visuales se hicieron en navegadores basados en Chromium. Safari en iPad es además el dispositivo previsto para las rondas de enfermería, así que es la prueba que más vale hacer antes de entregar.

### Coincide, con matices que conviene tener preparados

**6. "Contraseñas encriptadas".** Se guardan con BCrypt, que es un *hash* y no un cifrado: no se pueden descifrar ni siquiera teniendo acceso a la base. Cumple la intención del requerimiento con una garantía más fuerte, pero si en la defensa se pregunta dónde se descifran, la respuesta es que nunca: al iniciar sesión se compara el hash de lo escrito con el guardado.

**7. El alta del interno va en dos pasos.** El RF-01 se cumple —se registran los datos, las psicopatologías y los medicamentos—, pero los datos clínicos se escriben por un endpoint propio después del alta, para que haya un solo camino hacia cada dato. La tabla de API de `fase-diseno.md` todavía describe el alta con la ficha incluida; ese documento sí se puede actualizar.

**8. La cuota mensual es opcional al registrar** (YS-59). Ningún RF la exige en el alta, y el RF-12, que es el que la usa, quedó para la versión 1.1. Hoy solo puede completarla el Médico General al actualizar el interno; la vía para el Administrador del Asilo llega con el módulo financiero.

**9. Lo construido que no pide ningún RF.** Contraseña temporal obligatoria (HU-32), uso en tableta (HU-33), expiración de sesión y registro de accesos denegados. No contradicen nada: todos sirven a RNF-01, RNF-02, RNF-03 o RNF-04.

### Sobre el propio documento de requerimientos

**10.** Su encabezado nombra al cliente *Asilo "Nueva Vida"*, y el sistema usa *Asilo de Ancianos Cabeza de Algodón*. Es una decisión documentada (Notion, página "Estilo"): el nombre del documento de Análisis de Sistemas I se descartó por el del enunciado del proyecto. No es una desviación de los requerimientos, pero quien lea los dos documentos lo va a notar.

**11.** Las líneas 66 a 68 del archivo no son requerimientos: es texto que quedó de quien lo generó. Como el documento no se modifica, se deja tal cual y se señala aquí.
