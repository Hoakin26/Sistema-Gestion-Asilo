import { useEffect, useRef, useState } from "react";

import { useConfirmarDescarte } from "../../hooks/useConfirmarDescarte";
import { useToast } from "../../hooks/useToast";
import { erroresPorCampo, mensajeDeError } from "../../services/api";
import { obtenerCostoOrden, registrarResultado } from "../../services/laboratorioService";
import { formatearFechaHora } from "../../utils/fechas";
import { enfocarPrimerError } from "../../utils/formularios";
import { Alert } from "../ui/Alert";
import { Button } from "../ui/Button";
import { ConfirmDialog } from "../ui/ConfirmDialog";
import { CurrencyInput } from "../ui/CurrencyInput";
import { FileUpload } from "../ui/FileUpload";
import { FormField } from "../ui/FormField";
import { Textarea } from "../ui/Input";
import { Modal } from "../ui/Modal";

/* Los mismos límites que el backend (TipoArchivoResultado, ArchivosConfig). */
const TIPOS_PERMITIDOS = ["application/pdf", "image/jpeg", "image/png"];
const EXTENSIONES = /\.(pdf|jpe?g|png)$/i;
const TAMANO_MAXIMO = 10 * 1024 * 1024;
const LARGO_MAXIMO = 4000;
/* HU-14: hasta 8 enteros y 2 decimales, como el backend (LaboratorioService). */
const MONTO = /^\d{1,8}(\.\d{1,2})?$/;

/** Mismas reglas y mensajes que el backend, con la coma aceptada como punto. */
function errorDeCosto(texto) {
  const limpio = texto.trim().replace(",", ".");
  if (!limpio) return "Indique el costo del examen";
  if (limpio.startsWith("-")) return "El costo tiene que ser mayor que cero";
  if (!MONTO.test(limpio)) return "Escriba un monto como 75 o 75.50";
  if (Number(limpio) === 0) return "El costo tiene que ser mayor que cero";
  return undefined;
}

/*
 * Se avisa al elegir el archivo, no al enviar: así quien lo eligió mal lo ve
 * enseguida y no después de escribir todo el resultado (criterio de YS-118).
 * El backend vuelve a revisarlo por el contenido del archivo.
 */
function errorDeArchivo(archivo) {
  if (!archivo) return undefined;
  if (!TIPOS_PERMITIDOS.includes(archivo.type) && !EXTENSIONES.test(archivo.name)) {
    return "Solo se aceptan archivos PDF, JPG o PNG";
  }
  if (archivo.size > TAMANO_MAXIMO) return "El archivo pesa más de 10 MB, el máximo permitido";
  if (archivo.size === 0) return "El archivo está vacío";
  return undefined;
}

function FormularioResultado({ orden, alCambiar, alCerrar, alGuardar }) {
  const { mostrar } = useToast();
  const refFormulario = useRef(null);
  const [resultado, setResultado] = useState("");
  const [costo, setCosto] = useState("");
  const [archivo, setArchivo] = useState(null);
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState(null);
  const [guardando, setGuardando] = useState(false);

  /*
   * HU-14. El costo de referencia del catálogo, ya escrito: el laboratorista
   * lo confirma o lo corrige (RF-08 v1.1). Si no llega, el campo queda vacío y
   * se escribe a mano; no impide cargar el resultado.
   */
  useEffect(() => {
    let vigente = true;
    obtenerCostoOrden(orden.id)
      .then(({ costo: sugerido }) => {
        if (vigente && sugerido != null) setCosto((actual) => actual || Number(sugerido).toFixed(2));
      })
      .catch(() => {});
    return () => {
      vigente = false;
    };
  }, [orden.id]);

  const fallar = (encontrados) => {
    setErrores(encontrados);
    enfocarPrimerError(refFormulario.current);
  };

  const enviar = async (evento) => {
    evento.preventDefault();
    const encontrados = {};
    if (!resultado.trim()) encontrados.resultado = "Escriba el resultado del examen";
    const deCosto = errorDeCosto(costo);
    if (deCosto) encontrados.costo = deCosto;
    const deArchivo = errorDeArchivo(archivo);
    if (deArchivo) encontrados.archivo = deArchivo;
    if (Object.keys(encontrados).length > 0) {
      fallar(encontrados);
      return;
    }

    setGuardando(true);
    setErrorGeneral(null);
    try {
      const actualizada = await registrarResultado(orden.id, {
        resultado: resultado.trim(),
        costo: costo.trim().replace(",", "."),
        archivo,
      });
      mostrar(`Se cargó el resultado de «${orden.examen.nombre}» de ${orden.paciente.nombreCompleto}.`);
      alGuardar(actualizada);
    } catch (fallo) {
      const porCampo = erroresPorCampo(fallo);
      // 409: otra persona ya lo cargó, o la orden se anuló mientras tanto.
      if (porCampo) fallar(porCampo);
      else setErrorGeneral(mensajeDeError(fallo, "No se pudo cargar el resultado"));
    } finally {
      setGuardando(false);
    }
  };

  return (
    <form ref={refFormulario} onSubmit={enviar} noValidate>
      <div className="flex flex-col gap-6 px-4 py-6 sm:px-6">
        {errorGeneral && <Alert>{errorGeneral}</Alert>}

        <dl className="grid gap-4 rounded-tarjeta bg-fondo px-4 py-3 sm:grid-cols-2">
          <div>
            <dt className="text-auxiliar text-texto-secundario">Solicitado por</dt>
            <dd className="mt-1 text-texto">{orden.medicoSolicitante.nombreCompleto}</dd>
          </div>
          <div>
            <dt className="text-auxiliar text-texto-secundario">Fecha de la orden</dt>
            <dd className="mt-1 text-texto tabular-nums slashed-zero">{formatearFechaHora(orden.fechaOrden)}</dd>
          </div>
          {orden.indicaciones && (
            <div className="sm:col-span-2">
              <dt className="text-auxiliar text-texto-secundario">Indicaciones</dt>
              <dd className="mt-1 text-texto">{orden.indicaciones}</dd>
            </div>
          )}
        </dl>

        <FormField
          etiqueta="Resultado"
          obligatorio
          error={errores.resultado}
          ayuda="Los valores y su interpretación. Un informe largo, adjúntelo como archivo."
        >
          {(campo) => (
            <Textarea
              {...campo}
              rows={5}
              maxLength={LARGO_MAXIMO}
              value={resultado}
              onChange={(evento) => {
                setResultado(evento.target.value);
                setErrores((previos) => ({ ...previos, resultado: undefined }));
                alCambiar();
              }}
              placeholder="Ej.: Hemoglobina 13.2 g/dL, dentro de lo normal"
            />
          )}
        </FormField>

        {/* HU-14 (YS-121). Lo que se carga a la cuenta del familiar. */}
        <FormField
          etiqueta="Costo del examen"
          obligatorio
          error={errores.costo}
          ayuda="En quetzales. Viene el costo de referencia del catálogo; corríjalo si el examen costó otro monto. Se carga a la cuenta del familiar."
        >
          {(campo) => (
            <CurrencyInput
              {...campo}
              className="sm:w-56"
              value={costo}
              onChange={(evento) => {
                setCosto(evento.target.value);
                setErrores((previos) => ({ ...previos, costo: undefined }));
                alCambiar();
              }}
              placeholder="75.00"
            />
          )}
        </FormField>

        {/* El tipo y el tamaño permitidos se ven antes de elegir (YS-118). */}
        <FormField
          etiqueta="Archivo del resultado"
          error={errores.archivo}
          ayuda="Opcional. PDF, JPG o PNG, hasta 10 MB."
        >
          {(campo) => (
            <FileUpload
              {...campo}
              archivo={archivo}
              aceptar=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
              disabled={guardando}
              alCambiar={(elegido) => {
                setArchivo(elegido);
                setErrores((previos) => ({ ...previos, archivo: errorDeArchivo(elegido) }));
                alCambiar();
              }}
            />
          )}
        </FormField>
      </div>

      <div className="sticky bottom-0 flex flex-wrap justify-end gap-3 border-t border-divisor bg-superficie px-4 py-4 sm:px-6">
        <Button type="button" variante="secundario" onClick={alCerrar} disabled={guardando}>
          Cancelar
        </Button>
        <Button type="submit" cargando={guardando}>
          Cargar resultado
        </Button>
      </div>
    </form>
  );
}

/**
 * HU-13 (YS-118). La carga del resultado de una orden desde la bandeja del
 * laboratorio: el texto, obligatorio, y el archivo, si lo hay. Al guardar, la
 * orden pasa a Resultados Cargados y el especialista ya la ve en su visita.
 *
 * HU-14 (YS-121). Con el costo del examen, que el laboratorio registra al
 * cargar el resultado y queda como cargo de la visita.
 */
export function ResultadoFormModal({ abierto, orden, claveFormulario, alCerrar, alGuardar }) {
  const { marcarCambios, olvidarCambios, intentarCerrar, dialogo } = useConfirmarDescarte(alCerrar);

  return (
    <>
      <Modal
        abierto={abierto}
        alCerrar={intentarCerrar}
        titulo="Cargar resultado"
        descripcion={orden ? `${orden.examen.nombre} · ${orden.paciente.nombreCompleto}` : undefined}
      >
        {orden && (
          <FormularioResultado
            key={claveFormulario}
            orden={orden}
            alCambiar={marcarCambios}
            alCerrar={intentarCerrar}
            alGuardar={(actualizada) => {
              olvidarCambios();
              alGuardar(actualizada);
            }}
          />
        )}
      </Modal>

      <ConfirmDialog {...dialogo} />
    </>
  );
}
