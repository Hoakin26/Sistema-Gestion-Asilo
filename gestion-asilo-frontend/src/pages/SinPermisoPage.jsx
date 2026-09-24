import { Link } from "react-router-dom";
import { ShieldAlert } from "lucide-react";

import { AppLayout } from "../layouts/AppLayout";

/**
 * YS-81. Lo que ve quien escribe a mano la dirección de un módulo que no es de
 * su rol.
 *
 * Se muestra dentro del esqueleto de la aplicación, con su menú al lado, en vez
 * de una pantalla suelta: así el usuario entiende qué pasó y tiene a la vista
 * por dónde seguir. Y se le dice, en lugar de devolverlo callado a otro lado:
 * una redirección silenciosa lo dejaría creyendo que el enlace falló.
 *
 * El mensaje no nombra el rol que haría falta, igual que la respuesta del
 * backend (YS-54): eso le iría describiendo el mapa de permisos a quien esté
 * probando dónde puede entrar.
 */
export function SinPermisoPage() {
  return (
    <AppLayout migas={["Inicio", "Sin permiso"]}>
      <div className="mx-auto flex max-w-xl flex-col items-center gap-4 rounded-tarjeta border border-divisor bg-superficie px-4 py-16 text-center shadow-tarjeta sm:px-6">
        <ShieldAlert aria-hidden="true" size={24} className="text-advertencia" />

        <h1 className="text-h2 font-semibold">No tiene permiso para ver esta página</h1>

        <p className="text-texto-secundario">
          Esta sección no corresponde a su rol. Si cree que es un error,
          consúltelo con el Administrador del Asilo.
        </p>

        <Link
          to="/dashboard"
          className="inline-flex min-h-11 items-center rounded-tarjeta bg-primario px-4 font-medium text-white hover:bg-primario-oscuro"
        >
          Volver al inicio
        </Link>
      </div>
    </AppLayout>
  );
}
