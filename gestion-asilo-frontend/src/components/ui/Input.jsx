import { useState } from "react";
import { Eye, EyeOff } from "lucide-react";

/*
 * El borde usa texto-secundario (#64748B) y no divisor (#E2E8F0): el divisor no
 * alcanza 3:1 de contraste y no puede delimitar un control interactivo.
 */
const BASE =
  "min-h-11 w-full rounded-campo border bg-superficie px-4 text-texto " +
  "placeholder:text-texto-secundario disabled:bg-neutral-tenue disabled:cursor-not-allowed";

export function Input({ invalido = false, className = "", ...props }) {
  const borde = invalido ? "border-error" : "border-texto-secundario";
  return <input className={`${BASE} ${borde} ${className}`} {...props} />;
}

export function Select({ invalido = false, className = "", children, ...props }) {
  const borde = invalido ? "border-error" : "border-texto-secundario";
  return (
    <select className={`${BASE} ${borde} ${className}`} {...props}>
      {children}
    </select>
  );
}

/**
 * Contraseña con opción de revelarla. Escribir a ciegas es una fuente clásica
 * de errores de acceso, sobre todo en tableta; poder verificar lo escrito
 * previene el error en vez de solo reportarlo después.
 */
export function InputPassword({ invalido = false, className = "", ...props }) {
  const [visible, setVisible] = useState(false);
  const borde = invalido ? "border-error" : "border-texto-secundario";

  return (
    <div className="relative">
      <input
        type={visible ? "text" : "password"}
        className={`${BASE} ${borde} pr-14 ${className}`}
        {...props}
      />
      <button
        type="button"
        onClick={() => setVisible((valor) => !valor)}
        aria-label={visible ? "Ocultar contraseña" : "Mostrar contraseña"}
        aria-pressed={visible}
        className="absolute inset-y-0 right-0 flex w-12 items-center justify-center rounded-r-campo text-texto-secundario hover:text-primario"
      >
        {visible ? (
          <EyeOff aria-hidden="true" size={20} />
        ) : (
          <Eye aria-hidden="true" size={20} />
        )}
      </button>
    </div>
  );
}
