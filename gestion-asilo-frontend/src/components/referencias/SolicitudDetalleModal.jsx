import { useEffect, useState } from "react";
import { Mail, RotateCw } from "lucide-react";

import { useToast } from "../../hooks/useToast";
import { mensajeDeError } from "../../services/api";
import { listarAvisos, reintentarAvisos } from "../../services/referenciaService";
import { formatearFechaHora } from "../../utils/fechas";
import { Alert } from "../ui/Alert";
import { Button } from "../ui/Button";
import { Modal } from "../ui/Modal";
import { StatusBadge } from "../ui/StatusBadge";
import { Barra } from "../ui/TablaSkeleton";

function Dato({ etiqueta, children, ancho = false }) {
  return (
    <div className={ancho ? "sm:col-span-2" : undefined}>
      <dt className="text-auxiliar text-texto-secundario">{etiqueta}</dt>
      <dd className="mt-1 text-texto">{children}</dd>
    </div>
  );
}

/* Lo que cada estado del aviso le dice a quien da seguimiento. */
const EXPLICACION_AVISO = {
  PENDIENTE: "El aviso está por salir.",
  ENVIADA: "El familiar ya recibió el aviso por correo.",
  FALLIDA:
    "El servidor de correo no lo aceptó. El sistema lo vuelve a intentar solo cada pocos minutos; también puede reintentarlo ahora.",
  SIN_CORREO:
    "El familiar no tiene correo registrado. Si se lo agregan en los datos del interno, reintente el aviso.",
};

/**
 * YS-95. En qué quedó el aviso por correo al familiar, y el botón para
 * reintentarlo. Se monta al abrir el detalle: no hay que pedir el estado de
 * cada fila de la bandeja, solo el de la que se está viendo.
 */
function AvisoFamiliar({ solicitudId }) {
  const { mostrar } = useToast();
  const [avisos, setAvisos] = useState(null);
  const [error, setError] = useState(null);
  const [reintentando, setReintentando] = useState(false);

  useEffect(() => {
    let vigente = true;
    listarAvisos(solicitudId)
      .then((lista) => {
        if (vigente) setAvisos(lista);
      })
      .catch((fallo) => {
        if (vigente) setError(mensajeDeError(fallo, "No se pudo consultar el aviso al familiar"));
      });
    return () => {
      vigente = false;
    };
  }, [solicitudId]);

  const reintentar = async () => {
    setReintentando(true);
    try {
      const lista = await reintentarAvisos(solicitudId);
      setAvisos(lista);
      const estado = lista[0]?.estado;
      if (estado === "ENVIADA") {
        mostrar("Se envió el aviso al familiar.");
      } else if (estado === "SIN_CORREO") {
        mostrar("El familiar sigue sin correo registrado.", "error");
      } else {
        mostrar("Todavía no se pudo enviar. El sistema lo volverá a intentar.", "error");
      }
    } catch (fallo) {
      mostrar(mensajeDeError(fallo, "No se pudo reintentar el aviso"), "error");
    } finally {
      setReintentando(false);
    }
  };

  // Hoy cada solicitud tiene un solo aviso, el de su creación. HU-06 agrega
  // el de la cita asignada: por eso se recorre una lista.
  const aviso = avisos?.[0];

  return (
    <section aria-labelledby="titulo-aviso" className="border-t border-divisor px-4 py-6 sm:px-6">
      <h3 id="titulo-aviso" className="flex items-center gap-2 font-semibold text-texto">
        <Mail aria-hidden="true" size={20} className="text-texto-secundario" />
        Aviso por correo al familiar
      </h3>

      {error ? (
        <div className="mt-4">
          <Alert>{error}</Alert>
        </div>
      ) : avisos === null ? (
        <div role="status" className="mt-4 flex flex-col gap-2">
          <span className="sr-only">Consultando el aviso…</span>
          <Barra className="w-40" />
          <Barra className="w-64" />
        </div>
      ) : !aviso ? (
        <p className="mt-4 text-texto-secundario">Esta solicitud no tiene avisos registrados.</p>
      ) : (
        <div className="mt-4 flex flex-col gap-3">
          <div className="flex flex-wrap items-center gap-3">
            <StatusBadge estado={`AVISO_${aviso.estado}`} />
            {aviso.fechaEnvio && (
              <span className="text-auxiliar text-texto-secundario tabular-nums slashed-zero">
                {formatearFechaHora(aviso.fechaEnvio)}
              </span>
            )}
            {aviso.intentos > 1 && (
              <span className="text-auxiliar text-texto-secundario tabular-nums">
                {aviso.intentos} intentos
              </span>
            )}
          </div>
          <p className="text-texto-secundario">{EXPLICACION_AVISO[aviso.estado]}</p>

          {(aviso.estado === "FALLIDA" || aviso.estado === "SIN_CORREO") && (
            <div>
              <Button variante="secundario" onClick={reintentar} cargando={reintentando}>
                {!reintentando && <RotateCw aria-hidden="true" size={20} />}
                Reintentar aviso
              </Button>
            </div>
          )}
        </div>
      )}
    </section>
  );
}

/**
 * YS-88. Una solicitud de referencia completa, al seleccionarla en la bandeja
 * (criterio de HU-05: "permitir seleccionar una solicitud"). La comparten la
 * Fundación y el Médico General.
 *
 * Con HU-06, aquí mismo la Fundación le asigna la cita.
 */
export function SolicitudDetalleModal({ abierto, solicitud, claveApertura, alCerrar }) {
  return (
    <Modal
      abierto={abierto}
      alCerrar={alCerrar}
      titulo={solicitud ? `Solicitud ${solicitud.numero}` : "Solicitud"}
      descripcion={solicitud ? `Creada el ${formatearFechaHora(solicitud.fechaCreacion)}` : undefined}
    >
      {solicitud && (
        <>
          <div className="px-4 py-6 sm:px-6">
            <StatusBadge estado={solicitud.estado} />

            <dl className="mt-6 grid gap-4 sm:grid-cols-2">
              <Dato etiqueta="Interno">{solicitud.interno.nombreCompleto}</Dato>
              <Dato etiqueta="Especialidad">{solicitud.especialidad.nombre}</Dato>
              <Dato etiqueta="Motivo de la referencia" ancho>
                <span className="whitespace-pre-line">{solicitud.motivo}</span>
              </Dato>
              <Dato etiqueta="Enfermero(a) acompañante">
                {solicitud.enfermeroAcompanante.nombreCompleto}
              </Dato>
              <Dato etiqueta="Médico que refiere">{solicitud.medicoGeneral.nombreCompleto}</Dato>
            </dl>
          </div>

          {/* key por apertura: cada vez que se abre el detalle, el aviso se
              vuelve a pedir, aunque sea la misma solicitud de antes. Pudo
              haberse enviado mientras tanto. */}
          <AvisoFamiliar key={claveApertura} solicitudId={solicitud.id} />

          <div className="sticky bottom-0 flex justify-end border-t border-divisor bg-superficie px-4 py-4 sm:px-6">
            <Button type="button" variante="secundario" onClick={alCerrar}>
              Cerrar
            </Button>
          </div>
        </>
      )}
    </Modal>
  );
}
