import { api } from "./api";

export async function iniciarSesion(correo, password) {
  const { data } = await api.post("/auth/login", { correo, password });
  return data;
}

export async function cerrarSesion() {
  await api.post("/auth/logout");
}
