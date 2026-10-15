# Documento de Requerimientos del Sistema
**Contexto del Proyecto:** Sistema de gestión médica y financiera para el Asilo y su interacción con la Fundación.

**Versión:** 1.1 — 15 de octubre de 2026. Incorpora la revisión de requerimientos del 11 de octubre de 2026 (ver sección 3).

## 1. Requerimientos Funcionales (RF)

*   **RF-01: Gestión de Perfil del Interno**
    *   *Descripción:* El sistema debe permitir registrar, editar y consultar la información personal de cada interno, incluyendo sus psicopatologías y medicamentos.
*   **RF-02: Historial Médico Centralizado**
    *   *Descripción:* El sistema debe almacenar el historial médico de cada paciente, consolidando todas sus visitas, diagnósticos y padecimientos.
*   **RF-03: Solicitudes de Referencia Médica**
    *   *Descripción:* El sistema debe permitir al médico general crear una solicitud web para referir a un paciente a una especialidad de la fundación, asignando un enfermero(a) acompañante.
*   **RF-04: Notificación por Correo Electrónico** *(modificado en v1.1)*
    *   *Descripción:* El sistema debe enviar automáticamente un correo electrónico al familiar responsable del paciente en dos momentos: (1) al crearse la solicitud de referencia, con el número de solicitud, la especialidad y el estado de la solicitud; y (2) al asignarse la cita, con el nombre del especialista, la fecha y la hora. Por privacidad, el correo no incluye diagnósticos ni el motivo clínico de la referencia. Si el envío falla, la solicitud se guarda igual y el correo se reintenta.
*   **RF-05: Módulo de Asignación**
    *   *Descripción:* El sistema debe contar con un módulo para que la fundación visualice las solicitudes y asigne la fecha, horario y el médico especialista que atenderá al paciente.
*   **RF-06: Gestión de Visita Médica**
    *   *Descripción:* El sistema debe permitir al médico especialista convertir la solicitud en una visita médica, visualizando el motivo y registrando la fecha, diagnóstico y observaciones.
*   **RF-07: Consulta de Historial por Especialista**
    *   *Descripción:* El sistema debe otorgar acceso al médico para visualizar la ficha médica completa del paciente durante la consulta para analizar sus padecimientos.
*   **RF-08: Asignación de Exámenes de Laboratorio** *(modificado en v1.1)*
    *   *Descripción:* El sistema debe permitir al médico especialista asignar uno o más exámenes de laboratorio a la visita médica en curso, generando una orden en estado "Pendiente". El costo de cada examen lo registra el laboratorio al cargar el resultado (RF-09), tomando como valor sugerido el costo de referencia del catálogo de exámenes.
*   **RF-09: Gestión de Resultados de Laboratorio**
    *   *Descripción:* El sistema debe permitir al personal del laboratorio clínico consultar la visita médica, identificar al paciente y subir o guardar los resultados para que el médico los consulte.
*   **RF-10: Receta y Dosis de Medicamentos**
    *   *Descripción:* El sistema debe incluir un área específica donde el médico indique el medicamento aplicado o recetado, la cantidad y el tiempo o forma de tomarlo.
*   **RF-11: Despacho de Farmacia**
    *   *Descripción:* El sistema debe permitir a la farmacia despachar la receta y asignar el costo del medicamento directamente a la visita médica del paciente.
*   **RF-12: Control de Cuentas de Familiares**
    *   *Descripción:* El sistema debe cargar automáticamente a la cuenta mensual del familiar el costo de las citas, exámenes y farmacia, aplicando los descuentos de la fundación.
*   **RF-13: Control de Cuentas por Pagar**
    *   *Descripción:* El sistema debe registrar el costo total que el asilo le adeuda a la fundación por concepto de consultas, medicamentos y laboratorio.
*   **RF-14: Gestión de Citas Pendientes** *(modificado en v1.1)*
    *   *Descripción:* El sistema debe mostrar un listado de las citas y servicios médicos que no han sido pagados por completo, con el paciente, la fecha, el monto total, el monto pagado, el saldo pendiente y el estado ("Sin pago" o "Pago parcial"), filtrable por paciente y por rango de fechas.
*   **RF-15: Registro de Donaciones** *(modificado en v1.1)*
    *   *Descripción:* El sistema debe permitir registrar donaciones monetarias con el monto, la fecha, el nombre del donante y su tipo: empresa internacional, empresa nacional, gobierno o persona particular. Debe permitir consultar el histórico de donaciones filtrado por tipo de donante y por rango de fechas.
*   **RF-16: Registro de Gastos Operativos**
    *   *Descripción:* El sistema debe contar con un módulo para registrar los gastos operativos del asilo, tales como pagos de luz, agua, entre otros.
*   **RF-17: Reporte Estadístico de Costos**
    *   *Descripción:* El sistema debe generar un informe estadístico de los costos de cada cita por paciente, desglosando exámenes y medicamentos.
*   **RF-18: Reporte de Análisis Médicos**
    *   *Descripción:* El sistema debe generar un informe por paciente mostrando su ficha médica, motivo de reclusión y medicamentos recetados.
*   **RF-19: Reporte de Cobros a Familiares**
    *   *Descripción:* El sistema debe generar un informe de cobros por paciente filtrable por rango de fechas, incluyendo el detalle de los gastos médicos.
*   **RF-20a: Reporte de Pagos a la Fundación** *(nuevo en v1.1, reemplaza a RF-20)*
    *   *Descripción:* El sistema debe generar un reporte de los pagos realizados a la fundación, filtrable por rango de fechas, con la fecha, el concepto (consulta, laboratorio o farmacia) y el monto de cada pago.
*   **RF-20b: Reporte de Entradas** *(nuevo en v1.1, reemplaza a RF-20)*
    *   *Descripción:* El sistema debe generar un reporte de entradas filtrable por rango de fechas que incluya las donaciones recibidas y los cobros a familiares, con el total por cada tipo de entrada.
*   **RF-21a: Reporte de Exámenes por Paciente** *(nuevo en v1.1, reemplaza a RF-21)*
    *   *Descripción:* El sistema debe generar un reporte de los exámenes de laboratorio realizados a un paciente, filtrable por rango de fechas, con el examen, la fecha y el resultado.
*   **RF-21b: Reporte de Medicamentos por Paciente** *(nuevo en v1.1, reemplaza a RF-21)*
    *   *Descripción:* El sistema debe generar un reporte de los medicamentos aplicados a un paciente, filtrable por rango de fechas, con el medicamento, la dosis, la cantidad y la fecha de entrega.
*   **RF-22: Registro de Pagos del Familiar** *(nuevo en v1.1)*
    *   *Descripción:* El sistema debe permitir al Administrador del Asilo registrar los pagos que realiza el familiar sobre su cuenta mensual (cuota del asilo más servicios médicos con descuento), con la fecha, el monto y la forma de pago. Un pago total cambia los servicios incluidos a "Completamente cancelados"; un pago parcial se descuenta del saldo y los servicios quedan pendientes hasta cubrir el total.

---

## 2. Requerimientos No Funcionales (RNF)

*   **RNF-01: Arquitectura Web**
    *   *Descripción:* El sistema debe estar desarrollado bajo una arquitectura web, accesible desde navegadores estándar (Chrome, Edge, Safari) sin necesidad de instalar software local.
*   **RNF-02: Control de Roles y Accesos**
    *   *Descripción:* El sistema debe tener perfiles de usuario estrictos (Médico General, Especialista, Enfermero, Laboratorio, Farmacia, Administrador del Asilo, Administrador de la Fundación) para que cada quien vea solo la información de su competencia. *(Nota: Se ha consolidado a 7 perfiles según la ampliación indicada en la página 25 del documento base).*
*   **RNF-03: Privacidad y Seguridad**
    *   *Descripción:* Las contraseñas de los usuarios deben estar encriptadas y la base de datos debe proteger la privacidad de los historiales médicos de los pacientes del asilo.
*   **RNF-04: Usabilidad e Interfaz Intuitiva**
    *   *Descripción:* La interfaz gráfica debe ser clara y sencilla de utilizar, con tipografía legible, considerando que parte del personal administrativo o de enfermería puede no ser experto en tecnología.
*   **RNF-05: Disponibilidad**
    *   *Descripción:* El sistema debe estar disponible y en línea al menos un 99% del tiempo, ya que las emergencias médicas y consultas de historiales no pueden depender de caídas del servidor.
*   **RNF-06: Rendimiento y Tiempos de Respuesta**
    *   *Descripción:* El sistema debe procesar las consultas a los historiales médicos y la generación de reportes financieros en un tiempo máximo de 3 a 5 segundos.

---

## 3. Control de cambios

| Versión | Fecha | Cambio |
|---|---|---|
| 1.1 | 15/10/2026 | Revisión de requerimientos del 11/10/2026: se modifican RF-04, RF-08, RF-14 y RF-15; RF-20 se divide en RF-20a y RF-20b; RF-21 se divide en RF-21a y RF-21b; se agrega RF-22. |

| Requerimiento | Motivo del cambio | Historias de usuario |
|---|---|---|
| RF-04 | Al crear la solicitud aún no hay especialista; se describen los dos correos y se excluye el dato clínico por privacidad. | HU-04, HU-06 |
| RF-08 | El costo del examen tenía dos responsables; queda solo el laboratorio. | HU-11, HU-14 |
| RF-14 | "Indicar claramente" no era verificable; se definen los datos y filtros del listado. | HU-21 |
| RF-15 | Se nombran los cuatro tipos de donante del enunciado. | HU-22 |
| RF-20a, RF-20b | Un requerimiento contenía dos reportes distintos; se separan y se precisa qué entradas incluye. | HU-27 |
| RF-21a, RF-21b | Un requerimiento contenía dos reportes distintos; se separan y se corrige la redacción. | HU-28 |
| RF-22 | Ningún requerimiento cubría el registro del pago del familiar, necesario para RF-12 y RF-14. | HU-34 |