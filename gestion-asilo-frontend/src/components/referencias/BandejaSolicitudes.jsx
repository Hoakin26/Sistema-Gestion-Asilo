import { useEffect, useState } from "react";
import { ArrowDownUp, ChevronLeft, ChevronRight, Eye, Filter, Inbox, Search } from "lucide-react";

import { ESTADOS_SOLICITUD, ETIQUETAS_ESTADO } from "../../constants/estados";
import { mensajeDeError } from "../../services/api";
import { listarSolicitudes } from "../../services/referenciaService";
import { formatearFechaHora } from "../../utils/fechas";
import { Alert } from "../ui/Alert";
import { Button, IconButton } from "../ui/Button";
import { Input, Select } from "../ui/Input";
import { StatusBadge } from "../ui/StatusBadge";
import { TablaSkeleton } from "../ui/TablaSkeleton";
import { SolicitudDetalleModal } from "./SolicitudDetalleModal";

/* Lo bastante para no pedir en cada tecla, poco para que no se sienta lento. */
const ESPERA_BUSQUEDA_MS = 300;

const ENCABEZADO =
  "px-4 py-4 text-auxiliar font-semibold text-texto-secundario uppercase xl:px-6";

/**
 * YS-88. La lista de solicitudes de referencia con su filtro por estado, su
 * paginación y el detalle al seleccionar una (HU-05).
 *
 * La comparten la bandeja de la Fundación y el seguimiento del Médico
 * General: lo que cambia es el filtro con que arranca.
 *
 * YS-166. Además se puede buscar por número, interno o especialidad, y elegir
 * el orden: por defecto las más recientes primero, con la opción de ver las
 * más antiguas primero (pedido del 09/10).
 *
 * - `estadoInicial`: "" para todas, o una clave de DE-01.
 * - `version`: cambia cuando quien la usa sabe que hay datos nuevos (el médico
 *   acaba de crear una solicitud) y la lista tiene que volver a pedirse.
 * - `mensajeVacio`: qué decir cuando no hay ninguna, que no es lo mismo para
 *   la Fundación que para el médico.
 */
export function BandejaSolicitudes({ estadoInicial = "", version = 0, mensajeVacio }) {
  const [estado, setEstado] = useState(estadoInicial);
  const [orden, setOrden] = useState("desc");
  const [busqueda, setBusqueda] = useState("");
  // Lo que de verdad se consulta: la búsqueda, una vez que dejó de escribir.
  const [termino, setTermino] = useState("");
  const [pagina, setPagina] = useState(0);

  const [resultado, setResultado] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);

  const [seleccionada, setSeleccionada] = useState(null);
  const [detalleAbierto, setDetalleAbierto] = useState(false);
  const [aperturas, setAperturas] = useState(0);
  // HU-06. Sube cuando se asigna una cita desde el detalle: la solicitud
  // cambió de estado y la lista tiene que reflejarlo.
  const [recargas, setRecargas] = useState(0);

  useEffect(() => {
    const espera = setTimeout(() => {
      setTermino(busqueda.trim());
      // Una búsqueda nueva empieza siempre en la primera página.
      setPagina(0);
    }, ESPERA_BUSQUEDA_MS);
    return () => clearTimeout(espera);
  }, [busqueda]);

  useEffect(() => {
    // Una respuesta vieja que llega tarde no pisa a la del filtro actual.
    let vigente = true;
    listarSolicitudes({ estado, busqueda: termino, pagina, orden })
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
  }, [estado, termino, pagina, orden, version, recargas]);

  const cambiarEstado = (evento) => {
    setEstado(evento.target.value);
    // Un filtro nuevo empieza siempre en la primera página.
    setPagina(0);
  };

  const cambiarOrden = (evento) => {
    setOrden(evento.target.value);
    setPagina(0);
  };

  const quitarFiltros = () => {
    setEstado("");
    setBusqueda("");
    setTermino("");
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
      <div className="flex flex-wrap gap-4">
        <div className="relative min-w-64 flex-1">
          <Search
            aria-hidden="true"
            size={20}
            className="pointer-events-none absolute top-1/2 left-4 -translate-y-1/2 text-texto-secundario"
          />
          <Input
            type="search"
            value={busqueda}
            onChange={(evento) => setBusqueda(evento.target.value)}
            placeholder="Buscar por número, interno o especialidad…"
            aria-label="Buscar solicitudes por número, interno o especialidad"
            className="pl-12"
          />
        </div>

        <div className="relative w-full sm:w-64">
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

        <div className="relative w-full sm:w-64">
          <ArrowDownUp
            aria-hidden="true"
            size={20}
            className="pointer-events-none absolute top-1/2 left-4 -translate-y-1/2 text-texto-secundario"
          />
          <Select
            value={orden}
            onChange={cambiarOrden}
            aria-label="Ordenar solicitudes por fecha"
            className="pl-12"
          >
            <option value="desc">Más recientes primero</option>
            <option value="asc">Más antiguas primero</option>
          </Select>
        </div>
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
              {termino
                ? "Ninguna solicitud coincide con lo que buscó."
                : estado
                  ? `No hay solicitudes en estado "${ETIQUETAS_ESTADO[estado]}".`
                  : mensajeVacio}
            </p>
            {/* Vacío por el filtro ≠ vacío de verdad, como en usuarios. */}
            {(estado || termino) && (
              <Button variante="secundario" onClick={quitarFiltros}>
                Quitar filtros
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
            {/* relative: los sr-only de la tabla son absolutos; sin él escapan del
                recorte, estiran la página y el teléfono la achica a una esquina (YS-175). */}
            <div className="relative overflow-x-auto rounded-tarjeta border border-divisor bg-superficie shadow-tarjeta">
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
        alAsignar={() => setRecargas((valor) => valor + 1)}
      />
    </div>
  );
}
