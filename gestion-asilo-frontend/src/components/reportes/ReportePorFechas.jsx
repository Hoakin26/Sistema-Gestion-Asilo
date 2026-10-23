import { useEffect, useState } from "react";
import { FileDown, FileText } from "lucide-react";

import { AppLayout } from "../../layouts/AppLayout";
import { erroresPorCampo, mensajeDeError } from "../../services/api";
import { formatearFechaCorta, hoyISO, inicioDeMesISO } from "../../utils/fechas";
import { exportarPdf } from "../../utils/imprimir";
import { Alert } from "../ui/Alert";
import { Button } from "../ui/Button";
import { FormField } from "../ui/FormField";
import { Input } from "../ui/Input";
import { TablaSkeleton } from "../ui/TablaSkeleton";
import { PrintableReportLayout } from "./PrintableReportLayout";
import { VolverAReportes } from "./VolverAReportes";

/**
 * YS-161. La pantalla de un reporte que solo se filtra por fechas, como los
 * dos financieros de HU-27: el título, las fechas, "Generar reporte",
 * "Exportar a PDF" y el reporte sobre `PrintableReportLayout`. Del mismo
 * molde que el de costos (HU-24), sin el paciente.
 *
 * `obtener({ desde, hasta })` trae los datos; `children(datos)` dibuja el
 * contenido, y si `estaVacio(datos)`, en su lugar va `mensajeVacio`.
 * `etiquetaDesde` y `etiquetaPeriodo` dicen de qué son las fechas ("Pagos
 * desde", "Pagos realizados"). Se genera al entrar, del inicio del mes a hoy.
 */
export function ReportePorFechas({
  migas,
  titulo,
  descripcion,
  tituloImpreso,
  etiquetaDesde = "Desde",
  etiquetaPeriodo = "Período",
  nombreArchivo,
  obtener,
  estaVacio,
  mensajeVacio,
  children,
}) {
  const [desde, setDesde] = useState(inicioDeMesISO);
  const [hasta, setHasta] = useState(hoyISO);
  // Lo que de verdad se consulta: cambia al pulsar "Generar", no en cada cambio.
  const [filtros, setFiltros] = useState(() => ({ desde: inicioDeMesISO(), hasta: hoyISO() }));

  const [datos, setDatos] = useState(null);
  const [generado, setGenerado] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [errores, setErrores] = useState({});

  useEffect(() => {
    let vigente = true;
    obtener(filtros)
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
  }, [filtros, obtener]);

  const generar = (evento) => {
    evento.preventDefault();
    const encontrados = {};
    if (!desde) encontrados.desde = "Indique la fecha inicial";
    if (!hasta) encontrados.hasta = "Indique la fecha final";
    else if (desde && hasta < desde) encontrados.hasta = "La fecha final no puede ser anterior a la inicial";
    setErrores(encontrados);
    if (Object.keys(encontrados).length > 0) return;
    setCargando(true);
    setFiltros({ desde, hasta });
  };

  const periodo = datos ? `${formatearFechaCorta(datos.desde)} al ${formatearFechaCorta(datos.hasta)}` : "";
  const archivo = datos ? `${nombreArchivo} ${datos.desde} a ${datos.hasta}` : nombreArchivo;

  return (
    <AppLayout migas={migas}>
      <div className="print:hidden">
        <VolverAReportes />
        <h1 className="mt-4 text-h1 font-bold">{titulo}</h1>
        <p className="mt-2 text-texto-secundario">{descripcion}</p>

        <form onSubmit={generar} noValidate className="mt-6 flex flex-wrap items-start gap-4 sm:[&>button]:mt-8">
          <FormField etiqueta={etiquetaDesde} obligatorio error={errores.desde}>
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
            onClick={() => exportarPdf(archivo)}
          >
            <FileDown aria-hidden="true" size={20} />
            Exportar a PDF
          </Button>
        </form>
        <p className="mt-2 text-auxiliar text-texto-secundario">
          Para el PDF, elija «Guardar como PDF» al imprimir.
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
            <PrintableReportLayout titulo={tituloImpreso} filtros={[[etiquetaPeriodo, periodo]]} generado={generado}>
              {estaVacio(datos) ? (
                <p className="py-8 text-center text-texto-secundario">{mensajeVacio}</p>
              ) : (
                children(datos)
              )}
            </PrintableReportLayout>
          )
        )}
      </div>
    </AppLayout>
  );
}
