import { api } from "./api";

export async function listarUsuarios() {
  const { data } = await api.get("/usuarios");
  return data;
}

export async function crearUsuario(usuario) {
  const { data } = await api.post("/usuarios", usuario);
  return data;
}

export async function actualizarUsuario(id, usuario) {
  const { data } = await api.put(`/usuarios/${id}`, usuario);
  return data;
}

export async function cambiarEstadoUsuario(id, activo) {
  const { data } = await api.patch(`/usuarios/${id}/estado`, { activo });
  return data;
}

/** YS-71. Solo sobre otra persona: la propia se cambia desde el perfil. */
export async function restablecerPasswordUsuario(id, password) {
  const { data } = await api.put(`/usuarios/${id}/password`, { password });
  return data;
}
