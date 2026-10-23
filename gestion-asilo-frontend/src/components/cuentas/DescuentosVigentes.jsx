import { useEffect, useState } from "react";
import { BadgePercent } from "lucide-react";

import { CONCEPTOS_CUENTA, POR_CARGO, TIPOS_SERVICIO } from "../../constants/estados";
import { listarPoliticas } from "../../services/descuentoService";
import { formatearDescuento } from "../../utils/formato";

/**
 * HU-19 (YS-137). Los descuentos de la fundación que rigen hoy, para el
 * Administrador del Asilo: no los define (eso es de la fundación), pero los ve
 * aplicados en cada cuenta y tiene que poder explicarlos al familiar.
 */
export function DescuentosVigentes() {
  const [politicas, setPoliticas] = useState(null);

  useEffect(() => {
    let vigente = true;
    listarPoliticas()
      .then((lista) => {
        if (vigente) setPoliticas(lista);
      })
      // Es informativo: si falla, la lista de cuentas sigue sirviendo sin él.
      .catch(() => {
        if (vigente) setPoliticas([]);
      });
    return () => {
      vigente = false;
    };
  }, []);

  if (politicas === null) return null;

  return (
    <div className="mt-6 flex flex-wrap items-start gap-x-6 gap-y-2 rounded-tarjeta border border-divisor bg-superficie px-4 py-3 shadow-tarjeta">
      <span className="flex items-center gap-2 font-medium text-texto">
        <BadgePercent aria-hidden="true" size={20} className="text-texto-secundario" />
        Descuentos de la fundación vigentes
      </span>
      <dl className="flex flex-wrap gap-x-6 gap-y-1">
        {TIPOS_SERVICIO.map((tipo) => {
          const vigente = politicas.find((p) => p.tipoServicio === tipo && p.vigente);
          return (
            <div key={tipo} className="flex gap-2">
              <dt className="text-texto-secundario">{CONCEPTOS_CUENTA[tipo]}:</dt>
              <dd className="tabular-nums slashed-zero">
                {vigente ? formatearDescuento(vigente, POR_CARGO[tipo]) : "Sin descuento"}
              </dd>
            </div>
          );
        })}
      </dl>
    </div>
  );
}
