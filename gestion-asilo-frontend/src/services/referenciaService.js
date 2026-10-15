import { api } from "./api";

/** HU-03. Crea la solicitud; el backend toma al médico del token, no del cuerpo. */
export async function crearSolicitud(solicitud) {
  const { data } = await api.post("/solicitudes-referencia", solicitud);
  return data;
}

/**
 * HU-05 (YS-87). Paginado en el servidor. Sin `estado` trae todas; `orden`
 * es "desc" (las más recientes primero) o "asc" (las más antiguas primero).
 * YS-166: `busqueda` filtra por número, interno o especialidad.
 */
export async function listarSolicitudes({ estado = "", busqueda = "", pagina = 0, orden = "desc" } = {}) {
  const { data } = await api.get("/solicitudes-referencia", {
    params: {
      estado: estado || undefined,
      busqueda: busqueda || undefined,
      page: pagina,
      sort: `fechaCreacion,${orden}`,
    },
  });
  return data;
}

/** YS-95. En qué quedó el aviso por correo al familiar de una solicitud. */
export async function listarAvisos(solicitudId) {
  const { data } = await api.get(`/solicitudes-referencia/${solicitudId}/notificaciones`);
  return data;
}

/** YS-95. Reintenta en el momento los avisos que no salieron. */
export async function reintentarAvisos(solicitudId) {
  const { data } = await api.post(`/solicitudes-referencia/${solicitudId}/notificaciones/reintentar`);
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
