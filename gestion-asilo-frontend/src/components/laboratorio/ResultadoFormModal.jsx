import { useRef, useState } from "react";

import { useConfirmarDescarte } from "../../hooks/useConfirmarDescarte";
import { useToast } from "../../hooks/useToast";
import { erroresPorCampo, mensajeDeError } from "../../services/api";
import { registrarResultado } from "../../services/laboratorioService";
import { formatearFechaHora } from "../../utils/fechas";
import { enfocarPrimerError } from "../../utils/formularios";
import { Alert } from "../ui/Alert";
import { Button } from "../ui/Button";
import { ConfirmDialog } from "../ui/ConfirmDialog";
import { FileUpload } from "../ui/FileUpload";
import { FormField } from "../ui/FormField";
import { Textarea } from "../ui/Input";
import { Modal } from "../ui/Modal";

/* Los mismos límites que el backend (TipoArchivoResultado, ArchivosConfig). */
const TIPOS_PERMITIDOS = ["application/pdf", "image/jpeg", "image/png"];
const EXTENSIONES = /\.(pdf|jpe?g|png)$/i;
const TAMANO_MAXIMO = 10 * 1024 * 1024;
const LARGO_MAXIMO = 4000;

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
  const [archivo, setArchivo] = useState(null);
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState(null);
  const [guardando, setGuardando] = useState(false);

  const fallar = (encontrados) => {
    setErrores(encontrados);
    enfocarPrimerError(refFormulario.current);
  };

  const enviar = async (evento) => {
    evento.preventDefault();
    const encontrados = {};
    if (!resultado.trim()) encontrados.resultado = "Escriba el resultado del examen";
    const deArchivo = errorDeArchivo(archivo);
    if (deArchivo) encontrados.archivo = deArchivo;
    if (Object.keys(encontrados).length > 0) {
      fallar(encontrados);
      return;
    }

    setGuardando(true);
    setErrorGeneral(null);
    try {
      const actualizada = await registrarResultado(orden.id, { resultado: resultado.trim(), archivo });
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
