import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Banknote, Calculator, Inbox } from "lucide-react";

import { CitasPendientes } from "../components/cuentas/CitasPendientes";
import { DescuentosVigentes } from "../components/cuentas/DescuentosVigentes";
import { PagoCuentaMensualModal } from "../components/cuentas/PagoFormModal";
import { Alert } from "../components/ui/Alert";
import { Button } from "../components/ui/Button";
import { FormField } from "../components/ui/FormField";
import { Input } from "../components/ui/Input";
import { StatusBadge } from "../components/ui/StatusBadge";
import { TablaSkeleton } from "../components/ui/TablaSkeleton";
import { Tabs } from "../components/ui/Tabs";
import { useToast } from "../hooks/useToast";
import { AppLayout } from "../layouts/AppLayout";
import { erroresPorCampo, mensajeDeError } from "../services/api";
import { consolidarCuentas, listarCuentas } from "../services/cuentaService";
import { formatearFechaCorta, formatearFechaHora, formatearPeriodo, mesActualISO } from "../utils/fechas";
import { formatearQuetzales } from "../utils/formato";

// YS-179. Sin el xl:px-6 de las demás tablas: con seis columnas, esos 16 px de más por lado
// eran justo lo que le faltaba al interno para no partirse en cuatro líneas.
const ENCABEZADO = "px-4 py-4 text-auxiliar font-semibold text-texto-secundario uppercase";
const MONTO = "px-4 py-4 text-right tabular-nums slashed-zero";

/* En centavos: sumar y restar montos como números de JavaScript deja restos como 0.30000000000000004. */
function centavos(monto) {
  return Math.round(Number(monto) * 100);
}

/**
 * HU-18 (YS-134). "Cuentas por cobrar" del Administrador del Asilo (CU-006,
 * paso 1): la cuenta mensual de cada familiar, con la cuota base del asilo más
 * las consultas, los exámenes y la farmacia del mes. Se consolida desde aquí;
 * el detalle de cada una está en el perfil del interno. HU-34 (YS-177): aquí
 * también se registra el pago del familiar, y la fila toma el saldo y el
 * estado nuevos; el historial de pagos está en el perfil.
 *
 * HU-21 (YS-143). En su segunda pestaña, las citas que el familiar todavía no
 * termina de pagar (decidido el 21/10: aquí y no como módulo aparte, para que
 * el menú siga siendo el del diseño). La pestaña va en la URL (`?vista=`), así
 * que volver desde el perfil de un interno regresa a la misma.
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
  const [enPago, setEnPago] = useState(null);
  const [aperturas, setAperturas] = useState(0);

  const [parametros, setParametros] = useSearchParams();
  const pestana = parametros.get("vista") === "pendientes" ? "pendientes" : "cuentas";
  // Las citas pendientes se piden la primera vez que se abre su pestaña, no al entrar.
  const [pendientesVistas, setPendientesVistas] = useState(pestana === "pendientes");
  // Un pago o una consolidación cambian lo pendiente: la pestaña vuelve a pedirlo.
  const [cambios, setCambios] = useState(0);
  const cambiarPestana = (clave) => {
    if (clave === "pendientes") setPendientesVistas(true);
    setParametros(clave === "pendientes" ? { vista: "pendientes" } : {}, { replace: true });
  };

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
      setCambios((valor) => valor + 1);
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

  const alRegistrarPago = (historial) => {
    setEnPago(null);
    setCambios((valor) => valor + 1);
    setCuentas((previas) => previas.map((cuenta) => (cuenta.id === historial.cuenta.id ? historial.cuenta : cuenta)));
  };

  const lista = cuentas ?? [];
  const totalACobrar = lista.reduce((suma, cuenta) => suma + centavos(cuenta.totalAPagar), 0) / 100;
  const totalPagado = lista.reduce((suma, cuenta) => suma + centavos(cuenta.totalPagado), 0) / 100;
  const saldoPendiente = lista.reduce((suma, cuenta) => suma + centavos(cuenta.saldo), 0) / 100;

  return (
    <AppLayout migas={["Inicio", "Cuentas por cobrar"]}>
      <h1 className="text-h1 font-bold">Cuentas por cobrar</h1>
      <p className="mt-2 text-texto-secundario">
        La cuenta mensual de cada familiar: la cuota base del asilo más las consultas, los exámenes y la farmacia del
        mes. Y las citas que todavía no se terminan de pagar.
      </p>

      <div className="mt-6">
        <Tabs
          etiqueta="Cuentas por cobrar"
          activa={pestana}
          alCambiar={cambiarPestana}
          pestanas={[
            {
              clave: "cuentas",
              etiqueta: "Cuentas del mes",
              contenido: (
                <>
                  <div className="mt-6 flex flex-wrap items-start gap-4 sm:[&>button]:mt-8">
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
                    Suma la cuota base y los cargos del mes de cada interno, con los descuentos de la fundación vigentes en la
                    fecha de cada servicio. Puede volver a consolidar un mes: se recalcula sin duplicar nada. Si una cuenta ya
                    tiene pagos, lo cobrado se queda como estaba y solo se le suman los cargos nuevos.
                  </p>

                  {/* HU-19 (YS-137). El asilo los consulta; los define la fundación. */}
                  <DescuentosVigentes />

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
                          {formatearQuetzales(totalACobrar)} · Pagado {formatearQuetzales(totalPagado)} · Saldo pendiente{" "}
                          {formatearQuetzales(saldoPendiente)}
                        </p>

                        {/* relative: los sr-only de la tabla son absolutos; sin él escapan del
                            recorte, estiran la página y el teléfono la achica a una esquina (YS-175). */}
                        <div className="relative overflow-x-auto rounded-tarjeta border border-divisor bg-superficie shadow-tarjeta">
                          {/* YS-179. Seis columnas y no diez: el familiar va bajo el interno, el desglose en una
                              sola columna y lo pagado bajo el total, para que el botón quepa sin desplazarse.
                              Las de montos, estado y acción, con w-px: toman lo justo y el resto queda para el
                              interno, que es la que tiene texto largo. */}
                          <table className="w-full min-w-4xl border-collapse text-left">
                            <caption className="sr-only">
                              Cuentas de {periodo && formatearPeriodo(periodo)}: interno y familiar responsable, desglose, total,
                              pagado, saldo y estado
                            </caption>
                            <thead>
                              <tr className="border-b border-divisor bg-fondo">
                                {/* Con un mínimo: cuando la tabla no cabe y se desplaza, el navegador encogería
                                    esta columna hasta la palabra más larga. */}
                                <th scope="col" className={`${ENCABEZADO} min-w-56`}>
                                  Interno
                                </th>
                                <th scope="col" className={`${ENCABEZADO} w-px text-right whitespace-nowrap`}>
                                  Desglose
                                </th>
                                <th scope="col" className={`${ENCABEZADO} w-px text-right whitespace-nowrap`}>
                                  Total a pagar
                                </th>
                                <th scope="col" className={`${ENCABEZADO} w-px text-right whitespace-nowrap`}>
                                  Saldo
                                </th>
                                <th scope="col" className={`${ENCABEZADO} w-px whitespace-nowrap`}>
                                  Estado
                                </th>
                                <th scope="col" className={`${ENCABEZADO} w-px`}>
                                  <span className="sr-only">Acciones</span>
                                </th>
                              </tr>
                            </thead>
                            <tbody>
                              {lista.map((cuenta) => (
                                <tr key={cuenta.id} className="border-b border-divisor align-top last:border-b-0">
                                  <td className="px-4 py-4">
                                    {/* El estado de cuenta desglosado vive en el perfil del interno. */}
                                    <Link
                                      to={`/internos/${cuenta.interno.id}`}
                                      className="font-medium text-primario hover:underline"
                                    >
                                      {cuenta.interno.nombreCompleto}
                                    </Link>
                                    <span className="block text-auxiliar text-texto-secundario">
                                      A cargo de {cuenta.familiar.nombreCompleto}
                                    </span>
                                    <span className="block text-auxiliar text-texto-secundario tabular-nums slashed-zero">
                                      Consolidada: {formatearFechaHora(cuenta.fechaConsolidacion)}
                                    </span>
                                  </td>
                                  <td className="px-4 py-4">
                                    {/* HU-19: el costo original y lo que descontó la fundación; el total ya es neto. */}
                                    <dl className="ml-auto grid w-max grid-cols-[auto_auto] gap-x-4 text-auxiliar tabular-nums slashed-zero">
                                      <dt className="text-texto-secundario">Cuota base</dt>
                                      <dd className="text-right">{formatearQuetzales(cuenta.cuotaBase)}</dd>
                                      <dt className="text-texto-secundario">Servicios</dt>
                                      <dd className="text-right">{formatearQuetzales(cuenta.totalServicios)}</dd>
                                      <dt className="text-texto-secundario">Descuento</dt>
                                      <dd className="text-right">
                                        {centavos(cuenta.totalDescuentos) > 0
                                          ? `− ${formatearQuetzales(cuenta.totalDescuentos)}`
                                          : formatearQuetzales(cuenta.totalDescuentos)}
                                      </dd>
                                    </dl>
                                  </td>
                                  <td className={MONTO}>
                                    <span className="font-semibold">{formatearQuetzales(cuenta.totalAPagar)}</span>
                                    <span className="block text-auxiliar whitespace-nowrap text-texto-secundario">
                                      Pagado {formatearQuetzales(cuenta.totalPagado)}
                                    </span>
                                  </td>
                                  <td className={`${MONTO} font-semibold`}>{formatearQuetzales(cuenta.saldo)}</td>
                                  <td className="px-4 py-4">
                                    <StatusBadge estado={cuenta.estado} />
                                    {/* HU-34 (YS-176). Vence al final del mes siguiente; pasado eso, en mora mientras haya saldo. */}
                                    {centavos(cuenta.saldo) > 0 && (
                                      <span className="mt-1 block text-auxiliar whitespace-nowrap text-texto-secundario tabular-nums slashed-zero">
                                        Vence el {formatearFechaCorta(cuenta.fechaVencimiento)}
                                      </span>
                                    )}
                                  </td>
                                  <td className="px-4 py-4 text-right whitespace-nowrap">
                                    {centavos(cuenta.saldo) > 0 && (
                                      <Button
                                        variante="secundario"
                                        onClick={() => {
                                          setAperturas((valor) => valor + 1);
                                          setEnPago(cuenta);
                                        }}
                                        aria-label={`Registrar un pago de ${cuenta.interno.nombreCompleto}`}
                                      >
                                        <Banknote aria-hidden="true" size={20} />
                                        Registrar pago
                                      </Button>
                                    )}
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      </>
                    )}
                  </div>
                </>
              ),
            },
            {
              clave: "pendientes",
              etiqueta: "Citas pendientes",
              contenido: pendientesVistas && <CitasPendientes version={cambios} />,
            },
          ]}
        />
      </div>

      <PagoCuentaMensualModal
        abierto={enPago !== null}
        cuenta={enPago}
        claveFormulario={aperturas}
        alCerrar={() => setEnPago(null)}
        alRegistrar={alRegistrarPago}
      />
    </AppLayout>
  );
}
