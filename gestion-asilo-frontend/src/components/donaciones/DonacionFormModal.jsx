import { useRef, useState } from "react";

import { TIPOS_DONANTE } from "../../constants/estados";
import { useToast } from "../../hooks/useToast";
import { erroresPorCampo, mensajeDeError } from "../../services/api";
import { registrarDonacion } from "../../services/donacionService";
import { hoyISO } from "../../utils/fechas";
import { formatearQuetzales } from "../../utils/formato";
import { enfocarPrimerError } from "../../utils/formularios";
import { normalizarMonto } from "../../utils/montos";
import { Alert } from "../ui/Alert";
import { Button } from "../ui/Button";
import { CurrencyInput } from "../ui/CurrencyInput";
import { FormField } from "../ui/FormField";
import { Input, Select } from "../ui/Input";
import { Modal } from "../ui/Modal";

/* Un monto en quetzales: hasta 10 enteros, y si hay punto, uno o dos decimales (DonacionRequest). */
const MONTO = /^\d{1,10}(\.\d{1,2})?$/;

/** Mismas reglas y mensajes que el backend (DonacionRequest). */
function validar({ monto, fecha, donante, tipoDonante }) {
  const errores = {};
  const limpio = normalizarMonto(monto);
  if (!limpio) errores.monto = "Indique el monto de la donación";
  else if (limpio.startsWith("-")) errores.monto = "El monto tiene que ser mayor que cero";
  else if (!MONTO.test(limpio)) errores.monto = "Escriba un monto como 5000 o 5000.50";
  else if (Number(limpio) === 0) errores.monto = "El monto tiene que ser mayor que cero";
  if (!fecha) errores.fecha = "Indique la fecha de la donación";
  else if (fecha > hoyISO()) errores.fecha = "La fecha de la donación no puede estar en el futuro";
  if (!donante.trim()) errores.donante = "Escriba el nombre del donante";
  if (!tipoDonante) errores.tipoDonante = "Elija el tipo de donante";
  return errores;
}

function FormularioDonacion({ alCerrar, alRegistrar }) {
  const { mostrar } = useToast();
  const refFormulario = useRef(null);
  const [valores, setValores] = useState({ monto: "", fecha: hoyISO(), donante: "", tipoDonante: "" });
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
      const donacion = await registrarDonacion({
        monto: normalizarMonto(valores.monto),
        fecha: valores.fecha,
        donante: valores.donante.trim(),
        tipoDonante: valores.tipoDonante,
      });
      mostrar(`Se registró la donación de ${formatearQuetzales(donacion.monto)} de ${donacion.donante}.`);
      alRegistrar(donacion);
    } catch (fallo) {
      const porCampo = erroresPorCampo(fallo);
      if (porCampo) fallar(porCampo);
      else setErrorGeneral(mensajeDeError(fallo, "No se pudo registrar la donación"));
    } finally {
      setGuardando(false);
    }
  };

  return (
    <form ref={refFormulario} onSubmit={enviar} noValidate>
      <div className="flex flex-col gap-6 px-4 py-6 sm:px-6">
        {errorGeneral && <Alert>{errorGeneral}</Alert>}

        <FormField etiqueta="Donante" obligatorio error={errores.donante} ayuda="La persona, la empresa o la institución.">
          {(campo) => (
            <Input {...campo} maxLength={150} autoComplete="off" value={valores.donante} onChange={cambiar("donante")} />
          )}
        </FormField>

        <FormField etiqueta="Tipo de donante" obligatorio error={errores.tipoDonante}>
          {(campo) => (
            <Select {...campo} value={valores.tipoDonante} onChange={cambiar("tipoDonante")}>
              <option value="">Elija…</option>
              {Object.entries(TIPOS_DONANTE).map(([clave, etiqueta]) => (
                <option key={clave} value={clave}>
                  {etiqueta}
                </option>
              ))}
            </Select>
          )}
        </FormField>

        <div className="grid gap-6 sm:grid-cols-2">
          <FormField etiqueta="Monto" obligatorio error={errores.monto} ayuda="En quetzales.">
            {(campo) => <CurrencyInput {...campo} value={valores.monto} onChange={cambiar("monto")} />}
          </FormField>
          <FormField etiqueta="Fecha de la donación" obligatorio error={errores.fecha}>
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
          Registrar donación
        </Button>
      </div>
    </form>
  );
}

/**
 * HU-22 (YS-146). El registro de una donación monetaria (RF-15), en ventana
 * flotante como el resto del sistema. Se valida igual que en el backend.
 */
export function DonacionFormModal({ abierto, claveFormulario, alCerrar, alRegistrar }) {
  return (
    <Modal abierto={abierto} alCerrar={alCerrar} titulo="Registrar donación" descripcion="Una donación monetaria al asilo">
      {abierto && <FormularioDonacion key={claveFormulario} alCerrar={alCerrar} alRegistrar={alRegistrar} />}
    </Modal>
  );
}
