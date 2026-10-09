import { api } from "./api";

/**
 * HU-07 (YS-99). Las citas del especialista en un día ("AAAA-MM-DD"). El
 * backend responde 404 si la cuenta todavía no tiene perfil de especialista.
 */
export async function obtenerMiAgenda(fecha) {
  const { data } = await api.get("/citas/mi-agenda", { params: { fecha } });
  return data;
}

/** Convierte la cita en una visita activa; la solicitud pasa a En Atención. */
export async function iniciarVisita(citaId) {
  const { data } = await api.post(`/citas/${citaId}/visita`);
  return data;
}

/** Las visitas del especialista; `estado` es "ACTIVA", "FINALIZADA" o vacío para todas. */
export async function listarVisitas(estado = "") {
  const { data } = await api.get("/visitas", { params: { estado: estado || undefined } });
  return data;
}

export async function obtenerVisita(id) {
  const { data } = await api.get(`/visitas/${id}`);
  return data;
}
