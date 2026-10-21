import { api } from "./api";

/**
 * HU-20 (YS-140). Lo que el asilo le debe a la fundación, por mes y tipo de
 * servicio, del mes más reciente al más antiguo.
 */
export async function listarCuentasPorPagar() {
  const { data } = await api.get("/cuentas-por-pagar");
  return data;
}

/** HU-20 (YS-140). Una deuda ("2026-10", "CONSULTA") con sus pagos: `{ cuenta, pagos }`. */
export async function obtenerPagosFundacion(periodo, tipoServicio) {
  const { data } = await api.get(`/cuentas-por-pagar/${periodo}/${tipoServicio}/pagos`);
  return data;
}

/**
 * HU-20 (YS-140). Registra un pago a la fundación sobre esa deuda. El monto va
 * como texto, como los demás montos. Responde `{ cuenta, pagos }`.
 */
export async function registrarPagoFundacion(periodo, tipoServicio, { monto, fecha, formaPago, referencia }) {
  const { data } = await api.post(`/cuentas-por-pagar/${periodo}/${tipoServicio}/pagos`, {
    monto,
    fecha,
    formaPago,
    referencia,
  });
  return data;
}

/** HU-20 (YS-140). Anula un pago a la fundación mal registrado, con su motivo: `{ cuenta, pagos }`. */
export async function anularPagoFundacion(pagoId, motivo) {
  const { data } = await api.post(`/pagos-fundacion/${pagoId}/anulacion`, { motivo });
  return data;
}
