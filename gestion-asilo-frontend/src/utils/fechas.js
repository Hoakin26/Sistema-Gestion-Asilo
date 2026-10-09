/*
 * Las fechas viajan como "AAAA-MM-DD" (LocalDate en el backend), sin hora ni
 * zona. `new Date("1940-03-12")` las interpreta como medianoche UTC, que en
 * Guatemala (UTC-6) todavía es el 11: cada fecha se mostraría un día antes.
 * Por eso toda conversión pasa por aquí y arma la fecha en hora local.
 */

function aFechaLocal(iso) {
  const [anio, mes, dia] = iso.split("-").map(Number);
  return new Date(anio, mes - 1, dia);
}

/** La fecha de hoy en el mismo formato, para el `max` de los campos de fecha. */
export function hoyISO() {
  const ahora = new Date();
  const local = new Date(ahora.getTime() - ahora.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 10);
}

const FORMATO_LARGO = new Intl.DateTimeFormat("es-GT", {
  day: "numeric",
  month: "long",
  year: "numeric",
});

/** "12 de marzo de 1940". */
export function formatearFecha(iso) {
  return iso ? FORMATO_LARGO.format(aFechaLocal(iso)) : "";
}

const FORMATO_FECHA_HORA = new Intl.DateTimeFormat("es-GT", {
  day: "numeric",
  month: "short",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
});

/**
 * "9 oct 2026, 07:49". Para los LocalDateTime del backend ("2026-10-09T07:49:01"),
 * que llegan sin zona: `new Date` lee ese formato como hora local, que es
 * justo lo que son, a diferencia de las fechas solas de arriba.
 */
export function formatearFechaHora(iso) {
  return iso ? FORMATO_FECHA_HORA.format(new Date(iso)) : "";
}

export function edadEnAnios(iso) {
  const nacimiento = aFechaLocal(iso);
  const hoy = new Date();
  const edad = hoy.getFullYear() - nacimiento.getFullYear();
  const yaCumplio =
    hoy.getMonth() > nacimiento.getMonth() ||
    (hoy.getMonth() === nacimiento.getMonth() && hoy.getDate() >= nacimiento.getDate());
  return yaCumplio ? edad : edad - 1;
}
