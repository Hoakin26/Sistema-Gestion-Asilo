import { useCallback, useEffect, useMemo, useState } from "react";

import { CLAVE_TOKEN } from "../services/api";
import { cambiarPassword, cerrarSesion, iniciarSesion } from "../services/authService";
import { AuthContext } from "./authContext";

const CLAVE_USUARIO = "asilo.usuario";

/** Lo que se guarda de la sesión. El token va aparte, en CLAVE_TOKEN. */
function datosDeSesion(sesion) {
  return {
    correo: sesion.correo,
    nombreCompleto: sesion.nombreCompleto,
    rol: sesion.rol,
    debeCambiarPassword: sesion.debeCambiarPassword,
  };
}

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
  // YS-70: distingue "la sesión se cayó sola" de "el usuario decidió salir".
  // Solo el primer caso merece una explicación en la pantalla de login.
  const [sesionTerminada, setSesionTerminada] = useState(false);
  /*
   * YS-75. La misma distinción, pero para el destino que se recuerda. Quien
   * cierra sesión a propósito no está a medias de nada, y anotar su última
   * pantalla terminaba aplicándosela al siguiente que entrara, aunque fuera
   * otra persona: salir desde "Cambiar contraseña" mandaba ahí al que entrara
   * después.
   */
  const [salioPorSuCuenta, setSalioPorSuCuenta] = useState(false);

  const limpiar = useCallback(() => {
    localStorage.removeItem(CLAVE_TOKEN);
    localStorage.removeItem(CLAVE_USUARIO);
    setUsuario(null);
  }, []);

  const terminarSesionSola = useCallback(() => {
    limpiar();
    setSesionTerminada(true);
    setSalioPorSuCuenta(false);
  }, [limpiar]);

  // El interceptor avisa cuando el backend rechaza el token (expirado,
  // invalidado por un cierre de sesión, o usuario desactivado).
  useEffect(() => {
    window.addEventListener("asilo:sesion-expirada", terminarSesionSola);
    return () => window.removeEventListener("asilo:sesion-expirada", terminarSesionSola);
  }, [terminarSesionSola]);

  /*
   * YS-74. El backend puede encender la marca a mitad de sesión, cuando un
   * administrador restablece la contraseña de alguien que está trabajando. El
   * interceptor lo detecta por el código del 403 y avisa por aquí, para que la
   * siguiente navegación lo lleve a cambiarla.
   */
  const exigirCambioPassword = useCallback(() => {
    setUsuario((previo) => {
      if (!previo || previo.debeCambiarPassword) {
        return previo;
      }
      const actualizado = { ...previo, debeCambiarPassword: true };
      localStorage.setItem(CLAVE_USUARIO, JSON.stringify(actualizado));
      return actualizado;
    });
  }, []);

  useEffect(() => {
    window.addEventListener("asilo:cambio-password-requerido", exigirCambioPassword);
    return () =>
      window.removeEventListener("asilo:cambio-password-requerido", exigirCambioPassword);
  }, [exigirCambioPassword]);

  const guardarSesion = useCallback((sesion) => {
    const datos = datosDeSesion(sesion);
    localStorage.setItem(CLAVE_TOKEN, sesion.token);
    localStorage.setItem(CLAVE_USUARIO, JSON.stringify(datos));
    setSesionTerminada(false);
    setSalioPorSuCuenta(false);
    setUsuario(datos);
  }, []);

  const login = useCallback(
    async (correo, password) => guardarSesion(await iniciarSesion(correo, password)),
    [guardarSesion],
  );

  /**
   * YS-75. Guarda la sesión que devuelve el cambio porque el backend invalida
   * todos los tokens anteriores, incluido el que usó esta misma petición: sin
   * reemplazarlo, el usuario quedaría fuera justo después de cumplir con lo
   * que se le pidió. La respuesta trae además la marca ya apagada.
   */
  const cambiarPasswordPropia = useCallback(
    async (passwordActual, passwordNueva) =>
      guardarSesion(await cambiarPassword(passwordActual, passwordNueva)),
    [guardarSesion],
  );

  const logout = useCallback(async () => {
    try {
      await cerrarSesion();
    } finally {
      // Aunque el backend falle, quien pidió salir debe quedar fuera aquí.
      limpiar();
      // Salir por decisión propia no es una sesión caída. Se limpia al final
      // porque si el token ya estaba vencido, la llamada anterior habrá
      // respondido 401 y el interceptor la habrá marcado como caída.
      setSesionTerminada(false);
      setSalioPorSuCuenta(true);
    }
  }, [limpiar]);

  const descartarAvisoSesion = useCallback(() => setSesionTerminada(false), []);

  const valor = useMemo(
    () => ({
      usuario,
      login,
      logout,
      cambiarPasswordPropia,
      autenticado: usuario !== null,
      debeCambiarPassword: usuario?.debeCambiarPassword === true,
      sesionTerminada,
      salioPorSuCuenta,
      descartarAvisoSesion,
    }),
    [
      usuario,
      login,
      logout,
      cambiarPasswordPropia,
      sesionTerminada,
      salioPorSuCuenta,
      descartarAvisoSesion,
    ],
  );

  return <AuthContext value={valor}>{children}</AuthContext>;
}
