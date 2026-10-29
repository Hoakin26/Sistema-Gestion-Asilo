import { api } from "./api";

/**
 * HU-22 (YS-146). El histórico de donaciones entre dos fechas ("AAAA-MM-DD") y
 * de un tipo de donante; sin filtros, todas. Responde
 * `{ donaciones, total, porTipo }`.
 *
 * OBS-01. `donaciones` es una página de 20 (`content`, `totalElements`,
 * `totalPages`); los totales son de todas las donaciones del filtro.
 */
export async function listarDonaciones({ desde, hasta, tipo }, pagina = 0) {
  const { data } = await api.get("/donaciones", {
    params: { desde: desde || undefined, hasta: hasta || undefined, tipo: tipo || undefined, page: pagina },
  });
  return data;
}

/** HU-22 (YS-146). Registra una donación. El monto va como texto, como los demás montos. */
export async function registrarDonacion({ monto, fecha, donante, tipoDonante }) {
  const { data } = await api.post("/donaciones", { monto, fecha, donante, tipoDonante });
  return data;
}

/** HU-22 (YS-146). Anula una donación mal registrada, con su motivo. */
export async function anularDonacion(id, motivo) {
  const { data } = await api.post(`/donaciones/${id}/anulacion`, { motivo });
  return data;
}
