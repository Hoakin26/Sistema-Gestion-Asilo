import { Link } from "react-router-dom";
import { ChevronRight, ReceiptText } from "lucide-react";

import { AppLayout } from "../layouts/AppLayout";

/*
 * Los reportes del Administrador del Asilo, en el orden de las historias. Cada
 * uno se agrega aquí cuando está listo (HU-24 a HU-28); los clínicos (HU-25,
 * HU-28) serán de los roles clínicos, no de este menú (RNF-03).
 */
const REPORTES = [
  {
    ruta: "/reportes/costos-por-cita",
    titulo: "Costos por cita",
    descripcion: "Lo que costó cada cita por paciente, desglosado en consulta, exámenes y medicamentos.",
    Icono: ReceiptText,
  },
];

/**
 * HU-24 (YS-152). "Reportes" del Administrador del Asilo: la entrada a cada
 * reporte. Cada uno se genera con sus filtros y se exporta a PDF.
 */
export function ReportesPage() {
  return (
    <AppLayout migas={["Inicio", "Reportes"]}>
      <h1 className="text-h1 font-bold">Reportes</h1>
      <p className="mt-2 text-texto-secundario">Cada reporte se genera con sus filtros y se exporta a PDF.</p>

      <ul className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {REPORTES.map(({ ruta, titulo, descripcion, Icono }) => (
          <li key={ruta}>
            <Link
              to={ruta}
              className="flex min-h-11 items-start gap-4 rounded-tarjeta border border-divisor bg-superficie p-4 shadow-tarjeta hover:border-primario sm:p-6"
            >
              <Icono aria-hidden="true" size={24} className="shrink-0 text-primario" />
              <span className="min-w-0 flex-1">
                <span className="block font-semibold text-texto">{titulo}</span>
                <span className="mt-1 block text-auxiliar text-texto-secundario">{descripcion}</span>
              </span>
              <ChevronRight aria-hidden="true" size={20} className="shrink-0 text-texto-secundario" />
            </Link>
          </li>
        ))}
      </ul>
    </AppLayout>
  );
}
