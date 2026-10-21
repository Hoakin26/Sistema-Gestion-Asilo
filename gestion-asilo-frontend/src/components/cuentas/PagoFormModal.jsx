import { useRef, useState } from "react";

import { FORMAS_PAGO } from "../../constants/estados";
import { useToast } from "../../hooks/useToast";
import { erroresPorCampo, mensajeDeError } from "../../services/api";
import { registrarPago } from "../../services/cuentaService";
import { formatearPeriodo, hoyISO } from "../../utils/fechas";
import { formatearQuetzales } from "../../utils/formato";
import { enfocarPrimerError, opcional } from "../../utils/formularios";
import { normalizarMonto } from "../../utils/montos";
import { Alert } from "../ui/Alert";
import { Button } from "../ui/Button";
import { CurrencyInput } from "../ui/CurrencyInput";
import { FormField } from "../ui/FormField";
import { Input, Select } from "../ui/Input";
import { Modal } from "../ui/Modal";

/* Un monto en quetzales: dígitos, y si hay punto, uno o dos decimales. */
const MONTO = /^\d{1,8}(\.\d{1,2})?$/;

/* En centavos, para comparar con el saldo sin restos de punto flotante. */
function centavos(monto) {
  return Math.round(Number(monto) * 100);
}

/** Mismas reglas y mensajes que el backend (PagoRequest y PagoService). */
function validar({ monto, fecha, formaPago }, saldo) {
  const errores = {};
  const limpio = normalizarMonto(monto);
  if (!limpio) errores.monto = "Indique el monto del pago";
  else if (limpio.startsWith("-")) errores.monto = "El monto tiene que ser mayor que cero";
  else if (!MONTO.test(limpio)) errores.monto = "Escriba un monto como 500 o 500.50";
  else if (centavos(limpio) === 0) errores.monto = "El monto tiene que ser mayor que cero";
  else if (centavos(limpio) > centavos(saldo)) {
    errores.monto = `El pago no puede pasar del saldo pendiente, ${formatearQuetzales(saldo)}`;
  }
  if (!fecha) errores.fecha = "Indique la fecha del pago";
  else if (fecha > hoyISO()) errores.fecha = "La fecha del pago no puede estar en el futuro";
  if (!formaPago) errores.formaPago = "Elija la forma de pago";
  return errores;
}

function FormularioPago({ cuenta, alCerrar, alRegistrar }) {
  const { mostrar } = useToast();
  const refFormulario = useRef(null);
  const [valores, setValores] = useState({ monto: "", fecha: hoyISO(), formaPago: "", referencia: "" });
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState(null);
  const [guardando, setGuardando] = useState(false);

  const cambiar = (campo) => (evento) => {
    setValores((previos) => ({ ...previos, [campo]: evento.target.value }));
    setErrores((previos) => ({ ...previos, [campo]: undefined }));
  };

  const fallar = (encontrados) => {
    setErrores(encontrados);
    enfocarPrimerError(refFormulario.current);
  };

  const enviar = async (evento) => {
    evento.preventDefault();
    const encontrados = validar(valores, cuenta.saldo);
    if (Object.keys(encontrados).length > 0) {
      fallar(encontrados);
      return;
    }
    setGuardando(true);
    setErrorGeneral(null);
    try {
      const historial = await registrarPago(cuenta.id, {
        monto: normalizarMonto(valores.monto),
        fecha: valores.fecha,
        formaPago: valores.formaPago,
        referencia: opcional(valores.referencia),
      });
      mostrar(
        historial.cuenta.saldo > 0
          ? `Se registró el pago de ${formatearQuetzales(normalizarMonto(valores.monto))}. Saldo pendiente: ${formatearQuetzales(historial.cuenta.saldo)}.`
          : `Se registró el pago de ${formatearQuetzales(normalizarMonto(valores.monto))}. La cuenta quedó pagada completa.`,
      );
      alRegistrar(historial);
    } catch (fallo) {
      // 409: la cuenta ya estaba pagada, o alguien registró otro pago al mismo tiempo.
      const porCampo = erroresPorCampo(fallo);
      if (porCampo) fallar(porCampo);
      else setErrorGeneral(mensajeDeError(fallo, "No se pudo registrar el pago"));
    } finally {
      setGuardando(false);
    }
  };

  return (
    <form ref={refFormulario} onSubmit={enviar} noValidate>
      <div className="flex flex-col gap-6 px-4 py-6 sm:px-6">
        {errorGeneral && <Alert>{errorGeneral}</Alert>}

        <dl className="grid gap-4 rounded-tarjeta bg-fondo px-4 py-3 sm:grid-cols-3">
          <div>
            <dt className="text-auxiliar text-texto-secundario">Total de la cuenta</dt>
            <dd className="mt-1 text-texto tabular-nums slashed-zero">{formatearQuetzales(cuenta.totalAPagar)}</dd>
          </div>
          <div>
            <dt className="text-auxiliar text-texto-secundario">Pagado</dt>
            <dd className="mt-1 text-texto tabular-nums slashed-zero">{formatearQuetzales(cuenta.totalPagado)}</dd>
          </div>
          <div>
            <dt className="text-auxiliar text-texto-secundario">Saldo pendiente</dt>
            <dd className="mt-1 font-semibold text-texto tabular-nums slashed-zero">
              {formatearQuetzales(cuenta.saldo)}
            </dd>
          </div>
        </dl>

        <FormField
          etiqueta="Monto"
          obligatorio
          error={errores.monto}
          ayuda="En quetzales. Un pago parcial se descuenta del saldo; el pago del total cancela la cuenta."
        >
          {(campo) => (
            <div className="flex flex-wrap items-center gap-3">
              <CurrencyInput {...campo} className="sm:w-56" value={valores.monto} onChange={cambiar("monto")} />
              <Button
                type="button"
                variante="secundario"
                onClick={() => {
                  setValores((previos) => ({ ...previos, monto: Number(cuenta.saldo).toFixed(2) }));
                  setErrores((previos) => ({ ...previos, monto: undefined }));
                }}
              >
                Pagar el saldo completo
              </Button>
            </div>
          )}
        </FormField>

        <div className="grid gap-6 sm:grid-cols-2">
          <FormField etiqueta="Fecha del pago" obligatorio error={errores.fecha}>
            {(campo) => (
              <Input {...campo} type="date" max={hoyISO()} value={valores.fecha} onChange={cambiar("fecha")} />
            )}
          </FormField>
          <FormField etiqueta="Forma de pago" obligatorio error={errores.formaPago}>
            {(campo) => (
              <Select {...campo} value={valores.formaPago} onChange={cambiar("formaPago")}>
                <option value="">Elija…</option>
                {Object.entries(FORMAS_PAGO).map(([clave, etiqueta]) => (
                  <option key={clave} value={clave}>
                    {etiqueta}
                  </option>
                ))}
              </Select>
            )}
          </FormField>
        </div>

        <FormField
          etiqueta="Referencia"
          error={errores.referencia}
          ayuda="Opcional: el número de boleta, de transferencia o de cheque."
        >
          {(campo) => (
            <Input
              {...campo}
              maxLength={100}
              autoComplete="off"
              value={valores.referencia}
              onChange={cambiar("referencia")}
            />
          )}
        </FormField>
      </div>

      <div className="sticky bottom-0 flex flex-wrap justify-end gap-3 border-t border-divisor bg-superficie px-4 py-4 sm:px-6">
        <Button type="button" variante="secundario" onClick={alCerrar} disabled={guardando}>
          Cancelar
        </Button>
        <Button type="submit" cargando={guardando}>
          Registrar pago
        </Button>
      </div>
    </form>
  );
}

/**
 * HU-34 (YS-177). El pago del familiar sobre una cuenta mensual (CU-006, paso
 * 6), con el saldo pendiente a la vista. Se valida igual que en el backend: más
 * de cero y no más que el saldo.
 */
export function PagoFormModal({ abierto, cuenta, claveFormulario, alCerrar, alRegistrar }) {
  return (
    <Modal
      abierto={abierto}
      alCerrar={alCerrar}
      titulo="Registrar pago"
      descripcion={
        cuenta ? `${cuenta.interno.nombreCompleto} · cuenta de ${formatearPeriodo(cuenta.periodo)}` : undefined
      }
    >
      {cuenta && (
        <FormularioPago key={claveFormulario} cuenta={cuenta} alCerrar={alCerrar} alRegistrar={alRegistrar} />
      )}
    </Modal>
  );
}
