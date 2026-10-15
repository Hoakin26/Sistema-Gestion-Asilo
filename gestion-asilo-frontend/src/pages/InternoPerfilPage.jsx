import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, ClipboardList, Pencil, Stethoscope, UserRound } from "lucide-react";

import { FichaMedicaFormModal } from "../components/internos/FichaMedicaFormModal";
import { FichaMedicaDetalle } from "../components/internos/FichaMedicaDetalle";
import { InternoFormModal } from "../components/internos/InternoFormModal";
import { SolicitudReferenciaFormModal } from "../components/referencias/SolicitudReferenciaFormModal";
import { BotonEnlace, Button } from "../components/ui/Button";
import { StatusBadge } from "../components/ui/StatusBadge";
import { Barra } from "../components/ui/TablaSkeleton";
import { consultaHistorial, creaReferencias, editaInternos, leeDatosClinicos } from "../constants/roles";
import { useAuth } from "../hooks/useAuth";
import { useToast } from "../hooks/useToast";
import { AppLayout } from "../layouts/AppLayout";
import { mensajeDeError } from "../services/api";
import { obtenerFichaMedica, obtenerInterno } from "../services/internoService";
import { edadEnAnios, formatearFecha } from "../utils/fechas";
import { formatearDpi, formatearQuetzales } from "../utils/formato";

const TARJETA = "rounded-tarjeta border border-divisor bg-superficie p-4 shadow-tarjeta sm:p-6";

/** Un dato del perfil. Lo que falta se dice con palabras, no con un hueco. */
function Dato({ etiqueta, children, vacio = "No registrado", ancho = false }) {
  // `false` también es vacío: es lo que deja un `condición && <span>` sin dato.
  const tieneValor = children != null && children !== "" && children !== false;
  return (
    <div className={ancho ? "sm:col-span-2" : undefined}>
      <dt className="text-auxiliar text-texto-secundario">{etiqueta}</dt>
      <dd className={`mt-1 ${tieneValor ? "text-texto" : "text-texto-secundario"}`}>
        {tieneValor ? children : vacio}
      </dd>
    </div>
  );
}

function PerfilSkeleton() {
  return (
    <div role="status" aria-live="polite" className="flex flex-col gap-6">
      <span className="sr-only">Cargando el perfil…</span>
      <div aria-hidden="true" className="flex flex-col gap-3">
        <Barra className="h-8 w-72" />
        <Barra className="w-56" />
      </div>
      <div aria-hidden="true" className="grid gap-6 lg:grid-cols-2">
        {[0, 1].map((indice) => (
          <div key={indice} className={`${TARJETA} flex flex-col gap-4`}>
            <Barra className="w-40" />
            <Barra className="w-64" />
            <Barra className="w-48" />
          </div>
        ))}
      </div>
    </div>
  );
}

/**
 * YS-61. El perfil del interno: datos personales, familiar responsable y, para
 * el personal clínico, la ficha médica.
 *
 * El expediente y la ficha se piden por separado y su falla también se trata
 * por separado: si la ficha no carga, los datos personales igual se muestran.
 */
export function InternoPerfilPage() {
  const { id } = useParams();
  const { usuario } = useAuth();
  const { mostrar } = useToast();
  const puedeEditar = editaInternos(usuario?.rol);
  const veFicha = leeDatosClinicos(usuario?.rol);
  const puedeReferir = creaReferencias(usuario?.rol);
  const veHistorial = consultaHistorial(usuario?.rol);

  const [interno, setInterno] = useState(null);
  const [ficha, setFicha] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [errorFicha, setErrorFicha] = useState(null);

  const [edicionAbierta, setEdicionAbierta] = useState(false);
  const [fichaAbierta, setFichaAbierta] = useState(false);
  // Cambian en cada apertura para que el formulario se remonte limpio.
  const [aperturasEdicion, setAperturasEdicion] = useState(0);
  const [aperturasFicha, setAperturasFicha] = useState(0);
  const [referenciaAbierta, setReferenciaAbierta] = useState(false);
  const [aperturasReferencia, setAperturasReferencia] = useState(0);

  useEffect(() => {
    let vigente = true;
    Promise.allSettled([
      obtenerInterno(id),
      // RNF-03: para el Administrador del Asilo ni siquiera se pide.
      veFicha ? obtenerFichaMedica(id) : null,
    ]).then(([expediente, clinico]) => {
      if (!vigente) return;

      if (expediente.status === "fulfilled") {
        setInterno(expediente.value);
      } else {
        setError(
          expediente.reason.response?.status === 404
            ? "No existe un interno con ese número. Puede que la dirección esté mal escrita."
            : mensajeDeError(expediente.reason, "No se pudo cargar el perfil del interno"),
        );
      }

      if (clinico.status === "fulfilled") {
        setFicha(clinico.value);
      } else {
        setErrorFicha(mensajeDeError(clinico.reason, "No se pudo cargar la ficha médica"));
      }

      setCargando(false);
    });
    return () => {
      vigente = false;
    };
  }, [id, veFicha]);

  const abrirEdicion = () => {
    setAperturasEdicion((valor) => valor + 1);
    setEdicionAbierta(true);
  };

  const abrirFicha = () => {
    setAperturasFicha((valor) => valor + 1);
    setFichaAbierta(true);
  };

  const abrirReferencia = () => {
    setAperturasReferencia((valor) => valor + 1);
    setReferenciaAbierta(true);
  };

  const familiar = interno?.familiarResponsable;

  return (
    <AppLayout migas={["Inicio", "Internos", interno?.nombreCompleto ?? "Perfil"]}>
      <Link
        to="/internos"
        className="inline-flex min-h-11 items-center gap-2 rounded-tarjeta font-medium text-primario hover:underline"
      >
        <ArrowLeft aria-hidden="true" size={20} />
        Volver a internos
      </Link>

      <div className="mt-4">
        {cargando ? (
          <PerfilSkeleton />
        ) : error ? (
          <div className="flex flex-col items-center gap-4 rounded-tarjeta border border-divisor bg-superficie px-4 py-16 text-center shadow-tarjeta sm:px-6">
            <UserRound aria-hidden="true" size={24} className="text-texto-secundario" />
            <p className="text-texto-secundario">{error}</p>
          </div>
        ) : (
          <>
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <h1 className="text-h1 font-bold">{interno.nombreCompleto}</h1>
                <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-2 text-texto-secundario">
                  <span className="tabular-nums slashed-zero">
                    DPI {formatearDpi(interno.dpi)}
                  </span>
                  <span className="tabular-nums">{edadEnAnios(interno.fechaNacimiento)} años</span>
                  <StatusBadge activo={interno.activo} />
                </div>
              </div>

              <div className="flex flex-wrap gap-3">
                {puedeEditar && (
                  <Button variante="secundario" onClick={abrirEdicion}>
                    <Pencil aria-hidden="true" size={20} />
                    Editar datos
                  </Button>
                )}
                {/* HU-02 (YS-66): el historial se consulta desde el perfil. */}
                {veHistorial && (
                  <BotonEnlace to={`/historial-medico/${interno.id}`} variante="secundario">
                    <ClipboardList aria-hidden="true" size={20} />
                    Ver historial médico
                  </BotonEnlace>
                )}
                {/* YS-85. Desde aquí ya se sabe a quién se refiere: la ventana
                    llega con el interno elegido. Uno inactivo no se ofrece,
                    porque el backend rechazaría la solicitud. */}
                {puedeReferir && interno.activo && (
                  <Button onClick={abrirReferencia}>
                    <Stethoscope aria-hidden="true" size={20} />
                    Referir a especialidad
                  </Button>
                )}
              </div>
            </div>

            <div className="mt-6 grid gap-6 lg:grid-cols-2">
              <section aria-labelledby="titulo-datos" className={TARJETA}>
                <h2 id="titulo-datos" className="text-h3 font-semibold">
                  Datos personales
                </h2>
                <dl className="mt-4 grid gap-4 sm:grid-cols-2">
                  <Dato etiqueta="Fecha de nacimiento">
                    <span className="tabular-nums slashed-zero">
                      {formatearFecha(interno.fechaNacimiento)}
                    </span>
                  </Dato>
                  <Dato etiqueta="Fecha de ingreso">
                    <span className="tabular-nums slashed-zero">
                      {formatearFecha(interno.fechaIngreso)}
                    </span>
                  </Dato>
                  {/* La cuota es del Administrador del Asilo: al personal
                      clínico no le compete (RNF-02). */}
                  {!veFicha && (
                    <Dato etiqueta="Cuota mensual" vacio="Sin definir">
                      {interno.cuotaMensual != null && (
                        <span className="tabular-nums slashed-zero">
                          {formatearQuetzales(interno.cuotaMensual)}
                        </span>
                      )}
                    </Dato>
                  )}
                </dl>
              </section>

              <section aria-labelledby="titulo-familiar" className={TARJETA}>
                <h2 id="titulo-familiar" className="text-h3 font-semibold">
                  Familiar responsable
                </h2>
                <dl className="mt-4 grid gap-4 sm:grid-cols-2">
                  <Dato etiqueta="Nombre" ancho>
                    {familiar.nombreCompleto}
                  </Dato>
                  <Dato etiqueta="Parentesco">{familiar.parentesco}</Dato>
                  <Dato etiqueta="Teléfono">
                    {/* En tableta o teléfono, tocarlo abre la llamada. */}
                    <a
                      href={`tel:${familiar.telefono.replace(/\s/g, "")}`}
                      className="text-primario tabular-nums slashed-zero hover:underline"
                    >
                      {familiar.telefono}
                    </a>
                  </Dato>
                  <Dato etiqueta="Correo electrónico" ancho>
                    {familiar.correo && <span className="break-all">{familiar.correo}</span>}
                  </Dato>
                  <Dato etiqueta="Dirección" ancho>
                    {familiar.direccion}
                  </Dato>
                </dl>
              </section>
            </div>

            {veFicha && (
              <div className="mt-6">
                <FichaMedicaDetalle
                  ficha={ficha}
                  error={errorFicha}
                  puedeEditar={puedeEditar}
                  alEditar={abrirFicha}
                />
              </div>
            )}
          </>
        )}
      </div>

      {puedeEditar && interno && (
        <InternoFormModal
          abierto={edicionAbierta}
          interno={interno}
          claveFormulario={aperturasEdicion}
          alCerrar={() => setEdicionAbierta(false)}
          alGuardar={(guardado) => {
            setEdicionAbierta(false);
            setInterno(guardado);
            mostrar(`Se guardaron los cambios de ${guardado.nombreCompleto}.`);
          }}
        />
      )}

      {puedeReferir && interno && (
        <SolicitudReferenciaFormModal
          abierto={referenciaAbierta}
          internoInicial={interno}
          claveFormulario={aperturasReferencia}
          alCerrar={() => setReferenciaAbierta(false)}
        />
      )}

      {puedeEditar && ficha && (
        <FichaMedicaFormModal
          abierto={fichaAbierta}
          internoId={interno?.id}
          ficha={ficha}
          claveFormulario={aperturasFicha}
          alCerrar={() => setFichaAbierta(false)}
          alGuardar={(guardada) => {
            setFichaAbierta(false);
            setFicha(guardada);
            mostrar("Se guardó la ficha médica.");
          }}
        />
      )}
    </AppLayout>
  );
}
