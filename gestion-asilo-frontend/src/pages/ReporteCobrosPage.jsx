import { useEffect, useState } from "react";
import { FileDown, FileText, X } from "lucide-react";

import { Indicador } from "../components/reportes/Indicador";
import { PrintableReportLayout } from "../components/reportes/PrintableReportLayout";
import { Alert } from "../components/ui/Alert";
import { Button } from "../components/ui/Button";
import { FormField } from "../components/ui/FormField";
import { Input } from "../components/ui/Input";
import { SearchAutocomplete } from "../components/ui/SearchAutocomplete";
import { StatusBadge } from "../components/ui/StatusBadge";
import { TablaSkeleton } from "../components/ui/TablaSkeleton";
import { FORMAS_PAGO, SERVICIOS_CONTADOS } from "../constants/estados";
import { AppLayout } from "../layouts/AppLayout";
import { erroresPorCampo, mensajeDeError } from "../services/api";
import { listarInternos } from "../services/internoService";
import { obtenerReporteCobros } from "../services/reporteService";
import { formatearFechaCorta, formatearPeriodo, hoyISO, inicioDeMesISO } from "../utils/fechas";
import { formatearDpi, formatearQuetzales } from "../utils/formato";
import { exportarPdf } from "../utils/imprimir";

const ENCABEZADO = "px-4 py-3 text-auxiliar font-semibold text-texto-secundario uppercase";
const MONTO = "px-4 py-3 text-right tabular-nums slashed-zero whitespace-nowrap";

/* Los pacientes del reporte, también los que ya egresaron: lo que pagó su familiar cuenta. */
async function buscarInternos(termino) {
  const pagina = await listarInternos({ busqueda: termino });
  return pagina.content.slice(0, 8).map((interno) => ({
    clave: interno.id,
    titulo: interno.nombreCompleto,
    subtitulo: `DPI ${formatearDpi(interno.dpi)}${interno.activo ? "" : " · Egresado"}`,
    interno,
  }));
}

/* "2 pagos", "1 examen". */
function contar(cantidad, singular, plural) {
  return `${cantidad} ${cantidad === 1 ? singular : plural}`;
}

/* La parte de un monto en el total, como "42 %"; sin total, nada. */
function porcentaje(parte, total) {
  return Number(total) > 0 ? `${Math.round((Number(parte) / Number(total)) * 100)} %` : "—";
}

/** Los pagos que recibió un paciente en el período, con el subtotal al pie. */
function TablaPagos({ paciente }) {
  return (
    <div className="relative mt-2 overflow-x-auto rounded-tarjeta border border-divisor print:overflow-visible">
      <table className="w-full min-w-xl border-collapse text-left print:min-w-0">
        <caption className="sr-only">
          Pagos recibidos de {paciente.paciente.nombreCompleto}: fecha, mes de la cuenta, forma de pago, referencia y
          monto
        </caption>
        <thead>
          <tr className="border-b border-divisor bg-fondo">
            <th scope="col" className={ENCABEZADO}>
              Fecha
            </th>
            <th scope="col" className={ENCABEZADO}>
              Cuenta
            </th>
            <th scope="col" className={ENCABEZADO}>
              Forma de pago
            </th>
            <th scope="col" className={ENCABEZADO}>
              Referencia
            </th>
            <th scope="col" className={`${ENCABEZADO} text-right`}>
              Monto
            </th>
          </tr>
        </thead>
        <tbody>
          {paciente.pagos.map((pago) => (
            <tr key={pago.id} className="break-inside-avoid border-b border-divisor">
              <td className="px-4 py-3 whitespace-nowrap tabular-nums slashed-zero">{formatearFechaCorta(pago.fecha)}</td>
              <td className="px-4 py-3 whitespace-nowrap">{formatearPeriodo(pago.periodo)}</td>
              <td className="px-4 py-3 whitespace-nowrap">{FORMAS_PAGO[pago.formaPago]}</td>
              <td className="px-4 py-3 tabular-nums slashed-zero">
                {pago.referencia || <span className="text-texto-secundario">—</span>}
              </td>
              <td className={MONTO}>{formatearQuetzales(pago.monto)}</td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          <tr className="bg-fondo font-semibold">
            <th scope="row" colSpan={4} className="px-4 py-3 text-left">
              Subtotal cobrado
            </th>
            <td className={MONTO}>{formatearQuetzales(paciente.totalCobrado)}</td>
          </tr>
        </tfoot>
      </table>
    </div>
  );
}

/* Una línea de la cuenta: el concepto a la izquierda y el monto a la derecha. */
function Linea({ concepto, monto, total = false }) {
  return (
    <div className={`flex justify-between gap-4 ${total ? "mt-1 border-t border-divisor pt-1 font-semibold" : ""}`}>
      <dt className={total ? "" : "text-texto-secundario"}>{concepto}</dt>
      <dd className="tabular-nums slashed-zero whitespace-nowrap">{monto}</dd>
    </div>
  );
}

/*
 * Las cuentas a las que se aplicaron esos pagos, cada una como su factura: la
 * cuota, los gastos médicos por tipo sin nombres (RNF-03) y el descuento de la
 * fundación, que suman el total; y al lado lo cobrado de ella en el período, lo
 * pagado hasta hoy y el saldo. En tarjetas y no en tabla: con tantas cifras por
 * cuenta, la tabla no cabía en la tableta sin esconder el saldo.
 */
function ListaCuentas({ paciente }) {
  return (
    <ul className="mt-2 space-y-3">
      {paciente.cuentas.map((cuenta) => (
        <li key={cuenta.id} className="break-inside-avoid rounded-tarjeta border border-divisor p-4">
          <div className="flex flex-wrap items-start justify-between gap-2">
            <div>
              <p className="font-semibold">{formatearPeriodo(cuenta.periodo)}</p>
              <p className="text-auxiliar text-texto-secundario">A cargo de {cuenta.familiar}</p>
            </div>
            <StatusBadge estado={cuenta.estado} />
          </div>
          <div className="mt-3 grid gap-x-8 gap-y-3 sm:grid-cols-2 print:grid-cols-2">
            <dl aria-label={`Desglose de la cuenta de ${formatearPeriodo(cuenta.periodo)}`} className="space-y-1">
              <Linea concepto="Cuota base" monto={formatearQuetzales(cuenta.cuotaBase)} />
              {cuenta.gastos.map((gasto) => (
                <Linea
                  key={gasto.tipo}
                  concepto={contar(gasto.cantidad, ...SERVICIOS_CONTADOS[gasto.tipo])}
                  monto={formatearQuetzales(gasto.original)}
                />
              ))}
              {cuenta.gastos.length === 0 && <Linea concepto="Sin gastos médicos" monto={formatearQuetzales(0)} />}
              {Number(cuenta.totalDescuentos) > 0 && (
                <Linea concepto="Descuento" monto={`−${formatearQuetzales(cuenta.totalDescuentos)}`} />
              )}
              <Linea concepto="Total de la cuenta" monto={formatearQuetzales(cuenta.totalAPagar)} total />
            </dl>
            <dl aria-label={`Pagos de la cuenta de ${formatearPeriodo(cuenta.periodo)}`} className="space-y-1">
              <Linea concepto="Cobrado en el período" monto={formatearQuetzales(cuenta.cobradoEnPeriodo)} />
              <Linea concepto="Pagado hasta hoy" monto={formatearQuetzales(cuenta.totalPagado)} />
              <Linea concepto="Saldo" monto={formatearQuetzales(cuenta.saldo)} total />
            </dl>
          </div>
        </li>
      ))}
    </ul>
  );
}

/** Lo cobrado a un paciente: sus pagos y las cuentas que cubren. */
function SeccionPaciente({ paciente }) {
  return (
    <section className="mt-8 break-inside-avoid-page first:mt-0">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h3 className="text-h3 font-semibold">{paciente.paciente.nombreCompleto}</h3>
        <p className="text-auxiliar text-texto-secundario tabular-nums slashed-zero">
          {contar(paciente.pagos.length, "pago", "pagos")} · Cobrado {formatearQuetzales(paciente.totalCobrado)}
        </p>
      </div>
      <h4 className="mt-4 font-semibold">Pagos recibidos</h4>
      <TablaPagos paciente={paciente} />
      <h4 className="mt-4 font-semibold">Cuentas a las que se aplicaron</h4>
      <ListaCuentas paciente={paciente} />
    </section>
  );
}

/** El reporte ya generado: el resumen, lo cobrado por forma de pago y cada paciente. */
function Reporte({ datos }) {
  const promedio = datos.cantidadPagos > 0 ? Number(datos.totalCobrado) / datos.cantidadPagos : 0;
  const formas = datos.porFormaPago.filter((forma) => forma.cantidad > 0);

  return (
    <>
      <h3 className="sr-only">Resumen</h3>
      <dl className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4 print:grid-cols-4">
        <Indicador etiqueta="Pagos recibidos" valor={datos.cantidadPagos} />
        <Indicador etiqueta="Pacientes" valor={datos.pacientes.length} />
        <Indicador etiqueta="Total cobrado" valor={formatearQuetzales(datos.totalCobrado)} />
        <Indicador etiqueta="Promedio por pago" valor={formatearQuetzales(promedio)} />
      </dl>

      <section className="mt-8 break-inside-avoid">
        <h3 className="text-h3 font-semibold">Por forma de pago</h3>
        <div className="relative mt-2 overflow-x-auto rounded-tarjeta border border-divisor print:overflow-visible">
          <table className="w-full min-w-lg border-collapse text-left print:min-w-0">
            <caption className="sr-only">Lo cobrado con cada forma de pago, en cuántos pagos y su parte del total</caption>
            <thead>
              <tr className="border-b border-divisor bg-fondo">
                <th scope="col" className={ENCABEZADO}>
                  Forma de pago
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
              {formas.map((forma) => (
                <tr key={forma.formaPago} className="border-b border-divisor">
                  <th scope="row" className="px-4 py-3 text-left font-medium">
                    {FORMAS_PAGO[forma.formaPago]}
                  </th>
                  <td className={MONTO}>{forma.cantidad}</td>
                  <td className={MONTO}>{formatearQuetzales(forma.total)}</td>
                  <td className={MONTO}>{porcentaje(forma.total, datos.totalCobrado)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr className="bg-fondo font-semibold">
                <th scope="row" className="px-4 py-3 text-left">
                  Total cobrado
                </th>
                <td className={MONTO}>{datos.cantidadPagos}</td>
                <td className={MONTO}>{formatearQuetzales(datos.totalCobrado)}</td>
                <td className={MONTO}>100 %</td>
              </tr>
            </tfoot>
          </table>
        </div>
      </section>

      <div className="mt-8">
        {datos.pacientes.map((paciente) => (
          <SeccionPaciente key={paciente.paciente.id} paciente={paciente} />
        ))}
      </div>
    </>
  );
}

/**
 * HU-26 (YS-158). El reporte de cobros a familiares (RF-19) del Administrador
 * del Asilo: los pagos recibidos entre dos fechas (decidido el 22/10: el rango
 * es de los pagos), por paciente, con las cuentas que cubren y el detalle de
 * sus gastos médicos, y el total cobrado. Se filtra por paciente y por rango de
 * fechas, y se exporta a PDF con la impresión del navegador.
 *
 * Los gastos van por tipo de servicio y nunca por nombre (RNF-03): cuántas
 * consultas, exámenes y medicamentos, sin decir cuáles.
 */
export function ReporteCobrosPage() {
  const [interno, setInterno] = useState(null);
  const [desde, setDesde] = useState(inicioDeMesISO);
  const [hasta, setHasta] = useState(hoyISO);
  // Lo que de verdad se consulta: cambia al pulsar "Generar", no en cada cambio.
  const [filtros, setFiltros] = useState(() => ({ interno: null, desde: inicioDeMesISO(), hasta: hoyISO() }));

  const [datos, setDatos] = useState(null);
  const [generado, setGenerado] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [errores, setErrores] = useState({});

  useEffect(() => {
    let vigente = true;
    obtenerReporteCobros({ internoId: filtros.interno?.id, desde: filtros.desde, hasta: filtros.hasta })
      .then((respuesta) => {
        if (!vigente) return;
        setDatos(respuesta);
        setGenerado(new Date().toISOString());
        setError(null);
      })
      .catch((fallo) => {
        if (!vigente) return;
        const porCampo = erroresPorCampo(fallo);
        if (porCampo) setErrores(porCampo);
        else setError(mensajeDeError(fallo, "No se pudo generar el reporte"));
      })
      .finally(() => {
        if (vigente) setCargando(false);
      });
    return () => {
      vigente = false;
    };
  }, [filtros]);

  const generar = (evento) => {
    evento.preventDefault();
    const encontrados = {};
    if (!desde) encontrados.desde = "Indique la fecha inicial";
    if (!hasta) encontrados.hasta = "Indique la fecha final";
    else if (desde && hasta < desde) encontrados.hasta = "La fecha final no puede ser anterior a la inicial";
    setErrores(encontrados);
    if (Object.keys(encontrados).length > 0) return;
    setCargando(true);
    setFiltros({ interno, desde, hasta });
  };

  const periodo = datos ? `${formatearFechaCorta(datos.desde)} al ${formatearFechaCorta(datos.hasta)}` : "";
  const nombreArchivo = datos
    ? `Reporte de cobros ${datos.desde} a ${datos.hasta}${datos.paciente ? ` - ${datos.paciente.nombreCompleto}` : ""}`
    : "Reporte de cobros";

  return (
    <AppLayout migas={["Inicio", "Reportes", "Cobros"]}>
      <div className="print:hidden">
        <h1 className="text-h1 font-bold">Reporte de cobros</h1>
        <p className="mt-2 text-texto-secundario">
          Los pagos recibidos de los familiares, por paciente, con las cuentas que cubren y sus gastos médicos por tipo.
          Sin nombres de exámenes ni de medicamentos.
        </p>

        <form onSubmit={generar} noValidate className="mt-6 flex flex-wrap items-start gap-4 sm:[&>button]:mt-8">
          <FormField etiqueta="Paciente" error={errores.internoId}>
            {(campo) =>
              interno ? (
                <div
                  id={campo.id}
                  className="flex min-h-11 items-center justify-between gap-4 rounded-campo border border-texto-secundario bg-fondo px-4 py-2 sm:w-80"
                >
                  <p className="font-medium text-texto">{interno.nombreCompleto}</p>
                  <Button
                    type="button"
                    variante="texto"
                    aria-label={`Quitar el paciente elegido, ${interno.nombreCompleto}: el reporte será de todos`}
                    onClick={() => setInterno(null)}
                  >
                    <X aria-hidden="true" size={20} />
                    Quitar
                  </Button>
                </div>
              ) : (
                <div className="sm:w-80">
                  <SearchAutocomplete
                    {...campo}
                    flotante
                    buscar={buscarInternos}
                    alElegir={(opcion) => setInterno(opcion.interno)}
                    placeholder="Todos los pacientes"
                    etiquetaLista="Internos que coinciden"
                  />
                </div>
              )
            }
          </FormField>
          <FormField etiqueta="Pagos desde" obligatorio error={errores.desde}>
            {(campo) => (
              <Input
                {...campo}
                type="date"
                value={desde}
                max={hoyISO()}
                onChange={(evento) => setDesde(evento.target.value)}
                className="sm:w-48"
              />
            )}
          </FormField>
          <FormField etiqueta="Hasta" obligatorio error={errores.hasta}>
            {(campo) => (
              <Input
                {...campo}
                type="date"
                value={hasta}
                onChange={(evento) => setHasta(evento.target.value)}
                className="sm:w-48"
              />
            )}
          </FormField>
          <Button type="submit" cargando={cargando}>
            {!cargando && <FileText aria-hidden="true" size={20} />}
            Generar reporte
          </Button>
          <Button
            type="button"
            variante="secundario"
            disabled={!datos || cargando}
            onClick={() => exportarPdf(nombreArchivo)}
          >
            <FileDown aria-hidden="true" size={20} />
            Exportar a PDF
          </Button>
        </form>
        <p className="mt-2 text-auxiliar text-texto-secundario">
          «Exportar a PDF» abre la impresión del navegador: elija «Guardar como PDF» como destino.
        </p>
      </div>

      {error && (
        <div className="mt-6 print:hidden">
          <Alert>{error}</Alert>
        </div>
      )}

      <div className="mt-6 print:mt-0">
        {cargando && !datos ? (
          <TablaSkeleton etiqueta="Generando el reporte…" />
        ) : (
          datos && (
            <PrintableReportLayout
              titulo="Cobros a familiares"
              filtros={[
                ["Paciente", datos.paciente ? datos.paciente.nombreCompleto : "Todos"],
                ["Pagos recibidos", periodo],
              ]}
              generado={generado}
            >
              {datos.cantidadPagos === 0 ? (
                <p className="py-8 text-center text-texto-secundario">No se recibieron pagos en esas fechas.</p>
              ) : (
                <Reporte datos={datos} />
              )}
            </PrintableReportLayout>
          )
        )}
      </div>
    </AppLayout>
  );
}
