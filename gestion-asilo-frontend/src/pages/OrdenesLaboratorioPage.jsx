import { useEffect, useState } from "react";
import { Eye, FileUp, Filter, FlaskConical, Inbox, Search } from "lucide-react";

import { CostoRegistrado } from "../components/laboratorio/CostoRegistrado";
import { ResultadoExamen } from "../components/laboratorio/ResultadoExamen";
import { ResultadoFormModal } from "../components/laboratorio/ResultadoFormModal";
import { Alert } from "../components/ui/Alert";
import { Button } from "../components/ui/Button";
import { Input, Select } from "../components/ui/Input";
import { Modal } from "../components/ui/Modal";
import { Paginacion } from "../components/ui/Paginacion";
import { StatusBadge } from "../components/ui/StatusBadge";
import { TablaSkeleton } from "../components/ui/TablaSkeleton";
import { ESTADOS_ORDEN, ETIQUETAS_ESTADO } from "../constants/estados";
import { useToast } from "../hooks/useToast";
import { AppLayout } from "../layouts/AppLayout";
import { mensajeDeError } from "../services/api";
import { iniciarOrden, listarOrdenes } from "../services/laboratorioService";
import { formatearFechaHora } from "../utils/fechas";

/* Lo bastante para no pedir en cada tecla, poco para que no se sienta lento. */
const ESPERA_BUSQUEDA_MS = 300;

const ENCABEZADO =
  "px-4 py-4 text-auxiliar font-semibold text-texto-secundario uppercase xl:px-6";

/*
 * El filtro con que arranca: lo que todavía hay que atender. Una orden que el
 * laboratorio ya tomó sigue siendo trabajo suyo hasta cargar el resultado
 * (HU-13); con solo "Pendiente" desaparecería de la vista al tomarla.
 */
const POR_ATENDER = "POR_ATENDER";

function estadosDelFiltro(filtro) {
  if (filtro === POR_ATENDER) return ["PENDIENTE", "EN_PROCESO"];
  return filtro ? [filtro] : [];
}

/**
 * HU-12 (YS-115). La bandeja del laboratorio (RF-09): las órdenes que dieron
 * los especialistas, con el paciente, el examen, quién lo pidió y su estado.
 * Se busca por el nombre o el DPI del paciente y se filtra por estado, y desde
 * cada orden pendiente el laboratorio la toma (DE-02: pasa a En Proceso).
 *
 * El diagnóstico no aparece en ninguna parte, a propósito: el laboratorio ve
 * la orden, no la consulta, y el backend tampoco lo manda.
 *
 * HU-13 (YS-118). Desde cada orden pendiente o en proceso se carga el
 * resultado, y desde las que ya lo tienen se consulta. Con
 * `modo="resultados"` es el módulo "Registro de resultados": la misma
 * bandeja, solo con lo que el laboratorio ya entregó, del más reciente al más
 * antiguo (decidido el 19/10).
 */
export function OrdenesLaboratorioPage({ modo = "bandeja" }) {
  const { mostrar } = useToast();
  const enResultados = modo === "resultados";

  const [filtro, setFiltro] = useState(enResultados ? "RESULTADOS_CARGADOS" : POR_ATENDER);
  const [busqueda, setBusqueda] = useState("");
  // Lo que de verdad se consulta: la búsqueda, una vez que dejó de escribir.
  const [termino, setTermino] = useState("");
  const [pagina, setPagina] = useState(0);
  const [recargas, setRecargas] = useState(0);

  const [resultado, setResultado] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [iniciando, setIniciando] = useState(null);
  // La orden cuyo resultado se carga o se consulta; las aperturas remontan el formulario limpio.
  const [enCarga, setEnCarga] = useState(null);
  const [aperturasCarga, setAperturasCarga] = useState(0);
  const [enConsulta, setEnConsulta] = useState(null);

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
    listarOrdenes({
      estados: estadosDelFiltro(filtro),
      paciente: termino,
      pagina,
      orden: enResultados ? "fechaResultado,desc" : undefined,
    })
      .then((datos) => {
        if (!vigente) return;
        setResultado(datos);
        setError(null);
      })
      .catch((fallo) => {
        if (vigente) setError(mensajeDeError(fallo, "No se pudieron cargar las órdenes"));
      })
      .finally(() => {
        if (vigente) setCargando(false);
      });
    return () => {
      vigente = false;
    };
  }, [filtro, termino, pagina, recargas, enResultados]);

  const cambiarFiltro = (evento) => {
    setFiltro(evento.target.value);
    setPagina(0);
  };

  const quitarFiltros = () => {
    setFiltro(enResultados ? "RESULTADOS_CARGADOS" : "");
    setBusqueda("");
    setTermino("");
    setPagina(0);
  };

  const iniciar = async (orden) => {
    setIniciando(orden.id);
    try {
      await iniciarOrden(orden.id);
      mostrar(`Se inició «${orden.examen.nombre}» de ${orden.paciente.nombreCompleto}.`);
    } catch (fallo) {
      // 409 si otra persona la tomó primero: al recargar se ve en qué quedó.
      mostrar(mensajeDeError(fallo, "No se pudo iniciar la orden"), "error");
    } finally {
      setIniciando(null);
      setRecargas((valor) => valor + 1);
    }
  };

  const abrirCarga = (orden) => {
    setAperturasCarga((valor) => valor + 1);
    setEnCarga(orden);
  };

  const ordenes = resultado?.content ?? [];
  const total = resultado?.totalElements ?? 0;
  const totalPaginas = resultado?.totalPages ?? 0;

  return (
    <AppLayout migas={["Inicio", enResultados ? "Registro de resultados" : "Órdenes pendientes"]}>
      <h1 className="text-h1 font-bold">{enResultados ? "Registro de resultados" : "Órdenes de laboratorio"}</h1>
      <p className="mt-2 text-texto-secundario">
        {enResultados
          ? "Resultados entregados, del más reciente al más antiguo."
          : "Exámenes solicitados, del más antiguo al más reciente."}
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
            aria-label="Buscar órdenes por nombre o DPI del paciente"
            className="pl-12"
          />
        </div>

        <div className={`relative w-full sm:w-64 ${enResultados ? "hidden" : ""}`}>
          <Filter
            aria-hidden="true"
            size={20}
            className="pointer-events-none absolute top-1/2 left-4 -translate-y-1/2 text-texto-secundario"
          />
          <Select
            value={filtro}
            onChange={cambiarFiltro}
            aria-label="Filtrar órdenes por estado"
            className="pl-12"
          >
            <option value={POR_ATENDER}>Por atender</option>
            {ESTADOS_ORDEN.map((clave) => (
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
          <TablaSkeleton etiqueta="Cargando órdenes…" />
        ) : ordenes.length === 0 ? (
          <div className="flex flex-col items-center gap-4 rounded-tarjeta border border-divisor bg-superficie px-4 py-16 text-center shadow-tarjeta sm:px-6">
            <Inbox aria-hidden="true" size={24} className="text-texto-secundario" />
            <p className="max-w-md text-texto-secundario">
              {termino
                ? "Ninguna orden coincide con el paciente que buscó."
                : enResultados
                  ? "Todavía no hay resultados cargados."
                  : filtro === POR_ATENDER
                  ? "No hay órdenes por atender."
                  : filtro
                    ? `No hay órdenes en estado "${ETIQUETAS_ESTADO[filtro]}".`
                    : "Todavía no hay órdenes de laboratorio."}
            </p>
            {/* Vacío por el filtro ≠ vacío de verdad, como en las otras bandejas. */}
            {(termino || (filtro && !enResultados)) && (
              <Button variante="secundario" onClick={quitarFiltros}>
                Ver todas las órdenes
              </Button>
            )}
          </div>
        ) : (
          <>
            <p
              aria-live="polite"
              className="pb-3 text-auxiliar text-texto-secundario tabular-nums slashed-zero"
            >
              {total} {total === 1 ? "orden" : "órdenes"}
            </p>

            {/* relative: los sr-only de la tabla son absolutos; sin él escapan del
                recorte, estiran la página y el teléfono la achica a una esquina (YS-175). */}
            <div className="relative overflow-x-auto rounded-tarjeta border border-divisor bg-superficie shadow-tarjeta">
              <table className="w-full min-w-2xl border-collapse text-left">
                <caption className="sr-only">
                  Órdenes de laboratorio con el paciente, el examen, quién lo solicitó y su estado
                </caption>
                <thead>
                  <tr className="border-b border-divisor bg-fondo">
                    <th scope="col" className={ENCABEZADO}>
                      Paciente
                    </th>
                    <th scope="col" className={ENCABEZADO}>
                      Examen
                    </th>
                    <th scope="col" className={ENCABEZADO}>
                      Solicitado por
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
                  {ordenes.map((orden) => (
                    <tr key={orden.id} className="border-b border-divisor align-top last:border-b-0">
                      <td className="px-4 py-4 font-medium text-texto xl:px-6">
                        {orden.paciente.nombreCompleto}
                      </td>
                      <td className="max-w-80 px-4 py-4 xl:px-6">
                        <span className="block font-medium text-texto">{orden.examen.nombre}</span>
                        {orden.indicaciones && (
                          <span className="line-clamp-2 text-auxiliar text-texto-secundario">
                            {orden.indicaciones}
                          </span>
                        )}
                      </td>
                      <td className="px-4 py-4 xl:px-6">
                        <span className="block text-texto">{orden.medicoSolicitante.nombreCompleto}</span>
                        <span className="block text-auxiliar text-texto-secundario tabular-nums slashed-zero">
                          {formatearFechaHora(orden.fechaOrden)}
                        </span>
                      </td>
                      <td className="px-4 py-4 xl:px-6">
                        <StatusBadge estado={orden.estado} />
                      </td>
                      <td className="px-4 py-4 xl:px-6">
                        <div className="flex flex-wrap justify-end gap-2 whitespace-nowrap">
                          {orden.estado === "PENDIENTE" && (
                            <Button
                              variante="secundario"
                              cargando={iniciando === orden.id}
                              disabled={iniciando !== null}
                              onClick={() => iniciar(orden)}
                              aria-label={`Iniciar «${orden.examen.nombre}» de ${orden.paciente.nombreCompleto}`}
                            >
                              {iniciando !== orden.id && <FlaskConical aria-hidden="true" size={20} />}
                              Iniciar
                            </Button>
                          )}
                          {/* HU-13: también desde Pendiente, para un examen rápido que
                              se toma y se registra sin pasar por En Proceso. */}
                          {(orden.estado === "PENDIENTE" || orden.estado === "EN_PROCESO") && (
                            <Button
                              variante={orden.estado === "EN_PROCESO" ? "primario" : "secundario"}
                              disabled={iniciando !== null}
                              onClick={() => abrirCarga(orden)}
                              aria-label={`Cargar el resultado de «${orden.examen.nombre}» de ${orden.paciente.nombreCompleto}`}
                            >
                              <FileUp aria-hidden="true" size={20} />
                              Cargar resultado
                            </Button>
                          )}
                          {orden.estado === "RESULTADOS_CARGADOS" && (
                            <Button
                              variante="secundario"
                              onClick={() => setEnConsulta(orden)}
                              aria-label={`Ver el resultado de «${orden.examen.nombre}» de ${orden.paciente.nombreCompleto}`}
                            >
                              <Eye aria-hidden="true" size={20} />
                              Ver resultado
                            </Button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <Paginacion
              pagina={pagina}
              totalPaginas={totalPaginas}
              alCambiar={setPagina}
              etiqueta="Páginas de la lista de órdenes"
            />
          </>
        )}
      </div>

      <ResultadoFormModal
        abierto={enCarga !== null}
        orden={enCarga}
        claveFormulario={aperturasCarga}
        alCerrar={() => setEnCarga(null)}
        alGuardar={() => {
          setEnCarga(null);
          setRecargas((valor) => valor + 1);
        }}
      />

      <Modal
        abierto={enConsulta !== null}
        alCerrar={() => setEnConsulta(null)}
        titulo="Resultado"
        descripcion={enConsulta ? `${enConsulta.examen.nombre} · ${enConsulta.paciente.nombreCompleto}` : undefined}
      >
        {enConsulta && (
          <div className="px-4 py-6 sm:px-6">
            <p className="text-auxiliar text-texto-secundario">
              Solicitado por {enConsulta.medicoSolicitante.nombreCompleto}
            </p>
            <ResultadoExamen orden={enConsulta} />
            <CostoRegistrado ordenId={enConsulta.id} />
          </div>
        )}
      </Modal>
    </AppLayout>
  );
}
