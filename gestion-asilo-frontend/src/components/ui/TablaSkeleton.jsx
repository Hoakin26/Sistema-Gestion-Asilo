/*
 * Reserva la forma de la tabla mientras carga, para que el contenido no salte
 * al llegar. El latido es deliberadamente tenue: un bloque gris parpadeando
 * fuerte se lee como "se rompió" para personal no técnico (RNF-04).
 */
function Barra({ className }) {
  return (
    <span
      className={`block h-4 rounded-campo bg-neutral-tenue animate-latido motion-reduce:animate-none ${className}`}
    />
  );
}

export function TablaSkeleton({ filas = 4 }) {
  return (
    <div
      role="status"
      aria-live="polite"
      className="overflow-hidden rounded-tarjeta border border-divisor bg-superficie shadow-tarjeta"
    >
      <span className="sr-only">Cargando usuarios…</span>

      {Array.from({ length: filas }, (_, indice) => (
        <div
          key={indice}
          aria-hidden="true"
          className="flex items-center gap-6 border-b border-divisor px-6 py-5 last:border-b-0"
        >
          <div className="flex flex-1 flex-col gap-2">
            <Barra className="w-48" />
            <Barra className="w-64" />
          </div>
          <Barra className="w-32" />
          <Barra className="w-24" />
        </div>
      ))}
    </div>
  );
}
