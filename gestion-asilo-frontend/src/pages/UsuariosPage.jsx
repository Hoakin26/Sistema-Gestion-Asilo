import { useCallback, useEffect, useState } from "react";
import { Pencil, Plus, Users } from "lucide-react";

import { UsuarioFormModal } from "../components/usuarios/UsuarioFormModal";
import { Alert } from "../components/ui/Alert";
import { Button, IconButton } from "../components/ui/Button";
import { StatusBadge } from "../components/ui/StatusBadge";
import { TablaSkeleton } from "../components/ui/TablaSkeleton";
import { etiquetaDeRol } from "../constants/roles";
import { AppLayout } from "../layouts/AppLayout";
import { mensajeDeError } from "../services/api";
import { listarUsuarios } from "../services/usuarioService";

export function UsuariosPage() {
  const [usuarios, setUsuarios] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [enEdicion, setEnEdicion] = useState(null);
  const [modalAbierto, setModalAbierto] = useState(false);
  // Cambia en cada apertura para que el formulario se remonte limpio.
  const [aperturas, setAperturas] = useState(0);

  // No enciende el estado de carga: en la primera vez ya arranca encendido, y
  // al recargar tras guardar es preferible no parpadear la tabla completa.
  const cargar = useCallback(async () => {
    try {
      setUsuarios(await listarUsuarios());
      setError(null);
    } catch (fallo) {
      setError(mensajeDeError(fallo, "No se pudieron cargar los usuarios"));
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const abrir = (usuario) => {
    setEnEdicion(usuario);
    setAperturas((valor) => valor + 1);
    setModalAbierto(true);
  };

  const activos = usuarios.filter((usuario) => usuario.activo).length;

  return (
    <AppLayout migas={["Inicio", "Usuarios y roles"]}>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-h1 font-bold">Usuarios y roles</h1>
          <p className="mt-2 text-texto-secundario tabular-nums slashed-zero">
            {usuarios.length} usuarios registrados · {activos} activos
          </p>
        </div>

        <Button onClick={() => abrir(null)}>
          <Plus aria-hidden="true" size={20} />
          Nuevo usuario
        </Button>
      </div>

      <div className="mt-6">
        {error && <Alert>{error}</Alert>}

        {cargando ? (
          <TablaSkeleton />
        ) : usuarios.length === 0 ? (
          <div className="flex flex-col items-center gap-4 rounded-tarjeta border border-divisor bg-superficie px-6 py-16 text-center shadow-tarjeta">
            <Users aria-hidden="true" size={24} className="text-texto-secundario" />
            <p className="text-texto-secundario">
              Todavía no hay usuarios registrados.
            </p>
            <Button onClick={() => abrir(null)}>
              <Plus aria-hidden="true" size={20} />
              Nuevo usuario
            </Button>
          </div>
        ) : (
          <div className="overflow-x-auto rounded-tarjeta border border-divisor bg-superficie shadow-tarjeta">
            <table className="w-full min-w-3xl border-collapse text-left">
              <caption className="sr-only">
                Usuarios registrados con su rol y estado
              </caption>
              <thead>
                <tr className="border-b border-divisor bg-fondo">
                  <th scope="col" className="px-6 py-4 text-auxiliar font-semibold text-texto-secundario uppercase">
                    Usuario
                  </th>
                  <th scope="col" className="px-6 py-4 text-auxiliar font-semibold text-texto-secundario uppercase">
                    Rol
                  </th>
                  <th scope="col" className="px-6 py-4 text-auxiliar font-semibold text-texto-secundario uppercase">
                    Estado
                  </th>
                  <th scope="col" className="px-6 py-4 text-auxiliar font-semibold text-texto-secundario uppercase">
                    <span className="sr-only">Acciones</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {usuarios.map((usuario) => (
                  <tr key={usuario.id} className="border-b border-divisor last:border-b-0">
                    <td className="px-6 py-4">
                      <span className="block font-medium text-texto">
                        {usuario.nombreCompleto}
                      </span>
                      <span className="block text-auxiliar text-texto-secundario">
                        {usuario.correo}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <span className="inline-flex rounded-campo bg-primario-tenue px-2 py-1 text-auxiliar font-medium text-primario-oscuro">
                        {etiquetaDeRol(usuario.rol)}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <StatusBadge activo={usuario.activo} />
                    </td>
                    <td className="px-6 py-4 text-right">
                      <IconButton
                        etiqueta={`Editar a ${usuario.nombreCompleto}`}
                        onClick={() => abrir(usuario)}
                      >
                        <Pencil aria-hidden="true" size={20} />
                      </IconButton>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <UsuarioFormModal
        abierto={modalAbierto}
        usuario={enEdicion}
        claveFormulario={aperturas}
        alCerrar={() => setModalAbierto(false)}
        alGuardar={() => {
          setModalAbierto(false);
          cargar();
        }}
      />
    </AppLayout>
  );
}
