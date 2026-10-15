import { useEffect, useId, useRef, useState } from "react";
import { CalendarCheck, CalendarPlus, Mail, RotateCw } from "lucide-react";

import { asignaCitas } from "../../constants/roles";
import { useAuth } from "../../hooks/useAuth";
import { useToast } from "../../hooks/useToast";
import { mensajeDeError } from "../../services/api";
import { obtenerCita } from "../../services/especialistaService";
import { listarAvisos, reintentarAvisos } from "../../services/referenciaService";
import { formatearCita, formatearFechaHora } from "../../utils/fechas";
import { Alert } from "../ui/Alert";
import { Button } from "../ui/Button";
import { Modal } from "../ui/Modal";
import { StatusBadge } from "../ui/StatusBadge";
import { Barra } from "../ui/TablaSkeleton";
import { AsignarCita } from "./AsignarCita";

function Dato({ etiqueta, children, ancho = false }) {
  return (
    <div className={ancho ? "sm:col-span-2" : undefined}>
      <dt className="text-auxiliar text-texto-secundario">{etiqueta}</dt>
      <dd className="mt-1 text-texto">{children}</dd>
    </div>
  );
}

/* HU-06. Cada solicitud tiene hasta dos avisos: el de su creación y el de su cita. */
const TITULO_AVISO = {
  SOLICITUD_CREADA: "Al crear la solicitud",
  CITA_ASIGNADA: "Al asignar la cita",
};

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
 * YS-95. En qué quedaron los avisos por correo al familiar, y el botón para
 * reintentarlos. Se monta al abrir el detalle: no hay que pedir el estado de
 * cada fila de la bandeja, solo el de la que se está viendo.
 */
function AvisoFamiliar({ solicitudId }) {
  const { mostrar } = useToast();
  const idTitulo = useId();
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
      const sinEnviar = lista.filter((aviso) => aviso.estado !== "ENVIADA");
      if (sinEnviar.length === 0) {
        mostrar(lista.length > 1 ? "Se enviaron los avisos al familiar." : "Se envió el aviso al familiar.");
      } else if (sinEnviar.some((aviso) => aviso.estado === "SIN_CORREO")) {
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

  const hayQueReintentar = avisos?.some(
    (aviso) => aviso.estado === "FALLIDA" || aviso.estado === "SIN_CORREO",
  );

  return (
    <section aria-labelledby={idTitulo} className="border-t border-divisor px-4 py-6 sm:px-6">
      <h3 id={idTitulo} className="flex items-center gap-2 font-semibold text-texto">
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
      ) : avisos.length === 0 ? (
        <p className="mt-4 text-texto-secundario">Esta solicitud no tiene avisos registrados.</p>
      ) : (
        <div className="mt-4 flex flex-col gap-4">
          <ul className="flex flex-col gap-4">
            {avisos.map((aviso) => (
              <li key={aviso.id} className="flex flex-col gap-2">
                {/* Con uno solo, el título de la sección ya lo dice todo. */}
                {avisos.length > 1 && (
                  <p className="font-medium text-texto">{TITULO_AVISO[aviso.tipo] ?? "Aviso"}</p>
                )}
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
              </li>
            ))}
          </ul>

          {hayQueReintentar && (
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
 * YS-92. La cita de la solicitud: con quién, qué día y a qué hora. Si la cita
 * se acaba de asignar llega en `citaConocida` y no se vuelve a pedir; si no,
 * se consulta, salvo que la solicitud siga pendiente y no pueda tenerla.
 */
function CitaDeLaSolicitud({ solicitudId, pendiente, citaConocida, puedeAsignar, refSeccion }) {
  const idTitulo = useId();
  const [cita, setCita] = useState(citaConocida);
  const [sinCita, setSinCita] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (pendiente || citaConocida) return undefined;
    let vigente = true;
    obtenerCita(solicitudId)
      .then((encontrada) => {
        if (vigente) setCita(encontrada);
      })
      .catch((fallo) => {
        if (!vigente) return;
        if (fallo.response?.status === 404) setSinCita(true);
        else setError(mensajeDeError(fallo, "No se pudo consultar la cita"));
      });
    return () => {
      vigente = false;
    };
  }, [solicitudId, pendiente, citaConocida]);

  return (
    <section
      ref={refSeccion}
      tabIndex={-1}
      aria-labelledby={idTitulo}
      className="border-t border-divisor px-4 py-6 sm:px-6"
    >
      <h3 id={idTitulo} className="flex items-center gap-2 font-semibold text-texto">
        <CalendarCheck aria-hidden="true" size={20} className="text-texto-secundario" />
        Cita con el especialista
      </h3>

      {pendiente ? (
        <p className="mt-4 text-texto-secundario">
          {puedeAsignar
            ? "Todavía no tiene especialista, fecha ni hora."
            : "La fundación todavía no le asigna especialista, fecha ni hora."}
        </p>
      ) : error ? (
        <div className="mt-4">
          <Alert>{error}</Alert>
        </div>
      ) : sinCita ? (
        <p className="mt-4 text-texto-secundario">Esta solicitud no tiene cita registrada.</p>
      ) : !cita ? (
        <div role="status" className="mt-4 flex flex-col gap-2">
          <span className="sr-only">Consultando la cita…</span>
          <Barra className="w-48" />
          <Barra className="w-64" />
        </div>
      ) : (
        <dl className="mt-4 grid gap-4 sm:grid-cols-2">
          <Dato etiqueta="Especialista">{cita.especialista.nombreCompleto}</Dato>
          <Dato etiqueta="Duración">{cita.duracionMinutos} minutos</Dato>
          <Dato etiqueta="Fecha y hora" ancho>
            <span className="block tabular-nums first-letter:uppercase">{formatearCita(cita.fechaHora)}</span>
          </Dato>
        </dl>
      )}
    </section>
  );
}

/**
 * Los datos de la solicitud, su cita y sus avisos. `foco` dice a dónde llevar
 * el foco al volver de la asignación: a la cita, si se acaba de asignar, o al
 * botón "Asignar cita", si se volvió sin asignar. El botón que lo tenía ya no
 * existe, y sin esto el foco se perdería en el documento.
 */
function DetalleSolicitud({ solicitud, estado, cita, claveApertura, puedeAsignar, foco, alAsignar, alCerrar }) {
  const refCita = useRef(null);
  const refAsignar = useRef(null);

  useEffect(() => {
    if (foco === "cita") refCita.current?.focus();
    if (foco === "asignar") refAsignar.current?.focus();
  }, [foco]);

  /*
   * Al asignar la cita cambia la key: las secciones vuelven a pedir sus datos,
   * y los avisos ahora incluyen el de la cita. Con prefijo distinto para cada
   * una: dos hermanos con la misma key confunden a React, que dejaba en
   * pantalla la cita de la solicitud anterior al abrir otra.
   */
  const claveSecciones = `${claveApertura}-${cita?.id ?? "sin-cita"}`;

  return (
    <>
      <div className="px-4 py-6 sm:px-6">
        <StatusBadge estado={estado} />

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

      <CitaDeLaSolicitud
        key={`cita-${claveSecciones}`}
        solicitudId={solicitud.id}
        pendiente={estado === "PENDIENTE_ASIGNACION"}
        citaConocida={cita}
        puedeAsignar={puedeAsignar}
        refSeccion={refCita}
      />

      {/* key por apertura: cada vez que se abre el detalle, el aviso se
          vuelve a pedir, aunque sea la misma solicitud de antes. Pudo
          haberse enviado mientras tanto. */}
      <AvisoFamiliar key={`aviso-${claveSecciones}`} solicitudId={solicitud.id} />

      <div className="sticky bottom-0 flex flex-wrap justify-end gap-3 border-t border-divisor bg-superficie px-4 py-4 sm:px-6">
        <Button type="button" variante="secundario" onClick={alCerrar}>
          Cerrar
        </Button>
        {puedeAsignar && (
          <Button ref={refAsignar} type="button" onClick={alAsignar}>
            <CalendarPlus aria-hidden="true" size={20} />
            Asignar cita
          </Button>
        )}
      </div>
    </>
  );
}

/**
 * YS-88. Una solicitud de referencia completa, al seleccionarla en la bandeja
 * (criterio de HU-05: "permitir seleccionar una solicitud"). La comparten la
 * Fundación y el Médico General.
 *
 * HU-06 (YS-92). Aquí mismo la Fundación le asigna la cita: el botón "Asignar
 * cita" cambia el contenido del modal a la asignación, y al confirmar vuelve
 * al detalle ya con la cita. `alAsignar` avisa a la bandeja para que se
 * actualice: la solicitud dejó de estar pendiente.
 */
export function SolicitudDetalleModal({ abierto, solicitud, claveApertura, alCerrar, alAsignar }) {
  const { usuario } = useAuth();
  const { mostrar } = useToast();

  /*
   * Lo que pasa dentro de una apertura —qué vista se ve y la cita recién
   * asignada— se guarda junto a esa apertura, como la solicitud recién
   * creada: al abrir otra, deja de aplicar solo, sin un efecto que lo limpie.
   */
  const [deLaApertura, setDeLaApertura] = useState({ apertura: null });
  const actual =
    deLaApertura.apertura === claveApertura ? deLaApertura : { vista: "detalle", cita: null, foco: null };
  const actualizar = (cambios) => setDeLaApertura({ ...actual, ...cambios, apertura: claveApertura });

  const cita = actual.cita;
  // La fila de la bandeja que se abrió todavía dice "pendiente" aunque la
  // cita se acabe de asignar: la cita conocida manda.
  const estado = cita ? "CITA_PROGRAMADA" : solicitud?.estado;
  const puedeAsignar = asignaCitas(usuario?.rol) && estado === "PENDIENTE_ASIGNACION";
  const asignando = actual.vista === "asignar" && puedeAsignar;

  /*
   * Aparte y con `?.`: React Compiler memoriza el manejador de abajo y, para
   * saber cuándo rehacerlo, lee sus dependencias al renderizar, no al hacer
   * clic. Con `solicitud.numero` adentro, el modal cerrado (sin solicitud)
   * tumbaba la bandeja entera.
   */
  const numero = solicitud?.numero;

  const asignada = (nueva) => {
    actualizar({ vista: "detalle", cita: nueva, foco: "cita" });
    // Sin punto final: la hora ya termina en "a. m." o "p. m.".
    mostrar(`Se asignó la cita de ${numero} para el ${formatearCita(nueva.fechaHora)}`);
    alAsignar?.();
  };

  const encontradaAsignada = (existente) => {
    actualizar({ vista: "detalle", cita: existente, foco: "cita" });
    mostrar("Otra persona le asignó cita a esta solicitud mientras usted elegía. Esta es la cita que quedó.", "error");
    alAsignar?.();
  };

  return (
    <Modal
      abierto={abierto}
      alCerrar={alCerrar}
      titulo={
        asignando ? "Asignar cita" : solicitud ? `Solicitud ${solicitud.numero}` : "Solicitud"
      }
      descripcion={
        !solicitud
          ? undefined
          : asignando
            ? `Solicitud ${solicitud.numero}: elija especialista, fecha y una hora libre.`
            : `Creada el ${formatearFechaHora(solicitud.fechaCreacion)}`
      }
    >
      {solicitud &&
        (asignando ? (
          <AsignarCita
            solicitud={solicitud}
            alVolver={() => actualizar({ vista: "detalle", foco: "asignar" })}
            alAsignar={asignada}
            alEncontrarAsignada={encontradaAsignada}
          />
        ) : (
          <DetalleSolicitud
            solicitud={solicitud}
            estado={estado}
            cita={cita}
            claveApertura={claveApertura}
            puedeAsignar={puedeAsignar}
            foco={actual.foco}
            alAsignar={() => actualizar({ vista: "asignar" })}
            alCerrar={alCerrar}
          />
        ))}
    </Modal>
  );
}
