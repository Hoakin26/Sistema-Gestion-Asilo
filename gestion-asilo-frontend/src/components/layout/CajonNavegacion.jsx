import { useEffect, useRef } from "react";
import { X } from "lucide-react";

import { IconButton } from "../ui/Button";
import { Logotipo } from "./Logotipo";
import { MenuPrincipal } from "./MenuPrincipal";

/**
 * YS-79. El menú de teléfono y tableta. Por debajo de 1024px el menú fijo se
 * oculta, así que sin esto no habría forma de cambiar de módulo.
 *
 * Es un <dialog> abierto con showModal(), igual que el resto de los modales
 * del sistema: de ahí salen gratis el atrapado de foco, el cierre con Escape y
 * el fondo inerte, que escritos a mano casi siempre quedan incompletos.
 */
export function CajonNavegacion({ abierto, alCerrar }) {
  const referencia = useRef(null);
  const presionoElFondo = useRef(false);

  useEffect(() => {
    const dialogo = referencia.current;
    if (!dialogo) return;

    if (abierto && !dialogo.open) {
      dialogo.showModal();
    } else if (!abierto && dialogo.open) {
      dialogo.close();
    }
  }, [abierto]);

  /*
   * Al pasar a escritorio el cajón se cierra solo. Si no, al girar la tableta
   * a horizontal quedaría un menú modal encima del menú fijo que ya apareció,
   * bloqueando la pantalla entera.
   */
  useEffect(() => {
    if (!abierto) return;

    const escritorio = window.matchMedia("(min-width: 1024px)");
    const alCambiar = (evento) => {
      if (evento.matches) alCerrar();
    };

    escritorio.addEventListener("change", alCambiar);
    return () => escritorio.removeEventListener("change", alCambiar);
  }, [abierto, alCerrar]);

  return (
    <dialog
      ref={referencia}
      id="cajon-navegacion"
      onClose={alCerrar}
      /* Mismo criterio que Modal: el clic en el fondo solo cierra si empezó y
         terminó ahí, para que arrastrar desde dentro no cierre el cajón. */
      onMouseDown={(evento) => {
        presionoElFondo.current = evento.target === referencia.current;
      }}
      onClick={(evento) => {
        if (evento.target === referencia.current && presionoElFondo.current) {
          alCerrar();
        }
      }}
      aria-label="Menú de navegación"
      /* max-h-none: el navegador limita la altura de todo <dialog>, y sin
         quitarlo el cajón no llegaría al borde inferior de la pantalla. */
      className="cajon-animado m-0 h-dvh max-h-none w-72 overflow-y-auto bg-superficie p-0 text-texto shadow-modal"
    >
      <div className="flex items-center justify-between gap-3 border-b border-divisor px-4 py-4">
        <Logotipo />
        <IconButton etiqueta="Cerrar menú" type="button" onClick={alCerrar}>
          <X aria-hidden="true" size={24} />
        </IconButton>
      </div>

      <MenuPrincipal alNavegar={alCerrar} />
    </dialog>
  );
}
