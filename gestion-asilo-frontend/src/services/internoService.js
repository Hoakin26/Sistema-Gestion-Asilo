import { api } from "./api";

/**
 * HU-01. La lista viene paginada desde el servidor (YS-58): crece con cada
 * ingreso, y traerla entera para filtrarla aquí dejaría de servir en cuanto el
 * asilo tenga unos cientos de expedientes. Por lo mismo la búsqueda la hace el
 * backend, por nombre o por DPI.
 */
export async function listarInternos({ busqueda = "", pagina = 0 } = {}) {
  const { data } = await api.get("/internos", {
    params: { busqueda: busqueda || undefined, page: pagina },
  });
  return data;
}

export async function obtenerInterno(id) {
  const { data } = await api.get(`/internos/${id}`);
  return data;
}

export async function registrarInterno(interno) {
  const { data } = await api.post("/internos", interno);
  return data;
}

export async function actualizarInterno(id, interno) {
  const { data } = await api.put(`/internos/${id}`, interno);
  return data;
}

/**
 * Datos clínicos (RNF-03). El backend le responde 403 al Administrador del
 * Asilo; la pantalla además no la pide para ese rol, así que nunca debería
 * llegar a intentarlo.
 */
export async function obtenerFichaMedica(id) {
  const { data } = await api.get(`/internos/${id}/ficha-medica`);
  return data;
}

/** Reemplaza la ficha completa, listas incluidas: se manda como debe quedar. */
/**
 * HU-02 (YS-64). El historial médico centralizado: todas las consultas del
 * interno, con lo recetado y ordenado en cada una. Solo el Médico General.
 */
export async function obtenerHistorialMedico(id) {
  const { data } = await api.get(`/internos/${id}/historial`);
  return data;
}

export async function actualizarFichaMedica(id, ficha) {
  const { data } = await api.put(`/internos/${id}/ficha-medica`, ficha);
  return data;
}
