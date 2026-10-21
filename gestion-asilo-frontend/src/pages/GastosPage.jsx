import { useEffect, useState } from "react";
import { Ban, CircleDollarSign, Plus } from "lucide-react";

import { FiltrosHistorico } from "../components/finanzas/FiltrosHistorico";
import { TotalesPorTipo } from "../components/finanzas/TotalesPorTipo";
import { GastoFormModal } from "../components/gastos/GastoFormModal";
import { Alert } from "../components/ui/Alert";
import { Button } from "../components/ui/Button";
import { FormularioAnulacion } from "../components/ui/FormularioAnulacion";
import { Modal } from "../components/ui/Modal";
import { TablaSkeleton } from "../components/ui/TablaSkeleton";
import { TIPOS_GASTO } from "../constants/estados";
import { useToast } from "../hooks/useToast";
import { AppLayout } from "../layouts/AppLayout";
import { erroresPorCampo, mensajeDeError } from "../services/api";
import { anularGasto, listarGastos } from "../services/gastoService";
import { formatearFecha, formatearFechaHora } from "../utils/fechas";
import { formatearQuetzales } from "../utils/formato";

const ENCABEZADO = "px-4 py-4 text-auxiliar font-semibold text-texto-secundario uppercase xl:px-6";

/**
 * HU-23 (YS-149). "Gastos" del Administrador del Asilo (RF-16): el registro de
 * cada gasto operativo con su tipo (luz, agua y los demás egresos comunes, u
 * otro con su descripción) y el histórico, filtrable por tipo y por rango de
 * fechas, con lo que suma el período en total y por tipo. Uno mal registrado se
 * anula con su motivo, como las donaciones.
 */
export function GastosPage() {
  const { mostrar } = useToast();
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
    listarGastos(filtros)
      .then((respuesta) => {
        if (!vigente) return;
        setDatos(respuesta);
        setError(null);
      })
      .catch((fallo) => {
        if (!vigente) return;
        const porCampo = erroresPorCampo(fallo);
        if (porCampo) setErrores(porCampo);
        else setError(mensajeDeError(fallo, "No se pudieron cargar los gastos"));
      })
      .finally(() => {
        if (vigente) setCargando(false);
      });
    return () => {
      vigente = false;
    };
  }, [filtros, recargas]);

  const buscar = (nuevos) => {
    setErrores({});
    setCargando(true);
    setFiltros(nuevos);
  };

  const recargar = () => setRecargas((valor) => valor + 1);
  const gastos = datos?.gastos ?? [];
  const conFiltros = Boolean(filtros.tipo || filtros.desde || filtros.hasta);

  return (
    <AppLayout migas={["Inicio", "Gastos"]}>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-h1 font-bold">Gastos</h1>
          <p className="mt-2 text-texto-secundario">
            Los gastos operativos del asilo: luz, agua, teléfono, alimentación y los demás egresos de su operación.
          </p>
        </div>
        <Button
          onClick={() => {
            setAperturas((valor) => valor + 1);
            setRegistrando(true);
          }}
        >
          <Plus aria-hidden="true" size={20} />
          Registrar gasto
        </Button>
      </div>

      <FiltrosHistorico
        etiquetaTipo="Tipo de gasto"
        etiquetaTodos="Todos los tipos"
        opciones={TIPOS_GASTO}
        cargando={cargando}
        errores={errores}
        alBuscar={buscar}
      />

      {error && (
        <div className="mt-6">
          <Alert>{error}</Alert>
        </div>
      )}

      {datos && (
        <TotalesPorTipo
          etiqueta="Lo que suman los gastos del período"
          titulo={conFiltros ? "Total del período" : "Total de todos los gastos"}
          total={datos.total}
          grupos={datos.porTipo.map((grupo) => ({
            clave: grupo.tipoGasto,
            etiqueta: TIPOS_GASTO[grupo.tipoGasto],
            cantidad: grupo.cantidad,
            total: grupo.total,
          }))}
          unidad={["gasto", "gastos"]}
          soloConMovimientos
        />
      )}

      <div className="mt-6">
        {cargando && !datos ? (
          <TablaSkeleton etiqueta="Cargando gastos…" />
        ) : gastos.length === 0 ? (
          !error && (
            <div className="flex flex-col items-center gap-4 rounded-tarjeta border border-divisor bg-superficie px-4 py-16 text-center shadow-tarjeta sm:px-6">
              <CircleDollarSign aria-hidden="true" size={24} className="text-texto-secundario" />
              <p className="max-w-md text-texto-secundario">
                {conFiltros
                  ? "No hay gastos con esos filtros."
                  : "Todavía no hay gastos registrados. Registre el primero con «Registrar gasto»."}
              </p>
            </div>
          )
        ) : (
          /* relative: los sr-only de la tabla son absolutos; sin él escapan del recorte (YS-175). */
          <div className="relative overflow-x-auto rounded-tarjeta border border-divisor bg-superficie shadow-tarjeta">
            <table className="w-full min-w-3xl border-collapse text-left">
              <caption className="sr-only">Histórico de gastos operativos: fecha, tipo, descripción y monto</caption>
              <thead>
                <tr className="border-b border-divisor bg-fondo">
                  <th scope="col" className={`${ENCABEZADO} w-px whitespace-nowrap`}>
                    Fecha
                  </th>
                  <th scope="col" className={ENCABEZADO}>
                    Gasto
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
                {gastos.map((gasto) => (
                  <tr key={gasto.id} className="border-b border-divisor align-top last:border-b-0">
                    <td className="px-4 py-4 whitespace-nowrap tabular-nums slashed-zero xl:px-6">
                      {formatearFecha(gasto.fecha)}
                    </td>
                    <td className="px-4 py-4 xl:px-6">
                      <span className={`font-medium ${gasto.anulado ? "text-texto-secundario line-through" : ""}`}>
                        {TIPOS_GASTO[gasto.tipoGasto]}
                      </span>
                      {gasto.descripcion && <span className="block text-texto-secundario">{gasto.descripcion}</span>}
                      <span className="block text-auxiliar text-texto-secundario">Registró {gasto.registradoPor}</span>
                      {gasto.anulado && (
                        <span className="mt-1 block text-auxiliar text-error">
                          Anulado por {gasto.anuladoPor} el {formatearFechaHora(gasto.fechaAnulacion)}:{" "}
                          {gasto.motivoAnulacion}
                        </span>
                      )}
                    </td>
                    <td
                      className={`px-4 py-4 text-right font-semibold whitespace-nowrap tabular-nums slashed-zero xl:px-6 ${
                        gasto.anulado ? "text-texto-secundario line-through" : ""
                      }`}
                    >
                      {formatearQuetzales(gasto.monto)}
                    </td>
                    <td className="px-4 py-4 text-right whitespace-nowrap xl:px-6">
                      {!gasto.anulado && (
                        <Button
                          variante="secundario"
                          onClick={() => {
                            setAperturas((valor) => valor + 1);
                            setEnAnulacion(gasto);
                          }}
                          aria-label={`Anular el gasto de ${formatearQuetzales(gasto.monto)} (${TIPOS_GASTO[gasto.tipoGasto]}) del ${formatearFecha(gasto.fecha)}`}
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

      <GastoFormModal
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
        titulo="Anular gasto"
        descripcion={
          enAnulacion ? `${TIPOS_GASTO[enAnulacion.tipoGasto]} · ${formatearQuetzales(enAnulacion.monto)}` : undefined
        }
      >
        {enAnulacion && (
          <FormularioAnulacion
            key={aperturas}
            aviso={`El gasto de ${formatearQuetzales(enAnulacion.monto)} del ${formatearFecha(enAnulacion.fecha)} deja de contar en los totales. No se borra: queda en el histórico como anulado, con el motivo.`}
            etiquetaBoton="Anular gasto"
            anular={(motivo) => anularGasto(enAnulacion.id, motivo)}
            alCerrar={() => setEnAnulacion(null)}
            alAnular={() => {
              setEnAnulacion(null);
              mostrar("Se anuló el gasto.");
              recargar();
            }}
          />
        )}
      </Modal>
    </AppLayout>
  );
}
