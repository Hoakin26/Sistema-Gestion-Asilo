import { useState } from "react";
import { FileText, Plus } from "lucide-react";

import { SolicitudReferenciaFormModal } from "../components/referencias/SolicitudReferenciaFormModal";
import { Button } from "../components/ui/Button";
import { AppLayout } from "../layouts/AppLayout";

/**
 * YS-85. El módulo "Solicitudes de referencia" del Médico General.
 *
 * Por ahora solo crea: el listado de las solicitudes y su estado llega con
 * HU-05 (YS-87), que agrega el endpoint de consulta que esta página va a usar.
 * Se decidió así el 09/10 para que cada historia quede en su propia rama.
 */
export function SolicitudesReferenciaPage() {
  const [abierta, setAbierta] = useState(false);
  const [aperturas, setAperturas] = useState(0);

  const abrir = () => {
    setAperturas((valor) => valor + 1);
    setAbierta(true);
  };

  return (
    <AppLayout migas={["Inicio", "Solicitudes de referencia"]}>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-h1 font-bold">Solicitudes de referencia</h1>
          <p className="mt-2 text-texto-secundario">
            Refiera a un interno a una especialidad de la fundación.
          </p>
        </div>

        <Button onClick={abrir}>
          <Plus aria-hidden="true" size={20} />
          Nueva solicitud
        </Button>
      </div>

      <div className="mt-6 flex flex-col items-center gap-4 rounded-tarjeta border border-divisor bg-superficie px-4 py-16 text-center shadow-tarjeta sm:px-6">
        <FileText aria-hidden="true" size={24} className="text-texto-secundario" />
        <p className="max-w-md text-texto-secundario">
          Elija al interno, la especialidad y el enfermero(a) que lo acompañará. La fundación
          recibirá la solicitud para asignarle especialista, fecha y hora.
        </p>
        <Button onClick={abrir}>
          <Plus aria-hidden="true" size={20} />
          Nueva solicitud
        </Button>
      </div>

      <SolicitudReferenciaFormModal
        abierto={abierta}
        claveFormulario={aperturas}
        alCerrar={() => setAbierta(false)}
      />
    </AppLayout>
  );
}
