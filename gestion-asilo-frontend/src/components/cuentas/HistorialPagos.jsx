import { useEffect, useRef, useState } from "react";
import { Ban } from "lucide-react";

import { FORMAS_PAGO } from "../../constants/estados";
import { useToast } from "../../hooks/useToast";
import { erroresPorCampo, mensajeDeError } from "../../services/api";
import { formatearFecha, formatearFechaHora } from "../../utils/fechas";
import { formatearQuetzales } from "../../utils/formato";
import { enfocarPrimerError } from "../../utils/formularios";
import { Alert } from "../ui/Alert";
import { Button } from "../ui/Button";
import { FormField } from "../ui/FormField";
import { Textarea } from "../ui/Input";
import { Modal } from "../ui/Modal";
import { Barra } from "../ui/TablaSkeleton";

function FormularioAnulacion({ pago, anular, alCerrar, alAnular }) {
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
      alAnular(await anular(pago.id, motivo.trim()));
    } catch (fallo) {
      // 409: otra persona ya lo anuló.
      const porCampo = erroresPorCampo(fallo);
      if (porCampo?.motivo) {
        setError(porCampo.motivo);
        enfocarPrimerError(refFormulario.current);
      } else setErrorGeneral(mensajeDeError(fallo, "No se pudo anular el pago"));
    } finally {
      setGuardando(false);
    }
  };

  return (
    <form ref={refFormulario} onSubmit={enviar} noValidate>
      <div className="flex flex-col gap-6 px-4 py-6 sm:px-6">
        {errorGeneral && <Alert>{errorGeneral}</Alert>}
        <Alert tipo="info" rol="status">
          El pago de {formatearQuetzales(pago.monto)} del {formatearFecha(pago.fecha)} vuelve al saldo. No se borra:
          queda en el historial como anulado, con el motivo.
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
          Anular pago
        </Button>
      </div>
    </form>
  );
}

/**
 * HU-34 (YS-177). El historial de pagos de una cuenta, del más reciente al
 * más antiguo, con los anulados marcados y su motivo. Un pago vigente se puede
 * anular con su motivo (YS-176).
 *
 * YS-140. Sirve igual para la cuenta del familiar (HU-34) y para la deuda con
 * la fundación (HU-20): `cargar()` y `anular(pagoId, motivo)` llaman a su API
 * y responden `{ cuenta, pagos }`. `clave` identifica la cuenta: al cambiar,
 * se vuelve a cargar. `alCambiar` recibe la cuenta con su saldo y su estado
 * nuevos después de anular.
 */
export function HistorialPagos({ clave, cargar, anular, alCambiar }) {
  const { mostrar } = useToast();
  const [pagos, setPagos] = useState(null);
  const [error, setError] = useState(null);
  const [enAnulacion, setEnAnulacion] = useState(null);
  const [aperturas, setAperturas] = useState(0);
  // `cargar` es una función nueva en cada render del padre: se guarda la última
  // y solo se vuelve a pedir el historial cuando cambia de cuenta (`clave`).
  const refCargar = useRef(cargar);
  useEffect(() => {
    refCargar.current = cargar;
  });

  useEffect(() => {
    let vigente = true;
    refCargar
      .current()
      .then((historial) => {
        if (vigente) setPagos(historial.pagos);
      })
      .catch((fallo) => {
        if (!vigente) return;
        setPagos([]);
        setError(mensajeDeError(fallo, "No se pudo cargar el historial de pagos"));
      });
    return () => {
      vigente = false;
    };
  }, [clave]);

  if (pagos === null) {
    return (
      <div role="status" className="px-4 py-3">
        <span className="sr-only">Cargando los pagos…</span>
        <Barra className="w-48" />
      </div>
    );
  }

  return (
    <div className="px-4 pb-4">
      {error && <Alert>{error}</Alert>}
      {pagos.length === 0 && !error ? (
        <p className="text-auxiliar text-texto-secundario">Todavía no hay pagos registrados.</p>
      ) : (
        <ul className="divide-y divide-divisor rounded-tarjeta border border-divisor bg-fondo">
          {pagos.map((pago) => (
            <li key={pago.id} className="flex flex-wrap items-start justify-between gap-x-6 gap-y-2 px-4 py-3">
              <div className="min-w-0">
                <p className={`font-medium tabular-nums slashed-zero ${pago.anulado ? "text-texto-secundario line-through" : "text-texto"}`}>
                  {formatearQuetzales(pago.monto)} · {formatearFecha(pago.fecha)}
                </p>
                <p className="text-auxiliar text-texto-secundario">
                  {FORMAS_PAGO[pago.formaPago]}
                  {pago.referencia && ` · Ref. ${pago.referencia}`} · Registró {pago.registradoPor}
                </p>
                {pago.anulado && (
                  <p className="mt-1 text-auxiliar text-error">
                    Anulado por {pago.anuladoPor} el {formatearFechaHora(pago.fechaAnulacion)}: {pago.motivoAnulacion}
                  </p>
                )}
              </div>
              {!pago.anulado && (
                <Button
                  variante="secundario"
                  onClick={() => {
                    setAperturas((valor) => valor + 1);
                    setEnAnulacion(pago);
                  }}
                  aria-label={`Anular el pago de ${formatearQuetzales(pago.monto)} del ${formatearFecha(pago.fecha)}`}
                >
                  <Ban aria-hidden="true" size={20} />
                  Anular
                </Button>
              )}
            </li>
          ))}
        </ul>
      )}

      <Modal
        abierto={enAnulacion !== null}
        alCerrar={() => setEnAnulacion(null)}
        titulo="Anular pago"
        descripcion={enAnulacion ? `${formatearQuetzales(enAnulacion.monto)} · ${formatearFecha(enAnulacion.fecha)}` : undefined}
      >
        {enAnulacion && (
          <FormularioAnulacion
            key={aperturas}
            pago={enAnulacion}
            anular={anular}
            alCerrar={() => setEnAnulacion(null)}
            alAnular={(historial) => {
              setEnAnulacion(null);
              setPagos(historial.pagos);
              mostrar(`Se anuló el pago. Saldo pendiente: ${formatearQuetzales(historial.cuenta.saldo)}.`);
              alCambiar(historial.cuenta);
            }}
          />
        )}
      </Modal>
    </div>
  );
}
