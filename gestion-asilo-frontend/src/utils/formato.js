/** "1234 56789 0101": así viene impreso en el documento y así se dicta por teléfono. */
export function formatearDpi(dpi) {
  return /^\d{13}$/.test(dpi) ? `${dpi.slice(0, 4)} ${dpi.slice(4, 9)} ${dpi.slice(9)}` : dpi;
}

const QUETZALES = new Intl.NumberFormat("es-GT", { style: "currency", currency: "GTQ" });

/** "Q1,500.00". */
export function formatearQuetzales(monto) {
  return QUETZALES.format(monto);
}

const PORCENTAJE = new Intl.NumberFormat("es-GT", { maximumFractionDigits: 2 });

/**
 * YS-137. Un descuento de la fundación como lo lee una persona: "20 %",
 * "Q50.00 por consulta" o "Sin descuento". `porCargo` es "por consulta",
 * "por examen"…: un monto fijo se resta de cada cargo.
 */
export function formatearDescuento({ tipoDescuento, valor }, porCargo) {
  if (Number(valor) === 0) return "Sin descuento";
  if (tipoDescuento === "PORCENTAJE") return `${PORCENTAJE.format(Number(valor))} %`;
  return `${formatearQuetzales(valor)} ${porCargo}`;
}

const DECIMAL = new Intl.NumberFormat("es-GT", { maximumFractionDigits: 1 });

/** YS-118. "850 KB", "2.4 MB": el tamaño de un archivo, como lo lee una persona. */
export function formatearTamano(bytes) {
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  return `${DECIMAL.format(bytes / (1024 * 1024))} MB`;
}
