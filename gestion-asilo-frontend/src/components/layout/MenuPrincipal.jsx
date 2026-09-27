import { NavLink } from "react-router-dom";

import { modulosDeRol } from "../../constants/modulos";
import { useAuth } from "../../hooks/useAuth";

/* min-h-11 son los 44px de área táctil que el manual exige para tableta. */
const ITEM = "flex min-h-11 items-center gap-3 rounded-tarjeta px-4 font-medium";

/**
 * La lista de módulos, compartida por el menú fijo de escritorio y el cajón de
 * teléfono y tableta (YS-79). Vive aparte para que no existan dos copias que
 * se desincronicen cuando se agregue un módulo.
 *
 * YS-81. Muestra únicamente los módulos del rol del usuario. Los que no le
 * corresponden no aparecen: un médico no tiene por qué enterarse de que existe
 * un módulo de donaciones. Los que sí le corresponden pero aún no están
 * construidos siguen en gris, porque eso le da la medida del alcance del
 * sistema sin ofrecerle un enlace que no lleva a ninguna parte.
 *
 * `alNavegar` lo usa el cajón para cerrarse al elegir un módulo; en el menú
 * fijo no se pasa, porque ahí no hay nada que cerrar.
 */
export function MenuPrincipal({ alNavegar }) {
  const { usuario } = useAuth();
  const modulos = modulosDeRol(usuario?.rol);

  return (
    <nav aria-label="Menú principal" className="px-4 py-6">
      <p className="px-4 pb-3 text-auxiliar font-semibold tracking-wide text-texto-secundario uppercase">
        Menú principal
      </p>

      <ul className="flex flex-col gap-1">
        {modulos.map(({ etiqueta, Icono, ruta }) => (
          <li key={etiqueta}>
            {ruta ? (
              <NavLink
                to={ruta}
                onClick={alNavegar}
                className={({ isActive }) =>
                  `${ITEM} ${
                    isActive
                      ? "bg-primario-tenue text-primario"
                      : "text-texto hover:bg-neutral-tenue"
                  }`
                }
              >
                <Icono aria-hidden="true" size={20} />
                {etiqueta}
              </NavLink>
            ) : (
              <span
                aria-disabled="true"
                title="Disponible en una fase posterior del proyecto"
                className={`${ITEM} cursor-not-allowed text-texto-secundario opacity-60`}
              >
                <Icono aria-hidden="true" size={20} />
                {etiqueta}
              </span>
            )}
          </li>
        ))}
      </ul>
    </nav>
  );
}
