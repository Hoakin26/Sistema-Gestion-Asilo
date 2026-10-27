import { api } from "./api";

/**
 * HU-21 (YS-143). Las citas que el familiar todavía no termina de pagar, con
 * el paciente (nombre o DPI) y el rango de fechas que se pidan; sin filtros,
 * todas. Responde `{ citas, montoTotal, montoPagado, saldoPendiente }`.
 *
 * OBS-01. `citas` es una página de 20 (`content`, `totalElements`,
 * `totalPages`); los totales son de todas las citas del filtro.
 */
export async function listarServiciosPendientes({ paciente, desde, hasta }, pagina = 0) {
  const { data } = await api.get("/servicios-pendientes", {
    params: { paciente: paciente || undefined, desde: desde || undefined, hasta: hasta || undefined, page: pagina },
  });
  return data;
}
