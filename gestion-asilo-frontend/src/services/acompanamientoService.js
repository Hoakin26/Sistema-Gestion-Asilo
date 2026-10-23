import { api } from "./api";

/**
 * YS-189. Los acompañamientos de quien inició sesión (Enfermería): las
 * solicitudes en que acompaña al interno, con su cita si ya la tienen.
 */
export async function listarAcompanamientos() {
  const { data } = await api.get("/acompanamientos");
  return data;
}
