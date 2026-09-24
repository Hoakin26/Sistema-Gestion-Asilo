import { Navigate, Route, Routes, useLocation } from "react-router-dom";

import { puedeEntrar } from "./constants/modulos";
import { useAuth } from "./hooks/useAuth";
import { DashboardPage } from "./pages/DashboardPage";
import { LoginPage } from "./pages/LoginPage";
import { SinPermisoPage } from "./pages/SinPermisoPage";
import { UsuariosPage } from "./pages/UsuariosPage";

/**
 * Dos guardias distintos, en este orden: primero si hay sesión, después si el
 * rol tiene acceso a esta ruta (YS-81).
 *
 * El orden importa. Si se comprobara el permiso primero, alguien sin sesión
 * vería "no tiene permiso" en lugar de que se le pida iniciarla.
 *
 * El permiso se resuelve con la misma matriz que arma el menú, así que el
 * guardia y lo que se muestra no pueden discrepar. Y es solo comodidad para el
 * usuario: la autoridad es el backend (YS-53), que responde 403 aunque este
 * guardia se equivoque.
 */
function RutaPrivada({ children }) {
  const { autenticado, usuario } = useAuth();
  const ubicacion = useLocation();

  if (!autenticado) {
    // YS-70: se recuerda de dónde salió para devolverlo ahí al volver a entrar,
    // en vez de dejarlo siempre en la pantalla inicial.
    return (
      <Navigate to="/login" replace state={{ desde: ubicacion.pathname + ubicacion.search }} />
    );
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
      {/* Al dashboard y no a un módulo concreto: es la única ruta que los
          siete roles tienen en común. */}
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}

export default App;
