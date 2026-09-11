import { useCallback, useEffect, useMemo, useState } from "react";

import { CLAVE_TOKEN } from "../services/api";
import { cerrarSesion, iniciarSesion } from "../services/authService";
import { AuthContext } from "./authContext";

const CLAVE_USUARIO = "asilo.usuario";

function leerUsuarioGuardado() {
  const crudo = localStorage.getItem(CLAVE_USUARIO);
  if (!crudo || !localStorage.getItem(CLAVE_TOKEN)) {
    return null;
  }
  try {
    return JSON.parse(crudo);
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(leerUsuarioGuardado);

  const limpiar = useCallback(() => {
    localStorage.removeItem(CLAVE_TOKEN);
    localStorage.removeItem(CLAVE_USUARIO);
    setUsuario(null);
  }, []);

  // El interceptor avisa cuando el backend rechaza el token (expirado,
  // invalidado por un cierre de sesión, o usuario desactivado).
  useEffect(() => {
    window.addEventListener("asilo:sesion-expirada", limpiar);
    return () => window.removeEventListener("asilo:sesion-expirada", limpiar);
  }, [limpiar]);

  const login = useCallback(async (correo, password) => {
    const sesion = await iniciarSesion(correo, password);
    const datos = {
      correo: sesion.correo,
      nombreCompleto: sesion.nombreCompleto,
      rol: sesion.rol,
    };

    localStorage.setItem(CLAVE_TOKEN, sesion.token);
    localStorage.setItem(CLAVE_USUARIO, JSON.stringify(datos));
    setUsuario(datos);
  }, []);

  const logout = useCallback(async () => {
    try {
      await cerrarSesion();
    } finally {
      // Aunque el backend falle, quien pidió salir debe quedar fuera aquí.
      limpiar();
    }
  }, [limpiar]);

  const valor = useMemo(
    () => ({ usuario, login, logout, autenticado: usuario !== null }),
    [usuario, login, logout],
  );

  return <AuthContext value={valor}>{children}</AuthContext>;
}
