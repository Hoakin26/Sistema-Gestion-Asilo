import { useEffect, useState } from "react";

import { obtenerCostoOrden } from "../../services/laboratorioService";
import { formatearQuetzales } from "../../utils/formato";
import { Barra } from "../ui/TablaSkeleton";

/**
 * HU-14 (YS-121). El costo que el laboratorio registró al cargar el resultado,
 * en el detalle del resultado. Solo lo ve el laboratorio: la bandeja no
 * muestra costos (HU-12) y el especialista nunca los ve (decidido el 19/10).
 */
export function CostoRegistrado({ ordenId }) {
  const [costo, setCosto] = useState(undefined);

  useEffect(() => {
    let vigente = true;
    obtenerCostoOrden(ordenId)
      .then((datos) => {
        if (vigente) setCosto(datos.registrado ? datos.costo : null);
      })
      .catch(() => {
        if (vigente) setCosto(null);
      });
    return () => {
      vigente = false;
    };
  }, [ordenId]);

  if (costo === undefined) {
    return (
      <div role="status" className="mt-4">
        <span className="sr-only">Cargando el costo…</span>
        <Barra className="w-40" />
      </div>
    );
  }
  if (costo === null) return null;

  return (
    <p className="mt-4 text-texto">
      <span className="text-auxiliar text-texto-secundario">Costo registrado: </span>
      <span className="font-medium tabular-nums slashed-zero">{formatearQuetzales(costo)}</span>
    </p>
  );
}
