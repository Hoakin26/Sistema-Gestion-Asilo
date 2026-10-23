import { Link } from "react-router-dom";
import { ArrowLeft } from "lucide-react";

/**
 * YS-186. El enlace de cada reporte a la lista de "Reportes", como "Volver a
 * internos" en el perfil del interno: la ruta de navegación no es un enlace.
 * No sale en el PDF.
 */
export function VolverAReportes() {
  return (
    <Link
      to="/reportes"
      className="inline-flex min-h-11 items-center gap-2 rounded-tarjeta font-medium text-primario hover:underline print:hidden"
    >
      <ArrowLeft aria-hidden="true" size={20} />
      Volver a reportes
    </Link>
  );
}
