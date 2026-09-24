import { NavLink } from "react-router-dom";
import {
  FileText,
  Gift,
  LayoutDashboard,
  Receipt,
  Settings,
  Users,
  UserRound,
  Wallet,
  CircleDollarSign,
} from "lucide-react";

import { Logotipo } from "./Logotipo";

/*
 * Menú del Administrador del Asilo (ACT-04). Los módulos que aún no existen se
 * muestran deshabilitados en vez de ocultarse: el usuario ve el alcance del
 * sistema y no encuentra enlaces que no llevan a ninguna parte.
 */
const MODULOS = [
  { etiqueta: "Dashboard", Icono: LayoutDashboard, ruta: null },
  { etiqueta: "Usuarios y roles", Icono: Users, ruta: "/usuarios" },
  { etiqueta: "Internos", Icono: UserRound, ruta: null },
  { etiqueta: "Cuentas por cobrar", Icono: Receipt, ruta: null },
  { etiqueta: "Cuentas por pagar", Icono: Wallet, ruta: null },
  { etiqueta: "Donaciones", Icono: Gift, ruta: null },
  { etiqueta: "Gastos", Icono: CircleDollarSign, ruta: null },
  { etiqueta: "Reportes", Icono: FileText, ruta: null },
  { etiqueta: "Configuración", Icono: Settings, ruta: null },
];

const ITEM = "flex min-h-11 items-center gap-3 rounded-tarjeta px-4 font-medium";

export function Sidebar() {
  return (
    /* overflow-y-auto: si los módulos no caen en pantallas bajas, se desplaza
       el menú por dentro y no la página entera (YS-78). */
    <aside className="hidden w-72 shrink-0 overflow-y-auto border-r border-divisor bg-superficie md:block">
      <div className="border-b border-divisor px-6 py-6">
        <Logotipo />
      </div>

      <nav aria-label="Menú principal" className="px-4 py-6">
        <p className="px-4 pb-3 text-auxiliar font-semibold tracking-wide text-texto-secundario uppercase">
          Menú principal
        </p>

        <ul className="flex flex-col gap-1">
          {MODULOS.map(({ etiqueta, Icono, ruta }) => (
            <li key={etiqueta}>
              {ruta ? (
                <NavLink
                  to={ruta}
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
    </aside>
  );
}
