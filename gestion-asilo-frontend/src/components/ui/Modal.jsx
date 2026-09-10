import { useEffect, useRef } from "react";
import { X } from "lucide-react";

import { IconButton } from "./Button";

/**
 * Usa el <dialog> nativo: aporta gratis el atrapado de foco, el cierre con
 * Escape y el fondo inerte, que a mano suelen quedar incompletos.
 */
export function Modal({ abierto, alCerrar, titulo, descripcion, children }) {
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
      onClose={alCerrar}
      onClick={(evento) => {
        if (evento.target === referencia.current) alCerrar();
      }}
      aria-labelledby="titulo-modal"
      className="modal-animado m-auto w-full max-w-xl rounded-modal bg-superficie p-0 text-texto shadow-modal"
    >
      <div className="flex items-start justify-between gap-4 border-b border-divisor px-6 py-4">
        <div>
          <h2 id="titulo-modal" className="text-h3 font-semibold">
            {titulo}
          </h2>
          {descripcion && (
            <p className="mt-1 text-auxiliar text-texto-secundario">{descripcion}</p>
          )}
        </div>
        <IconButton etiqueta="Cerrar" type="button" onClick={alCerrar}>
          <X aria-hidden="true" size={24} />
        </IconButton>
      </div>

      {children}
    </dialog>
  );
}
