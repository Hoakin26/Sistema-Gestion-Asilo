import { api } from "./api";

/**
 * HU-19 (YS-137). Los descuentos de la fundación: todas las versiones, por
 * servicio y de la más reciente a la más antigua, con `vigente` en la que
 * rige hoy.
 */
export async function listarPoliticas() {
  const { data } = await api.get("/politicas-descuento");
  return data;
}

/**
 * HU-19 (YS-137). Define el descuento de un servicio desde una fecha. El valor
 * va como texto, como los montos: el backend lo lee como BigDecimal.
 */
export async function definirPolitica(tipoServicio, { tipoDescuento, valor, vigenteDesde }) {
  const { data } = await api.put(`/politicas-descuento/${tipoServicio}`, { tipoDescuento, valor, vigenteDesde });
  return data;
}
