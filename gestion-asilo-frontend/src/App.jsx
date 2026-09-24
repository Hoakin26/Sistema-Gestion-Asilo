import { Navigate, Route, Routes, useLocation } from "react-router-dom";

import { useAuth } from "./hooks/useAuth";
import { LoginPage } from "./pages/LoginPage";
import { UsuariosPage } from "./pages/UsuariosPage";

function RutaPrivada({ children }) {
  const { autenticado } = useAuth();
  const ubicacion = useLocation();

  if (autenticado) {
    return children;
  }

  // YS-70: se recuerda de dónde salió para devolverlo ahí al volver a entrar,
  // en vez de dejarlo siempre en la pantalla inicial.
  return (
    <Navigate to="/login" replace state={{ desde: ubicacion.pathname + ubicacion.search }} />
  );
}

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/usuarios"
        element={
          <RutaPrivada>
            <UsuariosPage />
          </RutaPrivada>
        }
      />
      <Route path="*" element={<Navigate to="/usuarios" replace />} />
    </Routes>
  );
}

export default App;
