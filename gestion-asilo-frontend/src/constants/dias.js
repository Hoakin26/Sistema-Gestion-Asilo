/**
 * YS-168. Los días de la semana con las claves de `java.time.DayOfWeek`, que
 * es como los guarda y los devuelve el backend, en el orden de la semana.
 */
export const DIAS = [
  { valor: "MONDAY", etiqueta: "Lunes", corta: "Lun" },
  { valor: "TUESDAY", etiqueta: "Martes", corta: "Mar" },
  { valor: "WEDNESDAY", etiqueta: "Miércoles", corta: "Mié" },
  { valor: "THURSDAY", etiqueta: "Jueves", corta: "Jue" },
  { valor: "FRIDAY", etiqueta: "Viernes", corta: "Vie" },
  { valor: "SATURDAY", etiqueta: "Sábado", corta: "Sáb" },
  { valor: "SUNDAY", etiqueta: "Domingo", corta: "Dom" },
];

/**
 * YS-92. El día de la semana de una fecha "AAAA-MM-DD", como entrada de DIAS.
 * Se arma en hora local por lo mismo que utils/fechas.js: leída como UTC, en
 * Guatemala caería en el día anterior. `getDay()` cuenta desde el domingo y
 * DIAS desde el lunes; de ahí el corrimiento.
 */
export function diaDeFecha(iso) {
  const [anio, mes, dia] = iso.split("-").map(Number);
  return DIAS[(new Date(anio, mes - 1, dia).getDay() + 6) % 7];
}

/** "08:00:00" → "08:00". */
export function horaCorta(hora) {
  return hora ? hora.slice(0, 5) : "";
}

/**
 * El horario en una línea que se lea de un vistazo: los días seguidos con el
 * mismo horario se juntan. "Lun a Vie, 08:00–12:00 · Sáb, 09:00–11:00".
 */
export function resumirHorario(horarios) {
  const porDia = new Map(horarios.map((h) => [h.dia, h]));
  const tramos = [];
  let tramo = null;

  for (const { valor, corta } of DIAS) {
    const horario = porDia.get(valor);
    const horas = horario ? `${horaCorta(horario.horaInicio)}–${horaCorta(horario.horaFin)}` : null;
    if (horas && tramo?.horas === horas) {
      tramo.hasta = corta;
    } else {
      if (tramo) tramos.push(tramo);
      tramo = horas ? { desde: corta, hasta: corta, horas } : null;
    }
  }
  if (tramo) tramos.push(tramo);

  return tramos
    .map(({ desde, hasta, horas }) => `${desde === hasta ? desde : `${desde} a ${hasta}`}, ${horas}`)
    .join(" · ");
}
