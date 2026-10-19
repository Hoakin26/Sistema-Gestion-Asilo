import { api } from "./api";

/**
 * HU-12 (YS-115). La bandeja del laboratorio, paginada en el servidor.
 * `estados` es una lista de claves de DE-02 (vacía para todas); `paciente`
 * busca por nombre o DPI. De la más antigua a la más reciente.
 *
 * `indexes: null` manda el estado repetido (`estado=PENDIENTE&estado=EN_PROCESO`),
 * que es lo que entiende Spring; con el formato por defecto de Axios
 * (`estado[]=...`) el filtro se ignoraría.
 */
export async function listarOrdenes({ estados = [], paciente = "", pagina = 0 } = {}) {
  const { data } = await api.get("/ordenes-laboratorio", {
    params: {
      estado: estados.length > 0 ? estados : undefined,
      paciente: paciente || undefined,
      page: pagina,
    },
    paramsSerializer: { indexes: null },
  });
  return data;
}

/** DE-02. El laboratorio toma la orden: de Pendiente a En Proceso. */
export async function iniciarOrden(ordenId) {
  const { data } = await api.patch(`/ordenes-laboratorio/${ordenId}/iniciar`);
  return data;
}
