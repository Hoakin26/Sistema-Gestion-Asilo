import { useEffect, useState } from "react";
import { Filter, Pencil, Plus, Search, Stethoscope } from "lucide-react";

import { EspecialistaFormModal } from "../components/especialistas/EspecialistaFormModal";
import { TarifasConsulta } from "../components/especialistas/TarifasConsulta";
import { Alert } from "../components/ui/Alert";
import { Button, IconButton } from "../components/ui/Button";
import { Input, Select } from "../components/ui/Input";
import { StatusBadge } from "../components/ui/StatusBadge";
import { TablaSkeleton } from "../components/ui/TablaSkeleton";
import { resumirHorario } from "../constants/dias";
import { useToast } from "../hooks/useToast";
import { AppLayout } from "../layouts/AppLayout";
import { mensajeDeError } from "../services/api";
import { listarEspecialistas } from "../services/especialistaService";

const ENCABEZADO =
  "px-4 py-4 text-auxiliar font-semibold text-texto-secundario uppercase xl:px-6";

/* Sin tildes ni mayúsculas, como en usuarios: "perez" encuentra a "Pérez". */
function normalizar(texto) {
  return texto
    .toLowerCase()
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "");
}

/**
 * HU-06 (YS-168). El módulo "Médicos y disponibilidad" de la Fundación: los
 * especialistas que atienden a los internos referidos, con su especialidad y
 * su horario semanal. De ese horario salen las horas libres al asignar una
 * cita desde la bandeja.
 *
 * La lista es corta (los médicos de una fundación), así que se trae entera y
 * se filtra aquí, igual que la de usuarios.
 */
export function MedicosDisponibilidadPage() {
  const { mostrar } = useToast();

  const [especialistas, setEspecialistas] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);

  const [busqueda, setBusqueda] = useState("");
  const [especialidadFiltrada, setEspecialidadFiltrada] = useState("");

  const [enEdicion, setEnEdicion] = useState(null);
  const [modalAbierto, setModalAbierto] = useState(false);
  const [aperturas, setAperturas] = useState(0);
  // Sube después de guardar, para volver a pedir la lista.
  const [recargas, setRecargas] = useState(0);

  // Sin encender la carga al recargar: con la tabla ya en pantalla, es
  // preferible no hacerla parpadear.
  useEffect(() => {
    let vigente = true;
    listarEspecialistas()
      .then((lista) => {
        if (!vigente) return;
        setEspecialistas(lista);
        setError(null);
      })
      .catch((fallo) => {
        if (vigente) setError(mensajeDeError(fallo, "No se pudieron cargar los especialistas"));
      })
      .finally(() => {
        if (vigente) setCargando(false);
      });
    return () => {
      vigente = false;
    };
  }, [recargas]);

  const abrir = (especialista) => {
    setEnEdicion(especialista);
    setAperturas((valor) => valor + 1);
    setModalAbierto(true);
  };

  // Solo las especialidades que alguien atiende: filtrar por una sin médicos
  // siempre daría una lista vacía.
  const especialidades = [
    ...new Map(especialistas.map((e) => [e.especialidad.id, e.especialidad])).values(),
  ].sort((a, b) => a.nombre.localeCompare(b.nombre, "es"));

  const termino = normalizar(busqueda.trim());
  const visibles = especialistas.filter(
    (especialista) =>
      (!termino || normalizar(especialista.medico.nombreCompleto).includes(termino)) &&
      (!especialidadFiltrada || String(especialista.especialidad.id) === especialidadFiltrada),
  );

  const hayFiltros = Boolean(termino) || Boolean(especialidadFiltrada);
  const activos = especialistas.filter((especialista) => especialista.activo).length;

  const limpiarFiltros = () => {
    setBusqueda("");
    setEspecialidadFiltrada("");
  };

  return (
    <AppLayout migas={["Inicio", "Médicos y disponibilidad"]}>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-h1 font-bold">Médicos y disponibilidad</h1>
          <p className="mt-2 text-texto-secundario tabular-nums slashed-zero">
            {especialistas.length} {especialistas.length === 1 ? "especialista" : "especialistas"} ·{" "}
            {activos} {activos === 1 ? "atiende" : "atienden"} citas
          </p>
        </div>

        <Button onClick={() => abrir(null)}>
          <Plus aria-hidden="true" size={20} />
          Registrar especialista
        </Button>
      </div>

      {error && (
        <div className="mt-6">
          <Alert>{error}</Alert>
        </div>
      )}

      {especialistas.length > 0 && (
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
              placeholder="Buscar por nombre…"
              aria-label="Buscar especialistas por nombre"
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
              value={especialidadFiltrada}
              onChange={(evento) => setEspecialidadFiltrada(evento.target.value)}
              aria-label="Filtrar por especialidad"
              className="pl-12"
            >
              <option value="">Todas las especialidades</option>
              {especialidades.map((especialidad) => (
                <option key={especialidad.id} value={especialidad.id}>
                  {especialidad.nombre}
                </option>
              ))}
            </Select>
          </div>
        </div>
      )}

      <div className="mt-6">
        {cargando ? (
          <TablaSkeleton etiqueta="Cargando especialistas…" />
        ) : especialistas.length === 0 ? (
          <div className="flex flex-col items-center gap-4 rounded-tarjeta border border-divisor bg-superficie px-4 py-16 text-center shadow-tarjeta sm:px-6">
            <Stethoscope aria-hidden="true" size={24} className="text-texto-secundario" />
            {/* Dice en qué orden van las cosas: sin la cuenta creada por el
                Administrador del Asilo, el formulario no tiene a quién ofrecer. */}
            <p className="max-w-md text-texto-secundario">
              Todavía no hay especialistas registrados. Primero el Administrador del Asilo crea la
              cuenta del médico con el rol Médico Especialista; después, aquí se registra su
              especialidad y su horario.
            </p>
            <Button onClick={() => abrir(null)}>
              <Plus aria-hidden="true" size={20} />
              Registrar especialista
            </Button>
          </div>
        ) : visibles.length === 0 ? (
          <div className="flex flex-col items-center gap-4 rounded-tarjeta border border-divisor bg-superficie px-4 py-16 text-center shadow-tarjeta sm:px-6">
            <Search aria-hidden="true" size={24} className="text-texto-secundario" />
            <p className="text-texto-secundario">Ningún especialista coincide con lo que buscó.</p>
            <Button variante="secundario" onClick={limpiarFiltros}>
              Quitar filtros
            </Button>
          </div>
        ) : (
          <>
            {hayFiltros && (
              <p
                aria-live="polite"
                className="pb-3 text-auxiliar text-texto-secundario tabular-nums slashed-zero"
              >
                Mostrando {visibles.length} de {especialistas.length} especialistas
              </p>
            )}

            {/* relative: los sr-only de la tabla son absolutos; sin él escapan del
                recorte, estiran la página y el teléfono la achica a una esquina (YS-175). */}
            <div className="relative overflow-x-auto rounded-tarjeta border border-divisor bg-superficie shadow-tarjeta">
              <table className="w-full min-w-2xl border-collapse text-left">
                <caption className="sr-only">
                  Médicos especialistas con su especialidad, horario de atención y estado
                </caption>
                <thead>
                  <tr className="border-b border-divisor bg-fondo">
                    <th scope="col" className={ENCABEZADO}>
                      Especialista
                    </th>
                    <th scope="col" className={ENCABEZADO}>
                      Horario de atención
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
                  {visibles.map((especialista) => (
                    <tr key={especialista.id} className="border-b border-divisor align-top last:border-b-0">
                      <td className="px-4 py-4 xl:px-6">
                        <span className="block font-medium text-texto">
                          {especialista.medico.nombreCompleto}
                        </span>
                        <span className="block text-auxiliar text-texto-secundario">
                          {especialista.especialidad.nombre}
                        </span>
                      </td>
                      <td className="px-4 py-4 xl:px-6">
                        <span className="block text-texto tabular-nums slashed-zero">
                          {resumirHorario(especialista.horarios)}
                        </span>
                        <span className="block text-auxiliar text-texto-secundario tabular-nums">
                          Citas de {especialista.duracionCitaMinutos} minutos
                        </span>
                      </td>
                      <td className="px-4 py-4 xl:px-6">
                        <StatusBadge activo={especialista.activo} />
                      </td>
                      <td className="px-4 py-4 xl:px-6">
                        <div className="flex justify-end">
                          <IconButton
                            etiqueta={`Editar el perfil de ${especialista.medico.nombreCompleto}`}
                            onClick={() => abrir(especialista)}
                          >
                            <Pencil aria-hidden="true" size={20} />
                          </IconButton>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </div>

      {/* YS-169. En la misma pantalla: la Fundación ya viene aquí a
          administrar a sus especialistas, y la tarifa es de su especialidad. */}
      <TarifasConsulta />

      <EspecialistaFormModal
        abierto={modalAbierto}
        especialista={enEdicion}
        claveFormulario={aperturas}
        alCerrar={() => setModalAbierto(false)}
        alGuardar={(nombre) => {
          setModalAbierto(false);
          mostrar(
            enEdicion
              ? `Se guardaron los cambios de ${nombre}.`
              : `${nombre} ya puede recibir citas.`,
          );
          setRecargas((valor) => valor + 1);
        }}
      />
    </AppLayout>
  );
}
