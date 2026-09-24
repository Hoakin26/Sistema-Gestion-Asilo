/*
 * Alfabeto sin los caracteres que se confunden al dictar una contraseña por
 * teléfono o al copiarla de un papel: 0/O y 1/l/I. Es el mismo criterio que usa
 * el backend para el administrador inicial (ver docs/seguridad.md).
 */
const ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
const LARGO = 14;

/**
 * Contraseña temporal sugerida. Usa crypto y no Math.random, que es predecible
 * y no sirve para generar credenciales.
 */
export function generarPassword() {
  const valores = new Uint32Array(LARGO);
  crypto.getRandomValues(valores);
  return Array.from(valores, (valor) => ALFABETO[valor % ALFABETO.length]).join("");
}
