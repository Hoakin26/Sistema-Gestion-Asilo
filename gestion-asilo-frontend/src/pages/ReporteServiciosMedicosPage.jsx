import { useEffect, useState } from "react";
import { FileDown, FileText } from "lucide-react";

import { BotonDescargaResultado } from "../components/laboratorio/ResultadoExamen";
import { Indicador } from "../components/reportes/Indicador";
import { PacienteMedico } from "../components/reportes/PacienteMedico";
import { PrintableReportLayout } from "../components/reportes/PrintableReportLayout";
import { Alert } from "../components/ui/Alert";
import { Button } from "../components/ui/Button";
import { FormField } from "../components/ui/FormField";
import { Input, Select } from "../components/ui/Input";
import { TablaSkeleton } from "../components/ui/TablaSkeleton";
import { useAuth } from "../hooks/useAuth";
import { AppLayout } from "../layouts/AppLayout";
import { erroresPorCampo, mensajeDeError } from "../services/api";
import { obtenerReporteExamenes, obtenerReporteMedicamentos } from "../services/reporteService";
import { formatearFechaCorta, hoyISO, inicioDeAnioISO } from "../utils/fechas";
import { exportarPdf } from "../utils/imprimir";

const ENCABEZADO = "px-4 py-3 text-auxiliar font-semibold text-texto-secundario uppercase";

/* Los dos reportes de HU-28: de dónde salen y cómo se nombran. */
const TIPOS = {
  examenes: {
    etiqueta: "Exámenes de laboratorio",
    obtener: obtenerReporteExamenes,
    periodo: "Resultados",
    archivo: "Reporte de exámenes",
    vacio: "No se le realizaron exámenes en esas fechas.",
  },
  medicamentos: {
    etiqueta: "Medicamentos entregados",
    obtener: obtenerReporteMedicamentos,
    periodo: "Entregas",
    archivo: "Reporte de medicamentos",
    vacio: "No se le entregaron medicamentos en esas fechas.",
  },
};

/* "2026-10-20T15:04:00" → "20 oct 2026": aquí basta el día. */
function fechaDe(fechaHora) {
  return formatearFechaCorta(fechaHora.slice(0, 10));
}

/* Quién lo pidió: "Ordenado el 20 oct 2026 · Dra. López (Cardiología)". */
function origen(verbo, fecha, item) {
  return `${verbo} el ${fechaDe(fecha)} · ${item.especialista.nombreCompleto} (${item.especialidad.nombre})`;
}

/* Cuántos distintos hay en `lista` según `clave`. */
function distintos(lista, clave) {
  return new Set(lista.map(clave)).size;
}

/** RF-21a. Cada examen realizado: la fecha del resultado, el examen y el resultado. */
function ReporteExamenes({ examenes }) {
  return (
    <>
      <h3 className="sr-only">Resumen</h3>
      <dl className="grid gap-4 sm:grid-cols-2 print:grid-cols-2">
        <Indicador etiqueta="Exámenes realizados" valor={examenes.length} />
        <Indicador etiqueta="Exámenes distintos" valor={distintos(examenes, (examen) => examen.examen)} />
      </dl>
      <div className="relative mt-8 overflow-x-auto rounded-tarjeta border border-divisor print:overflow-visible">
        <table className="w-full min-w-xl border-collapse text-left print:min-w-0">
          <caption className="sr-only">Cada examen realizado: la fecha del resultado, el examen y el resultado</caption>
          <thead>
            <tr className="border-b border-divisor bg-fondo">
              <th scope="col" className={ENCABEZADO}>
                Fecha
              </th>
              <th scope="col" className={ENCABEZADO}>
                Examen
              </th>
              <th scope="col" className={ENCABEZADO}>
                Resultado
              </th>
            </tr>
          </thead>
          <tbody>
            {examenes.map((examen) => (
              <tr key={examen.id} className="break-inside-avoid border-b border-divisor align-top last:border-b-0">
                <td className="px-4 py-3 whitespace-nowrap tabular-nums slashed-zero">
                  {fechaDe(examen.fechaResultado)}
                </td>
                <td className="min-w-48 px-4 py-3">
                  <span className="block font-medium">{examen.examen}</span>
                  <span className="block text-auxiliar text-texto-secundario">
                    {origen("Ordenado", examen.fechaOrden, examen)}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <p className="whitespace-pre-line">{examen.resultado}</p>
                  {examen.archivo && (
                    <>
                      <div className="mt-1 print:hidden">
                        <BotonDescargaResultado ordenId={examen.id} archivo={examen.archivo} />
                      </div>
                      {/* En el PDF no hay botón: queda dicho que tiene archivo y cómo se llama. */}
                      <p className="hidden text-auxiliar text-texto-secundario print:block">
                        Archivo adjunto: {examen.archivo.nombre}
                      </p>
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}

/** RF-21b. Cada medicamento entregado: la fecha de entrega, el medicamento, la dosis y la cantidad. */
function ReporteMedicamentos({ medicamentos }) {
  return (
    <>
      <h3 className="sr-only">Resumen</h3>
      <dl className="grid gap-4 sm:grid-cols-2 print:grid-cols-2">
        <Indicador etiqueta="Entregas" valor={medicamentos.length} />
        <Indicador
          etiqueta="Medicamentos distintos"
          valor={distintos(medicamentos, (medicamento) => medicamento.medicamento.toLowerCase())}
        />
      </dl>
      <div className="relative mt-8 overflow-x-auto rounded-tarjeta border border-divisor print:overflow-visible">
        <table className="w-full min-w-xl border-collapse text-left print:min-w-0">
          <caption className="sr-only">
            Cada medicamento entregado: la fecha de entrega, el medicamento, la dosis con la forma de aplicación y la
            cantidad
          </caption>
          <thead>
            <tr className="border-b border-divisor bg-fondo">
              <th scope="col" className={ENCABEZADO}>
                Entrega
              </th>
              <th scope="col" className={ENCABEZADO}>
                Medicamento
              </th>
              <th scope="col" className={ENCABEZADO}>
                Dosis
              </th>
              <th scope="col" className={`${ENCABEZADO} text-right`}>
                Cantidad
              </th>
            </tr>
          </thead>
          <tbody>
            {medicamentos.map((medicamento) => (
              <tr key={medicamento.id} className="break-inside-avoid border-b border-divisor align-top last:border-b-0">
                <td className="px-4 py-3 whitespace-nowrap tabular-nums slashed-zero">
                  {fechaDe(medicamento.fechaEntrega)}
                </td>
                <td className="min-w-48 px-4 py-3">
                  <span className="block font-medium">{medicamento.medicamento}</span>
                  <span className="block text-auxiliar text-texto-secundario">
                    {origen("Recetado", medicamento.fechaReceta, medicamento)}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <span className="block whitespace-nowrap">{medicamento.dosis}</span>
                  <span className="block text-auxiliar text-texto-secundario">{medicamento.formaAplicacion}</span>
                </td>
                <td className="px-4 py-3 text-right tabular-nums slashed-zero">{medicamento.cantidad}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}

/**
 * HU-28 (YS-164). Los reportes médicos específicos de un paciente: los
 * exámenes de laboratorio que se le realizaron (RF-21a) o los medicamentos que
 * se le entregaron (RF-21b) entre dos fechas. Se elige el paciente, el tipo de
 * reporte y el rango, y se exporta a PDF. Por defecto, del 1 de enero a hoy:
 * en un mes casi nunca hay exámenes.
 *
 * Los permisos del análisis médico (HU-25): el Médico General, de cualquier
 * paciente; el especialista, del de su visita activa.
 */
export function ReporteServiciosMedicosPage() {
  const { usuario } = useAuth();

  const [paciente, setPaciente] = useState(null);
  const [tipo, setTipo] = useState("examenes");
  const [desde, setDesde] = useState(inicioDeAnioISO);
  const [hasta, setHasta] = useState(hoyISO);
  // Cada "Generar" es un pedido nuevo, aunque sea el mismo: así se vuelve a pedir.
  const [pedido, setPedido] = useState(null);

  const [datos, setDatos] = useState(null);
  const [generado, setGenerado] = useState(null);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);
  const [errores, setErrores] = useState({});

  useEffect(() => {
    if (!pedido) return undefined;
    let vigente = true;
    TIPOS[pedido.tipo]
      .obtener(pedido)
      .then((respuesta) => {
        if (!vigente) return;
        // El tipo va con los datos: cambiar el selector no cambia el reporte ya generado.
        setDatos({ ...respuesta, tipo: pedido.tipo });
        setGenerado(new Date().toISOString());
        setError(null);
      })
      .catch((fallo) => {
        if (!vigente) return;
        setDatos(null);
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
  }, [pedido]);

  const generar = (evento) => {
    evento.preventDefault();
    const encontrados = {};
    if (!paciente) encontrados.paciente = "Elija el paciente del reporte";
    if (!desde) encontrados.desde = "Indique la fecha inicial";
    if (!hasta) encontrados.hasta = "Indique la fecha final";
    else if (desde && hasta < desde) encontrados.hasta = "La fecha final no puede ser anterior a la inicial";
    setErrores(encontrados);
    if (Object.keys(encontrados).length > 0) return;
    setCargando(true);
    setPedido((anterior) => ({ tipo, internoId: paciente.id, desde, hasta, vez: (anterior?.vez ?? 0) + 1 }));
  };

  const elegido = datos ? TIPOS[datos.tipo] : null;
  const lista = datos ? datos[datos.tipo] : [];
  const nombreArchivo = datos
    ? `${elegido.archivo} - ${datos.paciente.nombreCompleto} ${datos.desde} a ${datos.hasta}`
    : "Reporte médico";

  return (
    <AppLayout migas={["Inicio", "Reportes", "Exámenes y medicamentos"]}>
      <div className="print:hidden">
        <h1 className="text-h1 font-bold">Reporte de exámenes y medicamentos</h1>
        <p className="mt-2 text-texto-secundario">
          Los exámenes de laboratorio realizados o los medicamentos entregados a un paciente, con su fecha y su detalle.
        </p>

        <form onSubmit={generar} noValidate className="mt-6 flex flex-wrap items-start gap-4 sm:[&>button]:mt-8">
          <PacienteMedico
            paciente={paciente}
            alElegir={(nuevo) => {
              setPaciente(nuevo);
              setErrores((anteriores) => ({ ...anteriores, paciente: undefined, internoId: undefined }));
            }}
            error={errores.paciente ?? errores.internoId}
          />
          <FormField etiqueta="Reporte" obligatorio>
            {(campo) => (
              <Select {...campo} value={tipo} onChange={(evento) => setTipo(evento.target.value)} className="sm:w-64">
                {Object.entries(TIPOS).map(([clave, { etiqueta }]) => (
                  <option key={clave} value={clave}>
                    {etiqueta}
                  </option>
                ))}
              </Select>
            )}
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
          «Exportar a PDF» abre la impresión del navegador: elija «Guardar como PDF» como destino. El reporte tiene
          datos clínicos: guárdelo y compártalo solo con personal médico.
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
              titulo={elegido.etiqueta}
              filtros={[
                ["Paciente", datos.paciente.nombreCompleto],
                [elegido.periodo, `${formatearFechaCorta(datos.desde)} al ${formatearFechaCorta(datos.hasta)}`],
                ["Generado por", usuario?.nombreCompleto ?? ""],
              ]}
              generado={generado}
            >
              {lista.length === 0 ? (
                <p className="py-8 text-center text-texto-secundario">{elegido.vacio}</p>
              ) : datos.tipo === "examenes" ? (
                <ReporteExamenes examenes={lista} />
              ) : (
                <ReporteMedicamentos medicamentos={lista} />
              )}
            </PrintableReportLayout>
          )
        )}
      </div>
    </AppLayout>
  );
}
