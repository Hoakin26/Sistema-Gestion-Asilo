import { api } from "./api";

/**
 * HU-15 (YS-124). La bandeja de la farmacia, paginada en el servidor.
 * `estados` es una lista de claves de DE-03 (vacía para todas); `paciente`
 * busca por nombre o DPI. De la más antigua a la más reciente.
 *
 * `indexes: null` manda el estado repetido, como en la bandeja del
 * laboratorio: con el formato por defecto de Axios (`estado[]=...`) Spring
 * ignoraría el filtro.
 */
export async function listarRecetas({ estados = [], paciente = "", pagina = 0, orden } = {}) {
  const { data } = await api.get("/recetas", {
    params: {
      estado: estados.length > 0 ? estados : undefined,
      paciente: paciente || undefined,
      page: pagina,
      // YS-127. "fechaDespacho,desc" para el historial de despachos.
      sort: orden,
    },
    paramsSerializer: { indexes: null },
  });
  return data;
}

/**
 * HU-16 (YS-127). Despacha la receta: `entregados` son los ids de los
 * medicamentos que se entregan ahora; los demás quedan pendientes. Devuelve la
 * receta con su nuevo estado (DE-03).
 */
export async function despacharReceta(recetaId, entregados) {
  const { data } = await api.post(`/recetas/${recetaId}/despacho`, {
    entregados: entregados.map((id) => ({ id })),
  });
  return data;
}
