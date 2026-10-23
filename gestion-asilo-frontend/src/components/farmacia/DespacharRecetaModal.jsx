import { useRef, useState } from "react";

import { ETIQUETAS_ESTADO } from "../../constants/estados";
import { useConfirmarDescarte } from "../../hooks/useConfirmarDescarte";
import { useToast } from "../../hooks/useToast";
import { erroresPorCampo, mensajeDeError } from "../../services/api";
import { despacharReceta } from "../../services/farmaciaService";
import { formatearFechaHora } from "../../utils/fechas";
import { formatearQuetzales } from "../../utils/formato";
import { enfocarPrimerError } from "../../utils/formularios";
import { errorDeCosto, normalizarMonto } from "../../utils/montos";
import { Alert } from "../ui/Alert";
import { Button } from "../ui/Button";
import { ConfirmDialog } from "../ui/ConfirmDialog";
import { CurrencyInput } from "../ui/CurrencyInput";
import { FormField } from "../ui/FormField";
import { Modal } from "../ui/Modal";

const COSTO_FALTANTE = "Indique el costo del medicamento";

/* El error del backend en una fila de lo enviado: "entregados[1].costo". */
const CAMPO_COSTO = /^entregados\[(\d+)\]\.costo$/;

function unidades(cantidad) {
  return `${cantidad} ${cantidad === 1 ? "unidad" : "unidades"}`;
}

/* "Losartán", "Losartán y Amlodipino", "Losartán, Amlodipino y Metformina". */
function enumerar(nombres) {
  if (nombres.length <= 1) return nombres.join("");
  return `${nombres.slice(0, -1).join(", ")} y ${nombres.at(-1)}`;
}

/** Lo que dice la confirmación: qué se entrega ahora, qué queda pendiente y cuánto se cobra. */
function resumenDelDespacho(pendientes, aEntregar, total) {
  const entregan = aEntregar.map((m) => m.medicamento);
  const quedan = pendientes.filter((m) => !aEntregar.includes(m)).map((m) => m.medicamento);
  if (entregan.length === 0) {
    return "No se entregará ningún medicamento: la receta quedará como «Medicamento Agotado» hasta que haya existencias. No se cobra nada.";
  }
  const entrega = `Se ${entregan.length === 1 ? "entregará" : "entregarán"} ${enumerar(entregan)}.`;
  const pendiente =
    quedan.length === 0
      ? "La receta quedará entregada."
      : `${enumerar(quedan)} ${quedan.length === 1 ? "quedará pendiente" : "quedarán pendientes"}.`;
  return `${entrega} ${pendiente} Se cargarán ${formatearQuetzales(total)} a la cuenta del familiar.`;
}

function FormularioDespacho({ receta, alCambiar, alCerrar, alDespachar }) {
  const { mostrar } = useToast();
  const refFormulario = useRef(null);
  const [marcados, setMarcados] = useState(() => new Set());
  // HU-17. El costo que se escribe en cada medicamento marcado, y su error, por id.
  const [costos, setCostos] = useState({});
  const [errores, setErrores] = useState({});
  const [error, setError] = useState(null);
  const [confirmando, setConfirmando] = useState(false);
  const [guardando, setGuardando] = useState(false);

  const pendientes = receta.medicamentos.filter((m) => !m.entregado);
  // En el orden de la receta: es el orden en que viajan, y el de los errores por fila del backend.
  const aEntregar = pendientes.filter((m) => marcados.has(m.id));
  // Lo que suman los costos ya bien escritos; el backend vuelve a sumarlos sin redondeos.
  const total = aEntregar.reduce((suma, m) => {
    const texto = costos[m.id] ?? "";
    return errorDeCosto(texto, COSTO_FALTANTE) ? suma : suma + Number(normalizarMonto(texto));
  }, 0);
  // Sin entregas solo la primera vez: deja constancia de que no había existencias (DE-03).
  const sinEntregasPermitido = receta.estado === "PENDIENTE_DESPACHO";

  const alternar = (id) => {
    setMarcados((previos) => {
      const siguientes = new Set(previos);
      if (siguientes.has(id)) siguientes.delete(id);
      else siguientes.add(id);
      return siguientes;
    });
    setErrores((previos) => ({ ...previos, [id]: undefined }));
    setError(null);
    alCambiar();
  };

  const cambiarCosto = (id, valor) => {
    setCostos((previos) => ({ ...previos, [id]: valor }));
    setErrores((previos) => ({ ...previos, [id]: undefined }));
    alCambiar();
  };

  const pedirConfirmacion = (evento) => {
    evento.preventDefault();
    if (aEntregar.length === 0 && !sinEntregasPermitido) {
      setError("Marque al menos un medicamento entregado");
      return;
    }
    const encontrados = {};
    aEntregar.forEach((m) => {
      const deCosto = errorDeCosto(costos[m.id] ?? "", COSTO_FALTANTE);
      if (deCosto) encontrados[m.id] = deCosto;
    });
    if (Object.keys(encontrados).length > 0) {
      setErrores(encontrados);
      enfocarPrimerError(refFormulario.current);
      return;
    }
    setConfirmando(true);
  };

  const despachar = async () => {
    setConfirmando(false);
    setGuardando(true);
    setError(null);
    try {
      const actualizada = await despacharReceta(
        receta.id,
        aEntregar.map((m) => ({ id: m.id, costo: normalizarMonto(costos[m.id]) })),
      );
      mostrar(
        `Se despachó la receta de ${receta.paciente.nombreCompleto}: ${ETIQUETAS_ESTADO[actualizada.estado]}.`,
      );
      alDespachar(actualizada);
    } catch (fallo) {
      // 409: otra persona la despachó, o el especialista la corrigió, mientras tanto.
      const porCampo = erroresPorCampo(fallo);
      if (!porCampo) {
        setError(mensajeDeError(fallo, "No se pudo despachar la receta"));
        return;
      }
      const enFilas = {};
      let general = null;
      Object.entries(porCampo).forEach(([campo, mensaje]) => {
        const fila = CAMPO_COSTO.exec(campo);
        const medicamento = fila ? aEntregar[Number(fila[1])] : undefined;
        if (medicamento) enFilas[medicamento.id] = mensaje;
        else general ??= mensaje;
      });
      setErrores(enFilas);
      setError(general);
      enfocarPrimerError(refFormulario.current);
    } finally {
      setGuardando(false);
    }
  };

  return (
    <>
      <form ref={refFormulario} onSubmit={pedirConfirmacion} noValidate>
        <div className="flex flex-col gap-6 px-4 py-6 sm:px-6">
          {error && <Alert>{error}</Alert>}

          <dl className="grid gap-4 rounded-tarjeta bg-fondo px-4 py-3 sm:grid-cols-2">
            <div>
              <dt className="text-auxiliar text-texto-secundario">Recetada por</dt>
              <dd className="mt-1 text-texto">{receta.medico.nombreCompleto}</dd>
            </div>
            <div>
              <dt className="text-auxiliar text-texto-secundario">Fecha de la receta</dt>
              <dd className="mt-1 text-texto tabular-nums slashed-zero">{formatearFechaHora(receta.fechaEmision)}</dd>
            </div>
            {receta.indicacionesGenerales && (
              <div className="sm:col-span-2">
                <dt className="text-auxiliar text-texto-secundario">Indicaciones generales</dt>
                <dd className="mt-1 text-texto">{receta.indicacionesGenerales}</dd>
              </div>
            )}
          </dl>

          <fieldset>
            <legend className="font-semibold text-texto">Medicamentos entregados</legend>
            <p className="mt-1 text-auxiliar text-texto-secundario">
              Marque lo que entrega y su costo. Lo que no marque queda pendiente y no se cobra.
            </p>
            <ul className="mt-3 divide-y divide-divisor rounded-tarjeta border border-divisor">
              {receta.medicamentos.map((m) => (
                <li key={m.id} className="px-4 py-3">
                  <label
                    className={`flex min-h-11 items-start gap-3 select-none ${m.entregado ? "cursor-not-allowed" : "cursor-pointer"}`}
                  >
                    <input
                      type="checkbox"
                      checked={m.entregado || marcados.has(m.id)}
                      disabled={m.entregado || guardando}
                      onChange={() => alternar(m.id)}
                      className="mt-1 size-5 shrink-0 cursor-pointer accent-primario disabled:cursor-not-allowed"
                    />
                    <span className="min-w-0 flex-1">
                      <span className="flex flex-wrap items-baseline justify-between gap-x-4">
                        <span className={`font-medium ${m.entregado ? "text-texto-secundario" : "text-texto"}`}>
                          {m.medicamento}
                        </span>
                        <span className="text-texto-secundario tabular-nums slashed-zero">
                          {m.dosis} · {unidades(m.cantidad)}
                        </span>
                      </span>
                      <span className="block text-auxiliar text-texto-secundario">
                        {m.entregado ? (
                          <>
                            Ya entregado el{" "}
                            <span className="tabular-nums slashed-zero">{formatearFechaHora(m.fechaEntrega)}</span>
                          </>
                        ) : (
                          m.formaAplicacion
                        )}
                      </span>
                    </span>
                  </label>

                  {/* HU-17 (YS-130). Solo lo entregado se cobra; pl-8 lo alinea con el nombre.
                      Sin texto de ejemplo: no hay costo sugerido, y un "25.00" en gris se
                      leía como un monto ya escrito. */}
                  {!m.entregado && marcados.has(m.id) && (
                    <div className="mt-3 pl-8">
                      <FormField
                        etiqueta={`Costo de ${m.medicamento}`}
                        obligatorio
                        error={errores[m.id]}
                        ayuda={`En quetzales, por las ${unidades(m.cantidad)}.`}
                      >
                        {(campo) => (
                          <CurrencyInput
                            {...campo}
                            className="sm:w-56"
                            value={costos[m.id] ?? ""}
                            disabled={guardando}
                            onChange={(evento) => cambiarCosto(m.id, evento.target.value)}
                          />
                        )}
                      </FormField>
                    </div>
                  )}
                </li>
              ))}
            </ul>
          </fieldset>

          {/* El total, visible antes de confirmar (YS-130). */}
          {aEntregar.length > 0 && (
            <div
              aria-live="polite"
              className="flex items-baseline justify-between gap-4 rounded-tarjeta bg-fondo px-4 py-3"
            >
              <span className="font-medium text-texto">Total de este despacho</span>
              <span className="font-semibold text-texto tabular-nums slashed-zero">{formatearQuetzales(total)}</span>
            </div>
          )}
        </div>

        <div className="sticky bottom-0 flex flex-wrap justify-end gap-3 border-t border-divisor bg-superficie px-4 py-4 sm:px-6">
          <Button type="button" variante="secundario" onClick={alCerrar} disabled={guardando}>
            Cancelar
          </Button>
          <Button type="submit" cargando={guardando}>
            Despachar
          </Button>
        </div>
      </form>

      {/* Explícita antes de despachar: no se puede deshacer (YS-127). */}
      <ConfirmDialog
        abierto={confirmando}
        titulo="¿Confirmar el despacho?"
        mensaje={`${resumenDelDespacho(pendientes, aEntregar, total)} El despacho no se puede deshacer.`}
        textoConfirmar="Despachar"
        textoCancelar="Revisar"
        alConfirmar={despachar}
        alCancelar={() => setConfirmando(false)}
      />
    </>
  );
}

/**
 * HU-16 (YS-127). El despacho de una receta desde la bandeja de la farmacia:
 * se marcan los medicamentos que se entregan y se confirma. Los que ya se
 * entregaron en un despacho anterior se ven marcados y no se pueden tocar.
 *
 * HU-17 (YS-130). Cada medicamento marcado pide su costo, y el total del
 * despacho se ve antes de confirmar; es lo que se carga a la cuenta del
 * familiar.
 */
export function DespacharRecetaModal({ abierto, receta, claveFormulario, alCerrar, alDespachar }) {
  const { marcarCambios, olvidarCambios, intentarCerrar, dialogo } = useConfirmarDescarte(alCerrar);

  return (
    <>
      <Modal
        abierto={abierto}
        alCerrar={intentarCerrar}
        titulo="Despachar receta"
        descripcion={receta ? receta.paciente.nombreCompleto : undefined}
      >
        {receta && (
          <FormularioDespacho
            key={claveFormulario}
            receta={receta}
            alCambiar={marcarCambios}
            alCerrar={intentarCerrar}
            alDespachar={(actualizada) => {
              olvidarCambios();
              alDespachar(actualizada);
            }}
          />
        )}
      </Modal>

      <ConfirmDialog {...dialogo} />
    </>
  );
}
