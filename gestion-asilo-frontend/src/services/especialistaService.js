import { api } from "./api";

/**
 * HU-06 (YS-167). Los especialistas de la fundación. Con `especialidadId`,
 * solo los activos que la atienden: a quiénes se les puede asignar la cita.
 */
export async function listarEspecialistas({ especialidadId } = {}) {
  const { data } = await api.get("/especialistas", {
    params: { especialidadId: especialidadId || undefined },
  });
  return data;
}

/** Las cuentas de Médico Especialista que todavía no tienen perfil. */
export async function listarUsuariosDisponibles() {
  const { data } = await api.get("/especialistas/usuarios-disponibles");
  return data;
}

export async function registrarEspecialista(especialista) {
  const { data } = await api.post("/especialistas", especialista);
  return data;
}

export async function actualizarEspecialista(id, especialista) {
  const { data } = await api.put(`/especialistas/${id}`, especialista);
  return data;
}

/** Las horas libres de un día ("AAAA-MM-DD"), como "08:00:00". */
export async function consultarDisponibilidad(especialistaId, fecha) {
  const { data } = await api.get(`/especialistas/${especialistaId}/disponibilidad`, {
    params: { fecha },
  });
  return data;
}

/** YS-91. Asigna la cita: especialista y fecha y hora ("AAAA-MM-DDTHH:mm"). */
export async function asignarCita(solicitudId, cita) {
  const { data } = await api.post(`/solicitudes-referencia/${solicitudId}/cita`, cita);
  return data;
}

/** YS-169. Las especialidades con su tarifa de consulta (null si no la tiene). */
export async function listarTarifas() {
  const { data } = await api.get("/especialidades/tarifas");
  return data;
}

export async function actualizarTarifa(especialidadId, tarifaConsulta) {
  const { data } = await api.put(`/especialidades/${especialidadId}/tarifa`, { tarifaConsulta });
  return data;
}

/** La cita de una solicitud; el backend responde 404 si todavía no tiene. */
export async function obtenerCita(solicitudId) {
  const { data } = await api.get(`/solicitudes-referencia/${solicitudId}/cita`);
  return data;
}
