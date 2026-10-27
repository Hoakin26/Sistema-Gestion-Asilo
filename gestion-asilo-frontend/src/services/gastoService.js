import { api } from "./api";

/**
 * HU-23 (YS-149). El histórico de gastos operativos entre dos fechas
 * ("AAAA-MM-DD") y de un tipo; sin filtros, todos. Responde
 * `{ gastos, total, porTipo }`.
 *
 * OBS-01. `gastos` es una página de 20 (`content`, `totalElements`,
 * `totalPages`); los totales son de todos los gastos del filtro.
 */
export async function listarGastos({ desde, hasta, tipo }, pagina = 0) {
  const { data } = await api.get("/gastos-operativos", {
    params: { desde: desde || undefined, hasta: hasta || undefined, tipo: tipo || undefined, page: pagina },
  });
  return data;
}

/** HU-23 (YS-149). Registra un gasto. El monto va como texto, como los demás montos. */
export async function registrarGasto({ tipoGasto, descripcion, monto, fecha }) {
  const { data } = await api.post("/gastos-operativos", { tipoGasto, descripcion, monto, fecha });
  return data;
}

/** HU-23 (YS-149). Anula un gasto mal registrado, con su motivo. */
export async function anularGasto(id, motivo) {
  const { data } = await api.post(`/gastos-operativos/${id}/anulacion`, { motivo });
  return data;
}
