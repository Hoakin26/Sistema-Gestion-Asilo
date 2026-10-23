import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { CalendarCheck, CalendarClock } from "lucide-react";

import { Alert } from "../components/ui/Alert";
import { StatusBadge } from "../components/ui/StatusBadge";
import { TablaSkeleton } from "../components/ui/TablaSkeleton";
import { Tabs } from "../components/ui/Tabs";
import { AppLayout } from "../layouts/AppLayout";
import { listarAcompanamientos } from "../services/acompanamientoService";
import { mensajeDeError } from "../services/api";
import { formatearCita } from "../utils/fechas";

/* Lo que todavía va a pasar: por asignar, con cita o en atención. */
const PROXIMOS = new Set(["PENDIENTE_ASIGNACION", "CITA_PROGRAMADA", "EN_ATENCION"]);

/* La cita más cercana primero; las que esperan asignación, al final. */
function porCitaMasCercana(a, b) {
  if (!a.cita || !b.cita) return (a.cita ? 0 : 1) - (b.cita ? 0 : 1);
  return a.cita.fechaHora.localeCompare(b.cita.fechaHora);
}

/* La más reciente primero, por la cita o, sin ella, por la solicitud. */
function porMasReciente(a, b) {
  return (b.cita?.fechaHora ?? b.fechaCreacion).localeCompare(a.cita?.fechaHora ?? a.fechaCreacion);
}

/** Un acompañamiento: a quién, a qué especialidad, cuándo y por qué. */
function Tarjeta({ acompanamiento }) {
  const { interno, especialidad, numero, estado, cita, motivo } = acompanamiento;
  return (
    <li className="rounded-tarjeta border border-divisor bg-superficie p-4 shadow-tarjeta sm:p-6">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div className="min-w-0">
          <Link
            to={`/internos/${interno.id}`}
            className="inline-flex min-h-11 items-center font-semibold text-primario hover:underline"
          >
            {interno.nombreCompleto}
          </Link>
          <p className="text-auxiliar text-texto-secundario tabular-nums slashed-zero">
            {especialidad.nombre} · {numero}
          </p>
        </div>
        <StatusBadge estado={estado} />
      </div>
      {cita ? (
        <p className="mt-3 flex items-start gap-2 font-medium">
          <CalendarCheck aria-hidden="true" size={20} className="mt-0.5 shrink-0 text-primario" />
          <span>
            <span className="block tabular-nums slashed-zero first-letter:uppercase">
              {formatearCita(cita.fechaHora)}
            </span>
            <span className="block text-auxiliar font-normal text-texto-secundario">
              {cita.especialista.nombreCompleto} · {cita.duracionMinutos} minutos
            </span>
          </span>
        </p>
      ) : (
        <p className="mt-3 flex items-start gap-2 text-texto-secundario">
          <CalendarClock aria-hidden="true" size={20} className="mt-0.5 shrink-0" />
          La fundación todavía no asigna la cita.
        </p>
      )}
      <p className="mt-2 text-auxiliar text-texto-secundario">{motivo}</p>
    </li>
  );
}

function Lista({ acompanamientos, vacio }) {
  if (acompanamientos.length === 0) {
    return <p className="py-8 text-center text-texto-secundario">{vacio}</p>;
  }
  return (
    <ul className="grid gap-4 lg:grid-cols-2">
      {acompanamientos.map((acompanamiento) => (
        <Tarjeta key={acompanamiento.solicitudId} acompanamiento={acompanamiento} />
      ))}
    </ul>
  );
}

/**
 * YS-189. "Mis acompañamientos" de Enfermería: las solicitudes de referencia
 * en que el médico general la eligió para acompañar al interno (RF-03), con la
 * cita que asignó la fundación. Reemplaza a "Solicitudes asignadas" y "Citas"
 * del menú del diseño, que nunca se construyeron (decidido el 22/10).
 */
export function MisAcompanamientosPage() {
  const [acompanamientos, setAcompanamientos] = useState(null);
  const [error, setError] = useState(null);
  const [pestana, setPestana] = useState("proximos");

  useEffect(() => {
    let vigente = true;
    listarAcompanamientos()
      .then((lista) => {
        if (vigente) setAcompanamientos(lista);
      })
      .catch((fallo) => {
        if (!vigente) return;
        setAcompanamientos([]);
        setError(mensajeDeError(fallo, "No se pudieron cargar sus acompañamientos"));
      });
    return () => {
      vigente = false;
    };
  }, []);

  const proximos = (acompanamientos ?? []).filter((a) => PROXIMOS.has(a.estado)).sort(porCitaMasCercana);
  const anteriores = (acompanamientos ?? []).filter((a) => !PROXIMOS.has(a.estado)).sort(porMasReciente);

  return (
    <AppLayout migas={["Inicio", "Mis acompañamientos"]}>
      <h1 className="text-h1 font-bold">Mis acompañamientos</h1>
      <p className="mt-2 text-texto-secundario">Los internos que acompaña a sus citas en la fundación.</p>

      {error && (
        <div className="mt-6">
          <Alert>{error}</Alert>
        </div>
      )}

      <div className="mt-6">
        {acompanamientos === null ? (
          <TablaSkeleton etiqueta="Cargando sus acompañamientos…" />
        ) : (
          <Tabs
            etiqueta="Mis acompañamientos"
            activa={pestana}
            alCambiar={setPestana}
            pestanas={[
              {
                clave: "proximos",
                etiqueta: `Próximos (${proximos.length})`,
                contenido: <Lista acompanamientos={proximos} vacio="No tiene acompañamientos próximos." />,
              },
              {
                clave: "anteriores",
                etiqueta: `Anteriores (${anteriores.length})`,
                contenido: <Lista acompanamientos={anteriores} vacio="Todavía no tiene acompañamientos anteriores." />,
              },
            ]}
          />
        )}
      </div>
    </AppLayout>
  );
}
