import { useState } from "react";
import { Download } from "lucide-react";

import { useToast } from "../../hooks/useToast";
import { descargarArchivoResultado } from "../../services/laboratorioService";
import { formatearFechaHora } from "../../utils/fechas";
import { formatearTamano } from "../../utils/formato";
import { Button } from "../ui/Button";

/*
 * Con `responseType: "blob"`, el cuerpo de un error también llega como Blob y
 * `mensajeDeError` no lo puede leer: se convierte a texto para sacar el
 * mensaje del backend.
 */
async function mensajeDeDescarga(fallo) {
  try {
    return JSON.parse(await fallo.response.data.text()).mensaje;
  } catch {
    return null;
  }
}

/** HU-13 (YS-118). Descarga el archivo de un resultado, con su nombre y tamaño a la vista. */
export function BotonDescargaResultado({ ordenId, archivo }) {
  const { mostrar } = useToast();
  const [descargando, setDescargando] = useState(false);

  const descargar = async () => {
    setDescargando(true);
    try {
      await descargarArchivoResultado(ordenId, archivo.nombre);
    } catch (fallo) {
      mostrar((await mensajeDeDescarga(fallo)) ?? "No se pudo descargar el archivo del resultado", "error");
    } finally {
      setDescargando(false);
    }
  };

  return (
    <Button
      type="button"
      variante="texto"
      onClick={descargar}
      cargando={descargando}
      aria-label={`Descargar el archivo del resultado, ${archivo.nombre}`}
      className="-ml-4 max-w-full"
    >
      {!descargando && <Download aria-hidden="true" size={20} />}
      <span className="truncate">{archivo.nombre}</span>
      <span className="shrink-0 text-auxiliar font-normal text-texto-secundario tabular-nums">
        {formatearTamano(archivo.tamano)}
      </span>
    </Button>
  );
}

/**
 * HU-13 (YS-118). El resultado de un examen para leerlo: el texto, cuándo se
 * cargó y el archivo, si lo hay. Lo usan la visita del especialista, los
 * antecedentes (HU-08), el historial del Médico General (HU-02) y el
 * laboratorio. Sin resultado todavía, no muestra nada.
 *
 * `orden` es una orden de laboratorio o un examen de los antecedentes: los dos
 * traen `id`, `resultado`, `fechaResultado` y `archivo`.
 */
export function ResultadoExamen({ orden }) {
  if (!orden.resultado) return null;

  return (
    <div className="mt-2 rounded-tarjeta bg-fondo px-4 py-3">
      <p className="text-auxiliar text-texto-secundario tabular-nums slashed-zero">
        Resultado del {formatearFechaHora(orden.fechaResultado)}
      </p>
      <p className="mt-1 whitespace-pre-line text-texto">{orden.resultado}</p>
      {orden.archivo && (
        <div className="mt-1">
          <BotonDescargaResultado ordenId={orden.id} archivo={orden.archivo} />
        </div>
      )}
    </div>
  );
}
