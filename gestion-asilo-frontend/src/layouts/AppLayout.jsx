import { useState } from "react";

import { CajonNavegacion } from "../components/layout/CajonNavegacion";
import { Navbar } from "../components/layout/Navbar";
import { Sidebar } from "../components/layout/Sidebar";
import { CambiarPasswordModal } from "../components/usuarios/CambiarPasswordModal";
import { useAuth } from "../hooks/useAuth";

/**
 * YS-78. La ventana no se desplaza: lo hace el área de contenido. Así el menú
 * y la barra superior quedan siempre a la vista, y queda una sola barra de
 * desplazamiento en vez de la de la página compitiendo con la del menú.
 *
 * `h-dvh` y no `h-screen`: en los teléfonos la barra del navegador aparece y
 * desaparece al desplazar, y con `h-screen` parte de la pantalla quedaría
 * fuera de alcance.
 *
 * YS-79. El mismo menú se sirve de dos formas según el ancho: fijo a la
 * izquierda desde 1024px, y en un cajón por debajo de eso. El estado vive aquí
 * porque lo abre la barra superior y lo cierra el cajón.
 *
 * YS-75. El cambio de contraseña vive aquí, y no en una pantalla propia, para
 * que flote sobre donde el usuario estaba en vez de sacarlo de contexto. La
 * obligatoria se abre sola y no se puede descartar; la voluntaria la abre el
 * menú de usuario.
 *
 * YS-152. Al imprimir (los reportes se exportan a PDF con la impresión del
 * navegador) solo queda el contenido: sin menú ni barra, y sin la altura fija
 * ni el desplazamiento propio, que cortarían la hoja en lo que cabe en pantalla.
 */
export function AppLayout({ migas, children }) {
  const { debeCambiarPassword } = useAuth();
  const [cajonAbierto, setCajonAbierto] = useState(false);
  const [passwordAbierto, setPasswordAbierto] = useState(false);
  // Cambia en cada apertura para que el formulario se remonte limpio.
  const [aperturasPassword, setAperturasPassword] = useState(0);

  const abrirCambioPassword = () => {
    setAperturasPassword((valor) => valor + 1);
    setPasswordAbierto(true);
  };

  return (
    <div className="flex h-dvh overflow-hidden bg-fondo print:block print:h-auto print:overflow-visible print:bg-superficie">
      {/* contents: el envoltorio no cuenta en el flex; print:hidden lo quita al imprimir. */}
      <div className="contents print:hidden">
        <Sidebar />
        <CajonNavegacion abierto={cajonAbierto} alCerrar={() => setCajonAbierto(false)} />
      </div>

      <div className="flex min-w-0 flex-1 flex-col print:block">
        <div className="contents print:hidden">
          <Navbar
            migas={migas}
            menuAbierto={cajonAbierto}
            alAbrirMenu={() => setCajonAbierto(true)}
            alCambiarPassword={abrirCambioPassword}
          />
        </div>
        {/* px-4 en teléfono: con px-6 los márgenes se comían 48 de los 375px
            disponibles, que es justo lo que le falta a la tabla (YS-80). */}
        <main className="flex-1 overflow-y-auto px-4 py-8 sm:px-6 print:overflow-visible print:p-0">{children}</main>
      </div>

      {/* La obligatoria gana: mientras la marca esté puesta no hay nada que
          descartar, así que no importa si además la abrieron desde el menú. */}
      <CambiarPasswordModal
        abierto={debeCambiarPassword || passwordAbierto}
        obligatorio={debeCambiarPassword}
        clave={aperturasPassword}
        alCerrar={() => setPasswordAbierto(false)}
      />
    </div>
  );
}
