import { useRef, useState } from "react";

import { erroresPorCampo, mensajeDeError } from "../../services/api";
import { actualizarCuota } from "../../services/cuentaService";
import { enfocarPrimerError } from "../../utils/formularios";
import { normalizarMonto } from "../../utils/montos";
import { Alert } from "../ui/Alert";
import { Button } from "../ui/Button";
import { CurrencyInput } from "../ui/CurrencyInput";
import { FormField } from "../ui/FormField";
import { Modal } from "../ui/Modal";

/* Un monto en quetzales: dígitos, y si hay punto, uno o dos decimales. */
const MONTO = /^\d{1,8}(\.\d{1,2})?$/;

/**
 * Mismas reglas que el backend (CuotaRequest), con la coma aceptada como
 * punto. Cero sí vale: es "sin cuota", como un interno becado por la fundación.
 */
function validar(texto) {
  const limpio = normalizarMonto(texto);
  if (!limpio) return { error: "Indique la cuota mensual" };
  if (limpio.startsWith("-")) return { error: "La cuota mensual no puede ser negativa" };
  if (!MONTO.test(limpio)) return { error: "Escriba un monto como 1500 o 1500.50" };
  return { monto: limpio };
}

function FormularioCuota({ interno, alCerrar, alGuardar }) {
  const refFormulario = useRef(null);
  const [texto, setTexto] = useState(interno.cuotaMensual == null ? "" : Number(interno.cuotaMensual).toFixed(2));
  const [error, setError] = useState(null);
  const [errorGeneral, setErrorGeneral] = useState(null);
  const [guardando, setGuardando] = useState(false);

  const fallar = (mensaje) => {
    setError(mensaje);
    enfocarPrimerError(refFormulario.current);
  };

  const enviar = async (evento) => {
    evento.preventDefault();
    const { error: encontrado, monto } = validar(texto);
    if (encontrado) {
      fallar(encontrado);
      return;
    }

    setGuardando(true);
    setErrorGeneral(null);
    try {
      // Como texto, igual que la tarifa de consulta: el backend lo lee como BigDecimal.
      alGuardar(await actualizarCuota(interno.id, monto));
    } catch (fallo) {
      const porCampo = erroresPorCampo(fallo);
      if (porCampo?.cuotaMensual) fallar(porCampo.cuotaMensual);
      else setErrorGeneral(mensajeDeError(fallo, "No se pudo guardar la cuota"));
    } finally {
      setGuardando(false);
    }
  };

  return (
    <form ref={refFormulario} onSubmit={enviar} noValidate>
      <div className="flex flex-col gap-6 px-4 py-6 sm:px-6">
        {errorGeneral && <Alert>{errorGeneral}</Alert>}

        <FormField
          etiqueta="Cuota mensual"
          obligatorio
          error={error}
          ayuda="En quetzales; 0 si no paga cuota. Entra en la cuenta del familiar al consolidar el mes."
        >
          {(campo) => (
            <CurrencyInput
              {...campo}
              className="sm:w-56"
              value={texto}
              onChange={(evento) => {
                setTexto(evento.target.value);
                setError(null);
              }}
            />
          )}
        </FormField>
      </div>

      <div className="sticky bottom-0 flex flex-wrap justify-end gap-3 border-t border-divisor bg-superficie px-4 py-4 sm:px-6">
        <Button type="button" variante="secundario" onClick={alCerrar}>
          Cancelar
        </Button>
        <Button type="submit" cargando={guardando}>
          Guardar cuota
        </Button>
      </div>
    </form>
  );
}

/**
 * HU-18 (YS-134). La cuota base mensual del interno, que fija el
 * Administrador del Asilo (YS-132). La toma la próxima consolidación del mes.
 */
export function CuotaFormModal({ abierto, interno, claveFormulario, alCerrar, alGuardar }) {
  return (
    <Modal
      abierto={abierto}
      alCerrar={alCerrar}
      titulo="Cuota mensual"
      descripcion={interno ? interno.nombreCompleto : undefined}
    >
      {interno && (
        <FormularioCuota key={claveFormulario} interno={interno} alCerrar={alCerrar} alGuardar={alGuardar} />
      )}
    </Modal>
  );
}
