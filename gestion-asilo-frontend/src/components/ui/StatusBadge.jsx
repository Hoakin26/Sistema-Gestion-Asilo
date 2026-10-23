import {
  Activity,
  CalendarCheck,
  CircleCheck,
  CircleDollarSign,
  CircleSlash,
  Clock,
  FlaskConical,
  PackageOpen,
  TriangleAlert,
} from "lucide-react";

import { ETIQUETAS_ESTADO } from "../../constants/estados";

/*
 * Color + ícono + texto siempre juntos: alrededor del 8% de los hombres no
 * distingue rojo de verde y no debe depender del color para leer un estado.
 */
const ESTILOS = {
  exito: "bg-exito-tenue text-exito",
  advertencia: "bg-advertencia-tenue text-advertencia",
  error: "bg-error-tenue text-error",
  info: "bg-info-tenue text-info",
  proceso: "bg-proceso-tenue text-proceso",
  neutral: "bg-neutral-tenue text-neutral",
};

/*
 * YS-85. El mapa de estados del manual de diseño, que hace de esta la única
 * fuente de verdad para pintarlos: un "Pendiente" se ve igual en cualquier
 * módulo. Las claves son las del enum del backend, para pasarlas tal cual; el
 * texto sale de constants/estados.js.
 *
 * DE-01 (la solicitud de referencia), DE-02 (la orden de laboratorio), DE-03
 * (la receta), DE-04 (la cuenta mensual, desde HU-18), el aviso al familiar,
 * la visita, la deuda con la fundación (HU-20) y la cita pendiente de pago
 * (HU-21).
 */
const ESTADOS = {
  ACTIVO: { Icono: CircleCheck, tono: "exito" },
  INACTIVO: { Icono: CircleSlash, tono: "neutral" },

  // DE-01 · Solicitud de referencia médica.
  PENDIENTE_ASIGNACION: { Icono: Clock, tono: "advertencia" },
  CITA_PROGRAMADA: { Icono: CalendarCheck, tono: "info" },
  EN_ATENCION: { Icono: Activity, tono: "proceso" },
  ATENDIDA: { Icono: CircleCheck, tono: "exito" },
  CANCELADA: { Icono: CircleSlash, tono: "neutral" },

  // YS-88 · Aviso por correo al familiar.
  AVISO_PENDIENTE: { Icono: Clock, tono: "advertencia" },
  AVISO_ENVIADA: { Icono: CircleCheck, tono: "exito" },
  AVISO_FALLIDA: { Icono: TriangleAlert, tono: "error" },
  AVISO_SIN_CORREO: { Icono: CircleSlash, tono: "neutral" },

  // YS-98 · Visita médica. "En curso" con el tono de En Atención, que es el
  // estado en que deja a la solicitud.
  VISITA_ACTIVA: { Icono: Activity, tono: "proceso" },
  VISITA_FINALIZADA: { Icono: CircleCheck, tono: "exito" },

  // DE-02 · Orden de laboratorio, con los tonos del manual de diseño.
  PENDIENTE: { Icono: Clock, tono: "advertencia" },
  EN_PROCESO: { Icono: FlaskConical, tono: "proceso" },
  RESULTADOS_CARGADOS: { Icono: CircleCheck, tono: "exito" },
  ANULADA: { Icono: CircleSlash, tono: "neutral" },

  // DE-03 · Receta médica, con los tonos del manual de diseño.
  PENDIENTE_DESPACHO: { Icono: Clock, tono: "advertencia" },
  PARCIALMENTE_ENTREGADA: { Icono: PackageOpen, tono: "proceso" },
  ENTREGADA: { Icono: CircleCheck, tono: "exito" },
  MEDICAMENTO_AGOTADO: { Icono: TriangleAlert, tono: "error" },

  // DE-04 · Cuenta mensual del familiar (YS-134).
  PENDIENTE_PAGO: { Icono: Clock, tono: "advertencia" },
  PAGO_PARCIAL: { Icono: CircleDollarSign, tono: "proceso" },
  COMPLETAMENTE_CANCELADA: { Icono: CircleCheck, tono: "exito" },
  EN_MORA: { Icono: TriangleAlert, tono: "error" },

  // YS-140 · Deuda con la fundación: Pendiente es el de DE-02, de arriba.
  PAGADA: { Icono: CircleCheck, tono: "exito" },

  // YS-143 · Cita pendiente de pago: Sin Pago con el tono de Pendiente de Pago,
  // y Pago Parcial es el de DE-04.
  SIN_PAGO: { Icono: Clock, tono: "advertencia" },
};

/**
 * `estado` es la clave del enum del backend ("PENDIENTE_ASIGNACION"). `activo`
 * se conserva por comodidad para usuarios e internos, que solo tienen dos.
 */
export function StatusBadge({ estado, activo }) {
  const clave = estado ?? (activo ? "ACTIVO" : "INACTIVO");
  const { Icono, tono } = ESTADOS[clave];

  return (
    /* whitespace-nowrap: partida en dos líneas, "Pendiente de / Asignación"
       deja de leerse como una etiqueta y parece texto suelto (YS-88). */
    <span
      className={`inline-flex items-center gap-2 rounded-campo px-2 py-1 text-auxiliar font-medium whitespace-nowrap ${ESTILOS[tono]}`}
    >
      <Icono aria-hidden="true" size={16} />
      {ETIQUETAS_ESTADO[clave]}
    </span>
  );
}
