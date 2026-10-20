import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Calculator, Inbox } from "lucide-react";

import { Alert } from "../components/ui/Alert";
import { Button } from "../components/ui/Button";
import { FormField } from "../components/ui/FormField";
import { Input } from "../components/ui/Input";
import { StatusBadge } from "../components/ui/StatusBadge";
import { TablaSkeleton } from "../components/ui/TablaSkeleton";
import { useToast } from "../hooks/useToast";
import { AppLayout } from "../layouts/AppLayout";
import { erroresPorCampo, mensajeDeError } from "../services/api";
import { consolidarCuentas, listarCuentas } from "../services/cuentaService";
import { formatearFechaHora, formatearPeriodo, mesActualISO } from "../utils/fechas";
import { formatearQuetzales } from "../utils/formato";

const ENCABEZADO =
  "px-4 py-4 text-auxiliar font-semibold text-texto-secundario uppercase xl:px-6";
const MONTO = "px-4 py-4 text-right tabular-nums slashed-zero xl:px-6";

/* En centavos: sumar y restar montos como números de JavaScript deja restos como 0.30000000000000004. */
function centavos(monto) {
  return Math.round(Number(monto) * 100);
}

/**
 * HU-18 (YS-134). "Cuentas por cobrar" del Administrador del Asilo (CU-006,
 * paso 1): la cuenta mensual de cada familiar, con la cuota base del asilo más
 * las consultas, los exámenes y la farmacia del mes. Se consolida desde aquí;
 * el detalle de cada una está en el perfil del interno.
 *
 * Nada clínico, como en todo lo del Administrador del Asilo (RNF-03): ni aquí
 * ni en el backend aparece de qué fue cada servicio.
 */
export function CuentasPorCobrarPage() {
  const { mostrar } = useToast();
  const [periodo, setPeriodo] = useState(mesActualISO);
  const [cuentas, setCuentas] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [errorPeriodo, setErrorPeriodo] = useState(null);
  const [consolidando, setConsolidando] = useState(false);

  useEffect(() => {
    if (!periodo) return undefined;
    let vigente = true;
    listarCuentas(periodo)
      .then((lista) => {
        if (!vigente) return;
        setCuentas(lista);
        setError(null);
      })
      .catch((fallo) => {
        if (vigente) setError(mensajeDeError(fallo, "No se pudieron cargar las cuentas"));
      })
      .finally(() => {
        if (vigente) setCargando(false);
      });
    return () => {
      vigente = false;
    };
  }, [periodo]);

  const cambiarPeriodo = (evento) => {
    setPeriodo(evento.target.value);
    setErrorPeriodo(null);
    if (evento.target.value) setCargando(true);
  };

  const consolidar = async () => {
    if (!periodo) {
      setErrorPeriodo("Elija el mes que quiere consolidar");
      return;
    }
    setConsolidando(true);
    setError(null);
    try {
      const consolidadas = await consolidarCuentas(periodo);
      setCuentas(consolidadas);
      mostrar(
        consolidadas.length === 1
          ? `Se consolidó 1 cuenta de ${formatearPeriodo(periodo)}.`
          : `Se consolidaron ${consolidadas.length} cuentas de ${formatearPeriodo(periodo)}.`,
      );
    } catch (fallo) {
      const porCampo = erroresPorCampo(fallo);
      if (porCampo?.periodo) setErrorPeriodo(porCampo.periodo);
      else setError(mensajeDeError(fallo, "No se pudo consolidar el mes"));
    } finally {
      setConsolidando(false);
    }
  };

  const lista = cuentas ?? [];
  const totalACobrar = lista.reduce((suma, cuenta) => suma + centavos(cuenta.totalAPagar), 0) / 100;
  const saldoPendiente = lista.reduce((suma, cuenta) => suma + centavos(cuenta.saldo), 0) / 100;

  return (
    <AppLayout migas={["Inicio", "Cuentas por cobrar"]}>
      <h1 className="text-h1 font-bold">Cuentas por cobrar</h1>
      <p className="mt-2 text-texto-secundario">
        La cuenta mensual de cada familiar: la cuota base del asilo más las consultas, los exámenes y la farmacia del
        mes.
      </p>

      <div className="mt-6 flex flex-wrap items-end gap-4">
        <FormField etiqueta="Mes" error={errorPeriodo}>
          {(campo) => (
            <Input
              {...campo}
              type="month"
              value={periodo}
              max={mesActualISO()}
              onChange={cambiarPeriodo}
              className="sm:w-56"
            />
          )}
        </FormField>
        <Button onClick={consolidar} cargando={consolidando}>
          {!consolidando && <Calculator aria-hidden="true" size={20} />}
          Consolidar el mes
        </Button>
      </div>
      <p className="mt-2 text-auxiliar text-texto-secundario">
        Suma la cuota base y los cargos del mes de cada interno. Puede volver a consolidar un mes: se recalcula sin
        duplicar nada.
      </p>

      {error && (
        <div className="mt-6">
          <Alert>{error}</Alert>
        </div>
      )}

      <div className="mt-6">
        {cargando ? (
          <TablaSkeleton etiqueta="Cargando cuentas…" />
        ) : lista.length === 0 ? (
          <div className="flex flex-col items-center gap-4 rounded-tarjeta border border-divisor bg-superficie px-4 py-16 text-center shadow-tarjeta sm:px-6">
            <Inbox aria-hidden="true" size={24} className="text-texto-secundario" />
            <p className="max-w-md text-texto-secundario">
              {periodo
                ? `Todavía no se consolida ${formatearPeriodo(periodo)}. Al consolidarlo se arma la cuenta de cada familiar.`
                : "Elija un mes para ver sus cuentas."}
            </p>
          </div>
        ) : (
          <>
            <p
              aria-live="polite"
              className="pb-3 text-auxiliar text-texto-secundario tabular-nums slashed-zero"
            >
              {lista.length} {lista.length === 1 ? "cuenta" : "cuentas"} · Total a cobrar{" "}
              {formatearQuetzales(totalACobrar)} · Saldo pendiente {formatearQuetzales(saldoPendiente)}
            </p>

            {/* relative: los sr-only de la tabla son absolutos; sin él escapan del
                recorte, estiran la página y el teléfono la achica a una esquina (YS-175). */}
            <div className="relative overflow-x-auto rounded-tarjeta border border-divisor bg-superficie shadow-tarjeta">
              <table className="w-full min-w-3xl border-collapse text-left">
                <caption className="sr-only">
                  Cuentas de {periodo && formatearPeriodo(periodo)}: interno, familiar responsable, montos y estado
                </caption>
                <thead>
                  <tr className="border-b border-divisor bg-fondo">
                    <th scope="col" className={ENCABEZADO}>
                      Interno
                    </th>
                    <th scope="col" className={ENCABEZADO}>
                      Familiar responsable
                    </th>
                    <th scope="col" className={`${ENCABEZADO} text-right`}>
                      Cuota base
                    </th>
                    <th scope="col" className={`${ENCABEZADO} text-right`}>
                      Servicios
                    </th>
                    <th scope="col" className={`${ENCABEZADO} text-right`}>
                      Total a pagar
                    </th>
                    <th scope="col" className={`${ENCABEZADO} text-right`}>
                      Saldo
                    </th>
                    <th scope="col" className={ENCABEZADO}>
                      Estado
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {lista.map((cuenta) => (
                    <tr key={cuenta.id} className="border-b border-divisor align-top last:border-b-0">
                      <td className="px-4 py-4 xl:px-6">
                        {/* El estado de cuenta desglosado vive en el perfil del interno. */}
                        <Link
                          to={`/internos/${cuenta.interno.id}`}
                          className="font-medium text-primario hover:underline"
                        >
                          {cuenta.interno.nombreCompleto}
                        </Link>
                        <span className="block text-auxiliar text-texto-secundario tabular-nums slashed-zero">
                          Consolidada: {formatearFechaHora(cuenta.fechaConsolidacion)}
                        </span>
                      </td>
                      <td className="px-4 py-4 xl:px-6">{cuenta.familiar.nombreCompleto}</td>
                      <td className={MONTO}>{formatearQuetzales(cuenta.cuotaBase)}</td>
                      {/* Los servicios ya con el descuento de la fundación (HU-19). */}
                      <td className={MONTO}>
                        {formatearQuetzales((centavos(cuenta.totalAPagar) - centavos(cuenta.cuotaBase)) / 100)}
                      </td>
                      <td className={`${MONTO} font-semibold`}>{formatearQuetzales(cuenta.totalAPagar)}</td>
                      <td className={MONTO}>{formatearQuetzales(cuenta.saldo)}</td>
                      <td className="px-4 py-4 xl:px-6">
                        <StatusBadge estado={cuenta.estado} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </div>
    </AppLayout>
  );
}
