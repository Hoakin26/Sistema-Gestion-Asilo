import { ChevronLeft, ChevronRight } from "lucide-react";

import { Button } from "./Button";

/**
 * OBS-01. Los botones de una lista por páginas: Anterior, "Página n de m" y
 * Siguiente. Estaba copiado en cada lista paginada; ahora que son más, vive
 * aquí. No se muestra si todo cabe en una página.
 *
 * `pagina` empieza en cero, como la del backend; `etiqueta` nombra la lista
 * para quien usa lector de pantalla ("Páginas de la lista de internos").
 */
export function Paginacion({ pagina, totalPaginas, alCambiar, etiqueta }) {
  if (totalPaginas <= 1) return null;
  return (
    <nav aria-label={etiqueta} className="mt-4 flex flex-wrap items-center justify-between gap-4">
      <Button variante="secundario" disabled={pagina === 0} onClick={() => alCambiar(pagina - 1)}>
        <ChevronLeft aria-hidden="true" size={20} />
        Anterior
      </Button>
      <p className="text-auxiliar text-texto-secundario tabular-nums slashed-zero">
        Página {pagina + 1} de {totalPaginas}
      </p>
      <Button variante="secundario" disabled={pagina + 1 >= totalPaginas} onClick={() => alCambiar(pagina + 1)}>
        Siguiente
        <ChevronRight aria-hidden="true" size={20} />
      </Button>
    </nav>
  );
}
