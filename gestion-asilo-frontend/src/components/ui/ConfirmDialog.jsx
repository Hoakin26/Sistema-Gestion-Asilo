import { useEffect, useRef } from "react";
import { TriangleAlert } from "lucide-react";

import { Button } from "./Button";

/**
 * Diálogo de confirmación para acciones que el usuario podría lamentar.
 * Es un <dialog> propio y no reutiliza Modal porque puede abrirse encima de
 * otro modal: el top layer del navegador se encarga del apilamiento.
 */
export function ConfirmDialog({
  abierto,
  titulo,
  mensaje,
  textoConfirmar = "Confirmar",
  textoCancelar = "Cancelar",
  varianteConfirmar = "primario",
  alConfirmar,
  alCancelar,
}) {
  const referencia = useRef(null);

  useEffect(() => {
    const dialogo = referencia.current;
    if (!dialogo) return;

    if (abierto && !dialogo.open) {
      dialogo.showModal();
    } else if (!abierto && dialogo.open) {
      dialogo.close();
    }
  }, [abierto]);

  return (
    <dialog
      ref={referencia}
      onClose={alCancelar}
      aria-labelledby="titulo-confirmacion"
      className="modal-animado m-auto w-full max-w-md rounded-modal bg-superficie p-0 text-texto shadow-modal"
    >
      <div className="flex gap-4 px-6 py-6">
        <TriangleAlert
          aria-hidden="true"
          size={24}
          className="mt-1 shrink-0 text-advertencia"
        />
        <div>
          <h2 id="titulo-confirmacion" className="text-h3 font-semibold">
            {titulo}
          </h2>
          <p className="mt-2 text-texto-secundario">{mensaje}</p>
        </div>
      </div>

      <div className="flex justify-end gap-3 border-t border-divisor px-6 py-4">
        <Button type="button" variante="secundario" onClick={alCancelar}>
          {textoCancelar}
        </Button>
        <Button type="button" variante={varianteConfirmar} onClick={alConfirmar}>
          {textoConfirmar}
        </Button>
      </div>
    </dialog>
  );
}
