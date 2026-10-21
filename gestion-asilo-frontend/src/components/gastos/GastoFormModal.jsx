import { useRef, useState } from "react";

import { TIPOS_GASTO } from "../../constants/estados";
import { useToast } from "../../hooks/useToast";
import { erroresPorCampo, mensajeDeError } from "../../services/api";
import { registrarGasto } from "../../services/gastoService";
import { hoyISO } from "../../utils/fechas";
import { formatearQuetzales } from "../../utils/formato";
import { enfocarPrimerError, opcional } from "../../utils/formularios";
import { normalizarMonto } from "../../utils/montos";
import { Alert } from "../ui/Alert";
import { Button } from "../ui/Button";
import { CurrencyInput } from "../ui/CurrencyInput";
import { FormField } from "../ui/FormField";
import { Input, Select } from "../ui/Input";
import { Modal } from "../ui/Modal";

/* Un monto en quetzales: hasta 10 enteros, y si hay punto, uno o dos decimales (GastoOperativoRequest). */
const MONTO = /^\d{1,10}(\.\d{1,2})?$/;

/** Mismas reglas y mensajes que el backend (GastoOperativoRequest y GastoOperativoService). */
function validar({ tipoGasto, descripcion, monto, fecha }) {
  const errores = {};
  if (!tipoGasto) errores.tipoGasto = "Elija el tipo de gasto";
  else if (tipoGasto === "OTRO" && !descripcion.trim()) errores.descripcion = "Con el tipo Otro, describa en qué se gastó";
  const limpio = normalizarMonto(monto);
  if (!limpio) errores.monto = "Indique el monto del gasto";
  else if (limpio.startsWith("-")) errores.monto = "El monto tiene que ser mayor que cero";
  else if (!MONTO.test(limpio)) errores.monto = "Escriba un monto como 850 o 850.50";
  else if (Number(limpio) === 0) errores.monto = "El monto tiene que ser mayor que cero";
  if (!fecha) errores.fecha = "Indique la fecha del gasto";
  else if (fecha > hoyISO()) errores.fecha = "La fecha del gasto no puede estar en el futuro";
  return errores;
}

function FormularioGasto({ alCerrar, alRegistrar }) {
  const { mostrar } = useToast();
  const refFormulario = useRef(null);
  const [valores, setValores] = useState({ tipoGasto: "", descripcion: "", monto: "", fecha: hoyISO() });
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
    const encontrados = validar(valores);
    if (Object.keys(encontrados).length > 0) {
      fallar(encontrados);
      return;
    }
    setGuardando(true);
    setErrorGeneral(null);
    try {
      const gasto = await registrarGasto({
        tipoGasto: valores.tipoGasto,
        descripcion: opcional(valores.descripcion),
        monto: normalizarMonto(valores.monto),
        fecha: valores.fecha,
      });
      mostrar(`Se registró el gasto: ${TIPOS_GASTO[gasto.tipoGasto]}, ${formatearQuetzales(gasto.monto)}.`);
      alRegistrar(gasto);
    } catch (fallo) {
      const porCampo = erroresPorCampo(fallo);
      if (porCampo) fallar(porCampo);
      else setErrorGeneral(mensajeDeError(fallo, "No se pudo registrar el gasto"));
    } finally {
      setGuardando(false);
    }
  };

  const esOtro = valores.tipoGasto === "OTRO";

  return (
    <form ref={refFormulario} onSubmit={enviar} noValidate>
      <div className="flex flex-col gap-6 px-4 py-6 sm:px-6">
        {errorGeneral && <Alert>{errorGeneral}</Alert>}

        <FormField etiqueta="Tipo de gasto" obligatorio error={errores.tipoGasto}>
          {(campo) => (
            <Select {...campo} value={valores.tipoGasto} onChange={cambiar("tipoGasto")}>
              <option value="">Elija…</option>
              {Object.entries(TIPOS_GASTO).map(([clave, etiqueta]) => (
                <option key={clave} value={clave}>
                  {etiqueta}
                </option>
              ))}
            </Select>
          )}
        </FormField>

        <FormField
          etiqueta="Descripción"
          obligatorio={esOtro}
          error={errores.descripcion}
          ayuda={esOtro ? "En qué se gastó." : "Opcional: el recibo, el periodo o el proveedor."}
        >
          {(campo) => (
            <Input
              {...campo}
              maxLength={200}
              autoComplete="off"
              value={valores.descripcion}
              onChange={cambiar("descripcion")}
            />
          )}
        </FormField>

        <div className="grid gap-6 sm:grid-cols-2">
          <FormField etiqueta="Monto" obligatorio error={errores.monto} ayuda="En quetzales.">
            {(campo) => <CurrencyInput {...campo} value={valores.monto} onChange={cambiar("monto")} />}
          </FormField>
          <FormField etiqueta="Fecha del gasto" obligatorio error={errores.fecha}>
            {(campo) => (
              <Input {...campo} type="date" max={hoyISO()} value={valores.fecha} onChange={cambiar("fecha")} />
            )}
          </FormField>
        </div>
      </div>

      <div className="sticky bottom-0 flex flex-wrap justify-end gap-3 border-t border-divisor bg-superficie px-4 py-4 sm:px-6">
        <Button type="button" variante="secundario" onClick={alCerrar} disabled={guardando}>
          Cancelar
        </Button>
        <Button type="submit" cargando={guardando}>
          Registrar gasto
        </Button>
      </div>
    </form>
  );
}

/**
 * HU-23 (YS-149). El registro de un gasto operativo (RF-16), en ventana
 * flotante como el resto del sistema. Se valida igual que en el backend.
 */
export function GastoFormModal({ abierto, claveFormulario, alCerrar, alRegistrar }) {
  return (
    <Modal abierto={abierto} alCerrar={alCerrar} titulo="Registrar gasto" descripcion="Un gasto operativo del asilo">
      {abierto && <FormularioGasto key={claveFormulario} alCerrar={alCerrar} alRegistrar={alRegistrar} />}
    </Modal>
  );
}
