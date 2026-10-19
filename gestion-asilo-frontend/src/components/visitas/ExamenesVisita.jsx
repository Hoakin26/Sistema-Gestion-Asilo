import { useEffect, useId, useRef, useState } from "react";
import { FlaskConical, Plus, Search } from "lucide-react";

import { useToast } from "../../hooks/useToast";
import { erroresPorCampo, mensajeDeError } from "../../services/api";
import { listarExamenesLaboratorio, ordenarExamenes } from "../../services/visitaService";
import { ResultadoExamen } from "../laboratorio/ResultadoExamen";
import { formatearFechaHora } from "../../utils/fechas";
import { Alert } from "../ui/Alert";
import { Button } from "../ui/Button";
import { Input } from "../ui/Input";
import { StatusBadge } from "../ui/StatusBadge";
import { Barra } from "../ui/TablaSkeleton";

const TARJETA = "rounded-tarjeta border border-divisor bg-superficie p-4 shadow-tarjeta sm:p-6";

/* Sin tildes ni mayúsculas: "acido" encuentra "Ácido úrico". */
function normalizar(texto) {
  return texto
    .toLowerCase()
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "");
}

/*
 * Cada orden con su fecha: en una misma visita se pueden ordenar exámenes en
 * dos momentos, y el laboratorio los recibe por separado.
 */
function ListaOrdenes({ ordenes }) {
  return (
    <ul className="divide-y divide-divisor">
      {ordenes.map((orden) => (
        <li key={orden.id} className="py-3">
          <div className="flex flex-wrap items-center justify-between gap-x-4 gap-y-2">
            <span className="font-medium text-texto">{orden.examen.nombre}</span>
            <StatusBadge estado={orden.estado} />
          </div>
          <p className="mt-1 text-auxiliar text-texto-secundario tabular-nums slashed-zero">
            Ordenado el {formatearFechaHora(orden.fechaOrden)}
          </p>
          {orden.indicaciones && <p className="mt-1 text-texto">{orden.indicaciones}</p>}
          {/* HU-13: en cuanto el laboratorio lo carga, el especialista lo ve aquí. */}
          <ResultadoExamen orden={orden} />
        </li>
      ))}
    </ul>
  );
}

/**
 * La selección de exámenes del catálogo. El catálogo se pide al abrirla, no
 * al cargar la visita: muchas consultas no ordenan exámenes.
 *
 * Casillas y no un selector múltiple: en tableta, marcar varias casillas de
 * 44px es más seguro que mantener presionada una lista. Un examen que ya se
 * ordenó en esta visita aparece marcado y deshabilitado, con la razón.
 */
function SeleccionExamenes({ visita, yaOrdenados, alCancelar, alOrdenar }) {
  const { mostrar } = useToast();
  const idLeyenda = useId();
  const [catalogo, setCatalogo] = useState(null);
  const [errorCatalogo, setErrorCatalogo] = useState(null);
  const [filtro, setFiltro] = useState("");
  // examenId → indicaciones, en el orden en que se marcaron.
  const [elegidos, setElegidos] = useState(new Map());
  const [error, setError] = useState(null);
  const [ordenando, setOrdenando] = useState(false);

  useEffect(() => {
    let vigente = true;
    listarExamenesLaboratorio()
      .then((lista) => {
        if (vigente) setCatalogo(lista);
      })
      .catch((fallo) => {
        if (vigente) setErrorCatalogo(mensajeDeError(fallo, "No se pudo cargar el catálogo de exámenes"));
      });
    return () => {
      vigente = false;
    };
  }, []);

  const alternar = (examenId) => {
    setError(null);
    setElegidos((previos) => {
      const siguientes = new Map(previos);
      if (siguientes.has(examenId)) siguientes.delete(examenId);
      else siguientes.set(examenId, "");
      return siguientes;
    });
  };

  const indicar = (examenId, texto) => {
    setElegidos((previos) => new Map(previos).set(examenId, texto));
  };

  const ordenar = async () => {
    if (elegidos.size === 0) {
      setError("Seleccione al menos un examen");
      return;
    }
    setOrdenando(true);
    setError(null);
    try {
      const nuevas = await ordenarExamenes(
        visita.id,
        [...elegidos].map(([examenId, indicaciones]) => ({
          examenId,
          indicaciones: indicaciones.trim() || null,
        })),
      );
      mostrar(
        nuevas.length === 1
          ? "Se envió la orden al laboratorio."
          : `Se enviaron ${nuevas.length} órdenes al laboratorio.`,
      );
      alOrdenar(nuevas);
    } catch (fallo) {
      // Un examen que ya no está en el catálogo o que otra pestaña ya ordenó:
      // el mensaje del backend dice cuál, y la selección se conserva.
      const porCampo = erroresPorCampo(fallo);
      setError(porCampo ? Object.values(porCampo)[0] : mensajeDeError(fallo, "No se pudieron ordenar los exámenes"));
    } finally {
      setOrdenando(false);
    }
  };

  // Los marcados se quedan a la vista aunque cambie la búsqueda: si
  // desaparecieran, se ordenaría un examen que ya no está en pantalla.
  const termino = normalizar(filtro.trim());
  const coincide = (examen) => !termino || normalizar(examen.nombre).includes(termino);
  const visibles = (catalogo ?? []).filter((examen) => elegidos.has(examen.id) || coincide(examen));
  const sinCoincidencias = !(catalogo ?? []).some(coincide);

  return (
    <div className="mt-6 flex flex-col gap-4 rounded-tarjeta border border-divisor p-4">
      {errorCatalogo ? (
        <Alert>{errorCatalogo}</Alert>
      ) : catalogo === null ? (
        <div role="status" className="flex flex-col gap-2">
          <span className="sr-only">Cargando el catálogo de exámenes…</span>
          <Barra className="w-56" />
          <Barra className="w-64" />
          <Barra className="w-48" />
        </div>
      ) : (
        <fieldset aria-describedby={error ? `${idLeyenda}-error` : undefined}>
          <legend id={idLeyenda} className="font-semibold text-texto">
            Exámenes a ordenar
          </legend>

          <div className="relative mt-4">
            <Search
              aria-hidden="true"
              size={20}
              className="pointer-events-none absolute top-1/2 left-4 -translate-y-1/2 text-texto-secundario"
            />
            <Input
              type="search"
              value={filtro}
              onChange={(evento) => setFiltro(evento.target.value)}
              placeholder="Buscar examen…"
              aria-label="Buscar en el catálogo de exámenes"
              className="pl-12"
            />
          </div>

          <ul className="mt-2 divide-y divide-divisor">
            {visibles.map((examen) => {
              const ordenado = yaOrdenados.has(examen.id);
              const elegido = elegidos.has(examen.id);
              return (
                <li key={examen.id} className="py-1">
                  <label
                    className={`flex min-h-11 items-center gap-3 select-none ${ordenado ? "cursor-not-allowed" : "cursor-pointer"}`}
                  >
                    <input
                      type="checkbox"
                      checked={ordenado || elegido}
                      disabled={ordenado}
                      onChange={() => alternar(examen.id)}
                      className="size-5 shrink-0 cursor-pointer accent-primario disabled:cursor-not-allowed"
                    />
                    <span className={ordenado ? "text-texto-secundario" : "text-texto"}>{examen.nombre}</span>
                    {ordenado && <span className="ml-auto text-auxiliar text-texto-secundario">Ya ordenado</span>}
                  </label>
                  {elegido && (
                    <div className="pb-2 pl-8">
                      <Input
                        value={elegidos.get(examen.id)}
                        onChange={(evento) => indicar(examen.id, evento.target.value)}
                        maxLength={500}
                        placeholder="Indicaciones para el laboratorio (opcional). Ej.: en ayunas"
                        aria-label={`Indicaciones para ${examen.nombre}`}
                      />
                    </div>
                  )}
                </li>
              );
            })}
            {sinCoincidencias && (
              <li className="py-4 text-texto-secundario">Ningún examen coincide con lo que buscó.</li>
            )}
          </ul>
        </fieldset>
      )}

      {error && (
        <div id={`${idLeyenda}-error`}>
          <Alert>{error}</Alert>
        </div>
      )}

      <div className="flex flex-wrap justify-end gap-3">
        <Button type="button" variante="secundario" onClick={alCancelar} disabled={ordenando}>
          Cancelar
        </Button>
        <Button type="button" onClick={ordenar} cargando={ordenando} disabled={catalogo === null}>
          {elegidos.size > 1 ? `Ordenar ${elegidos.size} exámenes` : "Ordenar examen"}
        </Button>
      </div>
    </div>
  );
}

/**
 * HU-11 (YS-112). Los exámenes de laboratorio de la visita: los que ya se
 * ordenaron, con su estado (DE-02), y la selección de exámenes nuevos
 * mientras la visita está activa. Cada examen genera una orden pendiente que
 * el laboratorio ve en su bandeja (HU-12).
 *
 * - `ordenes`: `undefined` mientras carga.
 */
export function ExamenesVisita({ visita, ordenes, error, alOrdenar }) {
  const idTitulo = useId();
  const refTitulo = useRef(null);
  const [seleccionando, setSeleccionando] = useState(false);
  const activa = visita.estado === "ACTIVA";

  // Al cerrarse la selección la página se acorta, y la vista quedaba en los
  // antecedentes, debajo. El foco vuelve al título, que trae a la vista la
  // lista con las órdenes recién creadas.
  const cerrar = () => {
    setSeleccionando(false);
    refTitulo.current?.focus();
  };

  // Los que ya tienen una orden vigente: no se pueden volver a ordenar.
  const yaOrdenados = new Set(
    (ordenes ?? []).filter((orden) => orden.estado !== "ANULADA").map((orden) => orden.examen.id),
  );

  return (
    <section aria-labelledby={idTitulo} className={TARJETA}>
      <div className="flex items-center gap-3">
        <FlaskConical aria-hidden="true" size={20} className="text-texto-secundario" />
        <h2 id={idTitulo} ref={refTitulo} tabIndex={-1} className="text-h3 font-semibold">
          Exámenes de laboratorio
        </h2>
      </div>

      {error ? (
        <div className="mt-4">
          <Alert>{error}</Alert>
        </div>
      ) : ordenes === undefined ? (
        <div role="status" className="mt-4 flex flex-col gap-2">
          <span className="sr-only">Cargando los exámenes…</span>
          <Barra className="w-48" />
          <Barra className="w-64" />
        </div>
      ) : (
        <>
          {ordenes.length === 0 ? (
            <p className="mt-4 text-texto-secundario">
              {activa ? "Todavía no se han ordenado exámenes en esta visita." : "En esta visita no se ordenaron exámenes."}
            </p>
          ) : (
            <div className="mt-2">
              <ListaOrdenes ordenes={ordenes} />
            </div>
          )}

          {activa && !seleccionando && (
            <div className="mt-4">
              <Button variante="secundario" onClick={() => setSeleccionando(true)}>
                <Plus aria-hidden="true" size={20} />
                Ordenar exámenes
              </Button>
            </div>
          )}

          {activa && seleccionando && (
            <SeleccionExamenes
              visita={visita}
              yaOrdenados={yaOrdenados}
              alCancelar={cerrar}
              alOrdenar={(nuevas) => {
                cerrar();
                alOrdenar(nuevas);
              }}
            />
          )}
        </>
      )}
    </section>
  );
}
