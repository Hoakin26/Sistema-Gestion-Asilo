import { useState } from "react";

import { ETIQUETAS_ESTADO } from "../../constants/estados";
import { useConfirmarDescarte } from "../../hooks/useConfirmarDescarte";
import { useToast } from "../../hooks/useToast";
import { erroresPorCampo, mensajeDeError } from "../../services/api";
import { despacharReceta } from "../../services/farmaciaService";
import { formatearFechaHora } from "../../utils/fechas";
import { Alert } from "../ui/Alert";
import { Button } from "../ui/Button";
import { ConfirmDialog } from "../ui/ConfirmDialog";
import { Modal } from "../ui/Modal";

function unidades(cantidad) {
  return `${cantidad} ${cantidad === 1 ? "unidad" : "unidades"}`;
}

/* "Losartán", "Losartán y Amlodipino", "Losartán, Amlodipino y Metformina". */
function enumerar(nombres) {
  if (nombres.length <= 1) return nombres.join("");
  return `${nombres.slice(0, -1).join(", ")} y ${nombres.at(-1)}`;
}

/** Lo que dice la confirmación: qué se entrega ahora y qué queda pendiente. */
function resumenDelDespacho(pendientes, marcados) {
  const entregan = pendientes.filter((m) => marcados.has(m.id)).map((m) => m.medicamento);
  const quedan = pendientes.filter((m) => !marcados.has(m.id)).map((m) => m.medicamento);
  if (entregan.length === 0) {
    return "No se entregará ningún medicamento: la receta quedará como «Medicamento Agotado» hasta que haya existencias.";
  }
  const entrega = `Se entregará ${enumerar(entregan)}.`;
  if (quedan.length === 0) return `${entrega} La receta quedará entregada.`;
  return `${entrega} ${enumerar(quedan)} ${quedan.length === 1 ? "quedará pendiente" : "quedarán pendientes"}.`;
}

function FormularioDespacho({ receta, alCambiar, alCerrar, alDespachar }) {
  const { mostrar } = useToast();
  const [marcados, setMarcados] = useState(() => new Set());
  const [error, setError] = useState(null);
  const [confirmando, setConfirmando] = useState(false);
  const [guardando, setGuardando] = useState(false);

  const pendientes = receta.medicamentos.filter((m) => !m.entregado);
  // Sin entregas solo la primera vez: deja constancia de que no había existencias (DE-03).
  const sinEntregasPermitido = receta.estado === "PENDIENTE_DESPACHO";

  const alternar = (id) => {
    setMarcados((previos) => {
      const siguientes = new Set(previos);
      if (siguientes.has(id)) siguientes.delete(id);
      else siguientes.add(id);
      return siguientes;
    });
    setError(null);
    alCambiar();
  };

  const pedirConfirmacion = (evento) => {
    evento.preventDefault();
    if (marcados.size === 0 && !sinEntregasPermitido) {
      setError("Marque al menos un medicamento entregado");
      return;
    }
    setConfirmando(true);
  };

  const despachar = async () => {
    setConfirmando(false);
    setGuardando(true);
    setError(null);
    try {
      const actualizada = await despacharReceta(receta.id, [...marcados]);
      mostrar(
        `Se despachó la receta de ${receta.paciente.nombreCompleto}: ${ETIQUETAS_ESTADO[actualizada.estado]}.`,
      );
      alDespachar(actualizada);
    } catch (fallo) {
      // 409: otra persona la despachó, o el especialista la corrigió, mientras tanto.
      const porCampo = erroresPorCampo(fallo);
      setError(porCampo ? Object.values(porCampo)[0] : mensajeDeError(fallo, "No se pudo despachar la receta"));
    } finally {
      setGuardando(false);
    }
  };

  return (
    <>
      <form onSubmit={pedirConfirmacion} noValidate>
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
              Marque los que entrega ahora. Los que no marque quedan pendientes, por ejemplo si no hay existencias.
            </p>
            <ul className="mt-3 divide-y divide-divisor rounded-tarjeta border border-divisor">
              {receta.medicamentos.map((m) => (
                <li key={m.id}>
                  <label
                    className={`flex min-h-11 items-start gap-3 px-4 py-3 select-none ${m.entregado ? "cursor-not-allowed" : "cursor-pointer"}`}
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
                </li>
              ))}
            </ul>
          </fieldset>
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
        mensaje={`${resumenDelDespacho(pendientes, marcados)} El despacho no se puede deshacer.`}
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
