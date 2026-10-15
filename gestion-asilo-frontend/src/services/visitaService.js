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

/** HU-09 (YS-105). Guarda el diagnóstico (obligatorio) y las observaciones de la visita activa. */
export async function guardarDiagnostico(visitaId, diagnostico) {
  const { data } = await api.put(`/visitas/${visitaId}/diagnostico`, diagnostico);
  return data;
}

/**
 * HU-09 (YS-105). Cierra la visita: la solicitud pasa a Atendida y se registra
 * el cargo de la consulta. 409 si la especialidad todavía no tiene tarifa.
 */
export async function finalizarVisita(visitaId) {
  const { data } = await api.patch(`/visitas/${visitaId}/finalizar`);
  return data;
}

/** HU-10 (YS-108). La receta de la visita, o null si todavía no tiene. */
export async function obtenerReceta(visitaId) {
  try {
    const { data } = await api.get(`/visitas/${visitaId}/receta`);
    return data;
  } catch (fallo) {
    if (fallo.response?.status === 404) return null;
    throw fallo;
  }
}

/** HU-10 (YS-108). Una receta por visita: nace pendiente de despacho. */
export async function registrarReceta(visitaId, receta) {
  const { data } = await api.post(`/visitas/${visitaId}/receta`, receta);
  return data;
}

/** HU-10 (YS-108). Se manda entera; 409 si la farmacia ya empezó a despacharla. */
export async function corregirReceta(visitaId, receta) {
  const { data } = await api.put(`/visitas/${visitaId}/receta`, receta);
  return data;
}

/** HU-11 (YS-111). El catálogo de exámenes de laboratorio que se pueden ordenar. */
export async function listarExamenesLaboratorio() {
  const { data } = await api.get("/examenes-laboratorio");
  return data;
}

/** HU-11 (YS-111). Una orden pendiente por examen; devuelve las órdenes creadas. */
export async function ordenarExamenes(visitaId, examenes) {
  const { data } = await api.post(`/visitas/${visitaId}/ordenes-laboratorio`, { examenes });
  return data;
}

/** HU-11 (YS-111). Las órdenes de laboratorio generadas desde la visita. */
export async function listarOrdenes(visitaId) {
  const { data } = await api.get(`/visitas/${visitaId}/ordenes-laboratorio`);
  return data;
}

/** HU-08 (YS-102). Los antecedentes del paciente; el backend responde 403 si la visita no está activa. */
export async function obtenerHistorial(visitaId) {
  const { data } = await api.get(`/visitas/${visitaId}/historial`);
  return data;
}
