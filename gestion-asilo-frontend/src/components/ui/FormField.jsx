import { useId } from "react";
import { TriangleAlert } from "lucide-react";

/**
 * Etiqueta + control + error. El error se anuncia con role="alert" y se enlaza
 * por aria-describedby para que un lector de pantalla lo lea al enfocarse.
 *
 * YS-182: la ayuda y el error llevan `w-0 min-w-full`, así toman el ancho del
 * campo sin agrandarlo. En una barra de filtros, cada campo mide lo que su
 * contenido más ancho, y un mensaje largo ensanchaba el campo y mandaba los
 * botones a la línea de abajo; ahora se parte en más líneas. En un formulario
 * de una columna no cambia nada: ahí el campo ya ocupa todo el ancho.
 *
 * Por la misma razón, las barras de filtros alinean los campos por arriba
 * (`items-start`) y bajan los botones lo que mide una etiqueta con su
 * separación (`sm:[&>button]:mt-8`, 24 + 8 px): alineadas por abajo, el campo
 * con error crecía hacia arriba y los demás bajaban.
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
        <p id={idAyuda} className="w-0 min-w-full text-auxiliar text-texto-secundario">
          {ayuda}
        </p>
      )}

      {error && (
        <p
          id={idError}
          role="alert"
          className="entrada-error flex w-0 min-w-full items-start gap-2 text-auxiliar text-error"
        >
          {/* shrink-0: en una columna angosta el mensaje ocupa dos líneas y,
              sin esto, el flex encogía el ícono hasta volverlo ilegible. */}
          <TriangleAlert aria-hidden="true" size={16} className="shrink-0" />
          {error}
        </p>
      )}
    </div>
  );
}
