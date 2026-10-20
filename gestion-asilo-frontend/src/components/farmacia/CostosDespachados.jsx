import { useEffect, useState } from "react";

import { obtenerCostosReceta } from "../../services/farmaciaService";
import { formatearQuetzales } from "../../utils/formato";
import { Barra } from "../ui/TablaSkeleton";

/**
 * HU-17 (YS-130). Lo que la farmacia cobró de una receta, en su detalle: el
 * costo de cada medicamento entregado y el total. Solo lo ve la farmacia: la
 * receta que recibe el especialista no lleva costos, como en el laboratorio
 * (decidido el 19/10).
 */
export function CostosDespachados({ receta }) {
  const hayEntregas = receta.medicamentos.some((m) => m.entregado);
  const [costos, setCostos] = useState(undefined);

  useEffect(() => {
    if (!hayEntregas) return undefined;
    let vigente = true;
    obtenerCostosReceta(receta.id)
      .then((datos) => {
        if (vigente) setCostos(datos);
      })
      .catch(() => {
        if (vigente) setCostos(null);
      });
    return () => {
      vigente = false;
    };
  }, [receta.id, hayEntregas]);

  if (!hayEntregas || costos === null) return null;
  if (costos === undefined) {
    return (
      <div role="status" className="mt-4">
        <span className="sr-only">Cargando lo cobrado…</span>
        <Barra className="w-40" />
      </div>
    );
  }

  const nombres = new Map(receta.medicamentos.map((m) => [m.id, m.medicamento]));
  return (
    <div className="mt-4 border-t border-divisor pt-4">
      <h3 className="font-semibold text-texto">Cobrado a la cuenta del familiar</h3>
      <dl className="mt-2 flex flex-col gap-1">
        {costos.entregados.map((c) => (
          <div key={c.id} className="flex justify-between gap-4">
            <dt className="text-texto">{nombres.get(c.id)}</dt>
            {/* Lo entregado antes de HU-17 no tiene costo. */}
            <dd className="text-texto tabular-nums slashed-zero">
              {c.costo == null ? "Sin costo registrado" : formatearQuetzales(c.costo)}
            </dd>
          </div>
        ))}
        <div className="mt-1 flex justify-between gap-4 border-t border-divisor pt-2">
          <dt className="font-semibold text-texto">Total</dt>
          <dd className="font-semibold text-texto tabular-nums slashed-zero">{formatearQuetzales(costos.total)}</dd>
        </div>
      </dl>
    </div>
  );
}
