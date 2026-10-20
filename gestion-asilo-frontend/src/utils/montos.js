/*
 * YS-130. Un costo como lo escribe una persona, con las mismas reglas que el
 * backend (Montos.java): hasta 8 enteros y 2 decimales, mayor que cero, con la
 * coma aceptada como punto. Salió del costo del laboratorio (HU-14) para que la
 * farmacia (HU-17) diga exactamente lo mismo.
 */
const MONTO = /^\d{1,8}(\.\d{1,2})?$/;

/** El texto como lo lee el backend: sin espacios y con punto decimal. */
export function normalizarMonto(texto) {
  return texto.trim().replace(",", ".");
}

/** El mensaje de error del costo, o `undefined` si sirve. `faltante` es el de cuando viene vacío. */
export function errorDeCosto(texto, faltante) {
  const limpio = normalizarMonto(texto);
  if (!limpio) return faltante;
  if (limpio.startsWith("-")) return "El costo tiene que ser mayor que cero";
  if (!MONTO.test(limpio)) return "Escriba un monto como 75 o 75.50";
  if (Number(limpio) === 0) return "El costo tiene que ser mayor que cero";
  return undefined;
}
