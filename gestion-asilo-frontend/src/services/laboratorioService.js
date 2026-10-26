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
export async function listarOrdenes({ estados = [], paciente = "", pagina = 0, orden } = {}) {
  const { data } = await api.get("/ordenes-laboratorio", {
    params: {
      estado: estados.length > 0 ? estados : undefined,
      paciente: paciente || undefined,
      page: pagina,
      // YS-118. "fechaResultado,desc" para el registro de resultados.
      sort: orden,
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

/**
 * HU-13 (YS-118). Carga el resultado de una orden: el texto y, si lo hay, el
 * archivo, como formulario multiparte.
 *
 * El Content-Type se declara aquí porque la instancia `api` fija JSON para
 * todas las peticiones, y con eso Axios convierte el FormData a JSON: el
 * archivo se perdía y el backend respondía 415. Declarado multiparte, Axios
 * deja que el navegador ponga el separador.
 */
export async function registrarResultado(ordenId, { resultado, costo, archivo }) {
  const datos = new FormData();
  datos.append("resultado", resultado);
  // HU-14. Como texto: el backend lo lee como BigDecimal, sin redondeos de JavaScript.
  datos.append("costo", costo);
  if (archivo) datos.append("archivo", archivo);
  const { data } = await api.post(`/ordenes-laboratorio/${ordenId}/resultado`, datos, {
    headers: { "Content-Type": "multipart/form-data" },
    timeout: 120000,
  });
  return data;
}

/**
 * HU-14 (YS-121). El costo de la orden para el laboratorio: `{ costo, registrado }`.
 * Sin resultado todavía es el sugerido del catálogo; con resultado, el que se
 * registró y se cargó a la cuenta.
 */
export async function obtenerCostoOrden(ordenId) {
  const { data } = await api.get(`/ordenes-laboratorio/${ordenId}/costo`);
  return data;
}

/**
 * HU-13 (YS-118). Descarga el archivo de un resultado. Se pide con Axios y no
 * con un enlace común porque la descarga exige el token, que un enlace no
 * manda; después se entrega al navegador como cualquier descarga, con el
 * nombre con que se subió.
 */
export async function descargarArchivoResultado(ordenId, nombre) {
  const { data } = await api.get(`/ordenes-laboratorio/${ordenId}/archivo`, { responseType: "blob" });
  const url = URL.createObjectURL(data);
  const enlace = document.createElement("a");
  enlace.href = url;
  enlace.download = nombre;
  document.body.append(enlace);
  enlace.click();
  enlace.remove();
  // Un momento después, cuando el navegador ya tomó el archivo.
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
