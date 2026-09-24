import { Logotipo } from "./Logotipo";
import { MenuPrincipal } from "./MenuPrincipal";

/**
 * Menú fijo, solo de 1024px en adelante (YS-79). En tableta vertical sus 288px
 * se comerían más de un tercio del ancho y dejarían la tabla de usuarios sin
 * sitio, así que de ahí para abajo el menú se sirve en el cajón.
 */
export function Sidebar() {
  return (
    /* overflow-y-auto: si los módulos no caben en pantallas bajas, se desplaza
       el menú por dentro y no la página entera (YS-78). */
    <aside className="hidden w-72 shrink-0 overflow-y-auto border-r border-divisor bg-superficie lg:block">
      <div className="border-b border-divisor px-6 py-6">
        <Logotipo />
      </div>

      <MenuPrincipal />
    </aside>
  );
}
