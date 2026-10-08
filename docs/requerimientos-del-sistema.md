# Documento de Requerimientos del Sistema
**Contexto del Proyecto:** Sistema de gestión médica y financiera para el Asilo y su interacción con la Fundación.

## 1. Requerimientos Funcionales (RF)

*   **RF-01: Gestión de Perfil del Interno**
    *   *Descripción:* El sistema debe permitir registrar, editar y consultar la información personal de cada interno, incluyendo sus psicopatologías y medicamentos.
*   **RF-02: Historial Médico Centralizado**
    *   *Descripción:* El sistema debe almacenar el historial médico de cada paciente, consolidando todas sus visitas, diagnósticos y padecimientos.
*   **RF-03: Solicitudes de Referencia Médica**
    *   *Descripción:* El sistema debe permitir al médico general crear una solicitud web para referir a un paciente a una especialidad de la fundación, asignando un enfermero(a) acompañante.
*   **RF-04: Notificación por Correo Electrónico**
    *   *Descripción:* El sistema debe enviar automáticamente un correo electrónico al familiar del paciente cuando se cree una solicitud médica, informando el estado y el especialista asignado.
*   **RF-05: Módulo de Asignación**
    *   *Descripción:* El sistema debe contar con un módulo para que la fundación visualice las solicitudes y asigne la fecha, horario y el médico especialista que atenderá al paciente.
*   **RF-06: Gestión de Visita Médica**
    *   *Descripción:* El sistema debe permitir al médico especialista convertir la solicitud en una visita médica, visualizando el motivo y registrando la fecha, diagnóstico y observaciones.
*   **RF-07: Consulta de Historial por Especialista**
    *   *Descripción:* El sistema debe otorgar acceso al médico para visualizar la ficha médica completa del paciente durante la consulta para analizar sus padecimientos.
*   **RF-08: Asignación de Exámenes de Laboratorio**
    *   *Descripción:* El sistema debe permitir al médico asignar exámenes de laboratorio a la visita médica en curso, registrando el costo para el asilo.
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
*   **RF-14: Gestión de Citas Pendientes**
    *   *Descripción:* El sistema debe identificar e indicar claramente qué citas médicas o servicios aún no han sido completamente cancelados.
*   **RF-15: Registro de Donaciones**
    *   *Descripción:* El sistema debe permitir el registro y clasificación de donaciones monetarias.
*   **RF-16: Registro de Gastos Operativos**
    *   *Descripción:* El sistema debe contar con un módulo para registrar los gastos operativos del asilo, tales como pagos de luz, agua, entre otros.
*   **RF-17: Reporte Estadístico de Costos**
    *   *Descripción:* El sistema debe generar un informe estadístico de los costos de cada cita por paciente, desglosando exámenes y medicamentos.
*   **RF-18: Reporte de Análisis Médicos**
    *   *Descripción:* El sistema debe generar un informe por paciente mostrando su ficha médica, motivo de reclusión y medicamentos recetados.
*   **RF-19: Reporte de Cobros a Familiares**
    *   *Descripción:* El sistema debe generar un informe de cobros por paciente filtrable por rango de fechas, incluyendo el detalle de los gastos médicos.
*   **RF-20: Reportes Financieros Consolidados**
    *   *Descripción:* El sistema debe generar informes detallados de: pagos realizados a la fundación y un reporte general de Entradas.
*   **RF-21: Reportes Médicos Específicos**
    *   *Descripción:* El sistema debe generar reportes individuales que Listen los exámenes médicos realizados por paciente y los medicamentos aplicados por paciente.

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
```eof