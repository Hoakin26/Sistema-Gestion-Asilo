import { Navigate, Route, Routes, useLocation } from "react-router-dom";

import { puedeEntrar } from "./constants/modulos";
import { useAuth } from "./hooks/useAuth";
import { DashboardPage } from "./pages/DashboardPage";
import { InternoPerfilPage } from "./pages/InternoPerfilPage";
import { InternosPage } from "./pages/InternosPage";
import { LoginPage } from "./pages/LoginPage";
import { SinPermisoPage } from "./pages/SinPermisoPage";
import { SolicitudesEntrantesPage } from "./pages/SolicitudesEntrantesPage";
import { SolicitudesReferenciaPage } from "./pages/SolicitudesReferenciaPage";
import { UsuariosPage } from "./pages/UsuariosPage";

/**
 * Tres guardias, en este orden: si hay sesión, si falta cambiar la contraseña,
 * y si el rol tiene acceso a esta ruta (YS-81).
 *
 * El orden importa en los dos escalones. Si se comprobara el permiso primero,
 * alguien sin sesión vería "no tiene permiso" en lugar de que se le pida
 * iniciarla; y si el cambio de contraseña fuera después del permiso, un usuario
 * marcado que escribiera una dirección ajena a su rol vería "no tiene permiso"
 * en vez de lo que de verdad le toca hacer.
 *
 * El permiso se resuelve con la misma matriz que arma el menú, así que el
 * guardia y lo que se muestra no pueden discrepar. Y es solo comodidad para el
 * usuario: la autoridad es el backend (YS-53 y YS-74), que responde 403 aunque
 * estos guardias se equivoquen.
 */
function RutaPrivada({ children }) {
  const { autenticado, usuario, debeCambiarPassword, salioPorSuCuenta } = useAuth();
  const ubicacion = useLocation();

  if (!autenticado) {
    /*
     * YS-70: se recuerda de dónde salió para devolverlo ahí al volver a entrar.
     *
     * Pero solo cuando la sesión se cayó o nunca la hubo. Quien cierra sesión a
     * propósito no está a medias de nada, y recordar su última pantalla tenía un
     * efecto peor: el destino quedaba anotado en la ruta de login y se lo
     * aplicaba al siguiente que entrara, aunque fuera otra persona.
     */
    const desde = salioPorSuCuenta ? undefined : ubicacion.pathname + ubicacion.search;
    return <Navigate to="/login" replace state={{ desde }} />;
  }

  /*
   * YS-75. Mientras falte cambiar la contraseña, el backend rechaza todo salvo
   * ese cambio, así que se lo deja en el Dashboard —la única pantalla que no
   * consulta nada— y encima se le abre la ventana obligatoria. Llevarlo a
   * cualquier otro módulo solo lo haría chocar contra un 403.
   */
  if (debeCambiarPassword && ubicacion.pathname !== "/dashboard") {
    return <Navigate to="/dashboard" replace />;
  }

  if (!puedeEntrar(usuario.rol, ubicacion.pathname)) {
    return <SinPermisoPage />;
  }

  return children;
}

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/dashboard"
        element={
          <RutaPrivada>
            <DashboardPage />
          </RutaPrivada>
        }
      />
      <Route
        path="/usuarios"
        element={
          <RutaPrivada>
            <UsuariosPage />
          </RutaPrivada>
        }
      />
      <Route
        path="/internos"
        element={
          <RutaPrivada>
            <InternosPage />
          </RutaPrivada>
        }
      />
      <Route
        path="/internos/:id"
        element={
          <RutaPrivada>
            <InternoPerfilPage />
          </RutaPrivada>
        }
      />
      <Route
        path="/solicitudes-referencia"
        element={
          <RutaPrivada>
            <SolicitudesReferenciaPage />
          </RutaPrivada>
        }
      />
      <Route
        path="/solicitudes-entrantes"
        element={
          <RutaPrivada>
            <SolicitudesEntrantesPage />
          </RutaPrivada>
        }
      />
      {/* Al dashboard y no a un módulo concreto: es la única ruta que los
          siete roles tienen en común. */}
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}

export default App;
