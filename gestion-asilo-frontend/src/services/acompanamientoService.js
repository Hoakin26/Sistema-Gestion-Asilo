import { api } from "./api";

/**
 * YS-189. Los acompañamientos de quien inició sesión (Enfermería): las
 * solicitudes en que acompaña al interno, con su cita si ya la tienen.
 *
 * OBS-01. Los próximos (por asignar, con cita o en atención) llegan enteros,
 * porque nunca son muchos.
 */
export async function listarAcompanamientosProximos() {
  const { data } = await api.get("/acompanamientos/proximos");
  return data;
}

/**
 * OBS-01. Los anteriores (atendidos o cancelados), que crecen con cada mes:
 * una página de 20, el más reciente primero. Responde la página de Spring
 * (`content`, `totalElements`, `totalPages`).
 */
export async function listarAcompanamientosAnteriores(pagina = 0) {
  const { data } = await api.get("/acompanamientos/anteriores", { params: { page: pagina } });
  return data;
}
