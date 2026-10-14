/**
 * YS-88. Los nombres de los estados tal como los lee una persona, con las
 * claves de los enums del backend. Los usan el `StatusBadge` y los filtros,
 * para que una misma clave no se lea distinto en dos lugares.
 */
export const ETIQUETAS_ESTADO = {
  ACTIVO: "Activo",
  INACTIVO: "Inactivo",

  // DE-01 · Solicitud de referencia médica.
  PENDIENTE_ASIGNACION: "Pendiente de Asignación",
  CITA_PROGRAMADA: "Cita Programada",
  EN_ATENCION: "En Atención",
  ATENDIDA: "Atendida",
  CANCELADA: "Cancelada",

  // YS-95 · Aviso por correo al familiar. Con prefijo porque "PENDIENTE"
  // también será un estado de la orden de laboratorio (DE-02).
  AVISO_PENDIENTE: "Por enviar",
  AVISO_ENVIADA: "Enviado",
  AVISO_FALLIDA: "No se pudo enviar",
  AVISO_SIN_CORREO: "Sin correo registrado",

  // YS-98 · Visita médica. Con prefijo, igual que el aviso.
  VISITA_ACTIVA: "En curso",
  VISITA_FINALIZADA: "Finalizada",

  // DE-02 · Orden de laboratorio (YS-111). "PENDIENTE" es suya: por eso el
  // aviso al familiar lleva prefijo.
  PENDIENTE: "Pendiente",
  EN_PROCESO: "En Proceso",
  RESULTADOS_CARGADOS: "Resultados Cargados",
  ANULADA: "Anulada",

  // DE-03 · Receta médica (YS-108). Sin prefijo: los nombres no chocan.
  PENDIENTE_DESPACHO: "Pendiente de Despacho",
  PARCIALMENTE_ENTREGADA: "Parcialmente Entregada",
  ENTREGADA: "Entregada",
  MEDICAMENTO_AGOTADO: "Medicamento Agotado",
};

/** DE-01, en el orden del diagrama: para los filtros de las bandejas. */
export const ESTADOS_SOLICITUD = [
  "PENDIENTE_ASIGNACION",
  "CITA_PROGRAMADA",
  "EN_ATENCION",
  "ATENDIDA",
  "CANCELADA",
];
