import { useRef } from "react";
import { FileText, Upload, X } from "lucide-react";

import { formatearTamano } from "../../utils/formato";
import { IconButton } from "./Button";

/* El anillo de foco del manual (2 px, primario, separado 2 px), dibujado en lo
   que se ve cuando el foco está en el campo oculto. */
const FOCO = "peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-primario";

/**
 * YS-118. El `FileUpload` del sistema de diseño: elegir un archivo, ver cuál
 * quedó elegido y quitarlo.
 *
 * El campo de archivo del navegador no se puede estilizar ni traducir ("Choose
 * file"), así que queda oculto y se dibuja un botón propio. Sigue siendo el
 * campo de verdad: recibe el foco con el teclado (Espacio o Enter abren el
 * diálogo del sistema), la etiqueta del `FormField` apunta a él y el lector de
 * pantalla lo anuncia. Lo que se ve repite su foco con `peer-focus-visible`:
 * sin eso, el foco quedaría en un elemento invisible.
 *
 * Se usa con el render-prop de `FormField`: recibe `id`, `invalido` y los
 * `aria-*` del campo.
 *
 * - `archivo`: el `File` elegido, o `null`.
 * - `aceptar`: los tipos para el diálogo del sistema (`accept`). Solo filtra
 *   lo que se ofrece; la validación de verdad la hacen quien lo usa y el
 *   backend.
 */
export function FileUpload({ archivo, alCambiar, aceptar, invalido = false, disabled = false, ...campo }) {
  const referencia = useRef(null);

  const elegir = (evento) => {
    alCambiar(evento.target.files?.[0] ?? null);
    // Vacío para que elegir el mismo archivo otra vez también avise.
    evento.target.value = "";
  };

  const quitar = () => {
    alCambiar(null);
    referencia.current?.focus();
  };

  const borde = invalido ? "border-error" : "border-texto-secundario";

  return (
    <div>
      <input
        ref={referencia}
        type="file"
        accept={aceptar}
        onChange={elegir}
        disabled={disabled}
        className="peer sr-only"
        {...campo}
      />
      {archivo ? (
        <div
          className={`flex min-h-11 items-center gap-3 rounded-campo border bg-superficie px-4 py-2 ${borde} ${FOCO}`}
        >
          <FileText aria-hidden="true" size={20} className="shrink-0 text-texto-secundario" />
          <div className="min-w-0 flex-1">
            <p className="truncate font-medium text-texto">{archivo.name}</p>
            <p className="text-auxiliar text-texto-secundario tabular-nums slashed-zero">
              {formatearTamano(archivo.size)}
            </p>
          </div>
          <IconButton etiqueta={`Quitar el archivo ${archivo.name}`} type="button" onClick={quitar} disabled={disabled}>
            <X aria-hidden="true" size={20} />
          </IconButton>
        </div>
      ) : (
        // Solo para el ratón y el toque: el teclado y el lector de pantalla
        // usan el campo de arriba, por eso esto no se anuncia.
        <div
          aria-hidden="true"
          onClick={() => !disabled && referencia.current?.click()}
          className={`inline-flex min-h-11 cursor-pointer items-center justify-center gap-2 rounded-tarjeta border bg-superficie px-4 font-medium text-texto select-none hover:bg-neutral-tenue ${borde} ${FOCO} ${
            disabled ? "cursor-not-allowed opacity-60" : ""
          }`}
        >
          <Upload aria-hidden="true" size={20} />
          Elegir archivo
        </div>
      )}
    </div>
  );
}
