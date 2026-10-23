import { formatearQuetzales } from "../../utils/formato";

/**
 * YS-149. Lo que suma un histórico financiero en el período: el total y, en
 * tarjetas, lo de cada tipo con cuántos registros. Salió de "Donaciones"
 * (HU-22) para servir también a "Gastos" (HU-23).
 *
 * `grupos` es `[{ clave, etiqueta, cantidad, total }]` y `unidad`,
 * `["donación", "donaciones"]`. Con `soloConMovimientos`, los tipos en cero no
 * se muestran: con ocho tipos de gasto, la mayoría lo estaría.
 */
export function TotalesPorTipo({ etiqueta, titulo, total, grupos, unidad, soloConMovimientos = false }) {
  const [singular, plural] = unidad;
  const visibles = soloConMovimientos ? grupos.filter((grupo) => grupo.cantidad > 0) : grupos;

  return (
    <section
      aria-label={etiqueta}
      className="mt-6 rounded-tarjeta border border-divisor bg-superficie p-4 shadow-tarjeta sm:p-6"
    >
      <div className="flex flex-wrap items-baseline justify-between gap-4">
        <p className="text-texto-secundario">{titulo}</p>
        <p className="text-h3 font-semibold tabular-nums slashed-zero">{formatearQuetzales(total)}</p>
      </div>
      {visibles.length > 0 && (
        <dl className="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {visibles.map((grupo) => (
            <div key={grupo.clave} className="rounded-tarjeta bg-fondo px-4 py-3">
              <dt className="text-auxiliar text-texto-secundario">{grupo.etiqueta}</dt>
              <dd className="mt-1 tabular-nums slashed-zero">
                <span className="font-semibold">{formatearQuetzales(grupo.total)}</span>
                <span className="text-auxiliar text-texto-secundario">
                  {" "}
                  · {grupo.cantidad} {grupo.cantidad === 1 ? singular : plural}
                </span>
              </dd>
            </div>
          ))}
        </dl>
      )}
    </section>
  );
}
