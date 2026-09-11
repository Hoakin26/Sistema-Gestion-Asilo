import { CircleCheck, CircleSlash } from "lucide-react";

/*
 * Color + ícono + texto siempre juntos: alrededor del 8% de los hombres no
 * distingue rojo de verde y no debe depender del color para leer un estado.
 */
const ESTADOS = {
  activo: {
    texto: "Activo",
    Icono: CircleCheck,
    clases: "bg-exito-tenue text-exito",
  },
  inactivo: {
    texto: "Inactivo",
    Icono: CircleSlash,
    clases: "bg-neutral-tenue text-neutral",
  },
};

export function StatusBadge({ activo }) {
  const { texto, Icono, clases } = ESTADOS[activo ? "activo" : "inactivo"];

  return (
    <span
      className={`inline-flex items-center gap-2 rounded-campo px-2 py-1 text-auxiliar font-medium ${clases}`}
    >
      <Icono aria-hidden="true" size={16} />
      {texto}
    </span>
  );
}
