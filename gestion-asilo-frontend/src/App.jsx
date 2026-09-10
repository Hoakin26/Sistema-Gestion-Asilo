import { Navigate, Route, Routes } from "react-router-dom";

import { useAuth } from "./hooks/useAuth";
import { LoginPage } from "./pages/LoginPage";
import { UsuariosPage } from "./pages/UsuariosPage";

function RutaPrivada({ children }) {
  const { autenticado } = useAuth();
  return autenticado ? children : <Navigate to="/login" replace />;
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
