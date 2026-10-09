import { useEffect, useId, useRef, useState } from "react";
import { TriangleAlert } from "lucide-react";

import { DIAS, horaCorta } from "../../constants/dias";
import { useConfirmarDescarte } from "../../hooks/useConfirmarDescarte";
import { erroresPorCampo, mensajeDeError } from "../../services/api";
import {
  actualizarEspecialista,
  listarUsuariosDisponibles,
  registrarEspecialista,
} from "../../services/especialistaService";
import { listarEspecialidades } from "../../services/referenciaService";
import { enfocarPrimerError } from "../../utils/formularios";
import { Alert } from "../ui/Alert";
import { Button } from "../ui/Button";
import { ConfirmDialog } from "../ui/ConfirmDialog";
import { FormField } from "../ui/FormField";
import { Input, Select } from "../ui/Input";
import { Modal } from "../ui/Modal";

/* Las duraciones habituales de una consulta. El backend acepta de 10 a 120. */
const DURACIONES = [15, 20, 30, 45, 60, 90, 120];

/* Con qué horas arranca un día recién marcado, si no hay otro que copiar. */
const HORARIO_POR_DEFECTO = { horaInicio: "08:00", horaFin: "12:00" };

function minutos(hora) {
  const [h, m] = hora.split(":").map(Number);
  return h * 60 + m;
}

/**
 * El horario como lo edita el formulario: los siete días, marcados o no. Un
 * día desmarcado conserva sus horas, por si lo vuelven a marcar; solo se
 * envían los marcados.
 */
function horarioInicial(horarios = []) {
  const porDia = new Map(horarios.map((h) => [h.dia, h]));
  return Object.fromEntries(
    DIAS.map(({ valor }) => {
      const horario = porDia.get(valor);
      return [
        valor,
        horario
          ? {
              atiende: true,
              horaInicio: horaCorta(horario.horaInicio),
              horaFin: horaCorta(horario.horaFin),
            }
          : { atiende: false, horaInicio: "", horaFin: "" },
      ];
    }),
  );
}

function diasMarcados(horario) {
  return DIAS.map(({ valor }) => valor).filter((dia) => horario[dia].atiende);
}

/** Mismas reglas y mensajes que el backend (YS-167). */
function validar(valores, esEdicion) {
  const errores = {};
  if (!esEdicion && !valores.usuarioId) errores.usuarioId = "Seleccione al médico especialista";
  if (!valores.especialidadId) errores.especialidadId = "Seleccione la especialidad";

  const marcados = diasMarcados(valores.horario);
  if (marcados.length === 0) errores.horarios = "Indique al menos un día de atención";

  const duracion = Number(valores.duracionCitaMinutos);
  for (const dia of marcados) {
    const { horaInicio, horaFin } = valores.horario[dia];
    if (!horaInicio || !horaFin) {
      errores[`horario.${dia}`] = "Indique la hora de inicio y la de fin";
    } else if (minutos(horaFin) <= minutos(horaInicio)) {
      errores[`horario.${dia}`] = "La hora de fin tiene que ser posterior a la de inicio";
    } else if (minutos(horaFin) - minutos(horaInicio) < duracion) {
      errores[`horario.${dia}`] = `El horario no alcanza para una cita de ${duracion} minutos`;
    }
  }
  return errores;
}

/**
 * El backend nombra los errores del horario por la posición en la lista que
 * recibió ("horarios[1].horaFin"), y la pantalla los muestra por día. `enviados`
 * es el orden en que se mandaron los días.
 */
function erroresDelBackend(porCampo, enviados) {
  return Object.fromEntries(
    Object.entries(porCampo).map(([campo, mensaje]) => {
      const fila = /^horarios\[(\d+)\]/.exec(campo);
      return [fila ? `horario.${enviados[Number(fila[1])]}` : campo, mensaje];
    }),
  );
}

/**
 * Un día de la semana: la casilla y, si atiende, sus dos horas. Las horas son
 * <input type="time">, que en tableta abre el selector del sistema en vez de
 * pedir que se escriba "08:00" a mano.
 */
function FilaDia({ dia, valor, error, invalidoGrupo, idErrorGrupo, alMarcar, alCambiarHora }) {
  const id = useId();
  const idError = `${id}-error`;
  const nombre = dia.etiqueta.toLowerCase();

  return (
    <div className="flex flex-col gap-2 border-b border-divisor py-2 last:border-b-0">
      <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
        <label className="flex min-h-11 w-32 cursor-pointer items-center gap-3 font-medium text-texto select-none">
          <input
            type="checkbox"
            checked={valor.atiende}
            onChange={(evento) => alMarcar(evento.target.checked)}
            aria-invalid={invalidoGrupo || undefined}
            aria-describedby={invalidoGrupo ? idErrorGrupo : undefined}
            className="size-5 cursor-pointer accent-primario"
          />
          {dia.etiqueta}
        </label>

        {valor.atiende ? (
          <div className="flex flex-1 flex-wrap items-center gap-2">
            <Input
              type="time"
              step={300}
              value={valor.horaInicio}
              onChange={(evento) => alCambiarHora("horaInicio", evento.target.value)}
              aria-label={`Hora de inicio del ${nombre}`}
              invalido={Boolean(error)}
              aria-invalid={Boolean(error)}
              aria-describedby={error ? idError : undefined}
              className="w-36 tabular-nums"
            />
            <span className="text-texto-secundario">a</span>
            <Input
              type="time"
              step={300}
              value={valor.horaFin}
              onChange={(evento) => alCambiarHora("horaFin", evento.target.value)}
              aria-label={`Hora de fin del ${nombre}`}
              invalido={Boolean(error)}
              aria-invalid={Boolean(error)}
              aria-describedby={error ? idError : undefined}
              className="w-36 tabular-nums"
            />
          </div>
        ) : (
          <span className="text-texto-secundario">No atiende</span>
        )}
      </div>

      {error && (
        <p id={idError} role="alert" className="entrada-error flex items-start gap-2 text-auxiliar text-error">
          <TriangleAlert aria-hidden="true" size={16} className="shrink-0" />
          {error}
        </p>
      )}
    </div>
  );
}

function FormularioEspecialista({ especialista, alCambiar, alCerrar, alGuardar }) {
  const esEdicion = Boolean(especialista);
  const refFormulario = useRef(null);
  const idErrorHorario = useId();

  const [valores, setValores] = useState(() => ({
    usuarioId: "",
    especialidadId: especialista ? String(especialista.especialidad.id) : "",
    duracionCitaMinutos: String(especialista?.duracionCitaMinutos ?? 30),
    horario: horarioInicial(especialista?.horarios),
    activo: especialista?.activo ?? true,
  }));
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState(null);
  const [guardando, setGuardando] = useState(false);

  // Los catálogos de los selectores. `null` mientras cargan. Las cuentas solo
  // hacen falta al registrar: al editar, el perfil no cambia de cuenta.
  const [especialidades, setEspecialidades] = useState(null);
  const [usuarios, setUsuarios] = useState(esEdicion ? [] : null);
  const [errorCatalogos, setErrorCatalogos] = useState(null);

  useEffect(() => {
    let vigente = true;
    Promise.all([listarEspecialidades(), esEdicion ? [] : listarUsuariosDisponibles()])
      .then(([listaEspecialidades, listaUsuarios]) => {
        if (!vigente) return;
        setEspecialidades(listaEspecialidades);
        setUsuarios(listaUsuarios);
      })
      .catch((fallo) => {
        if (vigente) {
          setErrorCatalogos(mensajeDeError(fallo, "No se pudieron cargar las especialidades ni las cuentas"));
        }
      });
    return () => {
      vigente = false;
    };
  }, [esEdicion]);

  const cambiar = (campo, valor) => {
    alCambiar();
    setValores((previos) => ({ ...previos, [campo]: valor }));
    setErrores((previos) => ({ ...previos, [campo]: undefined }));
  };

  /*
   * Un día recién marcado copia las horas del primero que ya tenga: casi
   * siempre un especialista atiende igual todos sus días, y así no hay que
   * escribir el mismo horario cinco veces.
   */
  const marcarDia = (dia, atiende) => {
    alCambiar();
    setValores((previos) => {
      const actual = previos.horario[dia];
      let horas = {};
      if (atiende && !actual.horaInicio && !actual.horaFin) {
        const modelo =
          diasMarcados(previos.horario)
            .map((otro) => previos.horario[otro])
            .find((otro) => otro.horaInicio && otro.horaFin) ?? HORARIO_POR_DEFECTO;
        horas = { horaInicio: modelo.horaInicio, horaFin: modelo.horaFin };
      }
      return { ...previos, horario: { ...previos.horario, [dia]: { ...actual, ...horas, atiende } } };
    });
    setErrores((previos) => ({ ...previos, [`horario.${dia}`]: undefined, horarios: undefined }));
  };

  const cambiarHora = (dia, campo, valor) => {
    alCambiar();
    setValores((previos) => ({
      ...previos,
      horario: { ...previos.horario, [dia]: { ...previos.horario[dia], [campo]: valor } },
    }));
    setErrores((previos) => ({ ...previos, [`horario.${dia}`]: undefined }));
  };

  const fallar = (encontrados) => {
    setErrores(encontrados);
    enfocarPrimerError(refFormulario.current);
  };

  const enviar = async (evento) => {
    evento.preventDefault();

    const encontrados = validar(valores, esEdicion);
    if (Object.keys(encontrados).length > 0) {
      fallar(encontrados);
      return;
    }

    setGuardando(true);
    setErrorGeneral(null);

    const enviados = diasMarcados(valores.horario);
    const datos = {
      especialidadId: Number(valores.especialidadId),
      duracionCitaMinutos: Number(valores.duracionCitaMinutos),
      horarios: enviados.map((dia) => ({
        dia,
        horaInicio: valores.horario[dia].horaInicio,
        horaFin: valores.horario[dia].horaFin,
      })),
    };

    try {
      const guardado = esEdicion
        ? await actualizarEspecialista(especialista.id, { ...datos, activo: valores.activo })
        : await registrarEspecialista({ ...datos, usuarioId: Number(valores.usuarioId) });
      alGuardar(guardado.medico.nombreCompleto);
    } catch (error) {
      const porCampo = erroresPorCampo(error);
      if (porCampo) {
        fallar(erroresDelBackend(porCampo, enviados));
      } else if (error.response?.status === 409) {
        // Otra persona le registró el perfil a esa cuenta mientras tanto.
        fallar({ usuarioId: mensajeDeError(error) });
      } else {
        setErrorGeneral(mensajeDeError(error, "No se pudo guardar el especialista"));
      }
    } finally {
      setGuardando(false);
    }
  };

  const sinCuentas = usuarios !== null && usuarios.length === 0 && !esEdicion;
  // Si se registró con una duración que no está en la lista, igual se ofrece.
  const duraciones = [...new Set([...DURACIONES, Number(valores.duracionCitaMinutos)])].sort(
    (a, b) => a - b,
  );

  return (
    <form ref={refFormulario} onSubmit={enviar} noValidate>
      <div className="flex flex-col gap-6 px-4 py-6 sm:px-6">
        {errorGeneral && <Alert>{errorGeneral}</Alert>}
        {errorCatalogos && <Alert>{errorCatalogos}</Alert>}

        {esEdicion ? (
          <div>
            <p className="font-medium text-texto">Médico especialista</p>
            <p className="mt-2 text-texto">{especialista.medico.nombreCompleto}</p>
          </div>
        ) : (
          <FormField
            etiqueta="Médico especialista"
            obligatorio
            error={errores.usuarioId}
            ayuda={sinCuentas ? undefined : "Las cuentas con rol Médico Especialista que todavía no tienen perfil."}
          >
            {(campo) => (
              <Select
                {...campo}
                value={valores.usuarioId}
                onChange={(evento) => cambiar("usuarioId", evento.target.value)}
                disabled={usuarios === null || sinCuentas}
              >
                <option value="">
                  {usuarios === null
                    ? "Cargando cuentas…"
                    : sinCuentas
                      ? "No hay cuentas disponibles"
                      : "Seleccione al médico…"}
                </option>
                {usuarios?.map((usuario) => (
                  <option key={usuario.id} value={usuario.id}>
                    {usuario.nombreCompleto}
                  </option>
                ))}
              </Select>
            )}
          </FormField>
        )}

        {/* Como en la solicitud sin enfermeros: no es un error de lo que
            escribió, sino algo que resuelve otra persona. */}
        {sinCuentas && (
          <Alert tipo="info" rol="status">
            No hay cuentas de Médico Especialista sin perfil. Pídale al Administrador del Asilo que
            cree la cuenta del médico con ese rol; después aparecerá aquí.
          </Alert>
        )}

        <div className="grid gap-6 sm:grid-cols-2">
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
            etiqueta="Duración de cada cita"
            obligatorio
            error={errores.duracionCitaMinutos}
            ayuda="Cada cuánto se puede dar una cita nueva."
          >
            {(campo) => (
              <Select
                {...campo}
                value={valores.duracionCitaMinutos}
                onChange={(evento) => {
                  cambiar("duracionCitaMinutos", evento.target.value);
                  // La duración cambia qué horario alcanza: los errores de los
                  // días se vuelven a calcular al guardar.
                  setErrores((previos) =>
                    Object.fromEntries(Object.entries(previos).filter(([clave]) => !clave.startsWith("horario."))),
                  );
                }}
              >
                {duraciones.map((duracion) => (
                  <option key={duracion} value={duracion}>
                    {duracion} minutos
                  </option>
                ))}
              </Select>
            )}
          </FormField>
        </div>

        <fieldset>
          <legend className="font-medium text-texto">
            Días y horas de atención
            <span className="text-error" aria-hidden="true">
              {" *"}
            </span>
          </legend>
          <p className="mt-2 text-auxiliar text-texto-secundario">
            Marque los días en que atiende. Las citas se ofrecen dentro de ese horario.
          </p>

          <div className="mt-2">
            {DIAS.map((dia) => (
              <FilaDia
                key={dia.valor}
                dia={dia}
                valor={valores.horario[dia.valor]}
                error={errores[`horario.${dia.valor}`]}
                invalidoGrupo={Boolean(errores.horarios)}
                idErrorGrupo={idErrorHorario}
                alMarcar={(atiende) => marcarDia(dia.valor, atiende)}
                alCambiarHora={(campo, valor) => cambiarHora(dia.valor, campo, valor)}
              />
            ))}
          </div>

          {errores.horarios && (
            <p
              id={idErrorHorario}
              role="alert"
              className="entrada-error mt-2 flex items-start gap-2 text-auxiliar text-error"
            >
              <TriangleAlert aria-hidden="true" size={16} className="shrink-0" />
              {errores.horarios}
            </p>
          )}
        </fieldset>

        {/* Desactivar en vez de borrar: sus citas pasadas siguen apuntando a
            él, y puede volver a atender. */}
        {esEdicion && (
          <label className="flex min-h-11 cursor-pointer items-start gap-3 select-none">
            <input
              type="checkbox"
              checked={valores.activo}
              onChange={(evento) => cambiar("activo", evento.target.checked)}
              className="mt-1 size-5 shrink-0 cursor-pointer accent-primario"
            />
            <span>
              <span className="block font-medium text-texto">Atiende citas nuevas</span>
              <span className="block text-auxiliar text-texto-secundario">
                Si lo desmarca, deja de ofrecerse al asignar citas. Las que ya tiene asignadas se
                mantienen.
              </span>
            </span>
          </label>
        )}
      </div>

      <div className="sticky bottom-0 flex flex-wrap justify-end gap-3 border-t border-divisor bg-superficie px-4 py-4 sm:px-6">
        <Button type="button" variante="secundario" onClick={alCerrar}>
          Cancelar
        </Button>
        <Button type="submit" cargando={guardando} disabled={sinCuentas}>
          {esEdicion ? "Guardar cambios" : "Registrar especialista"}
        </Button>
      </div>
    </form>
  );
}

/**
 * YS-168. Registrar o editar el perfil de un médico especialista de la
 * fundación: su especialidad, cuánto dura cada cita y en qué días y horas
 * atiende. De ese horario salen las horas libres al asignar una cita (HU-06).
 *
 * Mismo patrón que el formulario de usuarios: `claveFormulario` cambia en cada
 * apertura para remontarlo limpio.
 */
export function EspecialistaFormModal({ abierto, especialista, claveFormulario, alCerrar, alGuardar }) {
  const esEdicion = Boolean(especialista);
  const { marcarCambios, olvidarCambios, intentarCerrar, dialogo } = useConfirmarDescarte(alCerrar);

  return (
    <>
      <Modal
        abierto={abierto}
        alCerrar={intentarCerrar}
        titulo={esEdicion ? "Editar especialista" : "Registrar especialista"}
        descripcion={
          esEdicion
            ? "Actualice la especialidad, la duración de las citas o el horario de atención."
            : "Indique la especialidad del médico y en qué días y horas atiende."
        }
      >
        <FormularioEspecialista
          key={claveFormulario}
          especialista={especialista}
          alCambiar={marcarCambios}
          alCerrar={intentarCerrar}
          alGuardar={(nombre) => {
            olvidarCambios();
            alGuardar(nombre);
          }}
        />
      </Modal>

      <ConfirmDialog {...dialogo} />
    </>
  );
}
