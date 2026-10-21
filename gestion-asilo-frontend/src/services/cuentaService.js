import { api } from "./api";

/** HU-18 (YS-134). Las cuentas ya consolidadas de un mes ("2026-10"). */
export async function listarCuentas(periodo) {
  const { data } = await api.get("/cuentas", { params: { periodo } });
  return data;
}

/**
 * HU-18 (YS-134). Consolida el mes: crea o recalcula la cuenta de cada
 * interno. Consolidar dos veces no duplica nada.
 */
export async function consolidarCuentas(periodo) {
  const { data } = await api.post("/cuentas/consolidar", null, { params: { periodo } });
  return data;
}

/** HU-18 (YS-134). El estado de cuenta de un interno entre dos fechas ("AAAA-MM-DD"). */
export async function obtenerEstadoCuenta(internoId, { desde, hasta }) {
  const { data } = await api.get(`/internos/${internoId}/estado-cuenta`, { params: { desde, hasta } });
  return data;
}

/**
 * HU-34 (YS-177). Registra un pago del familiar sobre una cuenta mensual. El
 * monto va como texto, como los demás montos. Responde `{ cuenta, pagos }`: la
 * cuenta con su saldo y su estado nuevos, y el historial.
 */
export async function registrarPago(cuentaId, { monto, fecha, formaPago, referencia }) {
  const { data } = await api.post(`/cuentas/${cuentaId}/pagos`, { monto, fecha, formaPago, referencia });
  return data;
}

/** HU-34 (YS-177). El historial de pagos de una cuenta, los anulados incluidos: `{ cuenta, pagos }`. */
export async function obtenerPagos(cuentaId) {
  const { data } = await api.get(`/cuentas/${cuentaId}/pagos`);
  return data;
}

/** HU-34 (YS-177). Anula un pago mal registrado, con su motivo: `{ cuenta, pagos }`. */
export async function anularPago(pagoId, motivo) {
  const { data } = await api.post(`/pagos/${pagoId}/anulacion`, { motivo });
  return data;
}

/** HU-18 (YS-134). La cuota base del interno, como texto: el backend la lee como BigDecimal. */
export async function actualizarCuota(internoId, cuotaMensual) {
  const { data } = await api.put(`/internos/${internoId}/cuota`, { cuotaMensual });
  return data;
}
