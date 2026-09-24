import { Navbar } from "../components/layout/Navbar";
import { Sidebar } from "../components/layout/Sidebar";

/**
 * YS-78. La ventana no se desplaza: lo hace el área de contenido. Así el menú
 * y la barra superior quedan siempre a la vista, y queda una sola barra de
 * desplazamiento en vez de la de la página compitiendo con la del menú.
 *
 * `h-dvh` y no `h-screen`: en los teléfonos la barra del navegador aparece y
 * desaparece al desplazar, y con `h-screen` parte de la pantalla quedaría
 * fuera de alcance.
 */
export function AppLayout({ migas, children }) {
  return (
    <div className="flex h-dvh overflow-hidden bg-fondo">
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <Navbar migas={migas} />
        <main className="flex-1 overflow-y-auto px-6 py-8">{children}</main>
      </div>
    </div>
  );
}
