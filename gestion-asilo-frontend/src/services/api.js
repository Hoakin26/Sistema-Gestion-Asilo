import axios from "axios";

export const CLAVE_TOKEN = "asilo.token";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  headers: { "Content-Type": "application/json" },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(CLAVE_TOKEN);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/**
 * Un 401 significa que el token expiró, fue invalidado por un cierre de sesión
 * o el usuario fue desactivado. En los tres casos la sesión local ya no sirve.
 */
api.interceptors.response.use(
  (respuesta) => respuesta,
  (error) => {
    const esLogin = error.config?.url?.includes("/auth/login");
    if (error.response?.status === 401 && !esLogin) {
      localStorage.removeItem(CLAVE_TOKEN);
      window.dispatchEvent(new Event("asilo:sesion-expirada"));
    }
    return Promise.reject(error);
  },
);

/** Traduce el cuerpo de error uniforme del backend a algo mostrable. */
export function mensajeDeError(error, respaldo = "Ocurrió un error inesperado") {
  return error.response?.data?.mensaje ?? respaldo;
}

/** Errores por campo que devuelve el backend en una validación fallida. */
export function erroresPorCampo(error) {
  return error.response?.data?.errores ?? null;
}
