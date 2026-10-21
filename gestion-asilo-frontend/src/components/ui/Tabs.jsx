import { useId, useRef } from "react";

/**
 * YS-143. Las pestañas del manual de diseño ("Navegación: Tabs"), con el
 * patrón de pestañas de WAI-ARIA: la lista con `role="tablist"`, la pestaña
 * activa en el orden de tabulación y las demás con las flechas, Inicio y Fin.
 *
 * `pestanas` es `[{ clave, etiqueta, contenido }]`. Los paneles inactivos se
 * quedan montados, ocultos con `hidden`: al volver a una pestaña, sus filtros
 * y su tabla siguen como estaban.
 */
export function Tabs({ etiqueta, pestanas, activa, alCambiar }) {
  const id = useId();
  const refLista = useRef(null);

  const enfocar = (indice) => {
    const total = pestanas.length;
    const siguiente = pestanas[(indice + total) % total];
    alCambiar(siguiente.clave);
    refLista.current?.querySelector(`[data-clave="${siguiente.clave}"]`)?.focus();
  };

  const teclas = (evento, indice) => {
    const acciones = {
      ArrowRight: () => enfocar(indice + 1),
      ArrowLeft: () => enfocar(indice - 1),
      Home: () => enfocar(0),
      End: () => enfocar(pestanas.length - 1),
    };
    if (acciones[evento.key]) {
      evento.preventDefault();
      acciones[evento.key]();
    }
  };

  return (
    <div>
      <div ref={refLista} role="tablist" aria-label={etiqueta} className="flex gap-4 border-b border-divisor">
        {pestanas.map((pestana, indice) => {
          const seleccionada = pestana.clave === activa;
          return (
            <button
              key={pestana.clave}
              type="button"
              role="tab"
              id={`${id}-pestana-${pestana.clave}`}
              data-clave={pestana.clave}
              aria-selected={seleccionada}
              aria-controls={`${id}-panel-${pestana.clave}`}
              tabIndex={seleccionada ? 0 : -1}
              onClick={() => alCambiar(pestana.clave)}
              onKeyDown={(evento) => teclas(evento, indice)}
              className={`-mb-px min-h-11 border-b-2 px-1 font-medium ${
                seleccionada
                  ? "border-primario text-primario"
                  : "border-transparent text-texto-secundario hover:border-texto-secundario hover:text-texto"
              }`}
            >
              {pestana.etiqueta}
            </button>
          );
        })}
      </div>
      {pestanas.map((pestana) => (
        <div
          key={pestana.clave}
          role="tabpanel"
          id={`${id}-panel-${pestana.clave}`}
          aria-labelledby={`${id}-pestana-${pestana.clave}`}
          hidden={pestana.clave !== activa}
        >
          {pestana.contenido}
        </div>
      ))}
    </div>
  );
}
