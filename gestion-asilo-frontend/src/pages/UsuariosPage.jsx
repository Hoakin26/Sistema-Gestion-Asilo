import { useCallback, useEffect, useState } from "react";
import {
  Filter,
  KeyRound,
  Pencil,
  Plus,
  Search,
  UserCheck,
  Users,
  UserX,
} from "lucide-react";

import { RestablecerPasswordModal } from "../components/usuarios/RestablecerPasswordModal";
import { UsuarioFormModal } from "../components/usuarios/UsuarioFormModal";
import { Alert } from "../components/ui/Alert";
import { Button, IconButton } from "../components/ui/Button";
import { ConfirmDialog } from "../components/ui/ConfirmDialog";
import { Input, Select } from "../components/ui/Input";
import { StatusBadge } from "../components/ui/StatusBadge";
import { TablaSkeleton } from "../components/ui/TablaSkeleton";
import { ROLES, etiquetaDeRol } from "../constants/roles";
import { useAuth } from "../hooks/useAuth";
import { useToast } from "../hooks/useToast";
import { AppLayout } from "../layouts/AppLayout";
import { mensajeDeError } from "../services/api";
import { cambiarEstadoUsuario, listarUsuarios } from "../services/usuarioService";

/* Sin tildes ni mayúsculas: buscar "garcia" debe encontrar a "García". */
function normalizar(texto) {
  return texto
    .toLowerCase()
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "");
}

export function UsuariosPage() {
  const { mostrar } = useToast();
  const { usuario: sesion } = useAuth();

  const [usuarios, setUsuarios] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);

  const [busqueda, setBusqueda] = useState("");
  const [rolFiltrado, setRolFiltrado] = useState("");

  const [enEdicion, setEnEdicion] = useState(null);
  const [modalAbierto, setModalAbierto] = useState(false);
  // Cambia en cada apertura para que el formulario se remonte limpio.
  const [aperturas, setAperturas] = useState(0);
  const [porDesactivar, setPorDesactivar] = useState(null);

  // El usuario se conserva al cerrar para que el modal no quede vacío durante
  // su animación de salida; lo que controla la visibilidad es el booleano.
  const [porRestablecer, setPorRestablecer] = useState(null);
  const [modalPasswordAbierto, setModalPasswordAbierto] = useState(false);
  const [aperturasPassword, setAperturasPassword] = useState(0);

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

  const abrirRestablecer = (usuario) => {
    setPorRestablecer(usuario);
    setAperturasPassword((valor) => valor + 1);
    setModalPasswordAbierto(true);
  };

  const cambiarEstado = async (usuario, activo) => {
    setPorDesactivar(null);
    try {
      await cambiarEstadoUsuario(usuario.id, activo);
      mostrar(
        activo
          ? `${usuario.nombreCompleto} puede volver a ingresar al sistema.`
          : `${usuario.nombreCompleto} ya no puede ingresar al sistema.`,
      );
      cargar();
    } catch (fallo) {
      mostrar(mensajeDeError(fallo, "No se pudo cambiar el estado"), "error");
    }
  };

  // Derivado durante el renderizado: no hace falta estado ni efecto aparte.
  const termino = normalizar(busqueda.trim());
  const visibles = usuarios.filter((usuario) => {
    const coincideTexto =
      !termino ||
      normalizar(usuario.nombreCompleto).includes(termino) ||
      normalizar(usuario.correo).includes(termino);
    const coincideRol = !rolFiltrado || usuario.rol === rolFiltrado;
    return coincideTexto && coincideRol;
  });

  const hayFiltros = Boolean(termino) || Boolean(rolFiltrado);
  const activos = usuarios.filter((usuario) => usuario.activo).length;

  const limpiarFiltros = () => {
    setBusqueda("");
    setRolFiltrado("");
  };

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

      {error && (
        <div className="mt-6">
          <Alert>{error}</Alert>
        </div>
      )}

      <div className="mt-6 flex flex-wrap gap-4">
        <div className="relative min-w-64 flex-1">
          <Search
            aria-hidden="true"
            size={20}
            className="pointer-events-none absolute top-1/2 left-4 -translate-y-1/2 text-texto-secundario"
          />
          <Input
            type="search"
            value={busqueda}
            onChange={(evento) => setBusqueda(evento.target.value)}
            placeholder="Buscar por nombre o correo…"
            aria-label="Buscar usuarios por nombre o correo"
            className="pl-12"
          />
        </div>

        <div className="relative">
          <Filter
            aria-hidden="true"
            size={20}
            className="pointer-events-none absolute top-1/2 left-4 -translate-y-1/2 text-texto-secundario"
          />
          <Select
            value={rolFiltrado}
            onChange={(evento) => setRolFiltrado(evento.target.value)}
            aria-label="Filtrar por rol"
            className="pl-12"
          >
            <option value="">Todos los roles</option>
            {ROLES.map((rol) => (
              <option key={rol.valor} value={rol.valor}>
                {rol.etiqueta}
              </option>
            ))}
          </Select>
        </div>
      </div>

      <div className="mt-6">
        {cargando ? (
          <TablaSkeleton etiqueta="Cargando usuarios…" />
        ) : usuarios.length === 0 ? (
          <div className="flex flex-col items-center gap-4 rounded-tarjeta border border-divisor bg-superficie px-4 py-16 text-center shadow-tarjeta sm:px-6">
            <Users aria-hidden="true" size={24} className="text-texto-secundario" />
            <p className="text-texto-secundario">Todavía no hay usuarios registrados.</p>
            <Button onClick={() => abrir(null)}>
              <Plus aria-hidden="true" size={20} />
              Nuevo usuario
            </Button>
          </div>
        ) : visibles.length === 0 ? (
          /* Vacío por filtros ≠ vacío por falta de datos: el usuario necesita
             saber cuál de los dos es y cómo salir de él. */
          <div className="flex flex-col items-center gap-4 rounded-tarjeta border border-divisor bg-superficie px-4 py-16 text-center shadow-tarjeta sm:px-6">
            <Search aria-hidden="true" size={24} className="text-texto-secundario" />
            <p className="text-texto-secundario">
              Ningún usuario coincide con lo que buscó.
            </p>
            <Button variante="secundario" onClick={limpiarFiltros}>
              Quitar filtros
            </Button>
          </div>
        ) : (
          <>
            {hayFiltros && (
              <p
                aria-live="polite"
                className="pb-3 text-auxiliar text-texto-secundario tabular-nums slashed-zero"
              >
                Mostrando {visibles.length} de {usuarios.length} usuarios
              </p>
            )}

            {/* min-w-2xl y no 3xl: el piso anterior forzaba 768px y obligaba a
                desplazar la tabla de lado incluso en la laptop de 1024px, donde
                el menú fijo ya se queda con 288. El manual permite el
                desplazamiento horizontal, pero no si se puede evitar (YS-80). */}
            {/* relative: los sr-only de la tabla son absolutos; sin él escapan del
                recorte, estiran la página y el teléfono la achica a una esquina (YS-175). */}
            <div className="relative overflow-x-auto rounded-tarjeta border border-divisor bg-superficie shadow-tarjeta">
              <table className="w-full min-w-2xl border-collapse text-left">
                <caption className="sr-only">
                  Usuarios registrados con su rol y estado
                </caption>
                <thead>
                  <tr className="border-b border-divisor bg-fondo">
                    <th scope="col" className="px-4 py-4 text-auxiliar font-semibold text-texto-secundario uppercase xl:px-6">
                      Usuario
                    </th>
                    <th scope="col" className="px-4 py-4 text-auxiliar font-semibold text-texto-secundario uppercase xl:px-6">
                      Rol
                    </th>
                    <th scope="col" className="px-4 py-4 text-auxiliar font-semibold text-texto-secundario uppercase xl:px-6">
                      Estado
                    </th>
                    <th scope="col" className="px-4 py-4 text-auxiliar font-semibold text-texto-secundario uppercase xl:px-6">
                      <span className="sr-only">Acciones</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {visibles.map((usuario) => (
                    <tr key={usuario.id} className="border-b border-divisor last:border-b-0">
                      <td className="px-4 py-4 xl:px-6">
                        <span className="block font-medium text-texto">
                          {usuario.nombreCompleto}
                        </span>
                        <span className="block text-auxiliar text-texto-secundario">
                          {usuario.correo}
                        </span>
                      </td>
                      <td className="px-4 py-4 xl:px-6">
                        {/* Neutro y no azul: el color con significado queda
                            reservado al estado, que es lo que debe resaltar. */}
                        <span className="inline-flex rounded-campo bg-neutral-tenue px-2 py-1 text-auxiliar font-medium text-neutral">
                          {etiquetaDeRol(usuario.rol)}
                        </span>
                      </td>
                      <td className="px-4 py-4 xl:px-6">
                        <StatusBadge activo={usuario.activo} />
                      </td>
                      <td className="px-4 py-4 xl:px-6">
                        <div className="flex justify-end gap-1">
                          <IconButton
                            etiqueta={`Editar a ${usuario.nombreCompleto}`}
                            onClick={() => abrir(usuario)}
                          >
                            <Pencil aria-hidden="true" size={20} />
                          </IconButton>

                          {/* En la fila propia no se ofrece: restablecer no pide
                              la contraseña actual, así que el backend lo rechaza
                              (YS-71). La propia se cambia desde el perfil. */}
                          {usuario.correo !== sesion?.correo && (
                            <IconButton
                              etiqueta={`Restablecer la contraseña de ${usuario.nombreCompleto}, cerrará sus sesiones abiertas`}
                              onClick={() => abrirRestablecer(usuario)}
                            >
                              <KeyRound aria-hidden="true" size={20} />
                            </IconButton>
                          )}

                          {usuario.activo ? (
                            <IconButton
                              etiqueta={`Desactivar a ${usuario.nombreCompleto}, le cortará el acceso al sistema`}
                              onClick={() => setPorDesactivar(usuario)}
                            >
                              <UserX aria-hidden="true" size={20} />
                            </IconButton>
                          ) : (
                            <IconButton
                              etiqueta={`Activar a ${usuario.nombreCompleto}, podrá volver a ingresar`}
                              onClick={() => cambiarEstado(usuario, true)}
                            >
                              <UserCheck aria-hidden="true" size={20} />
                            </IconButton>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </div>

      <UsuarioFormModal
        abierto={modalAbierto}
        usuario={enEdicion}
        claveFormulario={aperturas}
        alCerrar={() => setModalAbierto(false)}
        alGuardar={(nombre) => {
          setModalAbierto(false);
          mostrar(
            enEdicion
              ? `Se guardaron los cambios de ${nombre}.`
              : `${nombre} ya puede ingresar al sistema.`,
          );
          cargar();
        }}
      />

      {/* No lleva confirmación aparte: a diferencia de desactivar, que es un
          clic inmediato, aquí ya hubo que abrir el modal, revisar la contraseña
          y pulsar el botón. El aviso de la consecuencia va dentro. */}
      <RestablecerPasswordModal
        abierto={modalPasswordAbierto}
        usuario={porRestablecer}
        clave={aperturasPassword}
        alCerrar={() => setModalPasswordAbierto(false)}
        alExito={(nombre) => mostrar(`Se asignó una contraseña nueva a ${nombre}.`)}
      />

      {/* Desactivar corta el acceso de inmediato, incluso con sesión abierta:
          merece confirmación explícita. Activar no la necesita. */}
      <ConfirmDialog
        abierto={porDesactivar !== null}
        titulo="¿Desactivar a este usuario?"
        mensaje={
          porDesactivar
            ? `${porDesactivar.nombreCompleto} dejará de poder ingresar al sistema de inmediato, aunque tenga la sesión abierta. Puede volver a activarlo cuando quiera.`
            : ""
        }
        textoConfirmar="Desactivar"
        textoCancelar="Cancelar"
        varianteConfirmar="peligro"
        alConfirmar={() => cambiarEstado(porDesactivar, false)}
        alCancelar={() => setPorDesactivar(null)}
      />
    </AppLayout>
  );
}
