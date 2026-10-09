import { useEffect, useRef, useState } from "react";
import { ChevronDown, KeyRound, LogOut, Menu } from "lucide-react";

import { etiquetaDeRol } from "../../constants/roles";
import { useAuth } from "../../hooks/useAuth";
import { IconButton } from "../ui/Button";

function iniciales(nombreCompleto) {
  return nombreCompleto
    .split(" ")
    .filter((parte) => parte.length > 2)
    .slice(0, 2)
    .map((parte) => parte[0].toUpperCase())
    .join("");
}

export function Navbar({ migas, menuAbierto, alAbrirMenu, alCambiarPassword }) {
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
    <header className="flex shrink-0 items-center justify-between gap-4 border-b border-divisor bg-superficie px-4 py-4 sm:px-6">
      <div className="flex min-w-0 items-center gap-2">
        {/* Único acceso al menú por debajo de 1024px, donde el menú fijo no
            está (YS-79). Arriba de ese ancho sobra y se oculta. */}
        <IconButton
          etiqueta="Abrir menú"
          type="button"
          onClick={alAbrirMenu}
          aria-expanded={menuAbierto}
          aria-controls="cajon-navegacion"
          className="lg:hidden"
        >
          <Menu aria-hidden="true" size={24} />
        </IconButton>

        <nav aria-label="Ruta de navegación" className="min-w-0">
          <ol className="flex items-center gap-2 text-texto-secundario">
            {migas.map((miga, indice) => {
              const esUltima = indice === migas.length - 1;
              return (
                /* En teléfono solo queda la miga actual: con el botón de menú
                   al lado, la ruta completa no cabe y empujaría al avatar
                   fuera de la pantalla. */
                /* min-w-0 en la última: sin él, el <li> no se encoge por
                   debajo de su texto y el `truncate` de adentro nunca corta.
                   No se notaba con "Usuarios y roles"; con el nombre completo
                   de un interno el texto se montaba sobre el avatar (YS-61). */
                <li
                  key={miga}
                  className={`items-center gap-2 ${esUltima ? "flex min-w-0" : "hidden sm:flex"}`}
                >
                  {indice > 0 && (
                    <span aria-hidden="true" className="hidden sm:inline">
                      ›
                    </span>
                  )}
                  <span className={esUltima ? "truncate text-texto" : undefined}>
                    {miga}
                  </span>
                </li>
              );
            })}
          </ol>
        </nav>
      </div>

      <div className="relative shrink-0" ref={contenedor}>
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
            {/* YS-75. Hasta ahora el endpoint de cambio de contraseña existía
                desde YS-69 pero no había dónde usarlo. */}
            <button
              type="button"
              role="menuitem"
              onClick={() => {
                setAbierto(false);
                alCambiarPassword();
              }}
              className="flex min-h-11 w-full items-center gap-3 px-4 text-left text-texto hover:bg-neutral-tenue"
            >
              <KeyRound aria-hidden="true" size={20} />
              Cambiar contraseña
            </button>

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
