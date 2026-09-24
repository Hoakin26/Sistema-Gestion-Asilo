import { useEffect, useRef, useState } from "react";
import { ChevronDown, LogOut } from "lucide-react";

import { etiquetaDeRol } from "../../constants/roles";
import { useAuth } from "../../hooks/useAuth";

function iniciales(nombreCompleto) {
  return nombreCompleto
    .split(" ")
    .filter((parte) => parte.length > 2)
    .slice(0, 2)
    .map((parte) => parte[0].toUpperCase())
    .join("");
}

export function Navbar({ migas }) {
  const { usuario, logout } = useAuth();
  const [abierto, setAbierto] = useState(false);
  const contenedor = useRef(null);

  useEffect(() => {
    if (!abierto) return;

    const alHacerClic = (evento) => {
      if (!contenedor.current?.contains(evento.target)) setAbierto(false);
    };
    const alPresionar = (evento) => {
      if (evento.key === "Escape") setAbierto(false);
    };

    document.addEventListener("mousedown", alHacerClic);
    document.addEventListener("keydown", alPresionar);
    return () => {
      document.removeEventListener("mousedown", alHacerClic);
      document.removeEventListener("keydown", alPresionar);
    };
  }, [abierto]);

  return (
    /* shrink-0: dentro del esqueleto de altura fija (YS-78), sin esto la barra
       se comprimiría cuando el contenido de abajo es alto. */
    <header className="flex shrink-0 items-center justify-between gap-4 border-b border-divisor bg-superficie px-6 py-4">
      <nav aria-label="Ruta de navegación">
        <ol className="flex items-center gap-2 text-texto-secundario">
          {migas.map((miga, indice) => (
            <li key={miga} className="flex items-center gap-2">
              {indice > 0 && <span aria-hidden="true">›</span>}
              <span className={indice === migas.length - 1 ? "text-texto" : undefined}>
                {miga}
              </span>
            </li>
          ))}
        </ol>
      </nav>

      <div className="relative" ref={contenedor}>
        <button
          type="button"
          onClick={() => setAbierto((valor) => !valor)}
          aria-expanded={abierto}
          aria-haspopup="menu"
          className="flex min-h-11 items-center gap-3 rounded-tarjeta px-2 hover:bg-neutral-tenue"
        >
          <span
            aria-hidden="true"
            className="flex size-10 items-center justify-center rounded-full bg-primario-tenue font-semibold text-primario-oscuro"
          >
            {iniciales(usuario.nombreCompleto)}
          </span>
          <span className="hidden text-left lg:block">
            <span className="block font-medium text-texto">{usuario.nombreCompleto}</span>
            <span className="block text-auxiliar text-texto-secundario">
              {etiquetaDeRol(usuario.rol)}
            </span>
          </span>
          <ChevronDown aria-hidden="true" size={20} className="text-texto-secundario" />
        </button>

        {abierto && (
          <div
            role="menu"
            className="menu-animado absolute right-0 z-10 mt-2 w-56 rounded-tarjeta border border-divisor bg-superficie py-2 shadow-menu"
          >
            <button
              type="button"
              role="menuitem"
              onClick={logout}
              className="flex min-h-11 w-full items-center gap-3 px-4 text-left text-texto hover:bg-neutral-tenue"
            >
              <LogOut aria-hidden="true" size={20} />
              Cerrar sesión
            </button>
          </div>
        )}
      </div>
    </header>
  );
}
