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

  // DE-04 · Cuenta mensual del familiar (YS-134). Sin prefijo: los nombres no chocan.
  PENDIENTE_PAGO: "Pendiente de Pago",
  PAGO_PARCIAL: "Pago Parcial",
  COMPLETAMENTE_CANCELADA: "Completamente Cancelada",
  EN_MORA: "En Mora",

  // YS-140 · Deuda con la fundación. "PENDIENTE" es el mismo de la orden de
  // laboratorio, a propósito: un Pendiente se ve igual en todos los módulos.
  PAGADA: "Pagada",

  // YS-143 · Cita pendiente de pago (RF-14). "PAGO_PARCIAL" es el de DE-04.
  SIN_PAGO: "Sin Pago",
};

/**
 * YS-134. Los conceptos del estado de cuenta, por tipo de servicio y nunca por
 * nombre (RNF-03): las claves de `TipoServicio` del backend, más la cuota base.
 */
export const CONCEPTOS_CUENTA = {
  CUOTA: "Cuota base del asilo",
  CONSULTA: "Consultas médicas",
  LABORATORIO: "Exámenes de laboratorio",
  FARMACIA: "Farmacia",
};

/** YS-134. Cada cargo, en singular, para el detalle del estado de cuenta. */
export const SERVICIOS = {
  CONSULTA: "Consulta",
  LABORATORIO: "Laboratorio",
  FARMACIA: "Farmacia",
};

/** YS-177. Cómo pagó el familiar, con las claves de `FormaPago` del backend. */
export const FORMAS_PAGO = {
  EFECTIVO: "Efectivo",
  TRANSFERENCIA: "Transferencia bancaria",
  DEPOSITO: "Depósito bancario",
  TARJETA: "Tarjeta",
  CHEQUE: "Cheque",
};

/** YS-143. Cuántos servicios de un tipo tuvo una cita, en singular y en plural. */
export const SERVICIOS_CONTADOS = {
  CONSULTA: ["consulta", "consultas"],
  LABORATORIO: ["examen", "exámenes"],
  FARMACIA: ["medicamento", "medicamentos"],
};

/** YS-146. Quién dona (RF-15), con las claves de `TipoDonante` del backend y en su orden. */
export const TIPOS_DONANTE = {
  EMPRESA_INTERNACIONAL: "Empresa internacional",
  EMPRESA_NACIONAL: "Empresa nacional",
  GOBIERNO: "Gobierno",
  PARTICULAR: "Persona particular",
};

/** YS-149. En qué gasta el asilo (RF-16), con las claves de `TipoGasto` del backend y en su orden. */
export const TIPOS_GASTO = {
  LUZ: "Luz",
  AGUA: "Agua",
  TELEFONO_INTERNET: "Teléfono e internet",
  GAS: "Gas",
  ALIMENTACION: "Alimentación",
  LIMPIEZA: "Limpieza",
  MANTENIMIENTO: "Mantenimiento",
  OTRO: "Otro",
};

/** YS-137. Los tipos de servicio con descuento de la fundación, en el orden de `TipoServicio`. */
export const TIPOS_SERVICIO = ["CONSULTA", "LABORATORIO", "FARMACIA"];

/** YS-137. A qué se le resta un descuento de monto fijo: es por cargo, y cada servicio cobra distinto. */
export const POR_CARGO = {
  CONSULTA: "por consulta",
  LABORATORIO: "por examen",
  FARMACIA: "por medicamento entregado",
};

/** DE-02, en el orden del diagrama: para el filtro de la bandeja del laboratorio (YS-115). */
export const ESTADOS_ORDEN = ["PENDIENTE", "EN_PROCESO", "RESULTADOS_CARGADOS", "ANULADA"];

/** DE-03, en el orden del diagrama: para el filtro de la bandeja de la farmacia (YS-124). */
export const ESTADOS_RECETA = ["PENDIENTE_DESPACHO", "PARCIALMENTE_ENTREGADA", "ENTREGADA", "MEDICAMENTO_AGOTADO"];

/** DE-01, en el orden del diagrama: para los filtros de las bandejas. */
export const ESTADOS_SOLICITUD = [
  "PENDIENTE_ASIGNACION",
  "CITA_PROGRAMADA",
  "EN_ATENCION",
  "ATENDIDA",
  "CANCELADA",
];
