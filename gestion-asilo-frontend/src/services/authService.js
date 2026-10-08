import { api } from "./api";

export async function iniciarSesion(correo, password) {
  const { data } = await api.post("/auth/login", { correo, password });
  return data;
}

export async function cerrarSesion() {
  await api.post("/auth/logout");
}

/**
 * YS-69. Cambia la contraseña propia. Devuelve una sesión completa, igual que
 * el login: el backend invalida todos los tokens anteriores —incluido el que
 * viajó en esta misma petición— y entrega uno nuevo, así que quien llame a
 * esto tiene que guardarlo o quedará con la sesión caída.
 */
export async function cambiarPassword(passwordActual, passwordNueva) {
  const { data } = await api.put("/auth/password", { passwordActual, passwordNueva });
  return data;
}
