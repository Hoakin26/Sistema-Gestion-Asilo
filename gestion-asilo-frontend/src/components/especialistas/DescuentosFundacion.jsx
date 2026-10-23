import { useEffect, useRef, useState } from "react";
import { Pencil } from "lucide-react";

import { CONCEPTOS_CUENTA, POR_CARGO, TIPOS_SERVICIO } from "../../constants/estados";
import { useToast } from "../../hooks/useToast";
import { erroresPorCampo, mensajeDeError } from "../../services/api";
import { definirPolitica, listarPoliticas } from "../../services/descuentoService";
import { formatearFecha, hoyISO, inicioDeMesISO } from "../../utils/fechas";
import { formatearDescuento } from "../../utils/formato";
import { enfocarPrimerError } from "../../utils/formularios";
import { normalizarMonto } from "../../utils/montos";
import { Alert } from "../ui/Alert";
import { Button, IconButton } from "../ui/Button";
import { CurrencyInput } from "../ui/CurrencyInput";
import { FormField } from "../ui/FormField";
import { Input, Select } from "../ui/Input";
import { Modal } from "../ui/Modal";
import { TablaSkeleton } from "../ui/TablaSkeleton";

const ENCABEZADO =
  "px-4 py-4 text-auxiliar font-semibold text-texto-secundario uppercase xl:px-6";

/* Un monto o un porcentaje: dígitos, y si hay punto, uno o dos decimales. */
const NUMERO = /^\d{1,8}(\.\d{1,2})?$/;

/** Mismas reglas y mensajes que el backend (PoliticaDescuentoRequest y PoliticaDescuentoService). */
function validar({ tipoDescuento, valor, vigenteDesde }) {
  const errores = {};
  const limpio = normalizarMonto(valor);
  if (!limpio) errores.valor = "Indique el valor del descuento";
  else if (limpio.startsWith("-")) errores.valor = "El descuento no puede ser negativo";
  else if (!NUMERO.test(limpio)) {
    errores.valor = tipoDescuento === "PORCENTAJE" ? "Escriba un porcentaje como 20 o 12.5" : "Escriba un monto como 50 o 50.50";
  } else if (tipoDescuento === "PORCENTAJE" && Number(limpio) > 100) {
    errores.valor = "Un porcentaje no puede pasar de 100";
  }
  if (!vigenteDesde) errores.vigenteDesde = "Indique desde qué fecha rige";
  return errores;
}

function FormularioDescuento({ tipoServicio, actual, alCerrar, alGuardar }) {
  const refFormulario = useRef(null);
  const [tipoDescuento, setTipoDescuento] = useState(actual?.tipoDescuento ?? "PORCENTAJE");
  const [valor, setValor] = useState(actual ? String(Number(actual.valor)) : "");
  // Por defecto, desde el inicio del mes: cubre los cargos del mes que todavía no se cobran.
  const [vigenteDesde, setVigenteDesde] = useState(inicioDeMesISO);
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState(null);
  const [guardando, setGuardando] = useState(false);

  const fallar = (encontrados) => {
    setErrores(encontrados);
    enfocarPrimerError(refFormulario.current);
  };

  const enviar = async (evento) => {
    evento.preventDefault();
    const encontrados = validar({ tipoDescuento, valor, vigenteDesde });
    if (Object.keys(encontrados).length > 0) {
      fallar(encontrados);
      return;
    }
    setGuardando(true);
    setErrorGeneral(null);
    try {
      alGuardar(await definirPolitica(tipoServicio, { tipoDescuento, valor: normalizarMonto(valor), vigenteDesde }));
    } catch (fallo) {
      const porCampo = erroresPorCampo(fallo);
      if (porCampo) fallar(porCampo);
      else setErrorGeneral(mensajeDeError(fallo, "No se pudo guardar el descuento"));
    } finally {
      setGuardando(false);
    }
  };

  const cambiarValor = (evento) => {
    setValor(evento.target.value);
    setErrores((previos) => ({ ...previos, valor: undefined }));
  };

  return (
    <form ref={refFormulario} onSubmit={enviar} noValidate>
      <div className="flex flex-col gap-6 px-4 py-6 sm:px-6">
        {errorGeneral && <Alert>{errorGeneral}</Alert>}

        <FormField etiqueta="Tipo de descuento" obligatorio error={errores.tipoDescuento}>
          {(campo) => (
            <Select
              {...campo}
              value={tipoDescuento}
              onChange={(evento) => {
                setTipoDescuento(evento.target.value);
                setErrores((previos) => ({ ...previos, tipoDescuento: undefined, valor: undefined }));
              }}
              className="sm:w-80"
            >
              <option value="PORCENTAJE">Porcentaje del costo</option>
              <option value="MONTO_FIJO">Monto fijo {POR_CARGO[tipoServicio]}</option>
            </Select>
          )}
        </FormField>

        <FormField
          etiqueta={tipoDescuento === "PORCENTAJE" ? "Porcentaje" : "Monto"}
          obligatorio
          error={errores.valor}
          ayuda={
            tipoDescuento === "PORCENTAJE"
              ? "De 0 a 100; 0 es sin descuento."
              : `En quetzales, ${POR_CARGO[tipoServicio]}. Si pasa del costo, ese cargo queda en cero, nunca negativo.`
          }
        >
          {(campo) =>
            tipoDescuento === "PORCENTAJE" ? (
              <div className="relative sm:w-56">
                <Input
                  {...campo}
                  inputMode="decimal"
                  autoComplete="off"
                  value={valor}
                  onChange={cambiarValor}
                  className="pr-10 text-right tabular-nums slashed-zero"
                />
                <span
                  aria-hidden="true"
                  className="pointer-events-none absolute top-1/2 right-4 -translate-y-1/2 text-texto-secundario"
                >
                  %
                </span>
              </div>
            ) : (
              <CurrencyInput {...campo} className="sm:w-56" value={valor} onChange={cambiarValor} />
            )
          }
        </FormField>

        <FormField
          etiqueta="Vigente desde"
          obligatorio
          error={errores.vigenteDesde}
          ayuda="Aplica a los servicios desde esa fecha."
        >
          {(campo) => (
            <Input
              {...campo}
              type="date"
              value={vigenteDesde}
              onChange={(evento) => {
                setVigenteDesde(evento.target.value);
                setErrores((previos) => ({ ...previos, vigenteDesde: undefined }));
              }}
              className="sm:w-56"
            />
          )}
        </FormField>
      </div>

      <div className="sticky bottom-0 flex flex-wrap justify-end gap-3 border-t border-divisor bg-superficie px-4 py-4 sm:px-6">
        <Button type="button" variante="secundario" onClick={alCerrar}>
          Cancelar
        </Button>
        <Button type="submit" cargando={guardando}>
          Guardar descuento
        </Button>
      </div>
    </form>
  );
}

/**
 * HU-19 (YS-137). Los descuentos de la fundación por tipo de servicio, junto a
 * las tarifas de consulta (decidido el 20/10): los dos son los precios que fija
 * la fundación. Cada cambio es una versión nueva, vigente desde una fecha; las
 * anteriores siguen rigiendo para los servicios de antes.
 */
export function DescuentosFundacion() {
  const { mostrar } = useToast();
  const [politicas, setPoliticas] = useState(null);
  const [error, setError] = useState(null);
  const [enEdicion, setEnEdicion] = useState(null);
  const [modalAbierto, setModalAbierto] = useState(false);
  const [aperturas, setAperturas] = useState(0);

  const cargar = () =>
    listarPoliticas()
      .then((lista) => {
        setPoliticas(lista);
        setError(null);
      })
      .catch((fallo) => {
        setPoliticas((previas) => previas ?? []);
        setError(mensajeDeError(fallo, "No se pudieron cargar los descuentos"));
      });

  useEffect(() => {
    let vigente = true;
    listarPoliticas()
      .then((lista) => {
        if (vigente) setPoliticas(lista);
      })
      .catch((fallo) => {
        if (!vigente) return;
        setPoliticas([]);
        setError(mensajeDeError(fallo, "No se pudieron cargar los descuentos"));
      });
    return () => {
      vigente = false;
    };
  }, []);

  const hoy = hoyISO();
  const lista = politicas ?? [];
  const vigenteDe = (tipo) => lista.find((p) => p.tipoServicio === tipo && p.vigente);
  // El próximo cambio ya programado: la versión más cercana que todavía no empieza.
  const proximoDe = (tipo) =>
    lista.filter((p) => p.tipoServicio === tipo && p.vigenteDesde > hoy).at(-1);

  const editar = (tipo) => {
    setEnEdicion(tipo);
    setAperturas((valor) => valor + 1);
    setModalAbierto(true);
  };

  return (
    <section aria-labelledby="titulo-descuentos" className="mt-12">
      <h2 id="titulo-descuentos" className="text-h2 font-semibold">
        Descuentos de la fundación
      </h2>
      <p className="mt-2 max-w-2xl text-texto-secundario">
        Porcentaje o monto fijo por servicio. La cuota base no lleva descuento.
      </p>

      {error && (
        <div className="mt-6">
          <Alert>{error}</Alert>
        </div>
      )}

      <div className="mt-6">
        {politicas === null ? (
          <TablaSkeleton etiqueta="Cargando descuentos…" filas={3} />
        ) : (
          <div className="relative overflow-x-auto rounded-tarjeta border border-divisor bg-superficie shadow-tarjeta">
            <table className="w-full min-w-2xl border-collapse text-left">
              <caption className="sr-only">Descuento vigente de cada tipo de servicio y su próximo cambio</caption>
              <thead>
                <tr className="border-b border-divisor bg-fondo">
                  <th scope="col" className={ENCABEZADO}>
                    Servicio
                  </th>
                  <th scope="col" className={ENCABEZADO}>
                    Descuento vigente
                  </th>
                  <th scope="col" className={ENCABEZADO}>
                    Próximo cambio
                  </th>
                  <th scope="col" className={ENCABEZADO}>
                    <span className="sr-only">Acciones</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {TIPOS_SERVICIO.map((tipo) => {
                  const vigente = vigenteDe(tipo);
                  const proximo = proximoDe(tipo);
                  return (
                    <tr key={tipo} className="border-b border-divisor last:border-b-0">
                      <td className="px-4 py-4 font-medium text-texto xl:px-6">{CONCEPTOS_CUENTA[tipo]}</td>
                      <td className="px-4 py-4 xl:px-6">
                        {vigente ? (
                          <>
                            <span className="block tabular-nums slashed-zero">
                              {formatearDescuento(vigente, POR_CARGO[tipo])}
                            </span>
                            <span className="block text-auxiliar text-texto-secundario">
                              Desde el {formatearFecha(vigente.vigenteDesde)}
                            </span>
                          </>
                        ) : (
                          <span className="text-texto-secundario">Sin descuento</span>
                        )}
                      </td>
                      <td className="px-4 py-4 xl:px-6">
                        {proximo ? (
                          <span className="tabular-nums slashed-zero">
                            {formatearDescuento(proximo, POR_CARGO[tipo])} desde el {formatearFecha(proximo.vigenteDesde)}
                          </span>
                        ) : (
                          <span className="text-texto-secundario">—</span>
                        )}
                      </td>
                      <td className="px-4 py-4 xl:px-6">
                        <div className="flex justify-end">
                          <IconButton etiqueta={`Cambiar el descuento de ${CONCEPTOS_CUENTA[tipo]}`} onClick={() => editar(tipo)}>
                            <Pencil aria-hidden="true" size={20} />
                          </IconButton>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Cada versión, para saber qué regía cuando se dio un servicio. */}
      {lista.length > 0 && (
        <details className="mt-4">
          <summary className="inline-flex min-h-11 cursor-pointer items-center font-medium text-primario">
            Historial de cambios ({lista.length})
          </summary>
          <ul className="mt-2 divide-y divide-divisor rounded-tarjeta border border-divisor bg-superficie">
            {lista.map((politica) => (
              <li key={politica.id} className="flex flex-wrap items-baseline justify-between gap-x-6 gap-y-1 px-4 py-3">
                <span className="font-medium text-texto">{CONCEPTOS_CUENTA[politica.tipoServicio]}</span>
                <span className="tabular-nums slashed-zero">
                  {formatearDescuento(politica, POR_CARGO[politica.tipoServicio])} desde el{" "}
                  {formatearFecha(politica.vigenteDesde)}
                </span>
                <span className="text-auxiliar text-texto-secundario">Definido por {politica.registradaPor}</span>
              </li>
            ))}
          </ul>
        </details>
      )}

      <Modal
        abierto={modalAbierto}
        alCerrar={() => setModalAbierto(false)}
        titulo="Descuento de la fundación"
        descripcion={enEdicion ? CONCEPTOS_CUENTA[enEdicion] : undefined}
      >
        {enEdicion && (
          <FormularioDescuento
            key={aperturas}
            tipoServicio={enEdicion}
            actual={vigenteDe(enEdicion)}
            alCerrar={() => setModalAbierto(false)}
            alGuardar={(guardada) => {
              setModalAbierto(false);
              cargar();
              mostrar(
                `${CONCEPTOS_CUENTA[guardada.tipoServicio]}: ${formatearDescuento(guardada, POR_CARGO[guardada.tipoServicio])} desde el ${formatearFecha(guardada.vigenteDesde)}.`,
              );
            }}
          />
        )}
      </Modal>
    </section>
  );
}
