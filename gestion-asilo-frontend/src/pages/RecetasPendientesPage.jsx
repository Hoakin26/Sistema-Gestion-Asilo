import { useEffect, useState } from "react";
import { ChevronLeft, ChevronRight, Eye, Filter, Inbox, PackageOpen, Search } from "lucide-react";

import { DespacharRecetaModal } from "../components/farmacia/DespacharRecetaModal";
import { ListaMedicamentos } from "../components/visitas/RecetaVisita";
import { Alert } from "../components/ui/Alert";
import { Button } from "../components/ui/Button";
import { Input, Select } from "../components/ui/Input";
import { Modal } from "../components/ui/Modal";
import { StatusBadge } from "../components/ui/StatusBadge";
import { TablaSkeleton } from "../components/ui/TablaSkeleton";
import { ESTADOS_RECETA, ETIQUETAS_ESTADO } from "../constants/estados";
import { AppLayout } from "../layouts/AppLayout";
import { mensajeDeError } from "../services/api";
import { listarRecetas } from "../services/farmaciaService";
import { formatearFechaHora } from "../utils/fechas";

/* Lo bastante para no pedir en cada tecla, poco para que no se sienta lento. */
const ESPERA_BUSQUEDA_MS = 300;

const ENCABEZADO =
  "px-4 py-4 text-auxiliar font-semibold text-texto-secundario uppercase xl:px-6";

/* Los medicamentos que se ven en la fila; el resto, en el detalle. */
const MEDICAMENTOS_EN_FILA = 3;

/*
 * El filtro con que arranca: lo que todavía hay que entregar. Una receta
 * parcialmente entregada o con medicamento agotado sigue siendo trabajo de la
 * farmacia: cuando llegan existencias se vuelve a despachar (DE-03, HU-16).
 */
const POR_DESPACHAR = "POR_DESPACHAR";
const ESTADOS_POR_DESPACHAR = ["PENDIENTE_DESPACHO", "PARCIALMENTE_ENTREGADA", "MEDICAMENTO_AGOTADO"];

/* YS-127. El historial de despachos: las recetas con alguna entrega (decidido el 19/10). */
const ESTADOS_DESPACHADOS = ["PARCIALMENTE_ENTREGADA", "ENTREGADA"];

function estadosDelFiltro(filtro) {
  if (filtro === POR_DESPACHAR) return ESTADOS_POR_DESPACHAR;
  return filtro ? [filtro] : [];
}

function unidades(cantidad) {
  return `${cantidad} ${cantidad === 1 ? "unidad" : "unidades"}`;
}

/**
 * HU-15 (YS-124). La bandeja de la farmacia (RF-11): las recetas que emitieron
 * los especialistas, con el paciente, los medicamentos con su dosis y
 * cantidad, quién la recetó y su estado (DE-03). Se busca por el nombre o el
 * DPI del paciente y se filtra por estado; el detalle de cada receta trae
 * además la forma de aplicación y las indicaciones generales.
 *
 * El diagnóstico no aparece en ninguna parte, a propósito: la farmacia
 * despacha la receta, no la consulta, y el backend tampoco lo manda.
 *
 * HU-16 (YS-127). Desde cada receta por despachar se despacha. Con
 * `modo="despachos"` es el módulo "Despacho de medicamentos": la misma
 * bandeja, solo con las recetas que ya tienen entregas, del despacho más
 * reciente al más antiguo (decidido el 19/10), como el registro de resultados
 * del laboratorio.
 */
export function RecetasPendientesPage({ modo = "bandeja" }) {
  const enDespachos = modo === "despachos";

  const [filtro, setFiltro] = useState(enDespachos ? "" : POR_DESPACHAR);
  const [busqueda, setBusqueda] = useState("");
  // Lo que de verdad se consulta: la búsqueda, una vez que dejó de escribir.
  const [termino, setTermino] = useState("");
  const [pagina, setPagina] = useState(0);

  const [resultado, setResultado] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [recargas, setRecargas] = useState(0);
  const [enDetalle, setEnDetalle] = useState(null);
  // La receta que se despacha; las aperturas remontan el formulario limpio.
  const [enDespacho, setEnDespacho] = useState(null);
  const [aperturasDespacho, setAperturasDespacho] = useState(0);

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
    listarRecetas({
      estados: enDespachos ? ESTADOS_DESPACHADOS : estadosDelFiltro(filtro),
      paciente: termino,
      pagina,
      orden: enDespachos ? "fechaDespacho,desc" : undefined,
    })
      .then((datos) => {
        if (!vigente) return;
        setResultado(datos);
        setError(null);
      })
      .catch((fallo) => {
        if (vigente) setError(mensajeDeError(fallo, "No se pudieron cargar las recetas"));
      })
      .finally(() => {
        if (vigente) setCargando(false);
      });
    return () => {
      vigente = false;
    };
  }, [filtro, termino, pagina, recargas, enDespachos]);

  const cambiarFiltro = (evento) => {
    setFiltro(evento.target.value);
    setPagina(0);
  };

  const quitarFiltros = () => {
    setFiltro("");
    setBusqueda("");
    setTermino("");
    setPagina(0);
  };

  const abrirDespacho = (receta) => {
    setAperturasDespacho((valor) => valor + 1);
    setEnDespacho(receta);
  };

  const recetas = resultado?.content ?? [];
  const total = resultado?.totalElements ?? 0;
  const totalPaginas = resultado?.totalPages ?? 0;

  return (
    <AppLayout migas={["Inicio", enDespachos ? "Despacho de medicamentos" : "Recetas pendientes"]}>
      <h1 className="text-h1 font-bold">{enDespachos ? "Despacho de medicamentos" : "Recetas pendientes"}</h1>
      <p className="mt-2 text-texto-secundario">
        {enDespachos
          ? "Recetas que la farmacia ya despachó, del despacho más reciente al más antiguo."
          : "Recetas que emitieron los especialistas, de la más antigua a la más reciente."}
      </p>

      <div className="mt-6 flex flex-wrap gap-4">
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
            placeholder="Buscar paciente por nombre o DPI…"
            aria-label="Buscar recetas por nombre o DPI del paciente"
            className="pl-12"
          />
        </div>

        <div className={`relative w-full sm:w-64 ${enDespachos ? "hidden" : ""}`}>
          <Filter
            aria-hidden="true"
            size={20}
            className="pointer-events-none absolute top-1/2 left-4 -translate-y-1/2 text-texto-secundario"
          />
          <Select value={filtro} onChange={cambiarFiltro} aria-label="Filtrar recetas por estado" className="pl-12">
            <option value={POR_DESPACHAR}>Por despachar</option>
            {ESTADOS_RECETA.map((clave) => (
              <option key={clave} value={clave}>
                {ETIQUETAS_ESTADO[clave]}
              </option>
            ))}
            <option value="">Todos los estados</option>
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
          <TablaSkeleton etiqueta="Cargando recetas…" />
        ) : recetas.length === 0 ? (
          <div className="flex flex-col items-center gap-4 rounded-tarjeta border border-divisor bg-superficie px-4 py-16 text-center shadow-tarjeta sm:px-6">
            <Inbox aria-hidden="true" size={24} className="text-texto-secundario" />
            <p className="max-w-md text-texto-secundario">
              {termino
                ? "Ninguna receta coincide con el paciente que buscó."
                : enDespachos
                  ? "Todavía no hay recetas despachadas."
                  : filtro === POR_DESPACHAR
                    ? "No hay recetas por despachar."
                    : filtro
                      ? `No hay recetas en estado "${ETIQUETAS_ESTADO[filtro]}".`
                      : "Todavía no hay recetas."}
            </p>
            {/* Vacío por el filtro ≠ vacío de verdad, como en las otras bandejas. */}
            {(termino || (filtro && !enDespachos)) && (
              <Button variante="secundario" onClick={quitarFiltros}>
                Ver todas las recetas
              </Button>
            )}
          </div>
        ) : (
          <>
            <p
              aria-live="polite"
              className="pb-3 text-auxiliar text-texto-secundario tabular-nums slashed-zero"
            >
              {total} {total === 1 ? "receta" : "recetas"}
            </p>

            {/* relative: los sr-only de la tabla son absolutos; sin él escapan del
                recorte, estiran la página y el teléfono la achica a una esquina (YS-175). */}
            <div className="relative overflow-x-auto rounded-tarjeta border border-divisor bg-superficie shadow-tarjeta">
              <table className="w-full min-w-2xl border-collapse text-left">
                <caption className="sr-only">
                  Recetas con el paciente, los medicamentos con su dosis y cantidad, quién las recetó y su estado
                </caption>
                <thead>
                  <tr className="border-b border-divisor bg-fondo">
                    <th scope="col" className={ENCABEZADO}>
                      Paciente
                    </th>
                    <th scope="col" className={ENCABEZADO}>
                      Medicamentos
                    </th>
                    <th scope="col" className={ENCABEZADO}>
                      Recetada por
                    </th>
                    <th scope="col" className={ENCABEZADO}>
                      Estado
                    </th>
                    <th scope="col" className={ENCABEZADO}>
                      <span className="sr-only">Acciones</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {recetas.map((receta) => {
                    const restantes = receta.medicamentos.length - MEDICAMENTOS_EN_FILA;
                    return (
                      <tr key={receta.id} className="border-b border-divisor align-top last:border-b-0">
                        <td className="px-4 py-4 font-medium text-texto xl:px-6">{receta.paciente.nombreCompleto}</td>
                        <td className="max-w-96 px-4 py-4 xl:px-6">
                          <ul className="flex flex-col gap-1">
                            {receta.medicamentos.slice(0, MEDICAMENTOS_EN_FILA).map((m) => (
                              <li key={m.id}>
                                <span className="font-medium text-texto">{m.medicamento}</span>
                                <span className="text-texto-secundario tabular-nums slashed-zero">
                                  {" "}
                                  · {m.dosis} · {unidades(m.cantidad)}
                                </span>
                              </li>
                            ))}
                          </ul>
                          {restantes > 0 && (
                            <span className="mt-1 block text-auxiliar text-texto-secundario tabular-nums slashed-zero">
                              y {restantes} {restantes === 1 ? "medicamento más" : "medicamentos más"}
                            </span>
                          )}
                        </td>
                        <td className="px-4 py-4 xl:px-6">
                          <span className="block text-texto">{receta.medico.nombreCompleto}</span>
                          <span className="block text-auxiliar text-texto-secundario tabular-nums slashed-zero">
                            {formatearFechaHora(receta.fechaEmision)}
                          </span>
                        </td>
                        <td className="px-4 py-4 xl:px-6">
                          <StatusBadge estado={receta.estado} />
                          {receta.fechaDespacho && (
                            <span className="mt-1 block text-auxiliar text-texto-secundario tabular-nums slashed-zero">
                              Última entrega: {formatearFechaHora(receta.fechaDespacho)}
                            </span>
                          )}
                        </td>
                        <td className="px-4 py-4 xl:px-6">
                          <div className="flex flex-wrap justify-end gap-2 whitespace-nowrap">
                            {/* HU-16: también las incompletas, cuando llegan existencias (DE-03). */}
                            {ESTADOS_POR_DESPACHAR.includes(receta.estado) && (
                              <Button
                                onClick={() => abrirDespacho(receta)}
                                aria-label={`Despachar la receta de ${receta.paciente.nombreCompleto}`}
                              >
                                <PackageOpen aria-hidden="true" size={20} />
                                Despachar
                              </Button>
                            )}
                            <Button
                              variante="secundario"
                              onClick={() => setEnDetalle(receta)}
                              aria-label={`Ver la receta de ${receta.paciente.nombreCompleto}`}
                            >
                              <Eye aria-hidden="true" size={20} />
                              Ver receta
                            </Button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {totalPaginas > 1 && (
              <nav
                aria-label="Páginas de la lista de recetas"
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

      <Modal
        abierto={enDetalle !== null}
        alCerrar={() => setEnDetalle(null)}
        titulo="Receta"
        descripcion={enDetalle ? enDetalle.paciente.nombreCompleto : undefined}
      >
        {enDetalle && (
          <div className="px-4 py-6 sm:px-6">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <p className="text-auxiliar text-texto-secundario">
                Recetada por {enDetalle.medico.nombreCompleto} ·{" "}
                <span className="tabular-nums slashed-zero">{formatearFechaHora(enDetalle.fechaEmision)}</span>
              </p>
              <StatusBadge estado={enDetalle.estado} />
            </div>
            <div className="mt-4">
              <ListaMedicamentos medicamentos={enDetalle.medicamentos} conEntrega />
            </div>
            {enDetalle.indicacionesGenerales && (
              <div className="mt-4 border-t border-divisor pt-4">
                <h3 className="font-semibold text-texto">Indicaciones generales</h3>
                <p className="mt-1 text-texto">{enDetalle.indicacionesGenerales}</p>
              </div>
            )}
          </div>
        )}
      </Modal>

      <DespacharRecetaModal
        abierto={enDespacho !== null}
        receta={enDespacho}
        claveFormulario={aperturasDespacho}
        alCerrar={() => setEnDespacho(null)}
        alDespachar={() => {
          setEnDespacho(null);
          setRecargas((valor) => valor + 1);
        }}
      />
    </AppLayout>
  );
}
