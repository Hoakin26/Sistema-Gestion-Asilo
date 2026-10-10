import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, HeartPulse } from "lucide-react";

import { FichaMedicaDetalle } from "../components/internos/FichaMedicaDetalle";
import { StatusBadge } from "../components/ui/StatusBadge";
import { Barra } from "../components/ui/TablaSkeleton";
import { Antecedentes } from "../components/visitas/Antecedentes";
import { AppLayout } from "../layouts/AppLayout";
import { mensajeDeError } from "../services/api";
import { obtenerFichaMedica } from "../services/internoService";
import { obtenerHistorial, obtenerVisita } from "../services/visitaService";
import { edadEnAnios, formatearCita, formatearFechaHora } from "../utils/fechas";

const TARJETA = "rounded-tarjeta border border-divisor bg-superficie p-4 shadow-tarjeta sm:p-6";

function Dato({ etiqueta, children, ancho = false }) {
  return (
    <div className={ancho ? "sm:col-span-2" : undefined}>
      <dt className="text-auxiliar text-texto-secundario">{etiqueta}</dt>
      <dd className="mt-1 text-texto">{children}</dd>
    </div>
  );
}

function VisitaSkeleton() {
  return (
    <div role="status" aria-live="polite" className="flex flex-col gap-6">
      <span className="sr-only">Cargando la visita…</span>
      <div aria-hidden="true" className="flex flex-col gap-3">
        <Barra className="h-8 w-72" />
        <Barra className="w-56" />
      </div>
      <div aria-hidden="true" className="grid gap-6 lg:grid-cols-2">
        {[0, 1].map((indice) => (
          <div key={indice} className={`${TARJETA} flex flex-col gap-3`}>
            <Barra className="w-40" />
            <Barra className="w-full" />
            <Barra className="w-64" />
          </div>
        ))}
      </div>
    </div>
  );
}

function TarjetaCargando({ etiqueta }) {
  return (
    <div role="status" className={`${TARJETA} flex flex-col gap-3`}>
      <span className="sr-only">{etiqueta}</span>
      <Barra className="w-40" />
      <Barra className="w-full" />
      <Barra className="w-64" />
    </div>
  );
}

/**
 * HU-07 (YS-100). La pantalla de una visita médica: a quién se atiende, por
 * qué lo refirieron (criterio de HU-07) y cuándo se inició. HU-09 a HU-11 le
 * agregan el diagnóstico, la receta y los exámenes.
 *
 * El motivo va primero y a lo ancho: es lo que el especialista necesita leer
 * antes de empezar la consulta. HU-08 (YS-103): debajo, la ficha médica y los
 * antecedentes lado a lado, para verlos sin salir de la consulta.
 */
export function VisitaMedicaPage() {
  const { id } = useParams();
  const [visita, setVisita] = useState(null);
  const [error, setError] = useState(null);
  const [ficha, setFicha] = useState(null);
  const [errorFicha, setErrorFicha] = useState(null);
  const [historial, setHistorial] = useState(null);
  const [errorHistorial, setErrorHistorial] = useState(null);

  useEffect(() => {
    let vigente = true;
    obtenerVisita(id)
      .then((encontrada) => {
        if (!vigente) return undefined;
        setVisita(encontrada);
        // La ficha y los antecedentes, en paralelo y cada uno con su propia
        // falla: si uno no carga, el resto de la visita se muestra igual. Los
        // antecedentes solo se piden con la visita activa (criterio de HU-08).
        return Promise.allSettled([
          obtenerFichaMedica(encontrada.paciente.id),
          encontrada.estado === "ACTIVA" ? obtenerHistorial(encontrada.id) : null,
        ]).then(([deFicha, deHistorial]) => {
          if (!vigente) return;
          if (deFicha.status === "fulfilled") setFicha(deFicha.value);
          else setErrorFicha(mensajeDeError(deFicha.reason, "No se pudo cargar la ficha médica"));
          if (deHistorial.status === "fulfilled") setHistorial(deHistorial.value);
          else setErrorHistorial(mensajeDeError(deHistorial.reason, "No se pudieron cargar los antecedentes"));
        });
      })
      .catch((fallo) => {
        if (!vigente) return;
        setError(
          fallo.response?.status === 404
            ? "No existe una visita con ese número. Puede que la dirección esté mal escrita."
            : mensajeDeError(fallo, "No se pudo cargar la visita"),
        );
      });
    return () => {
      vigente = false;
    };
  }, [id]);

  return (
    <AppLayout
      migas={["Inicio", "Visitas médicas", visita ? `Visita ${visita.numeroSolicitud}` : "Visita"]}
    >
      <Link
        to="/mi-agenda"
        className="inline-flex min-h-11 items-center gap-2 rounded-tarjeta font-medium text-primario hover:underline"
      >
        <ArrowLeft aria-hidden="true" size={20} />
        Volver a mi agenda
      </Link>

      <div className="mt-4">
        {error ? (
          <div className="flex flex-col items-center gap-4 rounded-tarjeta border border-divisor bg-superficie px-4 py-16 text-center shadow-tarjeta sm:px-6">
            <HeartPulse aria-hidden="true" size={24} className="text-texto-secundario" />
            <p className="max-w-md text-texto-secundario">{error}</p>
          </div>
        ) : !visita ? (
          <VisitaSkeleton />
        ) : (
          <>
            {/* El encabezado del paciente: quién es, de un vistazo. */}
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <h1 className="text-h1 font-bold">{visita.paciente.nombreCompleto}</h1>
                <p className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-2 text-texto-secundario">
                  <span className="tabular-nums">{edadEnAnios(visita.paciente.fechaNacimiento)} años</span>
                  <span className="tabular-nums slashed-zero">{visita.numeroSolicitud}</span>
                  <span>{visita.especialidad.nombre}</span>
                </p>
              </div>
              <StatusBadge estado={`VISITA_${visita.estado}`} />
            </div>

            <div className="mt-6 flex flex-col gap-6">
              <section aria-labelledby="titulo-motivo" className={TARJETA}>
                <h2 id="titulo-motivo" className="text-h3 font-semibold">
                  Motivo de la referencia
                </h2>
                <p className="mt-4 whitespace-pre-line text-texto">{visita.motivo}</p>
                <dl className="mt-6 grid gap-4 sm:grid-cols-2">
                  <Dato etiqueta="Médico que refiere">{visita.medicoGeneral.nombreCompleto}</Dato>
                  <Dato etiqueta="Enfermero(a) acompañante">
                    {visita.enfermeroAcompanante.nombreCompleto}
                  </Dato>
                </dl>
              </section>

              <div className="grid items-start gap-6 lg:grid-cols-2">
                {ficha || errorFicha ? (
                  <FichaMedicaDetalle ficha={ficha} error={errorFicha} />
                ) : (
                  <TarjetaCargando etiqueta="Cargando la ficha médica…" />
                )}
                <Antecedentes
                  activa={visita.estado === "ACTIVA"}
                  historial={historial}
                  error={errorHistorial}
                />
              </div>

              <section aria-labelledby="titulo-visita" className={TARJETA}>
                <h2 id="titulo-visita" className="text-h3 font-semibold">
                  Visita
                </h2>
                <dl className="mt-4 grid gap-4 sm:grid-cols-2">
                  <Dato etiqueta="Inicio de la visita">
                    <span className="tabular-nums slashed-zero">{formatearFechaHora(visita.fechaInicio)}</span>
                  </Dato>
                  <Dato etiqueta="Cita programada">
                    <span className="block tabular-nums first-letter:uppercase">
                      {formatearCita(visita.fechaCita)}
                    </span>
                  </Dato>
                  <Dato etiqueta="Especialista">{visita.especialista.nombreCompleto}</Dato>
                  <Dato etiqueta="Estado de la solicitud">
                    <StatusBadge estado={visita.estadoSolicitud} />
                  </Dato>
                </dl>
              </section>
            </div>
          </>
        )}
      </div>
    </AppLayout>
  );
}
