import logotipo from "../../assets/isotipo/horizontal-primario.svg";
import { formatearFechaHora } from "../../utils/fechas";

/**
 * YS-152. El `PrintableReportLayout` del manual de diseño, que reutilizan los
 * cinco reportes (HU-24 a HU-28): el encabezado con el logotipo horizontal, el
 * título, los filtros aplicados y la fecha de generación, el contenido, y un
 * pie con el nombre del asilo.
 *
 * En pantalla es una tarjeta; al imprimir, la hoja entera. "Exportar a PDF"
 * es la impresión del navegador (`exportarPdf`, en utils/imprimir.js): así el
 * PDF sale igual a lo que se ve, sin agregar una biblioteca al backend.
 *
 * `filtros` es `[["Paciente", "Todos"], ["Desde", "1 oct 2026"], …]`.
 */
export function PrintableReportLayout({ titulo, filtros, generado, children }) {
  return (
    <article className="rounded-tarjeta border border-divisor bg-superficie p-4 shadow-tarjeta sm:p-6 print:rounded-none print:border-0 print:p-0 print:shadow-none">
      <header className="flex flex-wrap items-start justify-between gap-4 border-b border-divisor pb-4">
        <img src={logotipo} alt="Asilo de Ancianos Cabeza de Algodón" className="h-12 w-auto" />
        <div className="text-right">
          <h2 className="text-h2 font-semibold">{titulo}</h2>
          <p className="mt-1 text-auxiliar text-texto-secundario tabular-nums slashed-zero">
            Generado el {formatearFechaHora(generado)}
          </p>
        </div>
      </header>

      <dl className="mt-4 flex flex-wrap gap-x-8 gap-y-2 text-auxiliar">
        {filtros.map(([etiqueta, valor]) => (
          <div key={etiqueta} className="flex gap-2">
            <dt className="text-texto-secundario">{etiqueta}:</dt>
            <dd className="font-medium tabular-nums slashed-zero">{valor}</dd>
          </div>
        ))}
      </dl>

      <div className="mt-6">{children}</div>

      <footer className="mt-8 border-t border-divisor pt-4 text-auxiliar text-texto-secundario">
        Asilo de Ancianos Cabeza de Algodón · Sistema de gestión
      </footer>
    </article>
  );
}

