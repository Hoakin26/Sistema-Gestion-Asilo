import { Indicador } from "../components/reportes/Indicador";
import { ReportePorFechas } from "../components/reportes/ReportePorFechas";
import { CONCEPTOS_CUENTA, FORMAS_PAGO, SERVICIOS } from "../constants/estados";
import { obtenerReportePagosFundacion } from "../services/reporteService";
import { formatearFechaCorta, formatearPeriodo } from "../utils/fechas";
import { formatearQuetzales } from "../utils/formato";

const ENCABEZADO = "px-4 py-3 text-auxiliar font-semibold text-texto-secundario uppercase";
const MONTO = "px-4 py-3 text-right tabular-nums slashed-zero whitespace-nowrap";

/* La parte de un monto en el total, como "42 %"; sin total, nada. */
function porcentaje(parte, total) {
  return Number(total) > 0 ? `${Math.round((Number(parte) / Number(total)) * 100)} %` : "—";
}

/** El reporte ya generado: el resumen, lo pagado por concepto y cada pago. */
function Reporte({ datos }) {
  const promedio = datos.cantidadPagos > 0 ? Number(datos.totalPagado) / datos.cantidadPagos : 0;

  return (
    <>
      <h3 className="sr-only">Resumen</h3>
      <dl className="grid gap-4 sm:grid-cols-3 print:grid-cols-3">
        <Indicador etiqueta="Pagos realizados" valor={datos.cantidadPagos} />
        <Indicador etiqueta="Total pagado" valor={formatearQuetzales(datos.totalPagado)} />
        <Indicador etiqueta="Promedio por pago" valor={formatearQuetzales(promedio)} />
      </dl>

      <section className="mt-8 break-inside-avoid">
        <h3 className="text-h3 font-semibold">Por concepto</h3>
        <div className="relative mt-2 overflow-x-auto rounded-tarjeta border border-divisor print:overflow-visible">
          <table className="w-full min-w-lg border-collapse text-left print:min-w-0">
            <caption className="sr-only">Lo pagado a la fundación por concepto, en cuántos pagos y su parte del total</caption>
            <thead>
              <tr className="border-b border-divisor bg-fondo">
                <th scope="col" className={ENCABEZADO}>
                  Concepto
                </th>
                <th scope="col" className={`${ENCABEZADO} text-right`}>
                  Pagos
                </th>
                <th scope="col" className={`${ENCABEZADO} text-right`}>
                  Total
                </th>
                <th scope="col" className={`${ENCABEZADO} text-right`}>
                  Parte del total
                </th>
              </tr>
            </thead>
            <tbody>
              {datos.porConcepto.map((concepto) => (
                <tr key={concepto.concepto} className="border-b border-divisor">
                  <th scope="row" className="px-4 py-3 text-left font-medium">
                    {CONCEPTOS_CUENTA[concepto.concepto]}
                  </th>
                  <td className={MONTO}>{concepto.cantidad}</td>
                  <td className={MONTO}>{formatearQuetzales(concepto.total)}</td>
                  <td className={MONTO}>{porcentaje(concepto.total, datos.totalPagado)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr className="bg-fondo font-semibold">
                <th scope="row" className="px-4 py-3 text-left">
                  Total pagado
                </th>
                <td className={MONTO}>{datos.cantidadPagos}</td>
                <td className={MONTO}>{formatearQuetzales(datos.totalPagado)}</td>
                <td className={MONTO}>100 %</td>
              </tr>
            </tfoot>
          </table>
        </div>
      </section>

      <section className="mt-8">
        <h3 className="text-h3 font-semibold">Detalle de los pagos</h3>
        <div className="relative mt-2 overflow-x-auto rounded-tarjeta border border-divisor print:overflow-visible">
          <table className="w-full min-w-xl border-collapse text-left print:min-w-0">
            <caption className="sr-only">
              Cada pago a la fundación: fecha, concepto con el mes de la deuda, forma de pago con su referencia y monto
            </caption>
            <thead>
              <tr className="border-b border-divisor bg-fondo">
                <th scope="col" className={ENCABEZADO}>
                  Fecha
                </th>
                <th scope="col" className={ENCABEZADO}>
                  Concepto
                </th>
                <th scope="col" className={ENCABEZADO}>
                  Forma de pago
                </th>
                <th scope="col" className={`${ENCABEZADO} text-right`}>
                  Monto
                </th>
              </tr>
            </thead>
            <tbody>
              {datos.pagos.map((pago) => (
                <tr key={pago.id} className="break-inside-avoid border-b border-divisor">
                  <td className="px-4 py-3 whitespace-nowrap tabular-nums slashed-zero">
                    {formatearFechaCorta(pago.fecha)}
                  </td>
                  {/* El mes de la deuda debajo del concepto y la referencia debajo de la forma de pago: en
                      columnas aparte, la tabla no cabía en la tableta. */}
                  <td className="px-4 py-3">
                    <span className="block">{SERVICIOS[pago.concepto]}</span>
                    <span className="block text-auxiliar text-texto-secundario">
                      Deuda de {formatearPeriodo(pago.periodo)}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <span className="block">{FORMAS_PAGO[pago.formaPago]}</span>
                    {pago.referencia && (
                      <span className="block text-auxiliar text-texto-secundario tabular-nums slashed-zero">
                        Ref. {pago.referencia}
                      </span>
                    )}
                  </td>
                  <td className={MONTO}>{formatearQuetzales(pago.monto)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr className="bg-fondo font-semibold">
                <th scope="row" colSpan={3} className="px-4 py-3 text-left">
                  Total pagado
                </th>
                <td className={MONTO}>{formatearQuetzales(datos.totalPagado)}</td>
              </tr>
            </tfoot>
          </table>
        </div>
      </section>
    </>
  );
}

/**
 * HU-27 (YS-161). El reporte de pagos a la fundación (RF-20a) del
 * Administrador del Asilo: cada pago que el asilo le hizo entre dos fechas, con
 * su fecha, su concepto (consulta, laboratorio o farmacia) y su monto, lo
 * pagado por concepto y el total. Decidido el 22/10: va aparte del de entradas
 * (RF-20b), como los separó la revisión v1.1.
 */
export function ReportePagosFundacionPage() {
  return (
    <ReportePorFechas
      migas={["Inicio", "Reportes", "Pagos a la fundación"]}
      titulo="Reporte de pagos a la fundación"
      descripcion="Lo que el asilo le pagó a la fundación por consultas, laboratorio y farmacia, con la fecha, el concepto y el monto de cada pago."
      tituloImpreso="Pagos a la fundación"
      etiquetaDesde="Pagos desde"
      etiquetaPeriodo="Pagos realizados"
      nombreArchivo="Reporte de pagos a la fundación"
      obtener={obtenerReportePagosFundacion}
      estaVacio={(datos) => datos.cantidadPagos === 0}
      mensajeVacio="No se registraron pagos a la fundación en esas fechas."
    >
      {(datos) => <Reporte datos={datos} />}
    </ReportePorFechas>
  );
}
