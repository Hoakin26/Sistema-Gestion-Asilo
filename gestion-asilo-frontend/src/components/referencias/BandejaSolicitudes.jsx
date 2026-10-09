import { useEffect, useState } from "react";
import { ChevronLeft, ChevronRight, Eye, Filter, Inbox } from "lucide-react";

import { ESTADOS_SOLICITUD, ETIQUETAS_ESTADO } from "../../constants/estados";
import { mensajeDeError } from "../../services/api";
import { listarSolicitudes } from "../../services/referenciaService";
import { formatearFechaHora } from "../../utils/fechas";
import { Alert } from "../ui/Alert";
import { Button, IconButton } from "../ui/Button";
import { Select } from "../ui/Input";
import { StatusBadge } from "../ui/StatusBadge";
import { TablaSkeleton } from "../ui/TablaSkeleton";
import { SolicitudDetalleModal } from "./SolicitudDetalleModal";

const ENCABEZADO =
  "px-4 py-4 text-auxiliar font-semibold text-texto-secundario uppercase xl:px-6";

/**
 * YS-88. La lista de solicitudes de referencia con su filtro por estado, su
 * paginación y el detalle al seleccionar una (HU-05).
 *
 * La comparten la bandeja de la Fundación y el seguimiento del Médico
 * General: lo que cambia es el filtro con que arranca y el orden.
 *
 * - `estadoInicial`: "" para todas, o una clave de DE-01.
 * - `orden`: "asc" (la que más espera primero) o "desc" (la más reciente primero).
 * - `version`: cambia cuando quien la usa sabe que hay datos nuevos (el médico
 *   acaba de crear una solicitud) y la lista tiene que volver a pedirse.
 * - `mensajeVacio`: qué decir cuando no hay ninguna, que no es lo mismo para
 *   la Fundación que para el médico.
 */
export function BandejaSolicitudes({ estadoInicial = "", orden = "asc", version = 0, mensajeVacio }) {
  const [estado, setEstado] = useState(estadoInicial);
  const [pagina, setPagina] = useState(0);

  const [resultado, setResultado] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);

  const [seleccionada, setSeleccionada] = useState(null);
  const [detalleAbierto, setDetalleAbierto] = useState(false);
  const [aperturas, setAperturas] = useState(0);

  useEffect(() => {
    // Una respuesta vieja que llega tarde no pisa a la del filtro actual.
    let vigente = true;
    listarSolicitudes({ estado, pagina, orden })
      .then((datos) => {
        if (!vigente) return;
        setResultado(datos);
        setError(null);
      })
      .catch((fallo) => {
        if (vigente) setError(mensajeDeError(fallo, "No se pudieron cargar las solicitudes"));
      })
      .finally(() => {
        if (vigente) setCargando(false);
      });
    return () => {
      vigente = false;
    };
  }, [estado, pagina, orden, version]);

  const cambiarEstado = (evento) => {
    setEstado(evento.target.value);
    // Un filtro nuevo empieza siempre en la primera página.
    setPagina(0);
  };

  const ver = (solicitud) => {
    setSeleccionada(solicitud);
    setAperturas((valor) => valor + 1);
    setDetalleAbierto(true);
  };

  const solicitudes = resultado?.content ?? [];
  const total = resultado?.totalElements ?? 0;
  const totalPaginas = resultado?.totalPages ?? 0;

  return (
    <div>
      <div className="relative w-full sm:w-80">
        <Filter
          aria-hidden="true"
          size={20}
          className="pointer-events-none absolute top-1/2 left-4 -translate-y-1/2 text-texto-secundario"
        />
        <Select
          value={estado}
          onChange={cambiarEstado}
          aria-label="Filtrar solicitudes por estado"
          className="pl-12"
        >
          <option value="">Todos los estados</option>
          {ESTADOS_SOLICITUD.map((clave) => (
            <option key={clave} value={clave}>
              {ETIQUETAS_ESTADO[clave]}
            </option>
          ))}
        </Select>
      </div>

      {error && (
        <div className="mt-6">
          <Alert>{error}</Alert>
        </div>
      )}

      <div className="mt-6">
        {cargando ? (
          <TablaSkeleton etiqueta="Cargando solicitudes…" />
        ) : solicitudes.length === 0 ? (
          <div className="flex flex-col items-center gap-4 rounded-tarjeta border border-divisor bg-superficie px-4 py-16 text-center shadow-tarjeta sm:px-6">
            <Inbox aria-hidden="true" size={24} className="text-texto-secundario" />
            <p className="max-w-md text-texto-secundario">
              {estado
                ? `No hay solicitudes en estado "${ETIQUETAS_ESTADO[estado]}".`
                : mensajeVacio}
            </p>
            {/* Vacío por el filtro ≠ vacío de verdad, como en usuarios. */}
            {estado && (
              <Button variante="secundario" onClick={() => cambiarEstado({ target: { value: "" } })}>
                Ver todos los estados
              </Button>
            )}
          </div>
        ) : (
          <>
            <p
              aria-live="polite"
              className="pb-3 text-auxiliar text-texto-secundario tabular-nums slashed-zero"
            >
              {total} {total === 1 ? "solicitud" : "solicitudes"}
            </p>

            {/* Cuatro columnas y no siete: el número, la fecha y el estado
                comparten celda, y la especialidad con el motivo. Con una
                columna por dato, en la laptop de 1024px el motivo quedaba en
                una franja tan angosta que no se podía leer. */}
            <div className="overflow-x-auto rounded-tarjeta border border-divisor bg-superficie shadow-tarjeta">
              <table className="w-full min-w-xl border-collapse text-left">
                <caption className="sr-only">
                  Solicitudes de referencia con su estado, interno, especialidad y motivo
                </caption>
                <thead>
                  <tr className="border-b border-divisor bg-fondo">
                    <th scope="col" className={ENCABEZADO}>
                      Solicitud
                    </th>
                    <th scope="col" className={ENCABEZADO}>
                      Interno
                    </th>
                    <th scope="col" className={ENCABEZADO}>
                      Especialidad y motivo
                    </th>
                    <th scope="col" className={ENCABEZADO}>
                      <span className="sr-only">Acciones</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {solicitudes.map((solicitud) => (
                    <tr key={solicitud.id} className="border-b border-divisor align-top last:border-b-0">
                      <td className="px-4 py-4 xl:px-6">
                        {/* Sin partir: "REF-2026-" en una línea y "0001" en
                            otra es un número que nadie puede dictar. */}
                        <span className="block font-medium whitespace-nowrap text-texto tabular-nums slashed-zero">
                          {solicitud.numero}
                        </span>
                        <span className="block text-auxiliar text-texto-secundario tabular-nums slashed-zero">
                          {formatearFechaHora(solicitud.fechaCreacion)}
                        </span>
                        <span className="mt-2 block">
                          <StatusBadge estado={solicitud.estado} />
                        </span>
                      </td>
                      <td className="px-4 py-4 xl:px-6">{solicitud.interno.nombreCompleto}</td>
                      <td className="max-w-80 px-4 py-4 xl:px-6">
                        <span className="block font-medium text-texto">
                          {solicitud.especialidad.nombre}
                        </span>
                        {/* Dos líneas bastan para decidir; el resto, en el detalle. */}
                        <span className="line-clamp-2 text-auxiliar text-texto-secundario">
                          {solicitud.motivo}
                        </span>
                      </td>
                      <td className="px-4 py-4 xl:px-6">
                        <div className="flex justify-end">
                          <IconButton
                            etiqueta={`Ver la solicitud ${solicitud.numero} de ${solicitud.interno.nombreCompleto}`}
                            onClick={() => ver(solicitud)}
                          >
                            <Eye aria-hidden="true" size={20} />
                          </IconButton>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {totalPaginas > 1 && (
              <nav
                aria-label="Páginas de la lista de solicitudes"
                className="mt-4 flex flex-wrap items-center justify-between gap-4"
              >
                <Button
                  variante="secundario"
                  disabled={pagina === 0}
                  onClick={() => setPagina((valor) => valor - 1)}
                >
                  <ChevronLeft aria-hidden="true" size={20} />
                  Anterior
                </Button>
                <p className="text-auxiliar text-texto-secundario tabular-nums slashed-zero">
                  Página {pagina + 1} de {totalPaginas}
                </p>
                <Button
                  variante="secundario"
                  disabled={pagina + 1 >= totalPaginas}
                  onClick={() => setPagina((valor) => valor + 1)}
                >
                  Siguiente
                  <ChevronRight aria-hidden="true" size={20} />
                </Button>
              </nav>
            )}
          </>
        )}
      </div>

      <SolicitudDetalleModal
        abierto={detalleAbierto}
        solicitud={seleccionada}
        claveApertura={aperturas}
        alCerrar={() => setDetalleAbierto(false)}
      />
    </div>
  );
}
