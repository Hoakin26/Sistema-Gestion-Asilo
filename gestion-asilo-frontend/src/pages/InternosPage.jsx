import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ChevronRight, Plus, Search, UserRound } from "lucide-react";

import { InternoFormModal } from "../components/internos/InternoFormModal";
import { Alert } from "../components/ui/Alert";
import { Button } from "../components/ui/Button";
import { Input } from "../components/ui/Input";
import { Paginacion } from "../components/ui/Paginacion";
import { StatusBadge } from "../components/ui/StatusBadge";
import { TablaSkeleton } from "../components/ui/TablaSkeleton";
import { editaInternos, leeDatosClinicos } from "../constants/roles";
import { useAuth } from "../hooks/useAuth";
import { useToast } from "../hooks/useToast";
import { AppLayout } from "../layouts/AppLayout";
import { mensajeDeError } from "../services/api";
import { listarInternos } from "../services/internoService";
import { formatearFecha } from "../utils/fechas";
import { formatearDpi } from "../utils/formato";

/* Lo bastante para no pedir en cada tecla, poco para que no se sienta lento. */
const ESPERA_BUSQUEDA_MS = 300;

const ENCABEZADO =
  "px-4 py-4 text-auxiliar font-semibold text-texto-secundario uppercase xl:px-6";

/**
 * YS-61. La lista de internos. La comparten el Médico General, enfermería y el
 * Administrador del Asilo, porque los tres trabajan con el expediente; lo que
 * cambia según el rol es qué pueden hacer con él, no qué filas ven. La fila no
 * lleva ningún dato clínico, así que es la misma para los tres (RNF-03).
 *
 * A diferencia de la de usuarios, la búsqueda y la paginación las hace el
 * backend: la lista crece con cada ingreso y no conviene traerla entera.
 *
 * HU-02 (YS-66). Con `modo="historial"` es el módulo "Historial médico" del
 * Médico General: la misma lista y la misma búsqueda, pero cada fila lleva al
 * historial del interno en vez de a su perfil, y no ofrece registrar uno
 * nuevo, porque ese no es el trabajo de este módulo.
 */
export function InternosPage({ modo = "expediente" }) {
  const { usuario } = useAuth();
  const { mostrar } = useToast();
  const navegar = useNavigate();
  const enHistorial = modo === "historial";
  const puedeRegistrar = editaInternos(usuario?.rol) && !enHistorial;
  const vistaClinica = leeDatosClinicos(usuario?.rol);
  const destino = (interno) => (enHistorial ? `/historial-medico/${interno.id}` : `/internos/${interno.id}`);
  const accion = (interno) =>
    enHistorial ? `Ver el historial médico de ${interno.nombreCompleto}` : `Ver el perfil de ${interno.nombreCompleto}`;

  const [busqueda, setBusqueda] = useState("");
  // Lo que de verdad se consulta: la búsqueda, una vez que dejó de escribir.
  const [termino, setTermino] = useState("");
  const [pagina, setPagina] = useState(0);

  const [resultado, setResultado] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);

  const [modalAbierto, setModalAbierto] = useState(false);
  const [aperturas, setAperturas] = useState(0);

  useEffect(() => {
    const espera = setTimeout(() => {
      setTermino(busqueda.trim());
      // Una búsqueda nueva empieza siempre en la primera página.
      setPagina(0);
    }, ESPERA_BUSQUEDA_MS);
    return () => clearTimeout(espera);
  }, [busqueda]);

  useEffect(() => {
    /*
     * Si la persona sigue escribiendo, la respuesta de una búsqueda vieja
     * puede llegar después de la nueva y pisarla. `vigente` descarta toda
     * respuesta que ya no corresponde a lo que está en pantalla.
     */
    let vigente = true;
    listarInternos({ busqueda: termino, pagina })
      .then((datos) => {
        if (!vigente) return;
        setResultado(datos);
        setError(null);
      })
      .catch((fallo) => {
        if (vigente) setError(mensajeDeError(fallo, "No se pudieron cargar los internos"));
      })
      .finally(() => {
        if (vigente) setCargando(false);
      });
    return () => {
      vigente = false;
    };
  }, [termino, pagina]);

  const abrirAlta = () => {
    setAperturas((valor) => valor + 1);
    setModalAbierto(true);
  };

  const internos = resultado?.content ?? [];
  const total = resultado?.totalElements ?? 0;
  const totalPaginas = resultado?.totalPages ?? 0;

  return (
    <AppLayout migas={["Inicio", enHistorial ? "Historial médico" : "Internos"]}>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-h1 font-bold">{enHistorial ? "Historial médico" : "Internos"}</h1>
          <p className="mt-2 text-texto-secundario">
            {/* El Administrador del Asilo entra a esta misma lista desde
                "Internos (vista administrativa)": se le recuerda qué incluye. */}
            {enHistorial
              ? "Elija un interno para ver sus consultas, diagnósticos, padecimientos y medicamentos."
              : vistaClinica
                ? "Expedientes de los residentes del asilo."
                : "Vista administrativa: datos personales y familiar responsable."}
          </p>
        </div>

        {puedeRegistrar && (
          <Button onClick={abrirAlta}>
            <Plus aria-hidden="true" size={20} />
            Nuevo interno
          </Button>
        )}
      </div>

      {error && (
        <div className="mt-6">
          <Alert>{error}</Alert>
        </div>
      )}

      <div className="relative mt-6">
        <Search
          aria-hidden="true"
          size={20}
          className="pointer-events-none absolute top-1/2 left-4 -translate-y-1/2 text-texto-secundario"
        />
        <Input
          type="search"
          value={busqueda}
          onChange={(evento) => setBusqueda(evento.target.value)}
          placeholder="Buscar por nombre o DPI…"
          aria-label="Buscar internos por nombre o DPI"
          className="pl-12"
        />
      </div>

      <div className="mt-6">
        {cargando ? (
          <TablaSkeleton etiqueta="Cargando internos…" />
        ) : internos.length === 0 && !termino ? (
          <div className="flex flex-col items-center gap-4 rounded-tarjeta border border-divisor bg-superficie px-4 py-16 text-center shadow-tarjeta sm:px-6">
            <UserRound aria-hidden="true" size={24} className="text-texto-secundario" />
            <p className="text-texto-secundario">Todavía no hay internos registrados.</p>
            {puedeRegistrar && (
              <Button onClick={abrirAlta}>
                <Plus aria-hidden="true" size={20} />
                Nuevo interno
              </Button>
            )}
          </div>
        ) : internos.length === 0 ? (
          /* Vacío por la búsqueda ≠ vacío por falta de datos, como en usuarios. */
          <div className="flex flex-col items-center gap-4 rounded-tarjeta border border-divisor bg-superficie px-4 py-16 text-center shadow-tarjeta sm:px-6">
            <Search aria-hidden="true" size={24} className="text-texto-secundario" />
            <p className="text-texto-secundario">Ningún interno coincide con lo que buscó.</p>
            <Button variante="secundario" onClick={() => setBusqueda("")}>
              Quitar búsqueda
            </Button>
          </div>
        ) : (
          <>
            <p
              aria-live="polite"
              className="pb-3 text-auxiliar text-texto-secundario tabular-nums slashed-zero"
            >
              {termino
                ? `${total} ${total === 1 ? "interno coincide" : "internos coinciden"} con la búsqueda`
                : `${total} ${total === 1 ? "interno registrado" : "internos registrados"}`}
            </p>

            {/* relative: los sr-only de la tabla son absolutos; sin él escapan del
                recorte, estiran la página y el teléfono la achica a una esquina (YS-175). */}
            <div className="relative overflow-x-auto rounded-tarjeta border border-divisor bg-superficie shadow-tarjeta">
              <table className="w-full min-w-xl border-collapse text-left">
                <caption className="sr-only">
                  {enHistorial
                    ? "Internos del asilo: elija uno para ver su historial médico"
                    : "Internos del asilo con su fecha de ingreso y estado"}
                </caption>
                <thead>
                  <tr className="border-b border-divisor bg-fondo">
                    <th scope="col" className={ENCABEZADO}>
                      Interno
                    </th>
                    <th scope="col" className={ENCABEZADO}>
                      Ingreso
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
                  {internos.map((interno) => (
                    <tr key={interno.id} className="border-b border-divisor last:border-b-0">
                      <td className="px-4 py-4 xl:px-6">
                        <Link
                          to={destino(interno)}
                          className="block font-medium text-texto hover:text-primario hover:underline"
                        >
                          {interno.nombreCompleto}
                        </Link>
                        <span className="block text-auxiliar text-texto-secundario tabular-nums slashed-zero">
                          DPI {formatearDpi(interno.dpi)}
                        </span>
                      </td>
                      <td className="px-4 py-4 tabular-nums slashed-zero xl:px-6">
                        {formatearFecha(interno.fechaIngreso)}
                      </td>
                      <td className="px-4 py-4 xl:px-6">
                        <StatusBadge activo={interno.activo} />
                      </td>
                      <td className="px-4 py-4 xl:px-6">
                        <div className="flex justify-end">
                          {/* El nombre ya enlaza al perfil, pero un texto es un
                              blanco chico en tableta: esto da los 44px. */}
                          <Link
                            to={destino(interno)}
                            aria-label={accion(interno)}
                            title={accion(interno)}
                            className="inline-flex size-11 items-center justify-center rounded-tarjeta text-texto-secundario hover:bg-neutral-tenue hover:text-primario"
                          >
                            <ChevronRight aria-hidden="true" size={20} />
                          </Link>
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
              etiqueta="Páginas de la lista de internos"
            />
          </>
        )}
      </div>

      <InternoFormModal
        abierto={modalAbierto}
        interno={null}
        claveFormulario={aperturas}
        alCerrar={() => setModalAbierto(false)}
        alGuardar={(guardado) => {
          setModalAbierto(false);
          mostrar(`Se registró a ${guardado.nombreCompleto}. Complete ahora su ficha médica.`);
          // Al perfil y no de vuelta a la lista: lo que sigue es la ficha médica.
          navegar(`/internos/${guardado.id}`);
        }}
      />
    </AppLayout>
  );
}
