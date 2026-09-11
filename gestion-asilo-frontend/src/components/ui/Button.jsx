import { Loader2 } from "lucide-react";

/*
 * Altura mínima 44px: área táctil obligatoria en tableta (rondas de enfermería).
 * El hundido al pulsar existe justamente por esa tableta: en táctil no hay
 * hover, así que sin él nada confirma que el toque entró. Deliberadamente
 * mínimo (0.98) porque estos botones se pulsan decenas de veces al día.
 *
 * `select-none` y el ícono sin eventos evitan que pulsar rápido termine
 * seleccionando texto: el navegador lee los clics seguidos como doble clic, y
 * si el objetivo fue un <svg> que luego cambia, la selección se va al texto
 * vecino. Con esto el objetivo del clic es siempre el botón, que no desaparece.
 */
const BASE =
  "inline-flex items-center justify-center gap-2 min-h-11 px-4 rounded-tarjeta font-medium " +
  "select-none [&_svg]:pointer-events-none " +
  "transition-[background-color,color,transform] duration-[--duracion-presion] ease-salida " +
  "active:scale-[0.98] motion-reduce:active:scale-100 " +
  "disabled:opacity-60 disabled:cursor-not-allowed disabled:active:scale-100";

const VARIANTES = {
  primario: "bg-primario text-white hover:bg-primario-oscuro",
  secundario:
    "bg-superficie text-texto border border-texto-secundario hover:bg-neutral-tenue",
  texto: "text-primario hover:bg-primario-tenue",
  peligro: "bg-error text-white hover:brightness-90",
};

export function Button({
  variante = "primario",
  cargando = false,
  children,
  className = "",
  disabled,
  ...props
}) {
  return (
    <button
      className={`${BASE} ${VARIANTES[variante]} ${className}`}
      disabled={disabled || cargando}
      {...props}
    >
      {cargando && <Loader2 aria-hidden="true" size={20} className="animate-spin" />}
      {children}
    </button>
  );
}

export function IconButton({ etiqueta, children, className = "", ...props }) {
  return (
    <button
      aria-label={etiqueta}
      title={etiqueta}
      className={`inline-flex size-11 items-center justify-center rounded-tarjeta text-texto-secundario select-none [&_svg]:pointer-events-none transition-[background-color,color,transform] duration-[--duracion-presion] ease-salida hover:bg-neutral-tenue hover:text-primario active:scale-[0.98] motion-reduce:active:scale-100 ${className}`}
      {...props}
    >
      {children}
    </button>
  );
}
