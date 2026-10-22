import { Indicador } from "../components/reportes/Indicador";
import { ReportePorFechas } from "../components/reportes/ReportePorFechas";
import { FORMAS_PAGO, TIPOS_DONANTE } from "../constants/estados";
import { obtenerReporteEntradas } from "../services/reporteService";
import { formatearFechaCorta, formatearPeriodo } from "../utils/fechas";
import { formatearQuetzales } from "../utils/formato";

const ENCABEZADO = "px-4 py-3 text-auxiliar font-semibold text-texto-secundario uppercase";
const MONTO = "px-4 py-3 text-right tabular-nums slashed-zero whitespace-nowrap";
const CELDA = "px-4 py-3 whitespace-nowrap";

/* La parte de un monto en el total, como "42 %"; sin total, nada. */
function porcentaje(parte, total) {
  return Number(total) > 0 ? `${Math.round((Number(parte) / Number(total)) * 100)} %` : "—";
}

/* "2 donaciones", "1 pago". */
function contar(cantidad, singular, plural) {
  return `${cantidad} ${cantidad === 1 ? singular : plural}`;
}

/** El encabezado de una tabla: cada columna con su alineación. */
function Encabezado({ columnas }) {
  return (
    <thead>
      <tr className="border-b border-divisor bg-fondo">
        {columnas.map(([titulo, derecha]) => (
          <th key={titulo} scope="col" className={`${ENCABEZADO} ${derecha ? "text-right" : ""}`}>
            {titulo}
          </th>
        ))}
      </tr>
    </thead>
  );
}

/** Una sección de un tipo de entrada: el título, cuántas y cuánto, y su tabla o el aviso de que no hubo. */
function Seccion({ titulo, resumen, vacia, mensajeVacio, children }) {
  return (
    <section className="mt-8">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h3 className="text-h3 font-semibold">{titulo}</h3>
        <p className="text-auxiliar text-texto-secundario tabular-nums slashed-zero">{resumen}</p>
      </div>
      {vacia ? (
        <p className="mt-2 rounded-tarjeta border border-divisor px-4 py-6 text-center text-texto-secundario">
          {mensajeVacio}
        </p>
      ) : (
        <div className="relative mt-2 overflow-x-auto rounded-tarjeta border border-divisor print:overflow-visible">
          {children}
        </div>
      )}
    </section>
  );
}

/** El reporte ya generado: el total por tipo de entrada, las donaciones y los cobros. */
function Reporte({ datos }) {
  const porTipo = [
    ["Donaciones", contar(datos.donaciones.length, "donación", "donaciones"), datos.totalDonaciones],
    ["Cobros a familiares", contar(datos.cobros.length, "pago", "pagos"), datos.totalCobros],
  ];
  const donantes = datos.porTipoDonante.filter((tipo) => tipo.cantidad > 0);

  return (
    <>
      <h3 className="sr-only">Resumen</h3>
      <dl className="grid gap-4 sm:grid-cols-3 print:grid-cols-3">
        <Indicador etiqueta="Donaciones" valor={formatearQuetzales(datos.totalDonaciones)} />
        <Indicador etiqueta="Cobros a familiares" valor={formatearQuetzales(datos.totalCobros)} />
        <Indicador etiqueta="Total de entradas" valor={formatearQuetzales(datos.totalEntradas)} />
      </dl>

      <section className="mt-8 break-inside-avoid">
        <h3 className="text-h3 font-semibold">Por tipo de entrada</h3>
        <div className="relative mt-2 overflow-x-auto rounded-tarjeta border border-divisor print:overflow-visible">
          <table className="w-full min-w-lg border-collapse text-left print:min-w-0">
            <caption className="sr-only">El total de cada tipo de entrada, cuántas hubo y su parte del total</caption>
            <Encabezado columnas={[["Tipo de entrada"], ["Cantidad", true], ["Total", true], ["Parte del total", true]]} />
            <tbody>
              {porTipo.map(([tipo, cantidad, total]) => (
                <tr key={tipo} className="border-b border-divisor">
                  <th scope="row" className="px-4 py-3 text-left font-medium">
                    {tipo}
                  </th>
                  <td className={MONTO}>{cantidad}</td>
                  <td className={MONTO}>{formatearQuetzales(total)}</td>
                  <td className={MONTO}>{porcentaje(total, datos.totalEntradas)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr className="bg-fondo font-semibold">
                <th scope="row" colSpan={2} className="px-4 py-3 text-left">
                  Total de entradas
                </th>
                <td className={MONTO}>{formatearQuetzales(datos.totalEntradas)}</td>
                <td className={MONTO}>100 %</td>
              </tr>
            </tfoot>
          </table>
        </div>
      </section>

      <Seccion
        titulo="Donaciones recibidas"
        resumen={`${contar(datos.donaciones.length, "donación", "donaciones")} · ${formatearQuetzales(datos.totalDonaciones)}`}
        vacia={datos.donaciones.length === 0}
        mensajeVacio="No se recibieron donaciones en esas fechas."
      >
        <table className="w-full min-w-xl border-collapse text-left print:min-w-0">
          <caption className="sr-only">Cada donación recibida: fecha, donante, tipo de donante y monto</caption>
          <Encabezado columnas={[["Fecha"], ["Donante"], ["Tipo de donante"], ["Monto", true]]} />
          <tbody>
            {datos.donaciones.map((donacion) => (
              <tr key={donacion.id} className="break-inside-avoid border-b border-divisor">
                <td className={`${CELDA} tabular-nums slashed-zero`}>{formatearFechaCorta(donacion.fecha)}</td>
                <td className="px-4 py-3">{donacion.donante}</td>
                <td className={CELDA}>{TIPOS_DONANTE[donacion.tipoDonante]}</td>
                <td className={MONTO}>{formatearQuetzales(donacion.monto)}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            {/* Por tipo de donante (RF-15), solo los que donaron en el período. */}
            {donantes.map((tipo) => (
              <tr key={tipo.tipoDonante} className="border-b border-divisor text-texto-secundario">
                <th scope="row" colSpan={3} className="px-4 py-2 text-left font-normal">
                  {TIPOS_DONANTE[tipo.tipoDonante]} · {contar(tipo.cantidad, "donación", "donaciones")}
                </th>
                <td className="px-4 py-2 text-right tabular-nums slashed-zero whitespace-nowrap">
                  {formatearQuetzales(tipo.total)}
                </td>
              </tr>
            ))}
            <tr className="bg-fondo font-semibold">
              <th scope="row" colSpan={3} className="px-4 py-3 text-left">
                Total de donaciones
              </th>
              <td className={MONTO}>{formatearQuetzales(datos.totalDonaciones)}</td>
            </tr>
          </tfoot>
        </table>
      </Seccion>

      <Seccion
        titulo="Cobros a familiares"
        resumen={`${contar(datos.cobros.length, "pago", "pagos")} · ${formatearQuetzales(datos.totalCobros)}`}
        vacia={datos.cobros.length === 0}
        mensajeVacio="No se recibieron pagos de familiares en esas fechas."
      >
        <table className="w-full min-w-xl border-collapse text-left print:min-w-0">
          <caption className="sr-only">Cada pago de un familiar: fecha, paciente, mes de la cuenta, forma de pago y monto</caption>
          <Encabezado columnas={[["Fecha"], ["Paciente"], ["Cuenta"], ["Forma de pago"], ["Monto", true]]} />
          <tbody>
            {datos.cobros.map((cobro) => (
              <tr key={cobro.id} className="break-inside-avoid border-b border-divisor">
                <td className={`${CELDA} tabular-nums slashed-zero`}>{formatearFechaCorta(cobro.fecha)}</td>
                <td className="px-4 py-3">{cobro.paciente.nombreCompleto}</td>
                <td className={CELDA}>{formatearPeriodo(cobro.periodo)}</td>
                <td className={CELDA}>{FORMAS_PAGO[cobro.formaPago]}</td>
                <td className={MONTO}>{formatearQuetzales(cobro.monto)}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr className="bg-fondo font-semibold">
              <th scope="row" colSpan={4} className="px-4 py-3 text-left">
                Total de cobros
              </th>
              <td className={MONTO}>{formatearQuetzales(datos.totalCobros)}</td>
            </tr>
          </tfoot>
        </table>
      </Seccion>
    </>
  );
}

/**
 * HU-27 (YS-161). El reporte de entradas (RF-20b) del Administrador del Asilo:
 * lo que entró entre dos fechas, por tipo de entrada. Las donaciones (HU-22),
 * también por tipo de donante, y los cobros a familiares (HU-34), cada uno con
 * su detalle y su total, y el total de las entradas. Decidido el 22/10: va
 * aparte del de pagos a la fundación (RF-20a).
 */
export function ReporteEntradasPage() {
  return (
    <ReportePorFechas
      migas={["Inicio", "Reportes", "Entradas"]}
      titulo="Reporte de entradas"
      descripcion="Las donaciones y los cobros a familiares recibidos, con el total de cada tipo de entrada."
      tituloImpreso="Entradas"
      nombreArchivo="Reporte de entradas"
      obtener={obtenerReporteEntradas}
      estaVacio={(datos) => datos.donaciones.length === 0 && datos.cobros.length === 0}
      mensajeVacio="No se recibieron donaciones ni pagos de familiares en esas fechas."
    >
      {(datos) => <Reporte datos={datos} />}
    </ReportePorFechas>
  );
}
