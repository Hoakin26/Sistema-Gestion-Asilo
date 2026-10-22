import { useEffect, useState } from "react";
import { FileDown, FileText, X } from "lucide-react";

import { Indicador } from "../components/reportes/Indicador";
import { PrintableReportLayout } from "../components/reportes/PrintableReportLayout";
import { Alert } from "../components/ui/Alert";
import { Button } from "../components/ui/Button";
import { FormField } from "../components/ui/FormField";
import { Input } from "../components/ui/Input";
import { SearchAutocomplete } from "../components/ui/SearchAutocomplete";
import { TablaSkeleton } from "../components/ui/TablaSkeleton";
import { AppLayout } from "../layouts/AppLayout";
import { erroresPorCampo, mensajeDeError } from "../services/api";
import { listarInternos } from "../services/internoService";
import { obtenerReporteCostos } from "../services/reporteService";
import { formatearFecha, formatearFechaCorta, hoyISO, inicioDeMesISO } from "../utils/fechas";
import { formatearDpi, formatearQuetzales } from "../utils/formato";
import { exportarPdf } from "../utils/imprimir";

const ENCABEZADO = "px-4 py-3 text-auxiliar font-semibold text-texto-secundario uppercase";
const MONTO = "px-4 py-3 text-right tabular-nums slashed-zero whitespace-nowrap";

/* Los pacientes del reporte, también los que ya egresaron: sus citas pasadas cuentan. */
async function buscarInternos(termino) {
  const pagina = await listarInternos({ busqueda: termino });
  return pagina.content.slice(0, 8).map((interno) => ({
    clave: interno.id,
    titulo: interno.nombreCompleto,
    subtitulo: `DPI ${formatearDpi(interno.dpi)}${interno.activo ? "" : " · Egresado"}`,
    interno,
  }));
}

/* "2 exámenes", "1 medicamento". */
function contar(cantidad, singular, plural) {
  return `${cantidad} ${cantidad === 1 ? singular : plural}`;
}

/* La parte de un costo en el total, como "42 %"; sin total, nada. */
function porcentaje(parte, total) {
  return Number(total) > 0 ? `${Math.round((Number(parte) / Number(total)) * 100)} %` : "—";
}

/** Las citas de un paciente, con el subtotal al pie. */
function TablaPaciente({ paciente }) {
  const { totales } = paciente;
  return (
    <section className="mt-8 break-inside-avoid-page first:mt-0">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h3 className="text-h3 font-semibold">{paciente.paciente.nombreCompleto}</h3>
        <p className="text-auxiliar text-texto-secundario tabular-nums slashed-zero">
          {contar(paciente.citas.length, "cita", "citas")} · Promedio por cita {formatearQuetzales(paciente.promedioPorCita)}
        </p>
      </div>
      {/* relative: los sr-only de la tabla son absolutos; sin él escapan del recorte (YS-175). */}
      <div className="relative mt-2 overflow-x-auto rounded-tarjeta border border-divisor print:overflow-visible">
        <table className="w-full min-w-4xl border-collapse text-left print:min-w-0">
          <caption className="sr-only">
            Costos de las citas de {paciente.paciente.nombreCompleto}: consulta, exámenes, medicamentos, total, descuento
            y neto
          </caption>
          <thead>
            <tr className="border-b border-divisor bg-fondo">
              <th scope="col" className={ENCABEZADO}>
                Fecha
              </th>
              <th scope="col" className={ENCABEZADO}>
                Solicitud
              </th>
              <th scope="col" className={`${ENCABEZADO} text-right`}>
                Consulta
              </th>
              <th scope="col" className={`${ENCABEZADO} text-right`}>
                Exámenes
              </th>
              <th scope="col" className={`${ENCABEZADO} text-right`}>
                Medicamentos
              </th>
              <th scope="col" className={`${ENCABEZADO} text-right`}>
                Total
              </th>
              <th scope="col" className={`${ENCABEZADO} text-right`}>
                Descuento
              </th>
              <th scope="col" className={`${ENCABEZADO} text-right`}>
                Neto
              </th>
            </tr>
          </thead>
          <tbody>
            {paciente.citas.map((cita) => (
              <tr key={cita.visitaId} className="break-inside-avoid border-b border-divisor align-top">
                <td className="px-4 py-3 whitespace-nowrap tabular-nums slashed-zero">{formatearFecha(cita.fecha)}</td>
                <td className="px-4 py-3 whitespace-nowrap tabular-nums slashed-zero">{cita.numeroSolicitud}</td>
                <td className={MONTO}>{formatearQuetzales(cita.costos.consulta)}</td>
                <td className={MONTO}>
                  {formatearQuetzales(cita.costos.costoExamenes)}
                  {cita.costos.examenes > 0 && (
                    <span className="block text-auxiliar text-texto-secundario">
                      {contar(cita.costos.examenes, "examen", "exámenes")}
                    </span>
                  )}
                </td>
                <td className={MONTO}>
                  {formatearQuetzales(cita.costos.costoMedicamentos)}
                  {cita.costos.medicamentos > 0 && (
                    <span className="block text-auxiliar text-texto-secundario">
                      {contar(cita.costos.medicamentos, "medicamento", "medicamentos")}
                    </span>
                  )}
                </td>
                <td className={`${MONTO} font-semibold`}>{formatearQuetzales(cita.costos.total)}</td>
                <td className={MONTO}>{formatearQuetzales(cita.costos.descuento)}</td>
                <td className={MONTO}>{formatearQuetzales(cita.costos.neto)}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr className="bg-fondo font-semibold">
              <th scope="row" colSpan={2} className="px-4 py-3 text-left">
                Subtotal
              </th>
              <td className={MONTO}>{formatearQuetzales(totales.consulta)}</td>
              <td className={MONTO}>{formatearQuetzales(totales.costoExamenes)}</td>
              <td className={MONTO}>{formatearQuetzales(totales.costoMedicamentos)}</td>
              <td className={MONTO}>{formatearQuetzales(totales.total)}</td>
              <td className={MONTO}>{formatearQuetzales(totales.descuento)}</td>
              <td className={MONTO}>{formatearQuetzales(totales.neto)}</td>
            </tr>
          </tfoot>
        </table>
      </div>
    </section>
  );
}

/** El reporte ya generado: el resumen, la distribución por tipo y las citas de cada paciente. */
function Reporte({ datos }) {
  const { totales } = datos;
  const consultas = datos.pacientes.reduce(
    (suma, paciente) => suma + paciente.citas.filter((cita) => Number(cita.costos.consulta) > 0).length,
    0,
  );
  const porTipo = [
    ["Consultas", contar(consultas, "consulta", "consultas"), totales.consulta],
    ["Exámenes de laboratorio", contar(totales.examenes, "examen", "exámenes"), totales.costoExamenes],
    ["Medicamentos", contar(totales.medicamentos, "medicamento", "medicamentos"), totales.costoMedicamentos],
  ];

  return (
    <>
      <h3 className="sr-only">Resumen</h3>
      <dl className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3 print:grid-cols-6">
        <Indicador etiqueta="Citas" valor={datos.cantidadCitas} />
        <Indicador etiqueta="Pacientes" valor={datos.pacientes.length} />
        <Indicador etiqueta="Costo total" valor={formatearQuetzales(totales.total)} />
        <Indicador etiqueta="Promedio por cita" valor={formatearQuetzales(datos.promedioPorCita)} />
        <Indicador etiqueta="Descuento de la fundación" valor={formatearQuetzales(totales.descuento)} />
        <Indicador etiqueta="Neto cobrado al familiar" valor={formatearQuetzales(totales.neto)} />
      </dl>

      <section className="mt-8 break-inside-avoid">
        <h3 className="text-h3 font-semibold">Por tipo de servicio</h3>
        <div className="relative mt-2 overflow-x-auto rounded-tarjeta border border-divisor print:overflow-visible">
          <table className="w-full min-w-lg border-collapse text-left print:min-w-0">
            <caption className="sr-only">El costo total por tipo de servicio, con cuántos hubo y su parte del total</caption>
            <thead>
              <tr className="border-b border-divisor bg-fondo">
                <th scope="col" className={ENCABEZADO}>
                  Servicio
                </th>
                <th scope="col" className={`${ENCABEZADO} text-right`}>
                  Cantidad
                </th>
                <th scope="col" className={`${ENCABEZADO} text-right`}>
                  Costo
                </th>
                <th scope="col" className={`${ENCABEZADO} text-right`}>
                  Parte del total
                </th>
              </tr>
            </thead>
            <tbody>
              {porTipo.map(([servicio, cantidad, costo]) => (
                <tr key={servicio} className="border-b border-divisor last:border-b-0">
                  <th scope="row" className="px-4 py-3 text-left font-medium">
                    {servicio}
                  </th>
                  <td className={MONTO}>{cantidad}</td>
                  <td className={MONTO}>{formatearQuetzales(costo)}</td>
                  <td className={MONTO}>{porcentaje(costo, totales.total)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <div className="mt-8">
        {datos.pacientes.map((paciente) => (
          <TablaPaciente key={paciente.paciente.id} paciente={paciente} />
        ))}
      </div>
    </>
  );
}

/**
 * HU-24 (YS-152). El reporte de costos por paciente y cita (RF-17) del
 * Administrador del Asilo: lo que costó cada cita, desglosado en consulta,
 * exámenes y medicamentos, con los totales, el promedio por cita y la parte de
 * cada tipo de servicio. Se filtra por paciente y por rango de fechas, y se
 * exporta a PDF con la impresión del navegador.
 *
 * Por tipo de servicio y nunca por nombre (RNF-03): cuántos exámenes y cuántos
 * medicamentos, sin decir cuáles.
 */
export function ReporteCostosPage() {
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
    obtenerReporteCostos({ internoId: filtros.interno?.id, desde: filtros.desde, hasta: filtros.hasta })
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
    ? `Reporte de costos por cita ${datos.desde} a ${datos.hasta}${datos.paciente ? ` - ${datos.paciente.nombreCompleto}` : ""}`
    : "Reporte de costos por cita";

  return (
    <AppLayout migas={["Inicio", "Reportes", "Costos por cita"]}>
      <div className="print:hidden">
        <h1 className="text-h1 font-bold">Reporte de costos por cita</h1>
        <p className="mt-2 text-texto-secundario">
          Lo que costó cada cita, por paciente, desglosado en consulta, exámenes y medicamentos. Sin nombres de exámenes
          ni de medicamentos.
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
          <FormField etiqueta="Desde" obligatorio error={errores.desde}>
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
              titulo="Costos por cita"
              filtros={[
                ["Paciente", datos.paciente ? datos.paciente.nombreCompleto : "Todos"],
                ["Período", periodo],
              ]}
              generado={generado}
            >
              {datos.cantidadCitas === 0 ? (
                <p className="py-8 text-center text-texto-secundario">No hubo citas con costos en esas fechas.</p>
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
