import { useRef, useState } from "react";

import { erroresPorCampo, mensajeDeError } from "../../services/api";
import { enfocarPrimerError } from "../../utils/formularios";
import { Alert } from "./Alert";
import { Button } from "./Button";
import { FormField } from "./FormField";
import { Textarea } from "./Input";

/**
 * YS-176. El formulario para anular un registro financiero con su motivo: va
 * dentro de un `Modal`. El motivo es obligatorio, porque queda escrito en el
 * historial; el registro no se borra.
 *
 * YS-146. Salió de `HistorialPagos` para servir también a las donaciones
 * (HU-22): `aviso` explica qué pasa al anular, `anular(motivo)` llama a la API
 * y `alAnular` recibe lo que responde.
 */
export function FormularioAnulacion({ aviso, anular, alCerrar, alAnular, etiquetaBoton = "Anular" }) {
  const refFormulario = useRef(null);
  const [motivo, setMotivo] = useState("");
  const [error, setError] = useState(null);
  const [errorGeneral, setErrorGeneral] = useState(null);
  const [guardando, setGuardando] = useState(false);

  const enviar = async (evento) => {
    evento.preventDefault();
    if (!motivo.trim()) {
      setError("Escriba el motivo de la anulación");
      enfocarPrimerError(refFormulario.current);
      return;
    }
    setGuardando(true);
    setErrorGeneral(null);
    try {
      alAnular(await anular(motivo.trim()));
    } catch (fallo) {
      // 409: otra persona ya lo anuló.
      const porCampo = erroresPorCampo(fallo);
      if (porCampo?.motivo) {
        setError(porCampo.motivo);
        enfocarPrimerError(refFormulario.current);
      } else setErrorGeneral(mensajeDeError(fallo, "No se pudo anular"));
    } finally {
      setGuardando(false);
    }
  };

  return (
    <form ref={refFormulario} onSubmit={enviar} noValidate>
      <div className="flex flex-col gap-6 px-4 py-6 sm:px-6">
        {errorGeneral && <Alert>{errorGeneral}</Alert>}
        <Alert tipo="info" rol="status">
          {aviso}
        </Alert>
        <FormField etiqueta="Motivo de la anulación" obligatorio error={error}>
          {(campo) => (
            <Textarea
              {...campo}
              rows={3}
              maxLength={300}
              value={motivo}
              onChange={(evento) => {
                setMotivo(evento.target.value);
                setError(null);
              }}
              placeholder="Ej.: se registró Q1500 en lugar de Q150"
            />
          )}
        </FormField>
      </div>
      <div className="sticky bottom-0 flex flex-wrap justify-end gap-3 border-t border-divisor bg-superficie px-4 py-4 sm:px-6">
        <Button type="button" variante="secundario" onClick={alCerrar} disabled={guardando}>
          Cancelar
        </Button>
        <Button type="submit" variante="peligro" cargando={guardando}>
          {etiquetaBoton}
        </Button>
      </div>
    </form>
  );
}
