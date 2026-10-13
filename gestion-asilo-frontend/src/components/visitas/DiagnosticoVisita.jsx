import { useId, useRef, useState } from "react";
import { CheckCheck, FileText, Save } from "lucide-react";

import { useToast } from "../../hooks/useToast";
import { erroresPorCampo, mensajeDeError } from "../../services/api";
import { finalizarVisita, guardarDiagnostico } from "../../services/visitaService";
import { formatearFechaHora } from "../../utils/fechas";
import { enfocarPrimerError } from "../../utils/formularios";
import { Alert } from "../ui/Alert";
import { Button } from "../ui/Button";
import { ConfirmDialog } from "../ui/ConfirmDialog";
import { FormField } from "../ui/FormField";
import { Textarea } from "../ui/Input";

const TARJETA = "rounded-tarjeta border border-divisor bg-superficie p-4 shadow-tarjeta sm:p-6";

/** Mismas reglas y mensajes que el backend (DiagnosticoRequest). */
function validar({ diagnostico, observaciones }) {
  const errores = {};
  if (!diagnostico.trim()) errores.diagnostico = "El diagnóstico es obligatorio";
  else if (diagnostico.trim().length > 2000) {
    errores.diagnostico = "El diagnóstico no puede exceder 2000 caracteres";
  }
  if (observaciones.trim().length > 2000) {
    errores.observaciones = "Las observaciones no pueden exceder 2000 caracteres";
  }
  return errores;
}

/** Lo registrado, en solo lectura: así queda la visita una vez finalizada. */
function DiagnosticoRegistrado({ visita, idTitulo }) {
  return (
    <section aria-labelledby={idTitulo} className={TARJETA}>
      <div className="flex items-center gap-3">
        <FileText aria-hidden="true" size={20} className="text-texto-secundario" />
        <h2 id={idTitulo} className="text-h3 font-semibold">
          Diagnóstico
        </h2>
      </div>
      <p className="mt-4 whitespace-pre-line text-texto">{visita.diagnostico}</p>

      <h3 className="mt-6 font-semibold text-texto">Observaciones</h3>
      <p className={`mt-2 whitespace-pre-line ${visita.observaciones ? "text-texto" : "text-texto-secundario"}`}>
        {visita.observaciones ?? "Sin observaciones."}
      </p>

      <p className="mt-6 text-auxiliar text-texto-secundario tabular-nums slashed-zero">
        Visita finalizada el {formatearFechaHora(visita.fechaCierre)}.
      </p>
    </section>
  );
}

/**
 * HU-09 (YS-106). El diagnóstico y las observaciones de la consulta, y el
 * cierre de la visita.
 *
 * "Guardar" se puede usar las veces que haga falta durante la consulta;
 * "Finalizar visita" guarda lo escrito y cierra, con confirmación, porque
 * después ya no se puede editar. Si la especialidad todavía no tiene tarifa,
 * el backend no deja finalizar (decidido el 10/10), pero lo escrito ya quedó
 * guardado y el aviso dice a quién acudir.
 *
 * Se monta con key por visita: el formulario nace de lo que la visita ya
 * tiene registrado.
 */
export function DiagnosticoVisita({ visita, alActualizar }) {
  const { mostrar } = useToast();
  const idTitulo = useId();
  const refFormulario = useRef(null);

  const [valores, setValores] = useState({
    diagnostico: visita.diagnostico ?? "",
    observaciones: visita.observaciones ?? "",
  });
  const [errores, setErrores] = useState({});
  const [aviso, setAviso] = useState(null);
  const [guardando, setGuardando] = useState(false);
  const [finalizando, setFinalizando] = useState(false);
  const [confirmando, setConfirmando] = useState(false);

  if (visita.estado !== "ACTIVA") {
    return <DiagnosticoRegistrado visita={visita} idTitulo={idTitulo} />;
  }

  // Derivado: si lo escrito difiere de lo guardado, se avisa. Sin esto, salir
  // de la pantalla tras escribir sin guardar parecería no tener consecuencias.
  const sinGuardar =
    valores.diagnostico.trim() !== (visita.diagnostico ?? "") ||
    (valores.observaciones.trim() || null) !== (visita.observaciones ?? null);

  const cambiar = (campo) => (evento) => {
    setValores((previos) => ({ ...previos, [campo]: evento.target.value }));
    setErrores((previos) => ({ ...previos, [campo]: undefined }));
  };

  const fallar = (encontrados) => {
    setErrores(encontrados);
    enfocarPrimerError(refFormulario.current);
  };

  /** Valida y guarda; devuelve la visita guardada, o null si no se pudo. */
  const guardar = async () => {
    const encontrados = validar(valores);
    if (Object.keys(encontrados).length > 0) {
      fallar(encontrados);
      return null;
    }
    setAviso(null);
    try {
      const guardada = await guardarDiagnostico(visita.id, {
        diagnostico: valores.diagnostico.trim(),
        observaciones: valores.observaciones.trim() || null,
      });
      alActualizar(guardada);
      return guardada;
    } catch (fallo) {
      const porCampo = erroresPorCampo(fallo);
      if (porCampo) fallar(porCampo);
      else setAviso({ tipo: "error", mensaje: mensajeDeError(fallo, "No se pudo guardar el diagnóstico") });
      return null;
    }
  };

  const alGuardar = async (evento) => {
    evento.preventDefault();
    setGuardando(true);
    const guardada = await guardar();
    setGuardando(false);
    if (guardada) mostrar("Se guardó el diagnóstico.");
  };

  const pedirFinalizar = () => {
    const encontrados = validar(valores);
    if (Object.keys(encontrados).length > 0) {
      fallar(encontrados);
      return;
    }
    setConfirmando(true);
  };

  const finalizar = async () => {
    setConfirmando(false);
    setFinalizando(true);
    try {
      if (!(await guardar())) return;
      const finalizada = await finalizarVisita(visita.id);
      alActualizar(finalizada);
      mostrar("La visita quedó finalizada.");
    } catch (fallo) {
      const porCampo = erroresPorCampo(fallo);
      if (porCampo) {
        fallar(porCampo);
      } else {
        // 409 sin tarifa: no es un error de lo que escribió. Lo escrito ya se
        // guardó en el paso anterior, y el mensaje dice a quién acudir.
        setAviso({
          tipo: fallo.response?.status === 409 ? "info" : "error",
          mensaje: mensajeDeError(fallo, "No se pudo finalizar la visita"),
        });
      }
    } finally {
      setFinalizando(false);
    }
  };

  const ocupado = guardando || finalizando;

  return (
    <section aria-labelledby={idTitulo} className={TARJETA}>
      <div className="flex items-center gap-3">
        <FileText aria-hidden="true" size={20} className="text-texto-secundario" />
        <h2 id={idTitulo} className="text-h3 font-semibold">
          Diagnóstico
        </h2>
      </div>

      <form ref={refFormulario} onSubmit={alGuardar} noValidate className="mt-6 flex flex-col gap-6">
        {aviso && (
          <Alert tipo={aviso.tipo} rol={aviso.tipo === "info" ? "status" : "alert"}>
            {aviso.mensaje}
          </Alert>
        )}

        <FormField
          etiqueta="Diagnóstico"
          obligatorio
          error={errores.diagnostico}
          ayuda="Obligatorio para finalizar la visita."
        >
          {(campo) => (
            <Textarea
              {...campo}
              rows={4}
              maxLength={2000}
              value={valores.diagnostico}
              onChange={cambiar("diagnostico")}
              placeholder="Lo que concluye de la consulta"
            />
          )}
        </FormField>

        <FormField etiqueta="Observaciones" error={errores.observaciones}>
          {(campo) => (
            <Textarea
              {...campo}
              rows={3}
              maxLength={2000}
              value={valores.observaciones}
              onChange={cambiar("observaciones")}
              placeholder="Indicaciones, controles, lo que enfermería debe vigilar"
            />
          )}
        </FormField>

        <div className="flex flex-wrap items-center justify-end gap-3">
          {sinGuardar && (
            <p aria-live="polite" className="mr-auto text-auxiliar text-texto-secundario">
              Hay cambios sin guardar.
            </p>
          )}
          <Button type="submit" variante="secundario" cargando={guardando} disabled={ocupado}>
            {!guardando && <Save aria-hidden="true" size={20} />}
            Guardar
          </Button>
          <Button type="button" onClick={pedirFinalizar} cargando={finalizando} disabled={ocupado}>
            {!finalizando && <CheckCheck aria-hidden="true" size={20} />}
            Finalizar visita
          </Button>
        </div>
      </form>

      {/* Finalizar no se deshace: cierra la consulta y genera el cargo. */}
      <ConfirmDialog
        abierto={confirmando}
        titulo="¿Finalizar la visita?"
        mensaje={`Después ya no podrá cambiar el diagnóstico ni las observaciones. La solicitud ${visita.numeroSolicitud} pasará a "Atendida" y se registrará el cargo de la consulta en la cuenta del familiar.`}
        textoConfirmar="Finalizar visita"
        textoCancelar="Seguir editando"
        alConfirmar={finalizar}
        alCancelar={() => setConfirmando(false)}
      />
    </section>
  );
}
