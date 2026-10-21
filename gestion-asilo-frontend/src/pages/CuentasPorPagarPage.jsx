import { Fragment, useEffect, useState } from "react";
import { Banknote, Filter, History, Inbox } from "lucide-react";

import { HistorialPagos } from "../components/cuentas/HistorialPagos";
import { PagoFormModal } from "../components/cuentas/PagoFormModal";
import { Alert } from "../components/ui/Alert";
import { Button } from "../components/ui/Button";
import { Select } from "../components/ui/Input";
import { StatusBadge } from "../components/ui/StatusBadge";
import { TablaSkeleton } from "../components/ui/TablaSkeleton";
import { CONCEPTOS_CUENTA, ETIQUETAS_ESTADO } from "../constants/estados";
import { AppLayout } from "../layouts/AppLayout";
import { mensajeDeError } from "../services/api";
import {
  anularPagoFundacion,
  listarCuentasPorPagar,
  obtenerPagosFundacion,
  registrarPagoFundacion,
} from "../services/cuentaPorPagarService";
import { formatearPeriodo } from "../utils/fechas";
import { formatearQuetzales } from "../utils/formato";

const ENCABEZADO = "px-4 py-4 text-auxiliar font-semibold text-texto-secundario uppercase xl:px-6";
const MONTO = "px-4 py-4 text-right tabular-nums slashed-zero xl:px-6";
const COLUMNAS = 6;

/* En centavos: sumar montos como números de JavaScript deja restos como 0.30000000000000004. */
function centavos(monto) {
  return Math.round(Number(monto) * 100);
}

/* Una deuda es un mes y un servicio: no tiene id hasta su primer pago. */
function claveDe(cuenta) {
  return `${cuenta.periodo}/${cuenta.tipoServicio}`;
}

function nombreDe(cuenta) {
  return `${CONCEPTOS_CUENTA[cuenta.tipoServicio]} de ${formatearPeriodo(cuenta.periodo)}`;
}

/**
 * HU-20 (YS-140). "Cuentas por pagar" del Administrador del Asilo (RF-13): lo
 * que el asilo le debe a la fundación por las consultas, los exámenes y la
 * farmacia de cada mes, con su estado, los pagos que le hace y su histórico.
 *
 * Decidido el 21/10: lo adeudado es la suma del neto de los cargos del mes, con
 * el descuento de la fundación, y está siempre al día; no hay que consolidar.
 * Por tipo de servicio y nunca por nombre (RNF-03): ni pacientes, ni exámenes,
 * ni medicamentos.
 */
export function CuentasPorPagarPage() {
  const [cuentas, setCuentas] = useState(null);
  const [error, setError] = useState(null);
  const [filtro, setFiltro] = useState("");
  const [abiertas, setAbiertas] = useState(() => new Set());
  const [enPago, setEnPago] = useState(null);
  const [aperturas, setAperturas] = useState(0);
  const [pagosRegistrados, setPagosRegistrados] = useState(0);

  useEffect(() => {
    let vigente = true;
    listarCuentasPorPagar()
      .then((lista) => {
        if (vigente) setCuentas(lista);
      })
      .catch((fallo) => {
        if (!vigente) return;
        setCuentas([]);
        setError(mensajeDeError(fallo, "No se pudieron cargar las cuentas por pagar"));
      });
    return () => {
      vigente = false;
    };
  }, []);

  // Un pago o una anulación devuelven la deuda con su saldo y su estado nuevos.
  const reemplazar = (actualizada) =>
    setCuentas((previas) =>
      previas.map((cuenta) => (claveDe(cuenta) === claveDe(actualizada) ? actualizada : cuenta)),
    );

  const alternarPagos = (clave) =>
    setAbiertas((previas) => {
      const siguientes = new Set(previas);
      if (siguientes.has(clave)) siguientes.delete(clave);
      else siguientes.add(clave);
      return siguientes;
    });

  const lista = cuentas ?? [];
  const visibles = filtro ? lista.filter((cuenta) => cuenta.estado === filtro) : lista;
  // El histórico por mes: el backend ya las manda del mes más reciente al más antiguo.
  const meses = [];
  for (const cuenta of visibles) {
    const ultimo = meses.at(-1);
    if (ultimo?.periodo === cuenta.periodo) ultimo.cuentas.push(cuenta);
    else meses.push({ periodo: cuenta.periodo, cuentas: [cuenta] });
  }
  const totalAdeudado = lista.reduce((suma, cuenta) => suma + centavos(cuenta.totalAdeudado), 0) / 100;
  const totalPagado = lista.reduce((suma, cuenta) => suma + centavos(cuenta.totalPagado), 0) / 100;
  const saldoPendiente = lista.reduce((suma, cuenta) => suma + centavos(cuenta.saldo), 0) / 100;

  return (
    <AppLayout migas={["Inicio", "Cuentas por pagar"]}>
      <h1 className="text-h1 font-bold">Cuentas por pagar</h1>
      <p className="mt-2 text-texto-secundario">
        Lo que el asilo le debe a la fundación por las consultas, los exámenes y la farmacia de cada mes: la suma de los
        servicios del mes con el descuento de la fundación. Se actualiza sola con cada servicio.
      </p>

      {error && (
        <div className="mt-6">
          <Alert>{error}</Alert>
        </div>
      )}

      <div className="mt-6">
        {cuentas === null ? (
          <TablaSkeleton etiqueta="Cargando cuentas por pagar…" />
        ) : lista.length === 0 ? (
          !error && (
            <div className="flex flex-col items-center gap-4 rounded-tarjeta border border-divisor bg-superficie px-4 py-16 text-center shadow-tarjeta sm:px-6">
              <Inbox aria-hidden="true" size={24} className="text-texto-secundario" />
              <p className="max-w-md text-texto-secundario">
                Todavía no hay nada que pagarle a la fundación. Las consultas, los exámenes y los medicamentos entregados
                aparecen aquí solos, por mes.
              </p>
            </div>
          )
        ) : (
          <>
            <div className="flex flex-wrap items-end justify-between gap-4 pb-3">
              <p aria-live="polite" className="text-auxiliar text-texto-secundario tabular-nums slashed-zero">
                Adeudado {formatearQuetzales(totalAdeudado)} · Pagado {formatearQuetzales(totalPagado)} · Saldo
                pendiente <span className="font-semibold text-texto">{formatearQuetzales(saldoPendiente)}</span>
              </p>
              <div className="relative w-full sm:w-64">
                <Filter
                  aria-hidden="true"
                  size={20}
                  className="pointer-events-none absolute top-1/2 left-4 -translate-y-1/2 text-texto-secundario"
                />
                <Select
                  value={filtro}
                  onChange={(evento) => setFiltro(evento.target.value)}
                  aria-label="Filtrar las deudas por estado"
                  className="pl-12"
                >
                  <option value="">Todos los estados</option>
                  <option value="PENDIENTE">{ETIQUETAS_ESTADO.PENDIENTE}</option>
                  <option value="PAGADA">{ETIQUETAS_ESTADO.PAGADA}</option>
                </Select>
              </div>
            </div>

            {visibles.length === 0 ? (
              <p className="rounded-tarjeta border border-divisor bg-superficie px-4 py-8 text-center text-texto-secundario shadow-tarjeta">
                Ninguna deuda está {filtro === "PAGADA" ? "pagada" : "pendiente"}.
              </p>
            ) : (
              /* relative: los sr-only de la tabla son absolutos; sin él escapan del recorte (YS-175). */
              <div className="relative overflow-x-auto rounded-tarjeta border border-divisor bg-superficie shadow-tarjeta">
                <table className="w-full min-w-4xl border-collapse text-left">
                  <caption className="sr-only">
                    Lo que el asilo le debe a la fundación por mes y servicio: adeudado, pagado, saldo y estado
                  </caption>
                  <thead>
                    <tr className="border-b border-divisor bg-fondo">
                      <th scope="col" className={ENCABEZADO}>
                        Servicio
                      </th>
                      <th scope="col" className={`${ENCABEZADO} text-right`}>
                        Adeudado
                      </th>
                      <th scope="col" className={`${ENCABEZADO} text-right`}>
                        Pagado
                      </th>
                      <th scope="col" className={`${ENCABEZADO} text-right`}>
                        Saldo
                      </th>
                      <th scope="col" className={ENCABEZADO}>
                        Estado
                      </th>
                      <th scope="col" className={ENCABEZADO}>
                        <span className="sr-only">Acciones</span>
                      </th>
                    </tr>
                  </thead>
                  {meses.map((mes) => (
                    <tbody key={mes.periodo}>
                      <tr className="border-b border-divisor bg-fondo">
                        <th
                          scope="rowgroup"
                          colSpan={COLUMNAS}
                          className="px-4 py-3 text-left font-semibold text-texto first-letter:uppercase xl:px-6"
                        >
                          {formatearPeriodo(mes.periodo)}
                        </th>
                      </tr>
                      {mes.cuentas.map((cuenta) => {
                        const clave = claveDe(cuenta);
                        const abierta = abiertas.has(clave);
                        const idPagos = `pagos-${cuenta.periodo}-${cuenta.tipoServicio}`;
                        return (
                          <Fragment key={clave}>
                            <tr className="border-b border-divisor align-top">
                              <td className="px-4 py-4 xl:px-6">
                                <span className="font-medium">{CONCEPTOS_CUENTA[cuenta.tipoServicio]}</span>
                                <span className="block text-auxiliar text-texto-secundario tabular-nums slashed-zero">
                                  {cuenta.cantidadServicios === 1 ? "1 servicio" : `${cuenta.cantidadServicios} servicios`}
                                </span>
                              </td>
                              <td className={MONTO}>{formatearQuetzales(cuenta.totalAdeudado)}</td>
                              <td className={MONTO}>{formatearQuetzales(cuenta.totalPagado)}</td>
                              <td className={`${MONTO} font-semibold`}>{formatearQuetzales(cuenta.saldo)}</td>
                              <td className="px-4 py-4 xl:px-6">
                                <StatusBadge estado={cuenta.estado} />
                              </td>
                              <td className="px-4 py-4 xl:px-6">
                                <div className="flex justify-end gap-3 whitespace-nowrap">
                                  <Button
                                    variante="secundario"
                                    aria-expanded={abierta}
                                    aria-controls={idPagos}
                                    aria-label={`${abierta ? "Ocultar" : "Ver"} los pagos de ${nombreDe(cuenta)}`}
                                    onClick={() => alternarPagos(clave)}
                                  >
                                    <History aria-hidden="true" size={20} />
                                    {abierta ? "Ocultar pagos" : "Ver pagos"}
                                  </Button>
                                  {centavos(cuenta.saldo) > 0 && (
                                    <Button
                                      variante="secundario"
                                      aria-label={`Registrar un pago de ${nombreDe(cuenta)}`}
                                      onClick={() => {
                                        setAperturas((valor) => valor + 1);
                                        setEnPago(cuenta);
                                      }}
                                    >
                                      <Banknote aria-hidden="true" size={20} />
                                      Registrar pago
                                    </Button>
                                  )}
                                </div>
                              </td>
                            </tr>
                            {abierta && (
                              <tr id={idPagos} className="border-b border-divisor">
                                <td colSpan={COLUMNAS} className="pt-4">
                                  {/* La clave cambia con cada pago nuevo: así el historial abierto lo incluye. */}
                                  <HistorialPagos
                                    key={pagosRegistrados}
                                    clave={clave}
                                    cargar={() => obtenerPagosFundacion(cuenta.periodo, cuenta.tipoServicio)}
                                    anular={anularPagoFundacion}
                                    alCambiar={reemplazar}
                                  />
                                </td>
                              </tr>
                            )}
                          </Fragment>
                        );
                      })}
                    </tbody>
                  ))}
                </table>
              </div>
            )}
          </>
        )}
      </div>

      <PagoFormModal
        abierto={enPago !== null}
        descripcion={enPago ? `A la fundación · ${nombreDe(enPago)}` : undefined}
        montos={
          enPago
            ? {
                etiquetaTotal: "Adeudado",
                total: enPago.totalAdeudado,
                pagado: enPago.totalPagado,
                saldo: enPago.saldo,
              }
            : null
        }
        claveFormulario={aperturas}
        // Con la guarda: el React Compiler lee `enPago.periodo` al memoizar la función, aunque nadie la llame.
        registrar={enPago ? (datos) => registrarPagoFundacion(enPago.periodo, enPago.tipoServicio, datos) : undefined}
        alCerrar={() => setEnPago(null)}
        alRegistrar={(historial) => {
          setEnPago(null);
          setPagosRegistrados((valor) => valor + 1);
          reemplazar(historial.cuenta);
        }}
      />
    </AppLayout>
  );
}
