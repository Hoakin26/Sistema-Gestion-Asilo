import { useEffect, useState } from "react";
import { Receipt, Search } from "lucide-react";

import { CONCEPTOS_CUENTA, SERVICIOS } from "../../constants/estados";
import { erroresPorCampo, mensajeDeError } from "../../services/api";
import { obtenerEstadoCuenta } from "../../services/cuentaService";
import { formatearFecha, formatearPeriodo, hoyISO, inicioDeMesISO } from "../../utils/fechas";
import { formatearQuetzales } from "../../utils/formato";
import { Alert } from "../ui/Alert";
import { Button } from "../ui/Button";
import { FormField } from "../ui/FormField";
import { Input } from "../ui/Input";
import { StatusBadge } from "../ui/StatusBadge";
import { Barra } from "../ui/TablaSkeleton";

const TARJETA = "rounded-tarjeta border border-divisor bg-superficie p-4 shadow-tarjeta sm:p-6";

const ENCABEZADO = "px-4 py-3 text-auxiliar font-semibold text-texto-secundario uppercase";
const MONTO = "px-4 py-3 text-right tabular-nums slashed-zero";

/**
 * HU-18 (YS-134). El estado de cuenta del interno, dentro de su vista
 * administrativa: lo que se le cobra al familiar entre dos fechas, desglosado
 * por servicio, con el detalle de cada cargo, las cuentas mensuales que caen en
 * el rango y lo que debe en total.
 *
 * Por tipo de servicio y nunca por nombre (RNF-03): el backend no manda ni la
 * especialidad, ni el examen, ni el medicamento, y aquí tampoco se piden.
 */
export function EstadoCuentaInterno({ internoId }) {
  const [desde, setDesde] = useState(inicioDeMesISO);
  const [hasta, setHasta] = useState(hoyISO);
  // El rango que de verdad se consulta: cambia al pulsar "Consultar", no en cada tecla.
  const [rango, setRango] = useState(() => ({ desde: inicioDeMesISO(), hasta: hoyISO() }));

  const [estado, setEstado] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [errores, setErrores] = useState({});

  useEffect(() => {
    let vigente = true;
    obtenerEstadoCuenta(internoId, rango)
      .then((datos) => {
        if (!vigente) return;
        setEstado(datos);
        setError(null);
      })
      .catch((fallo) => {
        if (!vigente) return;
        const porCampo = erroresPorCampo(fallo);
        if (porCampo) setErrores(porCampo);
        else setError(mensajeDeError(fallo, "No se pudo cargar el estado de cuenta"));
      })
      .finally(() => {
        if (vigente) setCargando(false);
      });
    return () => {
      vigente = false;
    };
  }, [internoId, rango]);

  const consultar = (evento) => {
    evento.preventDefault();
    const encontrados = {};
    if (!desde) encontrados.desde = "Indique la fecha inicial";
    if (!hasta) encontrados.hasta = "Indique la fecha final";
    else if (desde && hasta < desde) encontrados.hasta = "La fecha final no puede ser anterior a la inicial";
    setErrores(encontrados);
    if (Object.keys(encontrados).length > 0) return;
    setCargando(true);
    setRango({ desde, hasta });
  };

  return (
    <section aria-labelledby="titulo-estado-cuenta" className={TARJETA}>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 id="titulo-estado-cuenta" className="flex items-center gap-2 text-h3 font-semibold">
            <Receipt aria-hidden="true" size={24} className="text-texto-secundario" />
            Estado de cuenta
          </h2>
          <p className="mt-1 text-auxiliar text-texto-secundario">
            Lo que se le cobra al familiar, por servicio. Sin nombres de exámenes ni de medicamentos.
          </p>
        </div>
        {/* Lo que debe en total, por todas sus cuentas: no depende del rango. */}
        <div className="rounded-tarjeta bg-fondo px-4 py-3 text-right">
          <p className="text-auxiliar text-texto-secundario">Saldo pendiente</p>
          <p className="text-h3 font-semibold tabular-nums slashed-zero">
            {estado ? formatearQuetzales(estado.saldoTotal) : "—"}
          </p>
        </div>
      </div>

      <form onSubmit={consultar} noValidate className="mt-6 flex flex-wrap items-end gap-4">
        <FormField etiqueta="Desde" error={errores.desde}>
          {(campo) => (
            <Input
              {...campo}
              type="date"
              value={desde}
              max={hoyISO()}
              onChange={(evento) => setDesde(evento.target.value)}
              className="sm:w-48"
            />
          )}
        </FormField>
        <FormField etiqueta="Hasta" error={errores.hasta}>
          {(campo) => (
            <Input
              {...campo}
              type="date"
              value={hasta}
              onChange={(evento) => setHasta(evento.target.value)}
              className="sm:w-48"
            />
          )}
        </FormField>
        <Button type="submit" variante="secundario" cargando={cargando}>
          {!cargando && <Search aria-hidden="true" size={20} />}
          Consultar
        </Button>
      </form>

      {error && (
        <div className="mt-6">
          <Alert>{error}</Alert>
        </div>
      )}

      {cargando && !estado ? (
        <div role="status" className="mt-6 flex flex-col gap-3">
          <span className="sr-only">Cargando el estado de cuenta…</span>
          <Barra className="w-64" />
          <Barra className="w-48" />
        </div>
      ) : (
        estado && <Detalle estado={estado} />
      )}
    </section>
  );
}

function Detalle({ estado }) {
  return (
    <div className="mt-6 flex flex-col gap-6">
      <p className="text-texto-secundario">
        Del {formatearFecha(estado.desde)} al {formatearFecha(estado.hasta)}.
      </p>

      {/* Un cargo de un mes sin consolidar todavía no está en ninguna cuenta. */}
      {estado.cargosSinConsolidar > 0 && (
        <Alert tipo="info" rol="status">
          {estado.cargosSinConsolidar === 1
            ? "1 cargo de estas fechas todavía no se consolida"
            : `${estado.cargosSinConsolidar} cargos de estas fechas todavía no se consolidan`}
          : entran en la cuenta del familiar al consolidar su mes en «Cuentas por cobrar».
        </Alert>
      )}

      <div>
        <h3 className="font-semibold text-texto">Desglose por servicio</h3>
        <div className="relative mt-2 overflow-x-auto rounded-tarjeta border border-divisor">
          <table className="w-full min-w-lg border-collapse text-left">
            <caption className="sr-only">Lo cobrado por servicio: cantidad, monto original, descuento y neto</caption>
            <thead>
              <tr className="border-b border-divisor bg-fondo">
                <th scope="col" className={ENCABEZADO}>
                  Concepto
                </th>
                <th scope="col" className={`${ENCABEZADO} text-right`}>
                  Cantidad
                </th>
                <th scope="col" className={`${ENCABEZADO} text-right`}>
                  Original
                </th>
                <th scope="col" className={`${ENCABEZADO} text-right`}>
                  Descuento
                </th>
                <th scope="col" className={`${ENCABEZADO} text-right`}>
                  A pagar
                </th>
              </tr>
            </thead>
            <tbody>
              {estado.desglose.map((fila) => (
                <tr key={fila.concepto} className="border-b border-divisor">
                  <th scope="row" className="px-4 py-3 font-medium text-texto">
                    {CONCEPTOS_CUENTA[fila.concepto] ?? fila.concepto}
                  </th>
                  <td className={MONTO}>{fila.cantidad}</td>
                  <td className={MONTO}>{formatearQuetzales(fila.original)}</td>
                  <td className={MONTO}>{formatearQuetzales(fila.descuento)}</td>
                  <td className={MONTO}>{formatearQuetzales(fila.neto)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr className="bg-fondo">
                <th scope="row" className="px-4 py-3 font-semibold text-texto">
                  Total del período
                </th>
                <td className={MONTO} />
                <td className={`${MONTO} font-semibold`}>{formatearQuetzales(estado.totalOriginal)}</td>
                <td className={`${MONTO} font-semibold`}>{formatearQuetzales(estado.totalDescuentos)}</td>
                <td className={`${MONTO} font-semibold`}>{formatearQuetzales(estado.totalAPagar)}</td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>

      <div>
        <h3 className="font-semibold text-texto">Detalle de cargos</h3>
        {estado.cargos.length === 0 ? (
          <p className="mt-2 text-texto-secundario">No hay cargos de servicios en estas fechas.</p>
        ) : (
          <div className="relative mt-2 overflow-x-auto rounded-tarjeta border border-divisor">
            <table className="w-full min-w-2xl border-collapse text-left">
              <caption className="sr-only">Cada cargo con su fecha, servicio, solicitud, montos y cuenta</caption>
              <thead>
                <tr className="border-b border-divisor bg-fondo">
                  <th scope="col" className={ENCABEZADO}>
                    Fecha
                  </th>
                  <th scope="col" className={ENCABEZADO}>
                    Servicio
                  </th>
                  <th scope="col" className={ENCABEZADO}>
                    Solicitud
                  </th>
                  <th scope="col" className={`${ENCABEZADO} text-right`}>
                    Original
                  </th>
                  <th scope="col" className={`${ENCABEZADO} text-right`}>
                    Descuento
                  </th>
                  <th scope="col" className={`${ENCABEZADO} text-right`}>
                    A pagar
                  </th>
                  <th scope="col" className={ENCABEZADO}>
                    Cuenta
                  </th>
                </tr>
              </thead>
              <tbody>
                {estado.cargos.map((cargo) => (
                  <tr key={cargo.id} className="border-b border-divisor last:border-b-0">
                    <td className="px-4 py-3 whitespace-nowrap tabular-nums slashed-zero">
                      {formatearFecha(cargo.fecha)}
                    </td>
                    <td className="px-4 py-3">{SERVICIOS[cargo.tipo] ?? cargo.tipo}</td>
                    <td className="px-4 py-3 whitespace-nowrap tabular-nums slashed-zero">{cargo.numeroSolicitud}</td>
                    <td className={MONTO}>{formatearQuetzales(cargo.montoOriginal)}</td>
                    <td className={MONTO}>{formatearQuetzales(cargo.descuento)}</td>
                    <td className={MONTO}>{formatearQuetzales(cargo.montoNeto)}</td>
                    <td className="px-4 py-3 whitespace-nowrap text-texto-secundario">
                      {cargo.periodo ? formatearPeriodo(cargo.periodo) : "Sin consolidar"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div>
        <h3 className="font-semibold text-texto">Cuentas mensuales</h3>
        {estado.cuentas.length === 0 ? (
          <p className="mt-2 text-texto-secundario">
            Ningún mes de estas fechas se ha consolidado todavía. Se consolida en «Cuentas por cobrar».
          </p>
        ) : (
          <ul className="mt-2 divide-y divide-divisor rounded-tarjeta border border-divisor">
            {estado.cuentas.map((cuenta) => (
              <li key={cuenta.id} className="flex flex-wrap items-center justify-between gap-x-6 gap-y-2 px-4 py-3">
                <span className="font-medium text-texto first-letter:uppercase">{formatearPeriodo(cuenta.periodo)}</span>
                <span className="text-auxiliar text-texto-secundario">A cargo de {cuenta.familiar.nombreCompleto}</span>
                <span className="tabular-nums slashed-zero">
                  Total {formatearQuetzales(cuenta.totalAPagar)} · Saldo {formatearQuetzales(cuenta.saldo)}
                </span>
                <StatusBadge estado={cuenta.estado} />
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
