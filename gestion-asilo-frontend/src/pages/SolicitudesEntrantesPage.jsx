import { BandejaSolicitudes } from "../components/referencias/BandejaSolicitudes";
import { AppLayout } from "../layouts/AppLayout";

/**
 * HU-05 (YS-88). La bandeja del Administrador de la Fundación (RF-05): las
 * solicitudes que el asilo le refirió y que esperan cita.
 *
 * Arranca filtrada en "Pendiente de Asignación", que es lo que la Fundación
 * tiene que atender, con las más recientes primero y la opción de ver las más
 * antiguas primero (YS-166, pedido del 09/10). Desde el detalle de cada una
 * se asigna especialista, fecha y hora (HU-06).
 */
export function SolicitudesEntrantesPage() {
  return (
    <AppLayout migas={["Inicio", "Solicitudes entrantes"]}>
      <h1 className="text-h1 font-bold">Solicitudes entrantes</h1>
      <p className="mt-2 text-texto-secundario">
        Referencias del asilo que esperan especialista, fecha y hora.
      </p>

      <div className="mt-6">
        <BandejaSolicitudes
          estadoInicial="PENDIENTE_ASIGNACION"
          mensajeVacio="Todavía no hay solicitudes de referencia del asilo."
        />
      </div>
    </AppLayout>
  );
}
