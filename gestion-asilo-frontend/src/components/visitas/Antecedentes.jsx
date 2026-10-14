import { useId } from "react";
import { History } from "lucide-react";

import { formatearFechaHora } from "../../utils/fechas";
import { Alert } from "../ui/Alert";
import { StatusBadge } from "../ui/StatusBadge";
import { Barra } from "../ui/TablaSkeleton";
import { Timeline, TimelineItem } from "../ui/Timeline";
import { ListaMedicamentos } from "./RecetaVisita";

const TARJETA = "rounded-tarjeta border border-divisor bg-superficie p-4 shadow-tarjeta sm:p-6";

function VisitaAnterior({ visita }) {
  return (
    <TimelineItem>
      <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
        <p className="font-semibold text-texto tabular-nums slashed-zero">
          {formatearFechaHora(visita.fechaInicio)}
        </p>
        {/* Solo se marca la que sigue abierta: que una visita anterior esté
            finalizada es lo normal y no necesita insignia. */}
        {visita.estado === "ACTIVA" && <StatusBadge estado="VISITA_ACTIVA" />}
      </div>
      <p className="text-auxiliar text-texto-secundario tabular-nums slashed-zero">
        {visita.especialidad.nombre} · {visita.especialista.nombreCompleto} · {visita.numeroSolicitud}
      </p>

      <dl className="mt-2 flex flex-col gap-2">
        <div>
          <dt className="text-auxiliar text-texto-secundario">Motivo</dt>
          <dd className="whitespace-pre-line text-texto">{visita.motivo}</dd>
        </div>
        <div>
          <dt className="text-auxiliar text-texto-secundario">Diagnóstico</dt>
          <dd className={`whitespace-pre-line ${visita.diagnostico ? "text-texto" : "text-texto-secundario"}`}>
            {visita.diagnostico ?? "Sin diagnóstico registrado."}
          </dd>
        </div>
        {visita.observaciones && (
          <div>
            <dt className="text-auxiliar text-texto-secundario">Observaciones</dt>
            <dd className="whitespace-pre-line text-texto">{visita.observaciones}</dd>
          </div>
        )}
      </dl>
    </TimelineItem>
  );
}

/** YS-109. Una receta de una visita anterior: cuándo, quién y qué recetó. */
function RecetaAnterior({ receta }) {
  return (
    <TimelineItem>
      <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
        <p className="font-semibold text-texto tabular-nums slashed-zero">
          {formatearFechaHora(receta.fechaEmision)}
        </p>
        <StatusBadge estado={receta.estado} />
      </div>
      <p className="text-auxiliar text-texto-secundario tabular-nums slashed-zero">
        {receta.especialidad.nombre} · {receta.especialista.nombreCompleto} · {receta.numeroSolicitud}
      </p>
      <ListaMedicamentos medicamentos={receta.medicamentos} />
      {receta.indicacionesGenerales && (
        <p className="text-auxiliar text-texto-secundario">{receta.indicacionesGenerales}</p>
      )}
    </TimelineItem>
  );
}

/**
 * HU-11 (YS-103). Un examen ordenado en una visita anterior. Uno por fila y no
 * agrupados por visita: cada examen avanza por su cuenta en el laboratorio
 * (DE-02), y su estado es lo que el especialista busca.
 */
function ExamenAnterior({ examen }) {
  return (
    <TimelineItem>
      <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
        <p className="font-semibold text-texto tabular-nums slashed-zero">
          {formatearFechaHora(examen.fechaOrden)}
        </p>
        <StatusBadge estado={examen.estado} />
      </div>
      <p className="text-auxiliar text-texto-secundario tabular-nums slashed-zero">
        {examen.especialidad.nombre} · {examen.especialista.nombreCompleto} · {examen.numeroSolicitud}
      </p>
      <p className="mt-2 font-medium text-texto">{examen.examen.nombre}</p>
      {examen.indicaciones && <p className="text-texto">{examen.indicaciones}</p>}
    </TimelineItem>
  );
}

/**
 * HU-08 (YS-103). Los antecedentes del paciente dentro de la visita: sus
 * visitas anteriores con el diagnóstico, y los medicamentos y exámenes que se
 * le ordenaron antes, del más reciente al más antiguo.
 *
 * Decidido el 09/10: HU-08 va antes que la receta (HU-10) y los exámenes
 * (HU-11). Sus apartados ya estaban aquí; HU-10 llenó el de los medicamentos
 * recetados y HU-11 el de los exámenes. El resultado de cada examen lo agrega
 * HU-13, cuando el laboratorio pueda cargarlo.
 *
 * - `activa`: los antecedentes solo se consultan durante la visita activa
 *   (criterio de HU-08). Con la visita finalizada no se piden.
 * - `historial`: `null` mientras carga.
 */
export function Antecedentes({ activa, historial, error }) {
  const idTitulo = useId();
  const recetas = historial?.recetas ?? [];
  const examenes = historial?.examenes ?? [];

  return (
    <section aria-labelledby={idTitulo} className={TARJETA}>
      <div className="flex items-center gap-3">
        <History aria-hidden="true" size={20} className="text-texto-secundario" />
        <h2 id={idTitulo} className="text-h3 font-semibold">
          Antecedentes
        </h2>
      </div>

      {!activa ? (
        <p className="mt-4 text-texto-secundario">
          Los antecedentes del paciente se consultan solo durante la visita activa.
        </p>
      ) : error ? (
        <div className="mt-4">
          <Alert>{error}</Alert>
        </div>
      ) : historial === null ? (
        <div role="status" className="mt-4 flex flex-col gap-3">
          <span className="sr-only">Cargando los antecedentes…</span>
          <Barra className="w-48" />
          <Barra className="w-full" />
          <Barra className="w-64" />
        </div>
      ) : (
        <div className="mt-6 flex flex-col gap-6">
          <div>
            <h3 className="font-semibold text-texto">Visitas anteriores</h3>
            {historial.visitas.length === 0 ? (
              <p className="mt-2 text-texto-secundario">
                Es la primera visita con especialista registrada de este paciente.
              </p>
            ) : (
              <div className="mt-4">
                <Timeline etiqueta="Visitas anteriores, de la más reciente a la más antigua">
                  {historial.visitas.map((visita) => (
                    <VisitaAnterior key={visita.id} visita={visita} />
                  ))}
                </Timeline>
              </div>
            )}
          </div>

          <div>
            <h3 className="font-semibold text-texto">Medicamentos recetados</h3>
            {recetas.length === 0 ? (
              <p className="mt-2 text-texto-secundario">
                No hay medicamentos recetados en visitas anteriores.
              </p>
            ) : (
              <div className="mt-4">
                <Timeline etiqueta="Recetas anteriores, de la más reciente a la más antigua">
                  {recetas.map((receta) => (
                    <RecetaAnterior key={receta.id} receta={receta} />
                  ))}
                </Timeline>
              </div>
            )}
          </div>

          <div>
            <h3 className="font-semibold text-texto">Exámenes de laboratorio</h3>
            {examenes.length === 0 ? (
              <p className="mt-2 text-texto-secundario">
                No hay exámenes de laboratorio anteriores.
              </p>
            ) : (
              <div className="mt-4">
                <Timeline etiqueta="Exámenes anteriores, del más reciente al más antiguo">
                  {examenes.map((examen) => (
                    <ExamenAnterior key={examen.id} examen={examen} />
                  ))}
                </Timeline>
              </div>
            )}
          </div>
        </div>
      )}
    </section>
  );
}
