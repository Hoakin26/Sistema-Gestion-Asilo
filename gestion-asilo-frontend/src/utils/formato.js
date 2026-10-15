/** "1234 56789 0101": así viene impreso en el documento y así se dicta por teléfono. */
export function formatearDpi(dpi) {
  return /^\d{13}$/.test(dpi) ? `${dpi.slice(0, 4)} ${dpi.slice(4, 9)} ${dpi.slice(9)}` : dpi;
}

const QUETZALES = new Intl.NumberFormat("es-GT", { style: "currency", currency: "GTQ" });

/** "Q1,500.00". */
export function formatearQuetzales(monto) {
  return QUETZALES.format(monto);
}
