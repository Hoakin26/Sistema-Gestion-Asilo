import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, ClipboardList, UserRound } from "lucide-react";

import { ConsultasHistorial } from "../components/historial/ConsultasHistorial";
import { FichaMedicaDetalle } from "../components/internos/FichaMedicaDetalle";
import { BotonEnlace } from "../components/ui/Button";
import { StatusBadge } from "../components/ui/StatusBadge";
import { Barra } from "../components/ui/TablaSkeleton";
import { AppLayout } from "../layouts/AppLayout";
import { mensajeDeError } from "../services/api";
import { obtenerFichaMedica, obtenerHistorialMedico, obtenerInterno } from "../services/internoService";
import { edadEnAnios } from "../utils/fechas";
import { formatearDpi } from "../utils/formato";

const TARJETA = "rounded-tarjeta border border-divisor bg-superficie p-4 shadow-tarjeta sm:p-6";

function HistorialSkeleton() {
  return (
    <div role="status" aria-live="polite" className="flex flex-col gap-6">
      <span className="sr-only">Cargando el historial médico…</span>
      <div aria-hidden="true" className="flex flex-col gap-3">
        <Barra className="h-8 w-72" />
        <Barra className="w-56" />
      </div>
      {[0, 1].map((indice) => (
        <div key={indice} aria-hidden="true" className={`${TARJETA} flex flex-col gap-3`}>
          <Barra className="w-40" />
          <Barra className="w-full" />
          <Barra className="w-64" />
        </div>
      ))}
    </div>
  );
}

/**
 * HU-02 (YS-65). El historial médico centralizado de un interno (RF-02): sus
 * padecimientos y medicamentos habituales arriba, y debajo cada consulta con
 * su diagnóstico, lo recetado y lo ordenado, de la más reciente a la más
 * antigua.
 *
 * Solo el Médico General: el especialista ve los antecedentes dentro de su
 * visita activa (HU-08). Los padecimientos y medicamentos habituales salen de
 * la ficha médica, y las consultas del historial; se piden en paralelo y cada
 * uno falla por su cuenta, como en la visita.
 */
export function HistorialMedicoPage() {
  const { id } = useParams();
  const [interno, setInterno] = useState(null);
  const [error, setError] = useState(null);
  const [ficha, setFicha] = useState(null);
  const [errorFicha, setErrorFicha] = useState(null);
  const [historial, setHistorial] = useState(null);
  const [errorHistorial, setErrorHistorial] = useState(null);

  useEffect(() => {
    let vigente = true;
    Promise.allSettled([obtenerInterno(id), obtenerFichaMedica(id), obtenerHistorialMedico(id)]).then(
      ([expediente, deFicha, deHistorial]) => {
        if (!vigente) return;
        if (expediente.status === "fulfilled") setInterno(expediente.value);
        else {
          setError(
            expediente.reason.response?.status === 404
              ? "No existe un interno con ese número. Puede que la dirección esté mal escrita."
              : mensajeDeError(expediente.reason, "No se pudo cargar el historial médico"),
          );
        }
        if (deFicha.status === "fulfilled") setFicha(deFicha.value);
        else setErrorFicha(mensajeDeError(deFicha.reason, "No se pudo cargar la ficha médica"));
        if (deHistorial.status === "fulfilled") setHistorial(deHistorial.value);
        else setErrorHistorial(mensajeDeError(deHistorial.reason, "No se pudieron cargar las consultas"));
      },
    );
    return () => {
      vigente = false;
    };
  }, [id]);

  return (
    <AppLayout migas={["Inicio", "Historial médico", interno?.nombreCompleto ?? "Interno"]}>
      <Link
        to="/historial-medico"
        className="inline-flex min-h-11 items-center gap-2 rounded-tarjeta font-medium text-primario hover:underline"
      >
        <ArrowLeft aria-hidden="true" size={20} />
        Volver al historial médico
      </Link>

      <div className="mt-4">
        {error ? (
          <div className="flex flex-col items-center gap-4 rounded-tarjeta border border-divisor bg-superficie px-4 py-16 text-center shadow-tarjeta sm:px-6">
            <ClipboardList aria-hidden="true" size={24} className="text-texto-secundario" />
            <p className="max-w-md text-texto-secundario">{error}</p>
          </div>
        ) : !interno ? (
          <HistorialSkeleton />
        ) : (
          <>
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <p className="text-auxiliar font-semibold tracking-wide text-texto-secundario uppercase">
                  Historial médico
                </p>
                <h1 className="mt-1 text-h1 font-bold">{interno.nombreCompleto}</h1>
                <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-2 text-texto-secundario">
                  <span className="tabular-nums slashed-zero">DPI {formatearDpi(interno.dpi)}</span>
                  <span className="tabular-nums">{edadEnAnios(interno.fechaNacimiento)} años</span>
                  {/* Un interno que ya no está en el asilo conserva su historia. */}
                  {!interno.activo && <StatusBadge activo={false} />}
                </div>
              </div>
              <BotonEnlace to={`/internos/${interno.id}`} variante="secundario">
                <UserRound aria-hidden="true" size={20} />
                Ver perfil
              </BotonEnlace>
            </div>

            <div className="mt-6 flex flex-col gap-6">
              {ficha || errorFicha ? (
                <FichaMedicaDetalle
                  ficha={ficha}
                  error={errorFicha}
                  mensajeVacio="La ficha médica todavía está vacía. Se completa desde el perfil del interno."
                />
              ) : (
                <div role="status" className={`${TARJETA} flex flex-col gap-3`}>
                  <span className="sr-only">Cargando la ficha médica…</span>
                  <Barra className="w-40" />
                  <Barra className="w-full" />
                </div>
              )}
              <ConsultasHistorial historial={historial} error={errorHistorial} />
            </div>
          </>
        )}
      </div>
    </AppLayout>
  );
}
