import { useEffect, useId, useRef } from "react";
import { X } from "lucide-react";

import { IconButton } from "./Button";

/*
 * YS-60. "amplio" existe para la ficha médica: sus listas de padecimientos y
 * medicamentos ponen dos campos por fila, y en el ancho normal cada uno
 * quedaba demasiado estrecho para leer lo escrito.
 */
const ANCHOS = {
  normal: "max-w-xl",
  amplio: "max-w-2xl",
};

/**
 * Usa el <dialog> nativo: aporta gratis el atrapado de foco, el cierre con
 * Escape y el fondo inerte, que a mano suelen quedar incompletos.
 *
 * YS-75. `forzado` quita las tres formas de descartarlo —la X, Escape y el clic
 * en el fondo— para los casos en que el usuario tiene que resolver algo antes
 * de seguir. Entonces la única salida la pone el contenido: una ventana sin
 * botón de salir deja al usuario encerrado.
 */
export function Modal({
  abierto,
  alCerrar,
  titulo,
  descripcion,
  forzado = false,
  ancho = "normal",
  children,
}) {
  const referencia = useRef(null);
  const presionoElFondo = useRef(false);
  // Un id por modal: con uno fijo, dos modales montados en la misma página
  // compartían título y el lector de pantalla anunciaba el del primero.
  const idTitulo = useId();

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
      /*
       * El <dialog> nativo se cierra con Escape por su cuenta; cancelar ese
       * evento es la única forma de impedirlo. Y se impide siempre, no solo
       * en el forzado: Escape le pregunta a quien abrió el modal, igual que la
       * X y el fondo.
       *
       * YS-60. Antes se dejaba cerrar solo, y con un formulario a medias eso
       * fallaba de dos formas: la ventana desaparecía antes de poder preguntar
       * si descartar los cambios, y el estado seguía diciendo "abierto" con
       * la ventana ya cerrada, así que el botón que la abre dejaba de responder.
       */
      onCancel={(evento) => {
        evento.preventDefault();
        if (!forzado) alCerrar();
      }}
      /*
       * El clic solo cierra si empezó Y terminó en el fondo. Sin la primera
       * comprobación, arrastrar desde dentro del formulario y soltar afuera
       * cerraría el modal: el evento click se dispara en el <dialog>, que es
       * el ancestro común de ambos puntos.
       */
      onMouseDown={(evento) => {
        presionoElFondo.current = evento.target === referencia.current;
      }}
      onClick={(evento) => {
        if (!forzado && evento.target === referencia.current && presionoElFondo.current) {
          alCerrar();
        }
      }}
      aria-labelledby={idTitulo}
      className={`modal-animado m-auto w-full ${ANCHOS[ancho]} rounded-modal bg-superficie p-0 text-texto shadow-modal`}
    >
      {/* Pegado arriba: en teléfono el formulario no cabe entero y el <dialog>
          se desplaza por dentro, así que sin esto el título se iría de vista y
          el usuario perdería de referencia qué está haciendo (YS-80). */}
      <div className="sticky top-0 z-10 flex items-start justify-between gap-4 border-b border-divisor bg-superficie px-4 py-4 sm:px-6">
        <div>
          <h2 id={idTitulo} className="text-h3 font-semibold">
            {titulo}
          </h2>
          {descripcion && (
            <p className="mt-1 text-auxiliar text-texto-secundario">{descripcion}</p>
          )}
        </div>
        {!forzado && (
          <IconButton etiqueta="Cerrar" type="button" onClick={alCerrar}>
            <X aria-hidden="true" size={24} />
          </IconButton>
        )}
      </div>

      {children}
    </dialog>
  );
}
