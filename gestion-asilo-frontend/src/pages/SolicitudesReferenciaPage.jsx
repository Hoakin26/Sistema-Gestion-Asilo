import { useState } from "react";
import { Plus } from "lucide-react";

import { BandejaSolicitudes } from "../components/referencias/BandejaSolicitudes";
import { SolicitudReferenciaFormModal } from "../components/referencias/SolicitudReferenciaFormModal";
import { Button } from "../components/ui/Button";
import { AppLayout } from "../layouts/AppLayout";

/**
 * YS-85 y YS-88. El módulo "Solicitudes de referencia" del Médico General:
 * crear una solicitud y dar seguimiento a las existentes.
 *
 * Muestra todas las del asilo, no solo las del médico que entra: si hay más
 * de un médico general, cualquiera tiene que poder seguir una referencia que
 * empezó otro. La más reciente va primero, que es la que el médico acaba de
 * crear o la que más probablemente busca.
 */
export function SolicitudesReferenciaPage() {
  const [abierta, setAbierta] = useState(false);
  const [aperturas, setAperturas] = useState(0);
  // Cambia al crear una solicitud, para que la lista la muestre.
  const [version, setVersion] = useState(0);

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
            Refiera a un interno a una especialidad de la fundación y siga su estado.
          </p>
        </div>

        <Button onClick={abrir}>
          <Plus aria-hidden="true" size={20} />
          Nueva solicitud
        </Button>
      </div>

      <div className="mt-6">
        <BandejaSolicitudes
          orden="desc"
          version={version}
          mensajeVacio="Todavía no se ha referido a ningún interno. Use «Nueva solicitud» para crear la primera."
        />
      </div>

      <SolicitudReferenciaFormModal
        abierto={abierta}
        claveFormulario={aperturas}
        alCerrar={() => setAbierta(false)}
        alCrear={() => setVersion((valor) => valor + 1)}
      />
    </AppLayout>
  );
}
