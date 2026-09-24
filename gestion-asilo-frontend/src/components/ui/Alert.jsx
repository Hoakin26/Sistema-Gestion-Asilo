import { CircleCheck, Info, TriangleAlert } from "lucide-react";

const TIPOS = {
  error: {
    Icono: TriangleAlert,
    clases: "bg-error-tenue text-error",
  },
  exito: {
    Icono: CircleCheck,
    clases: "bg-exito-tenue text-exito",
  },
  info: {
    Icono: Info,
    clases: "bg-info-tenue text-info",
  },
};

/**
 * `rol` es "alert" por defecto, que interrumpe al lector de pantalla: lo
 * correcto para un error que impide seguir. Un aviso que solo explica algo ya
 * ocurrido debe usar "status", que espera a que termine la lectura en curso.
 */
export function Alert({ tipo = "error", rol = "alert", children }) {
  const { Icono, clases } = TIPOS[tipo];

  return (
    <div
      role={rol}
      className={`entrada-error flex items-start gap-2 rounded-tarjeta px-4 py-3 text-auxiliar font-medium ${clases}`}
    >
      <Icono aria-hidden="true" size={20} className="shrink-0" />
      <span>{children}</span>
    </div>
  );
}
