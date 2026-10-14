import { useId } from "react";
import { HeartPulse } from "lucide-react";

import { formatearFechaHora } from "../../utils/fechas";
import { Alert } from "../ui/Alert";
import { StatusBadge } from "../ui/StatusBadge";
import { Barra } from "../ui/TablaSkeleton";
import { Timeline, TimelineItem } from "../ui/Timeline";
import { ListaMedicamentos } from "../visitas/RecetaVisita";

const TARJETA = "rounded-tarjeta border border-divisor bg-superficie p-4 shadow-tarjeta sm:p-6";

/** Un apartado de la consulta. Lo que falta se dice con palabras, no con un hueco. */
function Apartado({ etiqueta, vacio, children }) {
  // `false` también es vacío: es lo que deja un `condición && <span>` sin dato.
  const tieneValor = children != null && children !== "" && children !== false;
  return (
    <div>
      <dt className="text-auxiliar text-texto-secundario">{etiqueta}</dt>
      <dd className={`mt-1 ${tieneValor ? "text-texto" : "text-texto-secundario"}`}>
        {tieneValor ? children : vacio}
      </dd>
    </div>
  );
}

function Examenes({ examenes }) {
  return (
    <ul className="divide-y divide-divisor">
      {examenes.map((examen) => (
        <li key={examen.id} className="py-2 first:pt-0">
          <div className="flex flex-wrap items-center justify-between gap-x-4 gap-y-2">
            <span className="font-medium text-texto">{examen.examen.nombre}</span>
            <StatusBadge estado={examen.estado} />
          </div>
          {examen.indicaciones && <p className="mt-1 text-texto">{examen.indicaciones}</p>}
        </li>
      ))}
    </ul>
  );
}

/**
 * Una consulta con todo lo que salió de ella: el diagnóstico, la receta y los
 * exámenes, juntos. Es lo que hace "centralizado" al historial: el Médico
 * General no tiene que cruzar tres listas para saber qué se recetó y por qué.
 */
function Consulta({ visita, receta, examenes }) {
  const activa = visita.estado === "ACTIVA";
  return (
    <TimelineItem>
      <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
        <p className="font-semibold text-texto tabular-nums slashed-zero">
          {formatearFechaHora(visita.fechaInicio)}
        </p>
        {/* Solo se marca la que sigue abierta: que una consulta anterior esté
            finalizada es lo normal y no necesita insignia. */}
        {activa && <StatusBadge estado="VISITA_ACTIVA" />}
      </div>
      <p className="text-auxiliar text-texto-secundario tabular-nums slashed-zero">
        {visita.especialidad.nombre} · {visita.especialista.nombreCompleto} · {visita.numeroSolicitud}
      </p>

      <dl className="mt-2 flex flex-col gap-3">
        <Apartado etiqueta="Motivo">
          <span className="whitespace-pre-line">{visita.motivo}</span>
        </Apartado>
        <Apartado
          etiqueta="Diagnóstico"
          vacio={activa ? "La consulta sigue en curso." : "Sin diagnóstico registrado."}
        >
          {visita.diagnostico && <span className="whitespace-pre-line">{visita.diagnostico}</span>}
        </Apartado>
        {visita.observaciones && (
          <Apartado etiqueta="Observaciones">
            <span className="whitespace-pre-line">{visita.observaciones}</span>
          </Apartado>
        )}
        <Apartado etiqueta="Medicamentos recetados" vacio="Ninguno.">
          {receta && (
            <div className="flex flex-col gap-2">
              <div>
                <StatusBadge estado={receta.estado} />
              </div>
              <ListaMedicamentos medicamentos={receta.medicamentos} />
              {receta.indicacionesGenerales && (
                <p className="text-auxiliar text-texto-secundario">{receta.indicacionesGenerales}</p>
              )}
            </div>
          )}
        </Apartado>
        <Apartado etiqueta="Exámenes de laboratorio" vacio="Ninguno.">
          {examenes.length > 0 && <Examenes examenes={examenes} />}
        </Apartado>
      </dl>
    </TimelineItem>
  );
}

/**
 * HU-02 (YS-65). Las consultas del historial médico centralizado, de la más
 * reciente a la más antigua: el backend ya las trae en ese orden, y la receta
 * y los exámenes se agrupan en la consulta de la que salieron (`visitaId`).
 *
 * - `historial`: `null` mientras carga.
 */
export function ConsultasHistorial({ historial, error }) {
  const idTitulo = useId();

  let contenido;
  if (error) {
    contenido = (
      <div className="mt-4">
        <Alert>{error}</Alert>
      </div>
    );
  } else if (historial === null) {
    contenido = (
      <div role="status" className="mt-4 flex flex-col gap-3">
        <span className="sr-only">Cargando las consultas…</span>
        <Barra className="w-48" />
        <Barra className="w-full" />
        <Barra className="w-64" />
      </div>
    );
  } else if (historial.visitas.length === 0) {
    contenido = (
      <p className="mt-4 text-texto-secundario">
        Todavía no tiene consultas con especialista registradas.
      </p>
    );
  } else {
    const recetaDe = new Map(historial.recetas.map((receta) => [receta.visitaId, receta]));
    const examenesDe = new Map();
    for (const examen of historial.examenes) {
      examenesDe.set(examen.visitaId, [...(examenesDe.get(examen.visitaId) ?? []), examen]);
    }
    const total = historial.visitas.length;

    contenido = (
      <>
        <p className="mt-2 text-auxiliar text-texto-secundario tabular-nums">
          {total === 1 ? "1 consulta" : `${total} consultas`}, de la más reciente a la más antigua
        </p>
        <div className="mt-6">
          <Timeline etiqueta="Consultas, de la más reciente a la más antigua">
            {historial.visitas.map((visita) => (
              <Consulta
                key={visita.id}
                visita={visita}
                receta={recetaDe.get(visita.id)}
                examenes={examenesDe.get(visita.id) ?? []}
              />
            ))}
          </Timeline>
        </div>
      </>
    );
  }

  return (
    <section aria-labelledby={idTitulo} className={TARJETA}>
      <div className="flex items-center gap-3">
        <HeartPulse aria-hidden="true" size={20} className="text-texto-secundario" />
        <h2 id={idTitulo} className="text-h3 font-semibold">
          Consultas
        </h2>
      </div>
      {contenido}
    </section>
  );
}
