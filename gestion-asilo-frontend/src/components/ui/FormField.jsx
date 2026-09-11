import { useId } from "react";
import { TriangleAlert } from "lucide-react";

/**
 * Etiqueta + control + error. El error se anuncia con role="alert" y se enlaza
 * por aria-describedby para que un lector de pantalla lo lea al enfocarse.
 */
export function FormField({ etiqueta, error, ayuda, obligatorio, children }) {
  const id = useId();
  const idError = `${id}-error`;
  const idAyuda = `${id}-ayuda`;
  const descritoPor = [error && idError, ayuda && idAyuda]
    .filter(Boolean)
    .join(" ");

  return (
    <div className="flex flex-col gap-2">
      <label htmlFor={id} className="font-medium text-texto">
        {etiqueta}
        {obligatorio && (
          <span className="text-error" aria-hidden="true">
            {" *"}
          </span>
        )}
      </label>

      {children({
        id,
        invalido: Boolean(error),
        "aria-invalid": Boolean(error),
        "aria-describedby": descritoPor || undefined,
      })}

      {ayuda && !error && (
        <p id={idAyuda} className="text-auxiliar text-texto-secundario">
          {ayuda}
        </p>
      )}

      {error && (
        <p
          id={idError}
          role="alert"
          className="entrada-error flex items-center gap-2 text-auxiliar text-error"
        >
          <TriangleAlert aria-hidden="true" size={16} />
          {error}
        </p>
      )}
    </div>
  );
}
