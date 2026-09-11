import { CircleCheck, TriangleAlert } from "lucide-react";

const TIPOS = {
  error: {
    Icono: TriangleAlert,
    clases: "bg-error-tenue text-error",
  },
  exito: {
    Icono: CircleCheck,
    clases: "bg-exito-tenue text-exito",
  },
};

export function Alert({ tipo = "error", children }) {
  const { Icono, clases } = TIPOS[tipo];

  return (
    <div
      role="alert"
      className={`entrada-error flex items-start gap-2 rounded-tarjeta px-4 py-3 text-auxiliar font-medium ${clases}`}
    >
      <Icono aria-hidden="true" size={20} className="shrink-0" />
      <span>{children}</span>
    </div>
  );
}
