import { useEffect, useId, useRef, useState } from "react";
import { ArrowLeft, CalendarCheck, TriangleAlert } from "lucide-react";
import { Link } from "react-router-dom";

import { diaDeFecha, horaCorta, resumirHorario } from "../../constants/dias";
import { erroresPorCampo, mensajeDeError } from "../../services/api";
import {
  asignarCita,
  consultarDisponibilidad,
  listarEspecialistas,
  obtenerCita,
} from "../../services/especialistaService";
import { formatearCita, formatearHora, hoyISO } from "../../utils/fechas";
import { enfocarPrimerError } from "../../utils/formularios";
import { Alert } from "../ui/Alert";
import { Button } from "../ui/Button";
import { FormField } from "../ui/FormField";
import { Input, Select } from "../ui/Input";

/* "lunes" sigue igual en plural; "sábado" y "domingo", no. */
function enPlural(dia) {
  const nombre = dia.etiqueta.toLowerCase();
  return nombre.endsWith("s") ? nombre : `${nombre}s`;
}

/**
 * Las horas libres de un especialista en una fecha, como botones. Quien la usa
 * le cambia la key cuando cambia el especialista, la fecha, o cuando hay que
 * volver a pedirlas: así arranca siempre cargando, sin un efecto que la
 * reinicie.
 *
 * Son radios nativos con forma de botón, no botones sueltos: el grupo se
 * recorre con las flechas y el lector de pantalla anuncia "3 de 8".
 */
function HorasLibres({ especialistaId, fecha, valor, error, idError, alElegir }) {
  const [horas, setHoras] = useState(null);
  const [errorCarga, setErrorCarga] = useState(null);

  useEffect(() => {
    let vigente = true;
    consultarDisponibilidad(especialistaId, fecha)
      .then((lista) => {
        if (vigente) setHoras(lista);
      })
      .catch((fallo) => {
        if (vigente) setErrorCarga(mensajeDeError(fallo, "No se pudieron consultar las horas libres"));
      });
    return () => {
      vigente = false;
    };
  }, [especialistaId, fecha]);

  if (errorCarga) return <Alert>{errorCarga}</Alert>;

  if (horas === null) {
    return (
      <div role="status" className="grid grid-cols-3 gap-2 sm:grid-cols-4">
        <span className="sr-only">Consultando las horas libres…</span>
        {[0, 1, 2, 3].map((indice) => (
          <span
            key={indice}
            aria-hidden="true"
            className="block h-11 rounded-tarjeta bg-neutral-tenue animate-latido motion-reduce:animate-none"
          />
        ))}
      </div>
    );
  }

  // Pasa si el día ya terminó, si todo está tomado o si ya pasaron las horas
  // de hoy. En los tres casos la salida es la misma: otra fecha.
  if (horas.length === 0) {
    return <p className="text-texto-secundario">No quedan horas libres ese día. Pruebe con otra fecha.</p>;
  }

  return (
    <div className="grid grid-cols-3 gap-2 sm:grid-cols-4">
      {horas.map((hora) => (
        <label
          key={hora}
          className="flex min-h-11 cursor-pointer items-center justify-center rounded-tarjeta border border-texto-secundario bg-superficie px-2 font-medium text-texto tabular-nums select-none hover:bg-neutral-tenue has-checked:border-primario has-checked:bg-primario has-checked:text-white has-checked:hover:bg-primario-oscuro has-focus-visible:outline-2 has-focus-visible:outline-offset-2 has-focus-visible:outline-primario"
        >
          <input
            type="radio"
            name="hora-cita"
            value={hora}
            checked={valor === hora}
            onChange={() => alElegir(hora)}
            aria-invalid={Boolean(error)}
            aria-describedby={error ? idError : undefined}
            className="sr-only"
          />
          {formatearHora(hora)}
        </label>
      ))}
    </div>
  );
}

/**
 * HU-06 (YS-92). La Fundación elige especialista, fecha y una de las horas
 * libres, y confirma. Vive dentro del detalle de la solicitud, en el mismo
 * modal: se asigna mirando la solicitud, sin saltar a otra pantalla.
 *
 * - `alAsignar(cita)`: se asignó.
 * - `alEncontrarAsignada(cita)`: otra persona le asignó cita mientras esta
 *   elegía. No es un error de esta persona, y lo que necesita ver es esa cita.
 */
export function AsignarCita({ solicitud, alVolver, alAsignar, alEncontrarAsignada }) {
  const refFormulario = useRef(null);
  const refInicio = useRef(null);
  const idErrorHora = useId();

  const [especialistas, setEspecialistas] = useState(null);
  const [errorCatalogo, setErrorCatalogo] = useState(null);

  const [especialistaId, setEspecialistaId] = useState("");
  const [fecha, setFecha] = useState(hoyISO);
  const [hora, setHora] = useState("");
  // Sube para volver a pedir las horas, después de que alguien tomó una.
  const [recargas, setRecargas] = useState(0);

  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState(null);
  const [guardando, setGuardando] = useState(false);

  // El botón "Asignar cita" que tenía el foco acaba de desaparecer: sin esto,
  // el lector de pantalla no anunciaría que la vista cambió.
  useEffect(() => {
    refInicio.current?.focus();
  }, []);

  useEffect(() => {
    let vigente = true;
    listarEspecialistas({ especialidadId: solicitud.especialidad.id })
      .then((lista) => {
        if (!vigente) return;
        setEspecialistas(lista);
        // Con uno solo no hay nada que elegir.
        if (lista.length === 1) setEspecialistaId(String(lista[0].id));
      })
      .catch((fallo) => {
        if (vigente) setErrorCatalogo(mensajeDeError(fallo, "No se pudieron cargar los especialistas"));
      });
    return () => {
      vigente = false;
    };
  }, [solicitud.especialidad.id]);

  const especialista = especialistas?.find((e) => String(e.id) === especialistaId) ?? null;
  const fechaValida = Boolean(fecha) && fecha >= hoyISO();
  const dia = fechaValida ? diaDeFecha(fecha) : null;
  const atiendeEseDia = Boolean(especialista && dia && especialista.horarios.some((h) => h.dia === dia.valor));

  const elegirEspecialista = (evento) => {
    setEspecialistaId(evento.target.value);
    setHora("");
    setErrores({});
  };

  const elegirFecha = (evento) => {
    setFecha(evento.target.value);
    setHora("");
    setErrores((previos) => ({ ...previos, fecha: undefined, hora: undefined }));
  };

  const elegirHora = (valor) => {
    setHora(valor);
    setErrores((previos) => ({ ...previos, hora: undefined }));
  };

  const fallar = (encontrados) => {
    setErrores(encontrados);
    enfocarPrimerError(refFormulario.current);
  };

  const confirmar = async (evento) => {
    evento.preventDefault();

    const encontrados = {};
    if (!especialistaId) encontrados.especialistaId = "Seleccione al especialista";
    if (!fecha) encontrados.fecha = "Elija la fecha de la cita";
    else if (!fechaValida) encontrados.fecha = "La cita tiene que ser hoy o en una fecha posterior";
    else if (especialistaId && !hora) encontrados.hora = "Elija una de las horas libres";
    if (Object.keys(encontrados).length > 0) {
      fallar(encontrados);
      return;
    }

    setGuardando(true);
    setErrorGeneral(null);

    try {
      const cita = await asignarCita(solicitud.id, {
        especialistaId: Number(especialistaId),
        fechaHora: `${fecha}T${horaCorta(hora)}`,
      });
      alAsignar(cita);
    } catch (error) {
      const porCampo = erroresPorCampo(error);
      if (porCampo) {
        // El backend habla de "fechaHora"; en pantalla eso es la hora elegida.
        const { fechaHora, ...resto } = porCampo;
        if (fechaHora) {
          setHora("");
          setRecargas((valor) => valor + 1);
        }
        fallar(fechaHora ? { ...resto, hora: fechaHora } : resto);
      } else if (error.response?.status === 409) {
        /*
         * Dos causas posibles: alguien tomó esa hora del especialista, o
         * alguien le asignó cita a esta misma solicitud. Se distingue
         * preguntando, no leyendo el texto del mensaje: si la solicitud ya
         * tiene cita, es lo segundo.
         */
        const yaAsignada = await obtenerCita(solicitud.id).catch(() => null);
        if (yaAsignada) {
          alEncontrarAsignada(yaAsignada);
          return;
        }
        setHora("");
        setRecargas((valor) => valor + 1);
        fallar({ hora: mensajeDeError(error, "Esa hora ya no está libre. Elija otra.") });
      } else {
        setErrorGeneral(mensajeDeError(error, "No se pudo asignar la cita"));
      }
    } finally {
      setGuardando(false);
    }
  };

  const sinEspecialistas = especialistas !== null && especialistas.length === 0;

  return (
    <form ref={refFormulario} onSubmit={confirmar} noValidate>
      <div className="flex flex-col gap-6 px-4 py-6 sm:px-6">
        {/* Lo que se está asignando, a la vista mientras se elige. */}
        <div ref={refInicio} tabIndex={-1} className="rounded-tarjeta bg-fondo px-4 py-3">
          <p className="font-medium text-texto">{solicitud.interno.nombreCompleto}</p>
          <p className="text-auxiliar text-texto-secundario">
            {solicitud.especialidad.nombre} · acompaña {solicitud.enfermeroAcompanante.nombreCompleto}
          </p>
        </div>

        {errorGeneral && <Alert>{errorGeneral}</Alert>}
        {errorCatalogo && <Alert>{errorCatalogo}</Alert>}

        {sinEspecialistas && (
          <Alert tipo="info" rol="status">
            No hay especialistas de {solicitud.especialidad.nombre} activos. Regístrelo o actívelo en{" "}
            <Link to="/medicos-disponibilidad" className="underline">
              Médicos y disponibilidad
            </Link>.
          </Alert>
        )}

        <FormField
          etiqueta="Especialista"
          obligatorio
          error={errores.especialistaId}
          ayuda={
            especialista
              ? `Atiende ${resumirHorario(especialista.horarios)}. Citas de ${especialista.duracionCitaMinutos} minutos.`
              : undefined
          }
        >
          {(campo) => (
            <Select
              {...campo}
              value={especialistaId}
              onChange={elegirEspecialista}
              disabled={especialistas === null || sinEspecialistas}
            >
              <option value="">
                {especialistas === null
                  ? "Cargando especialistas…"
                  : sinEspecialistas
                    ? "No hay especialistas disponibles"
                    : "Seleccione al especialista…"}
              </option>
              {especialistas?.map((opcion) => (
                <option key={opcion.id} value={opcion.id}>
                  {opcion.medico.nombreCompleto}
                </option>
              ))}
            </Select>
          )}
        </FormField>

        <FormField etiqueta="Fecha" obligatorio error={errores.fecha}>
          {(campo) => (
            <Input
              {...campo}
              type="date"
              value={fecha}
              min={hoyISO()}
              onChange={elegirFecha}
              className="tabular-nums sm:w-56"
            />
          )}
        </FormField>

        <fieldset>
          <legend className="font-medium text-texto">
            Hora
            <span className="text-error" aria-hidden="true">
              {" *"}
            </span>
          </legend>

          <div className="mt-2">
            {!especialista ? (
              <p className="text-texto-secundario">Elija primero al especialista para ver sus horas libres.</p>
            ) : !fechaValida ? (
              <p className="text-texto-secundario">Elija una fecha de hoy en adelante.</p>
            ) : !atiendeEseDia ? (
              /* Se sabe sin preguntarle al servidor: el horario ya llegó con
                 el especialista. Y dice qué días sí, para no probar a ciegas. */
              <p className="text-texto-secundario">
                {especialista.medico.nombreCompleto} no atiende los {enPlural(dia)}. Atiende{" "}
                {resumirHorario(especialista.horarios)}.
              </p>
            ) : (
              <HorasLibres
                key={`${especialistaId}-${fecha}-${recargas}`}
                especialistaId={especialistaId}
                fecha={fecha}
                valor={hora}
                error={errores.hora}
                idError={idErrorHora}
                alElegir={elegirHora}
              />
            )}
          </div>

          {errores.hora && (
            <p
              id={idErrorHora}
              role="alert"
              className="entrada-error mt-2 flex items-start gap-2 text-auxiliar text-error"
            >
              <TriangleAlert aria-hidden="true" size={16} className="shrink-0" />
              {errores.hora}
            </p>
          )}
        </fieldset>

        {/* La confirmación en palabras antes del botón: lo que se va a
            asignar, tal como lo leerá después el familiar en el correo. */}
        {especialista && hora && fechaValida && (
          <div aria-live="polite" className="flex items-start gap-3 rounded-tarjeta bg-info-tenue px-4 py-3">
            <CalendarCheck aria-hidden="true" size={20} className="shrink-0 text-info" />
            <p className="text-texto">
              Cita con <span className="font-medium">{especialista.medico.nombreCompleto}</span> el{" "}
              {formatearCita(`${fecha}T${hora}`)}
            </p>
          </div>
        )}
      </div>

      <div className="sticky bottom-0 flex flex-wrap justify-end gap-3 border-t border-divisor bg-superficie px-4 py-4 sm:px-6">
        <Button type="button" variante="secundario" onClick={alVolver}>
          <ArrowLeft aria-hidden="true" size={20} />
          Volver
        </Button>
        <Button type="submit" cargando={guardando} disabled={sinEspecialistas}>
          Confirmar cita
        </Button>
      </div>
    </form>
  );
}
