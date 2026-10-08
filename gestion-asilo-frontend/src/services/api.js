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

/** YS-74. Lo manda el backend en el 403 cuando falta cambiar la contraseña. */
const CODIGO_CAMBIO_REQUERIDO = "CAMBIO_PASSWORD_REQUERIDO";

/**
 * Un 401 significa que el token expiró, fue invalidado por un cierre de sesión
 * o el usuario fue desactivado. En los tres casos la sesión local ya no sirve.
 *
 * El 403 con código es otra cosa: la sesión es válida, pero el usuario sigue
 * usando una contraseña que le asignó otra persona. Se reconoce por el código
 * y no por el texto del mensaje, que es una frase en español que cualquiera
 * puede reescribir sin saber que algo depende de ella.
 *
 * Hace falta además del aviso que ya trae el login, porque la marca puede
 * encenderse a mitad de sesión: ocurre cada vez que un administrador
 * restablece la contraseña de alguien que está trabajando.
 */
api.interceptors.response.use(
  (respuesta) => respuesta,
  (error) => {
    const esLogin = error.config?.url?.includes("/auth/login");
    if (error.response?.status === 401 && !esLogin) {
      localStorage.removeItem(CLAVE_TOKEN);
      window.dispatchEvent(new Event("asilo:sesion-expirada"));
    }
    if (error.response?.data?.codigo === CODIGO_CAMBIO_REQUERIDO) {
      window.dispatchEvent(new Event("asilo:cambio-password-requerido"));
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
