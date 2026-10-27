import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowRight, CalendarCheck, Filter, HeartPulse } from "lucide-react";

import { Alert } from "../components/ui/Alert";
import { Button, IconButton } from "../components/ui/Button";
import { Select } from "../components/ui/Input";
import { Paginacion } from "../components/ui/Paginacion";
import { StatusBadge } from "../components/ui/StatusBadge";
import { TablaSkeleton } from "../components/ui/TablaSkeleton";
import { AppLayout } from "../layouts/AppLayout";
import { mensajeDeError } from "../services/api";
import { listarVisitas } from "../services/visitaService";
import { formatearFechaHora } from "../utils/fechas";

const ENCABEZADO =
  "px-4 py-4 text-auxiliar font-semibold text-texto-secundario uppercase xl:px-6";

/* Lo que se muestra si la lista no se pudo cargar: una página vacía. */
const SIN_VISITAS = { content: [], totalElements: 0, totalPages: 0 };

/**
 * HU-07 (YS-100). "Visitas médicas" del especialista: las que inició, en curso
 * y finalizadas, la más reciente primero. Es por donde se vuelve a una visita
 * en curso para terminarla (HU-09).
 *
 * OBS-01. Por páginas de 20: crecen con cada cita que atiende.
 */
export function VisitasMedicasPage() {
  const navegar = useNavigate();
  const [estado, setEstado] = useState("");
  const [pagina, setPagina] = useState(0);
  const [resultado, setResultado] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let vigente = true;
    listarVisitas(estado, pagina)
      .then((datos) => {
        if (!vigente) return;
        setResultado(datos);
        setError(null);
      })
      .catch((fallo) => {
        if (!vigente) return;
        setResultado(SIN_VISITAS);
        setError(mensajeDeError(fallo, "No se pudieron cargar las visitas"));
      });
    return () => {
      vigente = false;
    };
  }, [estado, pagina]);

  const filtrar = (evento) => {
    setResultado(null);
    setPagina(0);
    setEstado(evento.target.value);
  };

  const visitas = resultado?.content ?? null;
  const total = resultado?.totalElements ?? 0;

  return (
    <AppLayout migas={["Inicio", "Visitas médicas"]}>
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-h1 font-bold">Visitas médicas</h1>
          <p className="mt-2 text-texto-secundario">
            Las visitas que ha iniciado. Se inician desde su agenda del día.
          </p>
        </div>

        <div className="relative w-full sm:w-64">
          <Filter
            aria-hidden="true"
            size={20}
            className="pointer-events-none absolute top-1/2 left-4 -translate-y-1/2 text-texto-secundario"
          />
          <Select value={estado} onChange={filtrar} aria-label="Filtrar visitas por estado" className="pl-12">
            <option value="">Todas las visitas</option>
            <option value="ACTIVA">En curso</option>
            <option value="FINALIZADA">Finalizadas</option>
          </Select>
        </div>
      </div>

      {error && (
        <div className="mt-6">
          <Alert>{error}</Alert>
        </div>
      )}

      <div className="mt-6">
        {visitas === null ? (
          <TablaSkeleton etiqueta="Cargando visitas…" />
        ) : visitas.length === 0 ? (
          !error && (
            <div className="flex flex-col items-center gap-4 rounded-tarjeta border border-divisor bg-superficie px-4 py-16 text-center shadow-tarjeta sm:px-6">
              <HeartPulse aria-hidden="true" size={24} className="text-texto-secundario" />
              <p className="max-w-md text-texto-secundario">
                {estado
                  ? "No tiene visitas en ese estado."
                  : "Todavía no ha iniciado visitas. Se inician desde su agenda."}
              </p>
              <Button variante="secundario" onClick={() => navegar("/mi-agenda")}>
                <CalendarCheck aria-hidden="true" size={20} />
                Ir a mi agenda
              </Button>
            </div>
          )
        ) : (
          <>
            <p aria-live="polite" className="pb-3 text-auxiliar text-texto-secundario tabular-nums">
              {total} {total === 1 ? "visita" : "visitas"}
            </p>
            {/* relative: los sr-only de la tabla son absolutos; sin él escapan del
                recorte, estiran la página y el teléfono la achica a una esquina (YS-175). */}
            <div className="relative overflow-x-auto rounded-tarjeta border border-divisor bg-superficie shadow-tarjeta">
              <table className="w-full min-w-xl border-collapse text-left">
                <caption className="sr-only">Visitas médicas con su paciente, inicio y estado</caption>
                <thead>
                  <tr className="border-b border-divisor bg-fondo">
                    <th scope="col" className={ENCABEZADO}>
                      Paciente
                    </th>
                    <th scope="col" className={ENCABEZADO}>
                      Inicio
                    </th>
                    <th scope="col" className={ENCABEZADO}>
                      Estado
                    </th>
                    <th scope="col" className={ENCABEZADO}>
                      <span className="sr-only">Acciones</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {visitas.map((visita) => (
                    <tr key={visita.id} className="border-b border-divisor last:border-b-0">
                      <td className="px-4 py-4 xl:px-6">
                        <span className="block font-medium text-texto">{visita.paciente.nombreCompleto}</span>
                        <span className="block text-auxiliar text-texto-secundario tabular-nums slashed-zero">
                          {visita.numeroSolicitud} · {visita.especialidad.nombre}
                        </span>
                      </td>
                      <td className="px-4 py-4 tabular-nums slashed-zero xl:px-6">
                        {formatearFechaHora(visita.fechaInicio)}
                      </td>
                      <td className="px-4 py-4 xl:px-6">
                        <StatusBadge estado={`VISITA_${visita.estado}`} />
                      </td>
                      <td className="px-4 py-4 xl:px-6">
                        <div className="flex justify-end">
                          <IconButton
                            etiqueta={`Abrir la visita de ${visita.paciente.nombreCompleto}`}
                            onClick={() => navegar(`/visitas/${visita.id}`)}
                          >
                            <ArrowRight aria-hidden="true" size={20} />
                          </IconButton>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <Paginacion
              pagina={pagina}
              totalPaginas={resultado.totalPages}
              alCambiar={setPagina}
              etiqueta="Páginas de la lista de visitas"
            />
          </>
        )}
      </div>
    </AppLayout>
  );
}
