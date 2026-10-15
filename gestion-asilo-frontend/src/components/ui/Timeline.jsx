/**
 * YS-103. El `Timeline` del sistema de diseño: una lista en orden, cada entrada
 * con su punto y la línea que la une a la siguiente.
 *
 * Es una lista ordenada de verdad (<ol>), así que el lector de pantalla dice
 * cuántas entradas hay y en cuál va; el punto y la línea son decoración. Cada
 * entrada dibuja su propio tramo de línea, en vez de un borde en la lista, para
 * que el punto quede centrado sobre ella sin desplazamientos calculados a mano.
 */
export function Timeline({ etiqueta, children }) {
  return (
    <ol aria-label={etiqueta} className="flex flex-col">
      {children}
    </ol>
  );
}

export function TimelineItem({ children }) {
  return (
    <li className="group flex gap-4">
      <div aria-hidden="true" className="flex flex-col items-center">
        <span className="mt-1 size-4 shrink-0 rounded-full border-2 border-primario bg-superficie" />
        <span className="w-0.5 flex-1 bg-divisor group-last:hidden" />
      </div>
      <div className="min-w-0 flex-1 pb-6 group-last:pb-0">{children}</div>
    </li>
  );
}
