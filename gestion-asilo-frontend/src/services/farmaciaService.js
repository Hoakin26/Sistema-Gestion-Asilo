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
export async function listarRecetas({ estados = [], paciente = "", pagina = 0 } = {}) {
  const { data } = await api.get("/recetas", {
    params: {
      estado: estados.length > 0 ? estados : undefined,
      paciente: paciente || undefined,
      page: pagina,
    },
    paramsSerializer: { indexes: null },
  });
  return data;
}
