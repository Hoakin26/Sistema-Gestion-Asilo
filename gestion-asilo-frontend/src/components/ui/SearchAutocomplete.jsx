import { useEffect, useId, useState } from "react";
import { Loader2, Search } from "lucide-react";

import { Input } from "./Input";

/* Lo bastante para no pedir en cada tecla, poco para que no se sienta lento. */
const ESPERA_MS = 300;
const MINIMO_LETRAS = 2;

/**
 * YS-85. Un campo que busca mientras se escribe y ofrece las coincidencias en
 * una lista para elegir. Es el `SearchAutocomplete` del manual de diseño.
 *
 * `buscar(termino)` devuelve una promesa con opciones `{ clave, titulo,
 * subtitulo }`. El componente no guarda lo elegido: se lo entrega a
 * `alElegir` y quien lo usa decide cómo mostrarlo.
 *
 * Sigue el patrón "combobox" de ARIA: el foco se queda en el campo y las
 * flechas mueven la opción activa, que el lector de pantalla anuncia por
 * `aria-activedescendant`. La lista va debajo del campo y no flotando encima:
 * dentro de una ventana con desplazamiento propio, una lista flotante quedaba
 * recortada por el borde.
 */
export function SearchAutocomplete({
  buscar,
  alElegir,
  placeholder,
  etiquetaLista,
  invalido,
  ...propsCampo
}) {
  const idLista = useId();
  const [texto, setTexto] = useState("");
  const [opciones, setOpciones] = useState([]);
  const [buscando, setBuscando] = useState(false);
  const [activa, setActiva] = useState(-1);
  // El término de la última búsqueda que respondió: distingue "sin
  // resultados" de "todavía no se buscó".
  const [buscado, setBuscado] = useState(null);

  const termino = texto.trim();
  const suficiente = termino.length >= MINIMO_LETRAS;

  useEffect(() => {
    if (!suficiente) return undefined;

    // Si sigue escribiendo, la respuesta de una búsqueda vieja puede llegar
    // después de la nueva: `vigente` la descarta.
    let vigente = true;
    const espera = setTimeout(() => {
      setBuscando(true);
      buscar(termino)
        .then((encontradas) => {
          if (!vigente) return;
          setOpciones(encontradas);
          setActiva(encontradas.length > 0 ? 0 : -1);
          setBuscado(termino);
        })
        .catch(() => {
          if (vigente) setBuscado(null);
        })
        .finally(() => {
          if (vigente) setBuscando(false);
        });
    }, ESPERA_MS);

    return () => {
      vigente = false;
      clearTimeout(espera);
    };
  }, [termino, suficiente, buscar]);

  const abierta = suficiente && buscado === termino;
  const visibles = abierta ? opciones : [];

  const elegir = (opcion) => {
    alElegir(opcion);
    setTexto("");
    setOpciones([]);
    setBuscado(null);
  };

  const alPresionar = (evento) => {
    if (!abierta) return;

    if (evento.key === "ArrowDown" && visibles.length > 0) {
      evento.preventDefault();
      setActiva((actual) => (actual + 1) % visibles.length);
    } else if (evento.key === "ArrowUp" && visibles.length > 0) {
      evento.preventDefault();
      setActiva((actual) => (actual - 1 + visibles.length) % visibles.length);
    } else if (evento.key === "Enter" && activa >= 0) {
      // Sin esto, Enter enviaría el formulario en vez de elegir.
      evento.preventDefault();
      elegir(visibles[activa]);
    } else if (evento.key === "Escape") {
      // Cierra la lista sin cerrar la ventana que la contiene.
      evento.preventDefault();
      setBuscado(null);
    }
  };

  return (
    <div>
      <div className="relative">
        <Search
          aria-hidden="true"
          size={20}
          className="pointer-events-none absolute top-1/2 left-4 -translate-y-1/2 text-texto-secundario"
        />
        <Input
          {...propsCampo}
          invalido={invalido}
          role="combobox"
          aria-autocomplete="list"
          aria-expanded={abierta}
          aria-controls={idLista}
          aria-activedescendant={abierta && activa >= 0 ? `${idLista}-${activa}` : undefined}
          autoComplete="off"
          value={texto}
          onChange={(evento) => setTexto(evento.target.value)}
          onKeyDown={alPresionar}
          placeholder={placeholder}
          className="pl-12 pr-12"
        />
        {buscando && (
          <Loader2
            aria-hidden="true"
            size={20}
            className="absolute top-1/2 right-4 -translate-y-1/2 animate-spin text-texto-secundario"
          />
        )}
      </div>

      <ul
        id={idLista}
        role="listbox"
        aria-label={etiquetaLista}
        className={
          abierta && visibles.length > 0
            ? "mt-2 max-h-64 overflow-y-auto rounded-tarjeta border border-divisor bg-superficie shadow-menu"
            : "hidden"
        }
      >
        {visibles.map((opcion, indice) => (
          <li
            key={opcion.clave}
            id={`${idLista}-${indice}`}
            role="option"
            aria-selected={indice === activa}
            // mousedown y no click: el click llega después de que el campo
            // pierde el foco, y en algunos navegadores eso ya movió la lista.
            onMouseDown={(evento) => {
              evento.preventDefault();
              elegir(opcion);
            }}
            onMouseEnter={() => setActiva(indice)}
            className={`flex min-h-11 cursor-pointer flex-col justify-center px-4 py-2 ${
              indice === activa ? "bg-primario-tenue" : ""
            }`}
          >
            <span className="font-medium text-texto">{opcion.titulo}</span>
            {opcion.subtitulo && (
              <span className="text-auxiliar text-texto-secundario tabular-nums slashed-zero">
                {opcion.subtitulo}
              </span>
            )}
          </li>
        ))}
      </ul>

      <p aria-live="polite" className="mt-2 text-auxiliar text-texto-secundario empty:hidden">
        {abierta && visibles.length === 0 ? "Nadie coincide con lo que escribió." : ""}
      </p>
    </div>
  );
}
