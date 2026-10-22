/**
 * YS-155. Una cifra del resumen de un reporte: la etiqueta arriba y el valor
 * grande, dentro de un `<dl>`. Salió del reporte de costos (HU-24) para que
 * todos los reportes resuman igual.
 */
export function Indicador({ etiqueta, valor }) {
  return (
    <div className="rounded-tarjeta bg-fondo px-4 py-3">
      <dt className="text-auxiliar text-texto-secundario">{etiqueta}</dt>
      <dd className="mt-1 text-h3 font-semibold tabular-nums slashed-zero">{valor}</dd>
    </div>
  );
}
