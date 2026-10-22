import { useEffect, useState } from "react";
import { FileDown, FileText, X } from "lucide-react";

import { Indicador } from "../components/reportes/Indicador";
import { PrintableReportLayout } from "../components/reportes/PrintableReportLayout";
import { Alert } from "../components/ui/Alert";
import { Button } from "../components/ui/Button";
import { FormField } from "../components/ui/FormField";
import { Select } from "../components/ui/Input";
import { SearchAutocomplete } from "../components/ui/SearchAutocomplete";
import { StatusBadge } from "../components/ui/StatusBadge";
import { TablaSkeleton } from "../components/ui/TablaSkeleton";
import { useAuth } from "../hooks/useAuth";
import { AppLayout } from "../layouts/AppLayout";
import { mensajeDeError } from "../services/api";
import { listarInternos } from "../services/internoService";
import { obtenerReporteAnalisisMedico } from "../services/reporteService";
import { listarVisitas } from "../services/visitaService";
import { edadEnAnios, formatearFecha, formatearFechaHora } from "../utils/fechas";
import { formatearDpi } from "../utils/formato";
import { exportarPdf } from "../utils/imprimir";

const ENCABEZADO = "px-4 py-3 text-auxiliar font-semibold text-texto-secundario uppercase";

/* El Médico General busca entre todos los internos, también los que ya egresaron. */
async function buscarInternos(termino) {
  const pagina = await listarInternos({ busqueda: termino });
  return pagina.content.slice(0, 8).map((interno) => ({
    clave: interno.id,
    titulo: interno.nombreCompleto,
    subtitulo: `DPI ${formatearDpi(interno.dpi)}${interno.activo ? "" : " · Egresado"}`,
    interno,
  }));
}

/* "2026-10-20T15:04:00" → "20 de octubre de 2026": la receta y la entrega traen la hora, aquí basta el día. */
function fechaDe(fechaHora) {
  return formatearFecha(fechaHora.slice(0, 10));
}

/** Un dato de la ficha: la etiqueta y el valor, o "No registrado". */
function Dato({ etiqueta, children }) {
  return (
    <div>
      <dt className="text-auxiliar text-texto-secundario">{etiqueta}</dt>
      <dd className="mt-1 text-texto">{children || <span className="text-texto-secundario">No registrado</span>}</dd>
    </div>
  );
}

/** El reporte ya generado. */
function Reporte({ datos }) {
  const { paciente, ficha, historial } = datos;
  const diagnosticos = historial.visitas.filter((visita) => visita.diagnostico).length;
  const medicamentos = historial.recetas.flatMap((receta) =>
    receta.medicamentos.map((medicamento) => ({ ...medicamento, receta })),
  );

  return (
    <>
      <section className="break-inside-avoid">
        <h3 className="text-h3 font-semibold">Datos del paciente</h3>
        <dl className="mt-2 grid gap-4 sm:grid-cols-2 lg:grid-cols-4 print:grid-cols-4">
          <Dato etiqueta="Nombre">{paciente.nombreCompleto}</Dato>
          <Dato etiqueta="DPI">
            <span className="tabular-nums slashed-zero">{formatearDpi(paciente.dpi)}</span>
          </Dato>
          <Dato etiqueta="Edad">
            <span className="tabular-nums slashed-zero">
              {edadEnAnios(paciente.fechaNacimiento)} años · nació el {formatearFecha(paciente.fechaNacimiento)}
            </span>
          </Dato>
          <Dato etiqueta="Ingreso al asilo">
            <span className="tabular-nums slashed-zero">
              {formatearFecha(paciente.fechaIngreso)}
              {!paciente.activo && " · Egresado"}
            </span>
          </Dato>
        </dl>
      </section>

      <section className="mt-8 break-inside-avoid">
        <h3 className="text-h3 font-semibold">Ficha médica</h3>
        <dl className="mt-2 grid gap-4 sm:grid-cols-2 print:grid-cols-2">
          <div className="sm:col-span-2">
            <Dato etiqueta="Motivo de reclusión">{ficha.motivoIngreso}</Dato>
          </div>
          <Dato etiqueta="Tipo de sangre">{ficha.tipoSangre}</Dato>
          <Dato etiqueta="Alergias">{ficha.alergias}</Dato>
          <Dato etiqueta="Psicopatologías">
            {ficha.psicopatologias.length > 0 && (
              <ul className="flex flex-col gap-1">
                {ficha.psicopatologias.map((psicopatologia) => (
                  <li key={psicopatologia.id}>
                    <span className="font-medium">{psicopatologia.nombre}</span>
                    {psicopatologia.fechaDiagnostico && (
                      <span className="text-texto-secundario tabular-nums slashed-zero">
                        {" "}
                        · desde el {formatearFecha(psicopatologia.fechaDiagnostico)}
                      </span>
                    )}
                    {psicopatologia.observaciones && (
                      <span className="block text-auxiliar text-texto-secundario">{psicopatologia.observaciones}</span>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </Dato>
          <Dato etiqueta="Medicamentos habituales">
            {ficha.medicamentosHabituales.length > 0 && (
              <ul className="flex flex-col gap-1">
                {ficha.medicamentosHabituales.map((medicamento) => (
                  <li key={medicamento.id}>
                    <span className="font-medium">{medicamento.nombre}</span>
                    {[medicamento.dosis, medicamento.frecuencia].filter(Boolean).length > 0 && (
                      <span className="text-texto-secundario">
                        {" "}
                        · {[medicamento.dosis, medicamento.frecuencia].filter(Boolean).join(", ")}
                      </span>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </Dato>
        </dl>
      </section>

      <section className="mt-8 break-inside-avoid">
        <h3 className="sr-only">Resumen</h3>
        <dl className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4 print:grid-cols-4">
          <Indicador etiqueta="Visitas" valor={historial.visitas.length} />
          <Indicador etiqueta="Con diagnóstico" valor={diagnosticos} />
          <Indicador etiqueta="Medicamentos recetados" valor={medicamentos.length} />
          <Indicador etiqueta="Exámenes ordenados" valor={historial.examenes.length} />
        </dl>
      </section>

      <section className="mt-8">
        <h3 className="text-h3 font-semibold">Visitas y diagnósticos</h3>
        {historial.visitas.length === 0 ? (
          <p className="mt-2 text-texto-secundario">No tiene visitas registradas.</p>
        ) : (
          <ol className="mt-2 flex flex-col gap-4">
            {historial.visitas.map((visita) => {
              const examenes = historial.examenes.filter((examen) => examen.visitaId === visita.id);
              return (
                <li key={visita.id} className="break-inside-avoid rounded-tarjeta border border-divisor p-4">
                  <div className="flex flex-wrap items-start justify-between gap-2">
                    <div>
                      <p className="font-semibold tabular-nums slashed-zero">
                        {formatearFechaHora(visita.fechaInicio)} · {visita.especialidad.nombre}
                      </p>
                      <p className="text-auxiliar text-texto-secundario tabular-nums slashed-zero">
                        {visita.especialista.nombreCompleto} · Solicitud {visita.numeroSolicitud}
                      </p>
                    </div>
                    <StatusBadge estado={`VISITA_${visita.estado}`} />
                  </div>
                  <dl className="mt-3 grid gap-3 md:grid-cols-2 print:grid-cols-2">
                    <Dato etiqueta="Motivo">{visita.motivo}</Dato>
                    <Dato etiqueta="Diagnóstico">{visita.diagnostico}</Dato>
                    {visita.observaciones && (
                      <div className="md:col-span-2 print:col-span-2">
                        <Dato etiqueta="Observaciones">{visita.observaciones}</Dato>
                      </div>
                    )}
                    {examenes.length > 0 && (
                      <div className="md:col-span-2 print:col-span-2">
                        <Dato etiqueta="Exámenes">
                          <ul className="flex flex-col gap-1">
                            {examenes.map((examen) => (
                              <li key={examen.id}>
                                <span className="font-medium">{examen.examen.nombre}</span>
                                <span className="text-texto-secundario">
                                  {" "}
                                  · {examen.resultado ? `Resultado: ${examen.resultado}` : "Sin resultado todavía"}
                                </span>
                              </li>
                            ))}
                          </ul>
                        </Dato>
                      </div>
                    )}
                  </dl>
                </li>
              );
            })}
          </ol>
        )}
      </section>

      <section className="mt-8">
        <h3 className="text-h3 font-semibold">Medicamentos recetados</h3>
        {medicamentos.length === 0 ? (
          <p className="mt-2 text-texto-secundario">No tiene medicamentos recetados.</p>
        ) : (
          /* relative: los sr-only de la tabla son absolutos; sin él escapan del recorte (YS-175). */
          <div className="relative mt-2 overflow-x-auto rounded-tarjeta border border-divisor print:overflow-visible">
            <table className="w-full min-w-3xl border-collapse text-left print:min-w-0">
              <caption className="sr-only">
                Medicamentos recetados: fecha, medicamento, dosis, cantidad, forma de aplicación, quién lo recetó y si se
                entregó
              </caption>
              <thead>
                <tr className="border-b border-divisor bg-fondo">
                  <th scope="col" className={`${ENCABEZADO} w-px whitespace-nowrap`}>
                    Fecha
                  </th>
                  <th scope="col" className={ENCABEZADO}>
                    Medicamento
                  </th>
                  <th scope="col" className={`${ENCABEZADO} w-px text-right whitespace-nowrap`}>
                    Cantidad
                  </th>
                  <th scope="col" className={ENCABEZADO}>
                    Forma de aplicación
                  </th>
                  <th scope="col" className={ENCABEZADO}>
                    Recetó
                  </th>
                  <th scope="col" className={`${ENCABEZADO} w-px whitespace-nowrap`}>
                    Entregado
                  </th>
                </tr>
              </thead>
              <tbody>
                {medicamentos.map((medicamento) => (
                  <tr key={medicamento.id} className="break-inside-avoid border-b border-divisor align-top last:border-b-0">
                    <td className="px-4 py-3 whitespace-nowrap tabular-nums slashed-zero">
                      {fechaDe(medicamento.receta.fechaEmision)}
                    </td>
                    <td className="px-4 py-3">
                      <span className="font-medium">{medicamento.medicamento}</span>
                      <span className="block text-auxiliar text-texto-secundario">{medicamento.dosis}</span>
                    </td>
                    <td className="px-4 py-3 text-right tabular-nums slashed-zero">{medicamento.cantidad}</td>
                    <td className="px-4 py-3">{medicamento.formaAplicacion}</td>
                    <td className="px-4 py-3">
                      {medicamento.receta.especialista.nombreCompleto}
                      <span className="block text-auxiliar text-texto-secundario">
                        {medicamento.receta.especialidad.nombre}
                      </span>
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap tabular-nums slashed-zero">
                      {medicamento.entregado && medicamento.fechaEntrega ? fechaDe(medicamento.fechaEntrega) : "No"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  );
}

/**
 * HU-25 (YS-155). El reporte de análisis médico de un paciente (RF-18): sus
 * datos, su ficha médica con el motivo de reclusión, sus visitas con los
 * diagnósticos y los medicamentos recetados, exportable a PDF.
 *
 * Decidido el 21/10: el Médico General lo genera de cualquier paciente; el
 * especialista, solo del paciente de una visita suya en curso, como los
 * antecedentes de HU-08. Por eso el especialista elige de sus visitas activas
 * y no de todos los internos.
 */
export function ReporteAnalisisMedicoPage() {
  const { usuario } = useAuth();
  const esEspecialista = usuario?.rol === "MEDICO_ESPECIALISTA";

  const [interno, setInterno] = useState(null);
  // El especialista: los pacientes de sus visitas activas.
  const [enAtencion, setEnAtencion] = useState(null);
  // Cada "Generar" es un pedido nuevo, aunque sea del mismo paciente: así se vuelve a pedir.
  const [pedido, setPedido] = useState(null);

  const [datos, setDatos] = useState(null);
  const [generado, setGenerado] = useState(null);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);
  const [errorPaciente, setErrorPaciente] = useState(null);

  useEffect(() => {
    if (!esEspecialista) return undefined;
    let vigente = true;
    listarVisitas("ACTIVA")
      .then((visitas) => {
        if (!vigente) return;
        const unicos = new Map();
        visitas.forEach((visita) => unicos.set(visita.paciente.id, visita.paciente));
        setEnAtencion([...unicos.values()]);
      })
      .catch((fallo) => {
        if (!vigente) return;
        setEnAtencion([]);
        setError(mensajeDeError(fallo, "No se pudieron cargar sus visitas en curso"));
      });
    return () => {
      vigente = false;
    };
  }, [esEspecialista]);

  useEffect(() => {
    if (!pedido) return undefined;
    let vigente = true;
    obtenerReporteAnalisisMedico(pedido.internoId)
      .then((respuesta) => {
        if (!vigente) return;
        setDatos(respuesta);
        setGenerado(new Date().toISOString());
        setError(null);
      })
      .catch((fallo) => {
        if (!vigente) return;
        setDatos(null);
        setError(mensajeDeError(fallo, "No se pudo generar el reporte"));
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
    const elegido = esEspecialista ? Number(interno) || null : interno?.id;
    if (!elegido) {
      setErrorPaciente("Elija el paciente del reporte");
      return;
    }
    setErrorPaciente(null);
    setCargando(true);
    setPedido((anterior) => ({ internoId: elegido, vez: (anterior?.vez ?? 0) + 1 }));
  };

  const nombreArchivo = datos
    ? `Reporte de análisis médico - ${datos.paciente.nombreCompleto}`
    : "Reporte de análisis médico";

  return (
    <AppLayout migas={["Inicio", "Reportes", "Análisis médico"]}>
      <div className="print:hidden">
        <h1 className="text-h1 font-bold">Reporte de análisis médico</h1>
        <p className="mt-2 text-texto-secundario">
          La ficha médica de un paciente, con el motivo de reclusión, sus visitas con los diagnósticos y los medicamentos
          recetados.
        </p>

        {esEspecialista && enAtencion?.length === 0 && (
          <div className="mt-6">
            <Alert tipo="info" rol="status">
              No tiene visitas en curso. El reporte se genera del paciente que está atendiendo, mientras su visita siga
              activa.
            </Alert>
          </div>
        )}

        <form onSubmit={generar} noValidate className="mt-6 flex flex-wrap items-end gap-4">
          <FormField etiqueta="Paciente" obligatorio error={errorPaciente}>
            {(campo) =>
              esEspecialista ? (
                <Select
                  {...campo}
                  value={interno ?? ""}
                  onChange={(evento) => {
                    setInterno(evento.target.value);
                    setErrorPaciente(null);
                  }}
                  disabled={!enAtencion || enAtencion.length === 0}
                  className="sm:w-80"
                >
                  <option value="">{enAtencion === null ? "Cargando sus visitas…" : "Elija…"}</option>
                  {enAtencion?.map((paciente) => (
                    <option key={paciente.id} value={paciente.id}>
                      {paciente.nombreCompleto}
                    </option>
                  ))}
                </Select>
              ) : interno ? (
                <div
                  id={campo.id}
                  className="flex min-h-11 items-center justify-between gap-4 rounded-campo border border-texto-secundario bg-fondo px-4 py-2 sm:w-80"
                >
                  <p className="font-medium text-texto">{interno.nombreCompleto}</p>
                  <Button
                    type="button"
                    variante="texto"
                    aria-label={`Cambiar el paciente elegido, ${interno.nombreCompleto}`}
                    onClick={() => setInterno(null)}
                  >
                    <X aria-hidden="true" size={20} />
                    Cambiar
                  </Button>
                </div>
              ) : (
                <div className="sm:w-80">
                  <SearchAutocomplete
                    {...campo}
                    buscar={buscarInternos}
                    alElegir={(opcion) => {
                      setInterno(opcion.interno);
                      setErrorPaciente(null);
                    }}
                    placeholder="Escriba el nombre o el DPI…"
                    etiquetaLista="Internos que coinciden"
                  />
                </div>
              )
            }
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
              titulo="Análisis médico"
              filtros={[
                ["Paciente", datos.paciente.nombreCompleto],
                ["Generado por", `${usuario?.nombreCompleto ?? ""}`],
              ]}
              generado={generado}
            >
              <Reporte datos={datos} />
            </PrintableReportLayout>
          )
        )}
      </div>
    </AppLayout>
  );
}
