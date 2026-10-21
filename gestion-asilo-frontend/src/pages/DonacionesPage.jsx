import { useEffect, useState } from "react";
import { Ban, Gift, Plus, Search } from "lucide-react";

import { DonacionFormModal } from "../components/donaciones/DonacionFormModal";
import { Alert } from "../components/ui/Alert";
import { Button } from "../components/ui/Button";
import { FormField } from "../components/ui/FormField";
import { FormularioAnulacion } from "../components/ui/FormularioAnulacion";
import { Input, Select } from "../components/ui/Input";
import { Modal } from "../components/ui/Modal";
import { TablaSkeleton } from "../components/ui/TablaSkeleton";
import { TIPOS_DONANTE } from "../constants/estados";
import { useToast } from "../hooks/useToast";
import { AppLayout } from "../layouts/AppLayout";
import { erroresPorCampo, mensajeDeError } from "../services/api";
import { anularDonacion, listarDonaciones } from "../services/donacionService";
import { formatearFecha, formatearFechaHora } from "../utils/fechas";
import { formatearQuetzales } from "../utils/formato";

const ENCABEZADO = "px-4 py-4 text-auxiliar font-semibold text-texto-secundario uppercase xl:px-6";

/**
 * HU-22 (YS-146). "Donaciones" del Administrador del Asilo (RF-15): el registro
 * de cada donación monetaria con su donante y su tipo, y el histórico,
 * filtrable por tipo de donante y por rango de fechas, con lo que suma el
 * período en total y por tipo. Una mal registrada se anula con su motivo:
 * queda en el histórico tachada y no cuenta en los totales.
 */
export function DonacionesPage() {
  const { mostrar } = useToast();
  const [tipo, setTipo] = useState("");
  const [desde, setDesde] = useState("");
  const [hasta, setHasta] = useState("");
  // Lo que de verdad se consulta: cambia al pulsar "Buscar", no en cada cambio.
  const [filtros, setFiltros] = useState({ tipo: "", desde: "", hasta: "" });
  // Registrar o anular cambia el histórico: se vuelve a pedir con los mismos filtros.
  const [recargas, setRecargas] = useState(0);

  const [datos, setDatos] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [errores, setErrores] = useState({});

  const [registrando, setRegistrando] = useState(false);
  const [enAnulacion, setEnAnulacion] = useState(null);
  const [aperturas, setAperturas] = useState(0);

  useEffect(() => {
    let vigente = true;
    listarDonaciones(filtros)
      .then((respuesta) => {
        if (!vigente) return;
        setDatos(respuesta);
        setError(null);
      })
      .catch((fallo) => {
        if (!vigente) return;
        const porCampo = erroresPorCampo(fallo);
        if (porCampo) setErrores(porCampo);
        else setError(mensajeDeError(fallo, "No se pudieron cargar las donaciones"));
      })
      .finally(() => {
        if (vigente) setCargando(false);
      });
    return () => {
      vigente = false;
    };
  }, [filtros, recargas]);

  const buscar = (evento) => {
    evento.preventDefault();
    const encontrados = {};
    if (desde && hasta && hasta < desde) encontrados.hasta = "La fecha final no puede ser anterior a la inicial";
    setErrores(encontrados);
    if (Object.keys(encontrados).length > 0) return;
    setCargando(true);
    setFiltros({ tipo, desde, hasta });
  };

  const recargar = () => setRecargas((valor) => valor + 1);
  const donaciones = datos?.donaciones ?? [];
  const conFiltros = Boolean(filtros.tipo || filtros.desde || filtros.hasta);

  return (
    <AppLayout migas={["Inicio", "Donaciones"]}>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-h1 font-bold">Donaciones</h1>
          <p className="mt-2 text-texto-secundario">
            Las donaciones monetarias que recibe el asilo, con su donante y su tipo.
          </p>
        </div>
        <Button
          onClick={() => {
            setAperturas((valor) => valor + 1);
            setRegistrando(true);
          }}
        >
          <Plus aria-hidden="true" size={20} />
          Registrar donación
        </Button>
      </div>

      <form onSubmit={buscar} noValidate className="mt-6 flex flex-wrap items-end gap-4">
        <FormField etiqueta="Tipo de donante" error={errores.tipo}>
          {(campo) => (
            <Select {...campo} value={tipo} onChange={(evento) => setTipo(evento.target.value)} className="sm:w-64">
              <option value="">Todos los tipos</option>
              {Object.entries(TIPOS_DONANTE).map(([clave, etiqueta]) => (
                <option key={clave} value={clave}>
                  {etiqueta}
                </option>
              ))}
            </Select>
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

      {datos && (
        <section
          aria-label="Lo que suman las donaciones del período"
          className="mt-6 rounded-tarjeta border border-divisor bg-superficie p-4 shadow-tarjeta sm:p-6"
        >
          <div className="flex flex-wrap items-baseline justify-between gap-4">
            <p className="text-texto-secundario">{conFiltros ? "Total del período" : "Total de todas las donaciones"}</p>
            <p className="text-h3 font-semibold tabular-nums slashed-zero">{formatearQuetzales(datos.total)}</p>
          </div>
          <dl className="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            {datos.porTipo.map((grupo) => (
              <div key={grupo.tipoDonante} className="rounded-tarjeta bg-fondo px-4 py-3">
                <dt className="text-auxiliar text-texto-secundario">{TIPOS_DONANTE[grupo.tipoDonante]}</dt>
                <dd className="mt-1 tabular-nums slashed-zero">
                  <span className="font-semibold">{formatearQuetzales(grupo.total)}</span>
                  <span className="text-auxiliar text-texto-secundario">
                    {" "}
                    · {grupo.cantidad === 1 ? "1 donación" : `${grupo.cantidad} donaciones`}
                  </span>
                </dd>
              </div>
            ))}
          </dl>
        </section>
      )}

      <div className="mt-6">
        {cargando && !datos ? (
          <TablaSkeleton etiqueta="Cargando donaciones…" />
        ) : donaciones.length === 0 ? (
          !error && (
            <div className="flex flex-col items-center gap-4 rounded-tarjeta border border-divisor bg-superficie px-4 py-16 text-center shadow-tarjeta sm:px-6">
              <Gift aria-hidden="true" size={24} className="text-texto-secundario" />
              <p className="max-w-md text-texto-secundario">
                {conFiltros
                  ? "No hay donaciones con esos filtros."
                  : "Todavía no hay donaciones registradas. Registre la primera con «Registrar donación»."}
              </p>
            </div>
          )
        ) : (
          /* relative: los sr-only de la tabla son absolutos; sin él escapan del recorte (YS-175). */
          <div className="relative overflow-x-auto rounded-tarjeta border border-divisor bg-superficie shadow-tarjeta">
            <table className="w-full min-w-3xl border-collapse text-left">
              <caption className="sr-only">Histórico de donaciones: fecha, donante, tipo de donante y monto</caption>
              <thead>
                <tr className="border-b border-divisor bg-fondo">
                  <th scope="col" className={`${ENCABEZADO} w-px whitespace-nowrap`}>
                    Fecha
                  </th>
                  <th scope="col" className={ENCABEZADO}>
                    Donante
                  </th>
                  <th scope="col" className={`${ENCABEZADO} w-px whitespace-nowrap`}>
                    Tipo de donante
                  </th>
                  <th scope="col" className={`${ENCABEZADO} w-px text-right whitespace-nowrap`}>
                    Monto
                  </th>
                  <th scope="col" className={`${ENCABEZADO} w-px`}>
                    <span className="sr-only">Acciones</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {donaciones.map((donacion) => (
                  <tr key={donacion.id} className="border-b border-divisor align-top last:border-b-0">
                    <td className="px-4 py-4 whitespace-nowrap tabular-nums slashed-zero xl:px-6">
                      {formatearFecha(donacion.fecha)}
                    </td>
                    <td className="px-4 py-4 xl:px-6">
                      <span className={`font-medium ${donacion.anulada ? "text-texto-secundario line-through" : ""}`}>
                        {donacion.donante}
                      </span>
                      <span className="block text-auxiliar text-texto-secundario">Registró {donacion.registradaPor}</span>
                      {donacion.anulada && (
                        <span className="mt-1 block text-auxiliar text-error">
                          Anulada por {donacion.anuladaPor} el {formatearFechaHora(donacion.fechaAnulacion)}:{" "}
                          {donacion.motivoAnulacion}
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap xl:px-6">{TIPOS_DONANTE[donacion.tipoDonante]}</td>
                    <td
                      className={`px-4 py-4 text-right font-semibold whitespace-nowrap tabular-nums slashed-zero xl:px-6 ${
                        donacion.anulada ? "text-texto-secundario line-through" : ""
                      }`}
                    >
                      {formatearQuetzales(donacion.monto)}
                    </td>
                    <td className="px-4 py-4 text-right whitespace-nowrap xl:px-6">
                      {!donacion.anulada && (
                        <Button
                          variante="secundario"
                          onClick={() => {
                            setAperturas((valor) => valor + 1);
                            setEnAnulacion(donacion);
                          }}
                          aria-label={`Anular la donación de ${formatearQuetzales(donacion.monto)} de ${donacion.donante}`}
                        >
                          <Ban aria-hidden="true" size={20} />
                          Anular
                        </Button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <DonacionFormModal
        abierto={registrando}
        claveFormulario={aperturas}
        alCerrar={() => setRegistrando(false)}
        alRegistrar={() => {
          setRegistrando(false);
          recargar();
        }}
      />

      <Modal
        abierto={enAnulacion !== null}
        alCerrar={() => setEnAnulacion(null)}
        titulo="Anular donación"
        descripcion={enAnulacion ? `${formatearQuetzales(enAnulacion.monto)} · ${enAnulacion.donante}` : undefined}
      >
        {enAnulacion && (
          <FormularioAnulacion
            key={aperturas}
            aviso={`La donación de ${formatearQuetzales(enAnulacion.monto)} del ${formatearFecha(enAnulacion.fecha)} deja de contar en los totales. No se borra: queda en el histórico como anulada, con el motivo.`}
            etiquetaBoton="Anular donación"
            anular={(motivo) => anularDonacion(enAnulacion.id, motivo)}
            alCerrar={() => setEnAnulacion(null)}
            alAnular={() => {
              setEnAnulacion(null);
              mostrar("Se anuló la donación.");
              recargar();
            }}
          />
        )}
      </Modal>
    </AppLayout>
  );
}
