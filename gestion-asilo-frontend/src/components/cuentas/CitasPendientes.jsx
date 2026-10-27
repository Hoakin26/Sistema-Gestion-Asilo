import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { CircleCheck, Search } from "lucide-react";

import { SERVICIOS_CONTADOS } from "../../constants/estados";
import { erroresPorCampo, mensajeDeError } from "../../services/api";
import { listarServiciosPendientes } from "../../services/servicioPendienteService";
import { formatearFecha } from "../../utils/fechas";
import { formatearQuetzales } from "../../utils/formato";
import { Alert } from "../ui/Alert";
import { Button } from "../ui/Button";
import { FormField } from "../ui/FormField";
import { Input } from "../ui/Input";
import { Paginacion } from "../ui/Paginacion";
import { StatusBadge } from "../ui/StatusBadge";
import { TablaSkeleton } from "../ui/TablaSkeleton";

const ENCABEZADO = "px-4 py-4 text-auxiliar font-semibold text-texto-secundario uppercase";
const MONTO = "px-4 py-4 text-right tabular-nums slashed-zero whitespace-nowrap";

/* "1 consulta · 2 exámenes": lo que generó la cita, por tipo y sin nombres (RNF-03). */
function describirServicios(servicios) {
  return servicios
    .map(({ tipo, cantidad }) => {
      const [singular, plural] = SERVICIOS_CONTADOS[tipo];
      return `${cantidad} ${cantidad === 1 ? singular : plural}`;
    })
    .join(" · ");
}

/**
 * HU-21 (YS-143). Las citas que el familiar todavía no termina de pagar
 * (RF-14), en su pestaña de "Cuentas por cobrar": el paciente, la fecha, lo
 * que costó, lo pagado, el saldo y si va sin pago o con pago parcial. Se
 * filtra por paciente (nombre o DPI) y por rango de fechas de la cita.
 *
 * Decidido el 21/10: lo pagado de la cuenta del mes se reparte entre sus citas
 * en proporción, y las citas de un mes sin consolidar entran sin pago, con su
 * nota. Por tipo de servicio y nunca por nombre (RNF-03).
 *
 * `version` cambia cuando en la otra pestaña se registra un pago o se
 * consolida un mes: entonces vuelve a pedir el listado, con los mismos filtros.
 *
 * OBS-01. Las citas llegan por páginas de 20; los totales, de todas las del
 * filtro. Una búsqueda nueva vuelve a la primera página.
 */
export function CitasPendientes({ version }) {
  const [paciente, setPaciente] = useState("");
  const [desde, setDesde] = useState("");
  const [hasta, setHasta] = useState("");
  // Lo que de verdad se consulta: cambia al pulsar "Buscar", no en cada tecla.
  const [filtros, setFiltros] = useState({ paciente: "", desde: "", hasta: "" });
  const [pagina, setPagina] = useState(0);

  const [datos, setDatos] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [errores, setErrores] = useState({});

  useEffect(() => {
    let vigente = true;
    listarServiciosPendientes(filtros, pagina)
      .then((respuesta) => {
        if (!vigente) return;
        // Un pago en la otra pestaña puede sacar citas: si la página ya no existe, a la última.
        if (pagina > 0 && pagina >= respuesta.citas.totalPages) {
          setPagina(Math.max(respuesta.citas.totalPages - 1, 0));
          return;
        }
        setDatos(respuesta);
        setError(null);
      })
      .catch((fallo) => {
        if (!vigente) return;
        const porCampo = erroresPorCampo(fallo);
        if (porCampo) setErrores(porCampo);
        else setError(mensajeDeError(fallo, "No se pudieron cargar las citas pendientes"));
      })
      .finally(() => {
        if (vigente) setCargando(false);
      });
    return () => {
      vigente = false;
    };
  }, [filtros, version, pagina]);

  const buscar = (evento) => {
    evento.preventDefault();
    const encontrados = {};
    if (desde && hasta && hasta < desde) encontrados.hasta = "La fecha final no puede ser anterior a la inicial";
    setErrores(encontrados);
    if (Object.keys(encontrados).length > 0) return;
    setCargando(true);
    setPagina(0);
    setFiltros({ paciente: paciente.trim(), desde, hasta });
  };

  const conFiltros = Boolean(filtros.paciente || filtros.desde || filtros.hasta);
  const citas = datos?.citas.content ?? [];
  const totalCitas = datos?.citas.totalElements ?? 0;

  return (
    <div className="mt-6">
      <p className="text-texto-secundario">
        Citas con saldo pendiente. Lo pagado en el mes se reparte entre ellas.
      </p>

      <form onSubmit={buscar} noValidate className="mt-6 flex flex-wrap items-start gap-4 sm:[&>button]:mt-8">
        <FormField etiqueta="Paciente" error={errores.paciente}>
          {(campo) => (
            <Input
              {...campo}
              value={paciente}
              onChange={(evento) => setPaciente(evento.target.value)}
              placeholder="Nombre o DPI"
              autoComplete="off"
              className="sm:w-64"
            />
          )}
        </FormField>
        <FormField etiqueta="Desde" error={errores.desde}>
          {(campo) => (
            <Input
              {...campo}
              type="date"
              value={desde}
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
          Buscar
        </Button>
      </form>

      {error && (
        <div className="mt-6">
          <Alert>{error}</Alert>
        </div>
      )}

      <div className="mt-6">
        {cargando && !datos ? (
          <TablaSkeleton etiqueta="Cargando citas pendientes…" />
        ) : citas.length === 0 ? (
          !error && (
            <div className="flex flex-col items-center gap-4 rounded-tarjeta border border-divisor bg-superficie px-4 py-16 text-center shadow-tarjeta sm:px-6">
              <CircleCheck aria-hidden="true" size={24} className="text-exito" />
              <p className="max-w-md text-texto-secundario">
                {conFiltros
                  ? "Ninguna cita con esos filtros tiene saldo pendiente."
                  : "No hay citas pendientes de pago: todas están pagadas completas."}
              </p>
            </div>
          )
        ) : (
          <>
            <p aria-live="polite" className="pb-3 text-auxiliar text-texto-secundario tabular-nums slashed-zero">
              {totalCitas} {totalCitas === 1 ? "cita" : "citas"} · Total {formatearQuetzales(datos.montoTotal)} ·
              Pagado {formatearQuetzales(datos.montoPagado)} · Saldo pendiente{" "}
              <span className="font-semibold text-texto">{formatearQuetzales(datos.saldoPendiente)}</span>
            </p>

            {/* relative: los sr-only de la tabla son absolutos; sin él escapan del recorte (YS-175). */}
            <div className="relative overflow-x-auto rounded-tarjeta border border-divisor bg-superficie shadow-tarjeta">
              <table className="w-full min-w-4xl border-collapse text-left">
                <caption className="sr-only">
                  Citas pendientes de pago: paciente, fecha, servicios, total, pagado, saldo y estado
                </caption>
                <thead>
                  <tr className="border-b border-divisor bg-fondo">
                    <th scope="col" className={`${ENCABEZADO} min-w-56`}>
                      Paciente
                    </th>
                    <th scope="col" className={`${ENCABEZADO} w-px whitespace-nowrap`}>
                      Fecha
                    </th>
                    <th scope="col" className={ENCABEZADO}>
                      Servicios
                    </th>
                    <th scope="col" className={`${ENCABEZADO} w-px text-right whitespace-nowrap`}>
                      Total
                    </th>
                    <th scope="col" className={`${ENCABEZADO} w-px text-right whitespace-nowrap`}>
                      Pagado
                    </th>
                    <th scope="col" className={`${ENCABEZADO} w-px text-right whitespace-nowrap`}>
                      Saldo
                    </th>
                    <th scope="col" className={`${ENCABEZADO} w-px whitespace-nowrap`}>
                      Estado
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {citas.map((cita) => (
                    <tr key={cita.visitaId} className="border-b border-divisor align-top last:border-b-0">
                      <td className="px-4 py-4">
                        {/* El estado de cuenta del paciente vive en su perfil. */}
                        <Link to={`/internos/${cita.paciente.id}`} className="font-medium text-primario hover:underline">
                          {cita.paciente.nombreCompleto}
                        </Link>
                        <span className="block text-auxiliar text-texto-secundario tabular-nums slashed-zero">
                          Solicitud {cita.numeroSolicitud}
                        </span>
                      </td>
                      <td className="px-4 py-4 whitespace-nowrap tabular-nums slashed-zero">
                        {formatearFecha(cita.fecha)}
                      </td>
                      <td className="px-4 py-4">{describirServicios(cita.servicios)}</td>
                      <td className={MONTO}>{formatearQuetzales(cita.montoTotal)}</td>
                      <td className={MONTO}>{formatearQuetzales(cita.montoPagado)}</td>
                      <td className={`${MONTO} font-semibold`}>{formatearQuetzales(cita.saldo)}</td>
                      <td className="px-4 py-4 whitespace-nowrap">
                        <StatusBadge estado={cita.estado} />
                        {cita.vencida && (
                          <span className="mt-1 block text-auxiliar text-error">Cuenta en mora</span>
                        )}
                        {cita.sinConsolidar && (
                          <span className="mt-1 block text-auxiliar text-texto-secundario">Su mes no se ha consolidado</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <Paginacion
              pagina={pagina}
              totalPaginas={datos.citas.totalPages}
              alCambiar={setPagina}
              etiqueta="Páginas de las citas pendientes"
            />
          </>
        )}
      </div>
    </div>
  );
}
