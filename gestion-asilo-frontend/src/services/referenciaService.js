import { api } from "./api";

/** HU-03. Crea la solicitud; el backend toma al médico del token, no del cuerpo. */
export async function crearSolicitud(solicitud) {
  const { data } = await api.post("/solicitudes-referencia", solicitud);
  return data;
}

/** El catálogo de especialidades que ofrece la fundación, solo las activas. */
export async function listarEspecialidades() {
  const { data } = await api.get("/especialidades");
  return data;
}

/** Los enfermeros con cuenta activa que pueden acompañar al interno. */
export async function listarEnfermeros() {
  const { data } = await api.get("/enfermeros");
  return data;
}
