import { Link } from "react-router-dom";
import { ChevronRight, ClipboardList, FlaskConical, HandCoins, Landmark, PiggyBank, ReceiptText } from "lucide-react";

import { useAuth } from "../hooks/useAuth";
import { AppLayout } from "../layouts/AppLayout";

/*
 * Los reportes, en el orden de las historias, con los roles que los ven. Cada
 * uno se agrega aquí cuando está listo (HU-24 a HU-28). Los financieros son
 * del Administrador del Asilo; los clínicos, de los médicos (RNF-03). El
 * backend lo exige igual: esta lista solo decide qué tarjetas se muestran.
 */
const REPORTES = [
  {
    ruta: "/reportes/costos-por-cita",
    titulo: "Costos por cita",
    descripcion: "Lo que costó cada cita por paciente, desglosado en consulta, exámenes y medicamentos.",
    Icono: ReceiptText,
    roles: ["ADMINISTRADOR_ASILO"],
  },
  {
    ruta: "/reportes/analisis-medico",
    titulo: "Análisis médico",
    descripcion: "La ficha médica de un paciente, con el motivo de reclusión, sus diagnósticos y sus medicamentos.",
    Icono: ClipboardList,
    roles: ["MEDICO_GENERAL", "MEDICO_ESPECIALISTA"],
  },
  {
    ruta: "/reportes/cobros",
    titulo: "Cobros a familiares",
    descripcion: "Los pagos recibidos por paciente, con las cuentas que cubren y sus gastos médicos por tipo.",
    Icono: HandCoins,
    roles: ["ADMINISTRADOR_ASILO"],
  },
  // HU-27: dos reportes y no uno consolidado (decidido el 22/10), como RF-20a y RF-20b.
  {
    ruta: "/reportes/pagos-fundacion",
    titulo: "Pagos a la fundación",
    descripcion: "Cada pago a la fundación por consultas, laboratorio y farmacia, con su fecha y su monto.",
    Icono: Landmark,
    roles: ["ADMINISTRADOR_ASILO"],
  },
  {
    ruta: "/reportes/entradas",
    titulo: "Entradas",
    descripcion: "Las donaciones y los cobros a familiares recibidos, con el total de cada tipo.",
    Icono: PiggyBank,
    roles: ["ADMINISTRADOR_ASILO"],
  },
  // HU-28: los dos en una pantalla, que elige el tipo de reporte (YS-164).
  {
    ruta: "/reportes/examenes-medicamentos",
    titulo: "Exámenes y medicamentos",
    descripcion: "Los exámenes realizados o los medicamentos entregados a un paciente en un período.",
    Icono: FlaskConical,
    roles: ["MEDICO_GENERAL", "MEDICO_ESPECIALISTA"],
  },
];

/**
 * HU-24 (YS-152). "Reportes": la entrada a cada reporte del rol. Cada uno se
 * genera con sus filtros y se exporta a PDF. HU-25 (YS-155) le suma el de
 * análisis médico, para los médicos; HU-26 (YS-158), el de cobros, y HU-27
 * (YS-161), los de pagos a la fundación y entradas; HU-28 (YS-164), el de
 * exámenes y medicamentos, de los médicos.
 */
export function ReportesPage() {
  const { usuario } = useAuth();
  const disponibles = REPORTES.filter((reporte) => reporte.roles.includes(usuario?.rol));

  return (
    <AppLayout migas={["Inicio", "Reportes"]}>
      <h1 className="text-h1 font-bold">Reportes</h1>
      <p className="mt-2 text-texto-secundario">Cada reporte se genera con sus filtros y se exporta a PDF.</p>

      <ul className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {disponibles.map(({ ruta, titulo, descripcion, Icono }) => (
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
