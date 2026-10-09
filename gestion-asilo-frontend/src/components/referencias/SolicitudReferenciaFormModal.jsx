import { useEffect, useRef, useState } from "react";
import { CircleCheck, X } from "lucide-react";

import { useConfirmarDescarte } from "../../hooks/useConfirmarDescarte";
import { erroresPorCampo, mensajeDeError } from "../../services/api";
import { listarInternos } from "../../services/internoService";
import {
  crearSolicitud,
  listarEnfermeros,
  listarEspecialidades,
} from "../../services/referenciaService";
import { formatearDpi } from "../../utils/formato";
import { enfocarPrimerError } from "../../utils/formularios";
import { Alert } from "../ui/Alert";
import { Button } from "../ui/Button";
import { ConfirmDialog } from "../ui/ConfirmDialog";
import { FormField } from "../ui/FormField";
import { Select, Textarea } from "../ui/Input";
import { Modal } from "../ui/Modal";
import { SearchAutocomplete } from "../ui/SearchAutocomplete";
import { StatusBadge } from "../ui/StatusBadge";

/*
 * Fuera del componente para que sea siempre la misma función: el buscador la
 * usa como dependencia de su efecto, y una nueva en cada renderizado lo haría
 * buscar de nuevo sin que nadie haya escrito nada.
 *
 * Solo internos activos: el backend rechaza referir a uno dado de baja, y
 * ofrecerlo solo llevaría al usuario a un error.
 */
async function buscarInternos(termino) {
  const pagina = await listarInternos({ busqueda: termino });
  return pagina.content
    .filter((interno) => interno.activo)
    .slice(0, 8)
    .map((interno) => ({
      clave: interno.id,
      titulo: interno.nombreCompleto,
      subtitulo: `DPI ${formatearDpi(interno.dpi)}`,
      interno,
    }));
}

/* Más que el intervalo de un doble clic, que ronda el medio segundo. */
const ESPERA_ANTES_DE_CERRAR_MS = 700;

/** Mismas reglas y mensajes que el backend (YS-84). */
function validar(valores) {
  const errores = {};
  if (!valores.interno) errores.internoId = "Seleccione al interno que va a referir";
  if (!valores.especialidadId) errores.especialidadId = "Seleccione la especialidad";
  if (!valores.enfermeroAcompananteId) {
    errores.enfermeroAcompananteId = "Asigne el enfermero(a) acompañante";
  }
  const motivo = valores.motivo.trim();
  if (!motivo) errores.motivo = "El motivo de referencia es obligatorio";
  else if (motivo.length > 500) errores.motivo = "El motivo no puede exceder 500 caracteres";
  return errores;
}

function FormularioSolicitud({ internoInicial, alCambiar, alCerrar, alCrear }) {
  const refFormulario = useRef(null);
  const [valores, setValores] = useState({
    interno: internoInicial ?? null,
    especialidadId: "",
    enfermeroAcompananteId: "",
    motivo: "",
  });
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState(null);
  const [guardando, setGuardando] = useState(false);

  // Los catálogos de los dos selectores. `null` mientras cargan.
  const [especialidades, setEspecialidades] = useState(null);
  const [enfermeros, setEnfermeros] = useState(null);
  const [errorCatalogos, setErrorCatalogos] = useState(null);

  useEffect(() => {
    let vigente = true;
    Promise.all([listarEspecialidades(), listarEnfermeros()])
      .then(([listaEspecialidades, listaEnfermeros]) => {
        if (!vigente) return;
        setEspecialidades(listaEspecialidades);
        setEnfermeros(listaEnfermeros);
      })
      .catch((fallo) => {
        if (vigente) {
          setErrorCatalogos(
            mensajeDeError(fallo, "No se pudieron cargar las especialidades ni el personal de enfermería"),
          );
        }
      });
    return () => {
      vigente = false;
    };
  }, []);

  const cambiar = (campo, valor) => {
    alCambiar();
    setValores((previos) => ({ ...previos, [campo]: valor }));
    // El error del interno se guarda como "internoId", igual que lo nombra el backend.
    const claveError = campo === "interno" ? "internoId" : campo;
    setErrores((previos) => ({ ...previos, [claveError]: undefined }));
  };

  const fallar = (encontrados) => {
    setErrores(encontrados);
    enfocarPrimerError(refFormulario.current);
  };

  const enviar = async (evento) => {
    evento.preventDefault();

    const encontrados = validar(valores);
    if (Object.keys(encontrados).length > 0) {
      fallar(encontrados);
      return;
    }

    setGuardando(true);
    setErrorGeneral(null);

    try {
      const creada = await crearSolicitud({
        internoId: valores.interno.id,
        especialidadId: Number(valores.especialidadId),
        enfermeroAcompananteId: Number(valores.enfermeroAcompananteId),
        motivo: valores.motivo.trim(),
      });
      alCrear(creada);
    } catch (error) {
      const porCampo = erroresPorCampo(error);
      if (porCampo) {
        fallar(porCampo);
      } else {
        setErrorGeneral(mensajeDeError(error, "No se pudo crear la solicitud"));
      }
    } finally {
      setGuardando(false);
    }
  };

  const sinEnfermeros = enfermeros !== null && enfermeros.length === 0;

  return (
    <form ref={refFormulario} onSubmit={enviar} noValidate>
      <div className="flex flex-col gap-6 px-4 py-6 sm:px-6">
        {errorGeneral && <Alert>{errorGeneral}</Alert>}
        {errorCatalogos && <Alert>{errorCatalogos}</Alert>}

        <FormField
          etiqueta="Interno"
          obligatorio
          error={errores.internoId}
          ayuda={valores.interno ? undefined : "Busque por nombre o por DPI."}
        >
          {(campo) =>
            valores.interno ? (
              /* Elegido: se muestra como dato, con la opción de cambiarlo. Un
                 campo de texto con el nombre adentro invitaría a editarlo. */
              <div
                id={campo.id}
                className="flex min-h-11 items-center justify-between gap-4 rounded-campo border border-texto-secundario bg-fondo px-4 py-2"
              >
                <div>
                  <p className="font-medium text-texto">{valores.interno.nombreCompleto}</p>
                  {valores.interno.dpi && (
                    <p className="text-auxiliar text-texto-secundario tabular-nums slashed-zero">
                      DPI {formatearDpi(valores.interno.dpi)}
                    </p>
                  )}
                </div>
                {/* El id del campo va en el contenedor y no aquí: si la
                    etiqueta "Interno" apuntara al botón, el lector de
                    pantalla lo anunciaría como "Interno" y no como lo que hace. */}
                <Button
                  type="button"
                  variante="texto"
                  aria-label={`Cambiar el interno elegido, ${valores.interno.nombreCompleto}`}
                  onClick={() => cambiar("interno", null)}
                >
                  <X aria-hidden="true" size={20} />
                  Cambiar
                </Button>
              </div>
            ) : (
              <SearchAutocomplete
                {...campo}
                buscar={buscarInternos}
                alElegir={(opcion) => cambiar("interno", opcion.interno)}
                placeholder="Escriba el nombre o el DPI…"
                etiquetaLista="Internos que coinciden"
              />
            )
          }
        </FormField>

        <FormField etiqueta="Especialidad" obligatorio error={errores.especialidadId}>
          {(campo) => (
            <Select
              {...campo}
              value={valores.especialidadId}
              onChange={(evento) => cambiar("especialidadId", evento.target.value)}
              disabled={especialidades === null}
            >
              <option value="">
                {especialidades === null ? "Cargando especialidades…" : "Seleccione una especialidad…"}
              </option>
              {especialidades?.map((especialidad) => (
                <option key={especialidad.id} value={especialidad.id}>
                  {especialidad.nombre}
                </option>
              ))}
            </Select>
          )}
        </FormField>

        <FormField
          etiqueta="Enfermero(a) acompañante"
          obligatorio
          error={errores.enfermeroAcompananteId}
          ayuda="Quien acompañará al interno a la cita con el especialista."
        >
          {(campo) => (
            <Select
              {...campo}
              value={valores.enfermeroAcompananteId}
              onChange={(evento) => cambiar("enfermeroAcompananteId", evento.target.value)}
              disabled={enfermeros === null || sinEnfermeros}
            >
              <option value="">
                {enfermeros === null
                  ? "Cargando personal de enfermería…"
                  : sinEnfermeros
                    ? "No hay personal de enfermería activo"
                    : "Seleccione al enfermero(a)…"}
              </option>
              {enfermeros?.map((enfermero) => (
                <option key={enfermero.id} value={enfermero.id}>
                  {enfermero.nombreCompleto}
                </option>
              ))}
            </Select>
          )}
        </FormField>

        {/* No es un error de lo que escribió: es algo que solo puede resolver
            otra persona, y se lo dice para que sepa a quién acudir. */}
        {sinEnfermeros && (
          <Alert tipo="info" rol="status">
            No hay ningún enfermero(a) con cuenta activa para acompañar al interno. Pídale al
            Administrador del Asilo que registre o reactive una cuenta de Enfermería.
          </Alert>
        )}

        <FormField etiqueta="Motivo de la referencia" obligatorio error={errores.motivo}>
          {(campo) => (
            <Textarea
              {...campo}
              rows={4}
              value={valores.motivo}
              onChange={(evento) => cambiar("motivo", evento.target.value)}
              maxLength={500}
              placeholder="Qué se observó y qué se espera de la consulta con el especialista"
            />
          )}
        </FormField>
      </div>

      <div className="sticky bottom-0 flex flex-wrap justify-end gap-3 border-t border-divisor bg-superficie px-4 py-4 sm:px-6">
        <Button type="button" variante="secundario" onClick={alCerrar}>
          Cancelar
        </Button>
        <Button type="submit" cargando={guardando}>
          Crear solicitud
        </Button>
      </div>
    </form>
  );
}

/**
 * Lo que se ve después de crearla. El número es lo que el médico necesita
 * para darle seguimiento (criterio de YS-85), así que se muestra en grande y
 * en una pantalla que no desaparece sola, a diferencia de un aviso flotante.
 */
function SolicitudCreada({ solicitud, alCerrar }) {
  const refNumero = useRef(null);
  const aparecio = useRef(0);

  /*
   * El foco va al número: el botón que lo tenía ("Crear solicitud") acaba de
   * desaparecer, y sin esto el lector de pantalla no anunciaría nada.
   */
  useEffect(() => {
    aparecio.current = performance.now();
    refNumero.current?.focus();
  }, []);

  /*
   * "Listo" ocupa el mismo lugar que "Crear solicitud". Con el servidor
   * respondiendo en milisegundos, el segundo clic de un doble clic caía sobre
   * él y cerraba el resumen antes de que nadie leyera el número, que es justo
   * lo que esta pantalla existe para mostrar. Se ignoran los clics del primer
   * instante.
   */
  const cerrar = () => {
    if (performance.now() - aparecio.current < ESPERA_ANTES_DE_CERRAR_MS) return;
    alCerrar();
  };

  return (
    <div>
      <div
        ref={refNumero}
        tabIndex={-1}
        className="flex flex-col items-center gap-4 px-4 py-8 text-center sm:px-6"
      >
        <CircleCheck aria-hidden="true" size={24} className="text-exito" />
        <div>
          <p className="text-texto-secundario">Número de solicitud</p>
          <p className="mt-1 text-h2 font-semibold text-texto tabular-nums slashed-zero">
            {solicitud.numero}
          </p>
        </div>
        <StatusBadge estado={solicitud.estado} />
      </div>

      <dl className="grid gap-4 border-t border-divisor px-4 py-6 sm:grid-cols-2 sm:px-6">
        <div>
          <dt className="text-auxiliar text-texto-secundario">Interno</dt>
          <dd className="mt-1 text-texto">{solicitud.interno.nombreCompleto}</dd>
        </div>
        <div>
          <dt className="text-auxiliar text-texto-secundario">Especialidad</dt>
          <dd className="mt-1 text-texto">{solicitud.especialidad.nombre}</dd>
        </div>
        <div className="sm:col-span-2">
          <dt className="text-auxiliar text-texto-secundario">Enfermero(a) acompañante</dt>
          <dd className="mt-1 text-texto">{solicitud.enfermeroAcompanante.nombreCompleto}</dd>
        </div>
      </dl>

      <p className="px-4 pb-6 text-texto-secundario sm:px-6">
        La fundación la recibirá en su bandeja para asignarle especialista, fecha y hora.
      </p>

      <div className="sticky bottom-0 flex justify-end border-t border-divisor bg-superficie px-4 py-4 sm:px-6">
        <Button type="button" onClick={cerrar}>
          Listo
        </Button>
      </div>
    </div>
  );
}

/**
 * YS-85. El Médico General refiere a un interno a una especialidad de la
 * fundación, con un enfermero(a) acompañante (RF-03).
 *
 * `internoInicial` llega cuando se abre desde el perfil del interno: ahí ya
 * se sabe a quién se refiere y pedir que lo busque otra vez sería un paso de
 * más.
 */
export function SolicitudReferenciaFormModal({
  abierto,
  internoInicial,
  claveFormulario,
  alCerrar,
  alCrear,
}) {
  const { marcarCambios, olvidarCambios, intentarCerrar, dialogo } =
    useConfirmarDescarte(alCerrar);

  /*
   * La solicitud creada se guarda junto a la apertura en que se creó. Así, al
   * abrir la ventana otra vez, el resultado anterior deja de aplicar solo, sin
   * un efecto que lo limpie.
   */
  const [creada, setCreada] = useState(null);
  const resultado = creada?.apertura === claveFormulario ? creada.solicitud : null;

  return (
    <>
      <Modal
        abierto={abierto}
        alCerrar={resultado ? alCerrar : intentarCerrar}
        titulo={resultado ? "Solicitud creada" : "Nueva solicitud de referencia"}
        descripcion={
          resultado
            ? undefined
            : "Refiera a un interno a una especialidad de la fundación, con un enfermero(a) acompañante."
        }
      >
        {resultado ? (
          <SolicitudCreada solicitud={resultado} alCerrar={alCerrar} />
        ) : (
          <FormularioSolicitud
            key={claveFormulario}
            internoInicial={internoInicial}
            alCambiar={marcarCambios}
            alCerrar={intentarCerrar}
            alCrear={(solicitud) => {
              olvidarCambios();
              setCreada({ apertura: claveFormulario, solicitud });
              alCrear?.(solicitud);
            }}
          />
        )}
      </Modal>

      <ConfirmDialog {...dialogo} />
    </>
  );
}
