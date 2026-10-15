import { useEffect, useRef, useState } from "react";
import { Pencil, TriangleAlert } from "lucide-react";

import { useToast } from "../../hooks/useToast";
import { erroresPorCampo, mensajeDeError } from "../../services/api";
import { actualizarTarifa, listarTarifas } from "../../services/especialistaService";
import { formatearQuetzales } from "../../utils/formato";
import { enfocarPrimerError } from "../../utils/formularios";
import { Alert } from "../ui/Alert";
import { Button, IconButton } from "../ui/Button";
import { FormField } from "../ui/FormField";
import { Input } from "../ui/Input";
import { Modal } from "../ui/Modal";
import { TablaSkeleton } from "../ui/TablaSkeleton";

const ENCABEZADO =
  "px-4 py-4 text-auxiliar font-semibold text-texto-secundario uppercase xl:px-6";

/* Un monto en quetzales: dígitos, y si hay punto, uno o dos decimales. */
const MONTO = /^\d{1,8}(\.\d{1,2})?$/;

/** Mismas reglas que el backend (TarifaRequest), con la coma aceptada como punto. */
function validar(texto) {
  const limpio = texto.trim().replace(",", ".");
  if (!limpio) return { error: "Indique la tarifa de consulta" };
  if (limpio.startsWith("-")) return { error: "La tarifa no puede ser negativa" };
  if (!MONTO.test(limpio)) return { error: "Escriba un monto como 350 o 350.50" };
  return { monto: limpio };
}

function FormularioTarifa({ especialidad, alCerrar, alGuardar }) {
  const refFormulario = useRef(null);
  const [texto, setTexto] = useState(
    especialidad.tarifaConsulta == null ? "" : Number(especialidad.tarifaConsulta).toFixed(2),
  );
  const [error, setError] = useState(null);
  const [errorGeneral, setErrorGeneral] = useState(null);
  const [guardando, setGuardando] = useState(false);

  const fallar = (mensaje) => {
    setError(mensaje);
    enfocarPrimerError(refFormulario.current);
  };

  const enviar = async (evento) => {
    evento.preventDefault();
    const { error: encontrado, monto } = validar(texto);
    if (encontrado) {
      fallar(encontrado);
      return;
    }

    setGuardando(true);
    setErrorGeneral(null);
    try {
      // Como texto: un número de JavaScript podría convertir 350.10 en
      // 350.09999…, y el backend lo lee como BigDecimal.
      alGuardar(await actualizarTarifa(especialidad.id, monto));
    } catch (fallo) {
      const porCampo = erroresPorCampo(fallo);
      if (porCampo?.tarifaConsulta) fallar(porCampo.tarifaConsulta);
      else setErrorGeneral(mensajeDeError(fallo, "No se pudo guardar la tarifa"));
    } finally {
      setGuardando(false);
    }
  };

  return (
    <form ref={refFormulario} onSubmit={enviar} noValidate>
      <div className="flex flex-col gap-6 px-4 py-6 sm:px-6">
        {errorGeneral && <Alert>{errorGeneral}</Alert>}

        <FormField
          etiqueta="Tarifa de consulta"
          obligatorio
          error={error}
          ayuda="En quetzales. Las visitas que ya empezaron conservan la tarifa que tenían."
        >
          {(campo) => (
            <div className="relative sm:w-56">
              <span
                aria-hidden="true"
                className="pointer-events-none absolute top-1/2 left-4 -translate-y-1/2 text-texto-secundario"
              >
                Q
              </span>
              <Input
                {...campo}
                inputMode="decimal"
                value={texto}
                onChange={(evento) => {
                  setTexto(evento.target.value);
                  setError(null);
                }}
                placeholder="350.00"
                className="pl-10 text-right tabular-nums slashed-zero"
              />
            </div>
          )}
        </FormField>
      </div>

      <div className="sticky bottom-0 flex flex-wrap justify-end gap-3 border-t border-divisor bg-superficie px-4 py-4 sm:px-6">
        <Button type="button" variante="secundario" onClick={alCerrar}>
          Cancelar
        </Button>
        <Button type="submit" cargando={guardando}>
          Guardar tarifa
        </Button>
      </div>
    </form>
  );
}

/**
 * YS-169. La tarifa de consulta de cada especialidad, decidida el 09/10: la
 * define la Fundación y cada visita copia la vigente al iniciarse. La suman
 * la cuenta del familiar (RF-12), las cuentas por pagar (RF-13) y el reporte
 * de costos (RF-17).
 */
export function TarifasConsulta() {
  const { mostrar } = useToast();
  const [tarifas, setTarifas] = useState(null);
  const [error, setError] = useState(null);

  const [enEdicion, setEnEdicion] = useState(null);
  const [modalAbierto, setModalAbierto] = useState(false);
  const [aperturas, setAperturas] = useState(0);

  useEffect(() => {
    let vigente = true;
    listarTarifas()
      .then((lista) => {
        if (vigente) setTarifas(lista);
      })
      .catch((fallo) => {
        if (!vigente) return;
        setTarifas([]);
        setError(mensajeDeError(fallo, "No se pudieron cargar las tarifas"));
      });
    return () => {
      vigente = false;
    };
  }, []);

  const editar = (especialidad) => {
    setEnEdicion(especialidad);
    setAperturas((valor) => valor + 1);
    setModalAbierto(true);
  };

  const sinTarifa = tarifas?.filter((t) => t.tarifaConsulta == null).length ?? 0;

  return (
    <section aria-labelledby="titulo-tarifas" className="mt-12">
      <h2 id="titulo-tarifas" className="text-h2 font-semibold">
        Tarifas de consulta
      </h2>
      <p className="mt-2 max-w-2xl text-texto-secundario">
        Lo que cuesta una consulta de cada especialidad. Cada visita copia la tarifa vigente al
        iniciarse, y de ahí la toman la cuenta del familiar y los reportes.
      </p>

      {error && (
        <div className="mt-6">
          <Alert>{error}</Alert>
        </div>
      )}

      {/* La visita se inicia igual sin tarifa —el especialista no puede
          arreglarlo—, pero quedaría sin costo de consulta. */}
      {sinTarifa > 0 && (
        <div className="mt-6">
          <Alert tipo="info" rol="status">
            {sinTarifa === 1
              ? "Una especialidad no tiene tarifa."
              : `${sinTarifa} especialidades no tienen tarifa.`}{" "}
            Sus visitas se registrarán sin costo de consulta hasta que la defina.
          </Alert>
        </div>
      )}

      <div className="mt-6">
        {tarifas === null ? (
          <TablaSkeleton etiqueta="Cargando tarifas…" filas={3} />
        ) : (
          tarifas.length > 0 && (
            <div className="overflow-x-auto rounded-tarjeta border border-divisor bg-superficie shadow-tarjeta">
              <table className="w-full border-collapse text-left">
                <caption className="sr-only">Tarifa de consulta de cada especialidad</caption>
                <thead>
                  <tr className="border-b border-divisor bg-fondo">
                    <th scope="col" className={ENCABEZADO}>
                      Especialidad
                    </th>
                    <th scope="col" className={`${ENCABEZADO} text-right`}>
                      Tarifa de consulta
                    </th>
                    <th scope="col" className={ENCABEZADO}>
                      <span className="sr-only">Acciones</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {tarifas.map((especialidad) => (
                    <tr key={especialidad.id} className="border-b border-divisor last:border-b-0">
                      <td className="px-4 py-4 font-medium text-texto xl:px-6">{especialidad.nombre}</td>
                      <td className="px-4 py-4 text-right xl:px-6">
                        {especialidad.tarifaConsulta == null ? (
                          <span className="inline-flex items-center gap-2 text-advertencia">
                            <TriangleAlert aria-hidden="true" size={16} />
                            Sin tarifa
                          </span>
                        ) : (
                          <span className="tabular-nums slashed-zero">
                            {formatearQuetzales(especialidad.tarifaConsulta)}
                          </span>
                        )}
                      </td>
                      <td className="px-4 py-4 xl:px-6">
                        <div className="flex justify-end">
                          <IconButton
                            etiqueta={`Cambiar la tarifa de ${especialidad.nombre}`}
                            onClick={() => editar(especialidad)}
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
          )
        )}
      </div>

      <Modal
        abierto={modalAbierto}
        alCerrar={() => setModalAbierto(false)}
        titulo="Tarifa de consulta"
        descripcion={enEdicion?.nombre}
      >
        {enEdicion && (
          <FormularioTarifa
            key={aperturas}
            especialidad={enEdicion}
            alCerrar={() => setModalAbierto(false)}
            alGuardar={(guardada) => {
              setModalAbierto(false);
              setTarifas((previas) => previas.map((t) => (t.id === guardada.id ? guardada : t)));
              mostrar(`La consulta de ${guardada.nombre} cuesta ahora ${formatearQuetzales(guardada.tarifaConsulta)}.`);
            }}
          />
        )}
      </Modal>
    </section>
  );
}
