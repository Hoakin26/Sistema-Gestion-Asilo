import { Link } from "react-router-dom";

import { Alert } from "../components/ui/Alert";
import { modulosDeRol } from "../constants/modulos";
import { etiquetaDeRol } from "../constants/roles";
import { useAuth } from "../hooks/useAuth";
import { AppLayout } from "../layouts/AppLayout";

/**
 * YS-81. La pantalla inicial de los siete roles. Existe porque al restringir
 * los módulos por rol, seis de los siete se quedaban sin ningún lugar donde
 * aterrizar al iniciar sesión.
 *
 * Por ahora no muestra indicadores: los datos que resumiría —internos, citas,
 * cuentas— todavía no existen. En vez de inventar tarjetas vacías, dice con
 * qué cuenta el usuario hoy.
 */
export function DashboardPage() {
  const { usuario } = useAuth();

  // Los módulos construidos del rol, sin contar este mismo.
  const disponibles = modulosDeRol(usuario.rol).filter(
    (modulo) => modulo.ruta && modulo.ruta !== "/dashboard",
  );

  return (
    <AppLayout migas={["Inicio", "Dashboard"]}>
      <h1 className="text-h1 font-bold">Dashboard</h1>
      <p className="mt-2 text-texto-secundario">
        {usuario.nombreCompleto} · {etiquetaDeRol(usuario.rol)}
      </p>

      <div className="mt-6 max-w-2xl">
        {disponibles.length > 0 ? (
          <ul className="flex flex-col gap-3">
            {disponibles.map(({ etiqueta, Icono, ruta }) => (
              <li key={ruta}>
                <Link
                  to={ruta}
                  className="flex min-h-11 items-center gap-3 rounded-tarjeta border border-divisor bg-superficie px-4 py-4 font-medium text-texto shadow-tarjeta hover:bg-neutral-tenue"
                >
                  <Icono aria-hidden="true" size={20} className="text-primario" />
                  {etiqueta}
                </Link>
              </li>
            ))}
          </ul>
        ) : (
          /*
           * El aviso nombra lo que sí puede hacer. Decirle solo que no hay
           * nada disponible lo dejaría pensando que el sistema está roto.
           */
          <Alert tipo="info" rol="status">
            Los módulos de su rol estarán disponibles en una fase posterior del
            proyecto. Mientras tanto puede cambiar su contraseña desde el menú de
            usuario, arriba a la derecha.
          </Alert>
        )}
      </div>
    </AppLayout>
  );
}
