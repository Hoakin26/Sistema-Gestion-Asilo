import { Activity, CalendarCheck, CircleCheck, CircleSlash, Clock } from "lucide-react";

/*
 * Color + ícono + texto siempre juntos: alrededor del 8% de los hombres no
 * distingue rojo de verde y no debe depender del color para leer un estado.
 */
const ESTILOS = {
  exito: "bg-exito-tenue text-exito",
  advertencia: "bg-advertencia-tenue text-advertencia",
  info: "bg-info-tenue text-info",
  proceso: "bg-proceso-tenue text-proceso",
  neutral: "bg-neutral-tenue text-neutral",
};

/*
 * YS-85. El mapa de estados del manual de diseño, que hace de esta la única
 * fuente de verdad para pintarlos: un "Pendiente" se ve igual en cualquier
 * módulo. Las claves son las del enum del backend, para pasarlas tal cual.
 *
 * Por ahora solo DE-01 (la solicitud de referencia). Los estados de DE-02 a
 * DE-04 se agregan con sus historias, cuando existan sus enums: escribirlos
 * antes sería adivinar sus nombres.
 */
const ESTADOS = {
  // Activo / inactivo de usuarios e internos.
  ACTIVO: { texto: "Activo", Icono: CircleCheck, tono: "exito" },
  INACTIVO: { texto: "Inactivo", Icono: CircleSlash, tono: "neutral" },

  // DE-01 · Solicitud de referencia médica.
  PENDIENTE_ASIGNACION: { texto: "Pendiente de Asignación", Icono: Clock, tono: "advertencia" },
  CITA_PROGRAMADA: { texto: "Cita Programada", Icono: CalendarCheck, tono: "info" },
  EN_ATENCION: { texto: "En Atención", Icono: Activity, tono: "proceso" },
  ATENDIDA: { texto: "Atendida", Icono: CircleCheck, tono: "exito" },
  CANCELADA: { texto: "Cancelada", Icono: CircleSlash, tono: "neutral" },
};

/**
 * `estado` es la clave del enum del backend ("PENDIENTE_ASIGNACION"). `activo`
 * se conserva por comodidad para usuarios e internos, que solo tienen dos.
 */
export function StatusBadge({ estado, activo }) {
  const clave = estado ?? (activo ? "ACTIVO" : "INACTIVO");
  const { texto, Icono, tono } = ESTADOS[clave];

  return (
    <span
      className={`inline-flex items-center gap-2 rounded-campo px-2 py-1 text-auxiliar font-medium ${ESTILOS[tono]}`}
    >
      <Icono aria-hidden="true" size={16} />
      {texto}
    </span>
  );
}
