import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowRight, CalendarCheck, ChevronLeft, ChevronRight, Stethoscope } from "lucide-react";

import { Alert } from "../components/ui/Alert";
import { Button, IconButton } from "../components/ui/Button";
import { ConfirmDialog } from "../components/ui/ConfirmDialog";
import { Input } from "../components/ui/Input";
import { StatusBadge } from "../components/ui/StatusBadge";
import { Barra } from "../components/ui/TablaSkeleton";
import { useToast } from "../hooks/useToast";
import { AppLayout } from "../layouts/AppLayout";
import { mensajeDeError } from "../services/api";
import { iniciarVisita, obtenerMiAgenda } from "../services/visitaService";
import { formatearDia, formatearFecha, formatearHora, hoyISO, sumarDias } from "../utils/fechas";

const TARJETA = "rounded-tarjeta border border-divisor bg-superficie p-4 shadow-tarjeta sm:p-6";

function AgendaSkeleton() {
  return (
    <div role="status" aria-live="polite" className="flex flex-col gap-4">
      <span className="sr-only">Cargando la agenda…</span>
      {[0, 1, 2].map((indice) => (
        <div key={indice} aria-hidden="true" className={`${TARJETA} flex flex-col gap-3`}>
          <Barra className="w-24" />
          <Barra className="w-64" />
          <Barra className="w-80" />
        </div>
      ))}
    </div>
  );
}

/**
 * Lo que se puede hacer con una cita, según en qué va. Una sola acción por
 * cita, y siempre en el mismo lugar de la tarjeta: en tableta, de pie entre
 * paciente y paciente, no hay que buscarla.
 */
function AccionCita({ cita, iniciando, alIniciar, alAbrir }) {
  if (cita.visitaId) {
    return (
      <Button variante="secundario" onClick={() => alAbrir(cita.visitaId)}>
        {cita.estado === "EN_ATENCION" ? "Continuar visita" : "Ver visita"}
        <ArrowRight aria-hidden="true" size={20} />
      </Button>
    );
  }
  if (cita.estado !== "CITA_PROGRAMADA") return null;

  const dia = cita.fechaHora.slice(0, 10);
  // Decidido el 09/10: desde el día de la cita, nunca antes. El backend lo
  // vuelve a comprobar; aquí solo se evita ofrecer un botón que fallaría.
  if (dia > hoyISO()) {
    return (
      <p className="text-auxiliar text-texto-secundario sm:max-w-48 sm:text-right">
        Se podrá iniciar el {formatearFecha(dia)}.
      </p>
    );
  }
  return (
    <Button onClick={() => alIniciar(cita)} cargando={iniciando}>
      {!iniciando && <Stethoscope aria-hidden="true" size={20} />}
      Iniciar visita
    </Button>
  );
}

/**
 * HU-07 (YS-100). "Mi agenda del día" del Médico Especialista: sus citas con
 * el paciente, la hora y el motivo de la referencia, y el botón que convierte
 * la cita en una visita médica activa.
 *
 * Tarjetas y no tabla: se usa en tableta durante la jornada, y el motivo, que
 * es lo que el especialista lee antes de entrar, necesita ancho para leerse.
 */
export function MiAgendaPage() {
  const navegar = useNavigate();
  const { mostrar } = useToast();

  const [fecha, setFecha] = useState(hoyISO);
  // Sube para volver a pedir la agenda, después de un intento fallido.
  const [recargas, setRecargas] = useState(0);
  const [citas, setCitas] = useState(null);
  const [error, setError] = useState(null);

  const [porIniciar, setPorIniciar] = useState(null);
  const [iniciando, setIniciando] = useState(null);

  useEffect(() => {
    let vigente = true;
    obtenerMiAgenda(fecha)
      .then((lista) => {
        if (!vigente) return;
        setCitas(lista);
        setError(null);
      })
      .catch((fallo) => {
        if (!vigente) return;
        setCitas([]);
        // 404: la cuenta no tiene perfil de especialista. No es una falla del
        // sistema sino algo que resuelve la Fundación, y el mensaje lo dice.
        setError({
          tipo: fallo.response?.status === 404 ? "info" : "error",
          mensaje: mensajeDeError(fallo, "No se pudo cargar la agenda"),
        });
      });
    return () => {
      vigente = false;
    };
  }, [fecha, recargas]);

  // La carga se enciende aquí, en el evento, y no en el efecto: así la lista
  // del día anterior no se queda en pantalla mientras llega la nueva.
  const irA = (nueva) => {
    if (!nueva || nueva === fecha) return;
    setCitas(null);
    setFecha(nueva);
  };

  /*
   * La cita llega por parámetro y no se lee de `porIniciar`: React Compiler
   * lee las propiedades que un manejador usa al renderizar, y con el diálogo
   * cerrado `porIniciar` es null (el mismo tropiezo de HU-06).
   */
  const iniciar = async (cita) => {
    setPorIniciar(null);
    setIniciando(cita.id);
    try {
      const visita = await iniciarVisita(cita.id);
      mostrar(`Se inició la visita de ${cita.interno.nombreCompleto}.`);
      navegar(`/visitas/${visita.id}`);
    } catch (fallo) {
      mostrar(mensajeDeError(fallo, "No se pudo iniciar la visita"), "error");
      // Pudo cambiar algo mientras tanto (otra pestaña, una cancelación): la
      // agenda se vuelve a pedir para mostrar en qué quedó.
      setRecargas((valor) => valor + 1);
    } finally {
      setIniciando(null);
    }
  };

  const hoy = hoyISO();

  return (
    <AppLayout migas={["Inicio", "Mi agenda del día"]}>
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-h1 font-bold">Mi agenda del día</h1>
          <p className="mt-2 text-texto-secundario first-letter:uppercase">{formatearDia(fecha)}</p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <IconButton etiqueta="Día anterior" onClick={() => irA(sumarDias(fecha, -1))}>
            <ChevronLeft aria-hidden="true" size={24} />
          </IconButton>
          <Input
            type="date"
            value={fecha}
            onChange={(evento) => irA(evento.target.value)}
            aria-label="Fecha de la agenda"
            className="w-auto tabular-nums"
          />
          <IconButton etiqueta="Día siguiente" onClick={() => irA(sumarDias(fecha, 1))}>
            <ChevronRight aria-hidden="true" size={24} />
          </IconButton>
          <Button variante="secundario" onClick={() => irA(hoy)} disabled={fecha === hoy}>
            Hoy
          </Button>
        </div>
      </div>

      {error && (
        <div className="mt-6">
          <Alert tipo={error.tipo} rol={error.tipo === "info" ? "status" : "alert"}>
            {error.mensaje}
          </Alert>
        </div>
      )}

      <div className="mt-6">
        {citas === null ? (
          <AgendaSkeleton />
        ) : citas.length === 0 ? (
          !error && (
            <div className="flex flex-col items-center gap-4 rounded-tarjeta border border-divisor bg-superficie px-4 py-16 text-center shadow-tarjeta sm:px-6">
              <CalendarCheck aria-hidden="true" size={24} className="text-texto-secundario" />
              <p className="text-texto-secundario">No tiene citas para este día.</p>
              {fecha !== hoy && (
                <Button variante="secundario" onClick={() => irA(hoy)}>
                  Ver las de hoy
                </Button>
              )}
            </div>
          )
        ) : (
          <>
            <p aria-live="polite" className="pb-3 text-auxiliar text-texto-secundario tabular-nums">
              {citas.length} {citas.length === 1 ? "cita" : "citas"}
            </p>
            <ol className="flex flex-col gap-4">
              {citas.map((cita) => (
                <li key={cita.id}>
                  <article
                    aria-labelledby={`cita-${cita.id}`}
                    className={`${TARJETA} flex flex-col gap-4 sm:flex-row sm:items-start`}
                  >
                    <div className="shrink-0 sm:w-28">
                      <p className="text-h3 font-semibold text-texto tabular-nums">
                        {formatearHora(cita.fechaHora.slice(11))}
                      </p>
                      <p className="text-auxiliar text-texto-secundario tabular-nums">
                        {cita.duracionMinutos} minutos
                      </p>
                    </div>

                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
                        <h2 id={`cita-${cita.id}`} className="text-h3 font-semibold text-texto">
                          {cita.interno.nombreCompleto}
                        </h2>
                        <StatusBadge estado={cita.estado} />
                      </div>
                      <p className="mt-1 text-auxiliar text-texto-secundario tabular-nums slashed-zero">
                        {cita.numeroSolicitud} · {cita.especialidad.nombre}
                      </p>

                      <p className="mt-4 text-auxiliar text-texto-secundario">Motivo de la referencia</p>
                      <p className="mt-1 whitespace-pre-line text-texto">{cita.motivo}</p>

                      <p className="mt-4 text-auxiliar text-texto-secundario">
                        Acompaña: {cita.enfermeroAcompanante.nombreCompleto}
                      </p>
                    </div>

                    <div className="shrink-0">
                      <AccionCita
                        cita={cita}
                        iniciando={iniciando === cita.id}
                        alIniciar={setPorIniciar}
                        alAbrir={(visitaId) => navegar(`/visitas/${visitaId}`)}
                      />
                    </div>
                  </article>
                </li>
              ))}
            </ol>
          </>
        )}
      </div>

      {/* Iniciar no se deshace: la solicitud pasa a En Atención y la fecha de
          la visita queda registrada. Merece una confirmación. */}
      <ConfirmDialog
        abierto={porIniciar !== null}
        titulo="¿Iniciar la visita?"
        mensaje={
          porIniciar
            ? `Se registrará ahora el inicio de la visita de ${porIniciar.interno.nombreCompleto}, y la solicitud ${porIniciar.numeroSolicitud} pasará a "En Atención".`
            : ""
        }
        textoConfirmar="Iniciar visita"
        textoCancelar="Cancelar"
        alConfirmar={() => iniciar(porIniciar)}
        alCancelar={() => setPorIniciar(null)}
      />
    </AppLayout>
  );
}
