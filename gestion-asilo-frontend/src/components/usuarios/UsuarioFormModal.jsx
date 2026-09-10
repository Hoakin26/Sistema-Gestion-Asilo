import { useState } from "react";

import { ROLES } from "../../constants/roles";
import { erroresPorCampo, mensajeDeError } from "../../services/api";
import { actualizarUsuario, crearUsuario } from "../../services/usuarioService";
import { Alert } from "../ui/Alert";
import { Button } from "../ui/Button";
import { FormField } from "../ui/FormField";
import { Input, InputPassword, Select } from "../ui/Input";
import { Modal } from "../ui/Modal";

const VACIO = { nombreCompleto: "", correo: "", password: "", rol: "" };

/**
 * Las reglas replican las del backend (YS-42) para avisar antes de gastar una
 * petición, pero el backend sigue siendo la autoridad: si rechaza algo, sus
 * mensajes se pintan sobre estos campos.
 */
function validar(valores, esEdicion) {
  const errores = {};

  if (!valores.nombreCompleto.trim()) {
    errores.nombreCompleto = "El nombre completo es obligatorio";
  } else if (valores.nombreCompleto.trim().length > 150) {
    errores.nombreCompleto = "El nombre completo no puede exceder 150 caracteres";
  }

  const correo = valores.correo.trim();
  if (!correo) {
    errores.correo = "El correo es obligatorio";
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo)) {
    errores.correo = "El correo no tiene un formato válido";
  }

  if (!esEdicion) {
    if (!valores.password) {
      errores.password = "La contraseña es obligatoria";
    } else if (valores.password.length < 8 || valores.password.length > 64) {
      errores.password = "La contraseña debe tener entre 8 y 64 caracteres";
    }
  }

  if (!valores.rol) {
    errores.rol = "El rol es obligatorio";
  }

  return errores;
}

/**
 * El formulario se monta al abrir el modal y se desmonta al cerrarlo, con una
 * key distinta por usuario. Así su estado nace de las props y no hace falta un
 * efecto que lo resincronice cada vez.
 */
function FormularioUsuario({ usuario, alCerrar, alGuardar }) {
  const esEdicion = Boolean(usuario);
  const [valores, setValores] = useState(() =>
    usuario
      ? {
          nombreCompleto: usuario.nombreCompleto,
          correo: usuario.correo,
          password: "",
          rol: usuario.rol,
        }
      : VACIO,
  );
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState(null);
  const [guardando, setGuardando] = useState(false);

  const cambiar = (campo) => (evento) => {
    setValores((previos) => ({ ...previos, [campo]: evento.target.value }));
    // Limpiar el error al corregir evita que el mensaje contradiga lo escrito.
    setErrores((previos) => ({ ...previos, [campo]: undefined }));
  };

  const enviar = async (evento) => {
    evento.preventDefault();

    const encontrados = validar(valores, esEdicion);
    if (Object.keys(encontrados).length > 0) {
      setErrores(encontrados);
      return;
    }

    setGuardando(true);
    setErrorGeneral(null);

    try {
      const datos = {
        nombreCompleto: valores.nombreCompleto.trim(),
        correo: valores.correo.trim(),
        rol: valores.rol,
      };

      if (esEdicion) {
        await actualizarUsuario(usuario.id, datos);
      } else {
        await crearUsuario({ ...datos, password: valores.password });
      }

      alGuardar();
    } catch (error) {
      const porCampo = erroresPorCampo(error);
      if (porCampo) {
        setErrores(porCampo);
      } else if (error.response?.status === 409) {
        setErrores({ correo: mensajeDeError(error) });
      } else {
        setErrorGeneral(mensajeDeError(error, "No se pudo guardar el usuario"));
      }
    } finally {
      setGuardando(false);
    }
  };

  return (
      <form onSubmit={enviar} noValidate>
        <div className="flex flex-col gap-6 px-6 py-6">
          {errorGeneral && <Alert>{errorGeneral}</Alert>}

          <FormField etiqueta="Nombre completo" obligatorio error={errores.nombreCompleto}>
            {(campo) => (
              <Input
                {...campo}
                value={valores.nombreCompleto}
                onChange={cambiar("nombreCompleto")}
                autoComplete="name"
                maxLength={150}
                placeholder="Dra. Silvia Marroquín"
              />
            )}
          </FormField>

          <FormField
            etiqueta="Correo electrónico"
            obligatorio
            error={errores.correo}
            ayuda="Con este correo iniciará sesión en el sistema."
          >
            {(campo) => (
              <Input
                {...campo}
                type="email"
                value={valores.correo}
                onChange={cambiar("correo")}
                autoComplete="email"
                maxLength={150}
                placeholder="smarroquin@cabezadealgodon.org.gt"
              />
            )}
          </FormField>

          {/* En edición no se pide contraseña: cambiarla es una operación
              aparte, con sus propias reglas (ver docs/seguridad.md). */}
          {!esEdicion && (
            <FormField
              etiqueta="Contraseña"
              obligatorio
              error={errores.password}
              ayuda="Entre 8 y 64 caracteres."
            >
              {(campo) => (
                <InputPassword
                  {...campo}
                  value={valores.password}
                  onChange={cambiar("password")}
                  placeholder="Mínimo 8 caracteres"
                  autoComplete="new-password"
                  maxLength={64}
                />
              )}
            </FormField>
          )}

          <FormField
            etiqueta="Rol"
            obligatorio
            error={errores.rol}
            ayuda="Cada usuario tiene un único rol, que define a qué módulos entra."
          >
            {(campo) => (
              <Select {...campo} value={valores.rol} onChange={cambiar("rol")}>
                <option value="">Seleccione un rol…</option>
                {ROLES.map((rol) => (
                  <option key={rol.valor} value={rol.valor}>
                    {rol.etiqueta}
                  </option>
                ))}
              </Select>
            )}
          </FormField>
        </div>

        <div className="flex justify-end gap-3 border-t border-divisor px-6 py-4">
          <Button type="button" variante="secundario" onClick={alCerrar}>
            Cancelar
          </Button>
          <Button type="submit" cargando={guardando}>
            {esEdicion ? "Guardar cambios" : "Crear usuario"}
          </Button>
        </div>
      </form>
  );
}

/**
 * `claveFormulario` cambia en cada apertura para remontar el formulario con los
 * campos limpios. Se mantiene montado al cerrar —en vez de condicionar su
 * renderizado a `abierto`— porque desmontarlo dejaría el modal vacío durante
 * su animación de salida.
 */
export function UsuarioFormModal({
  abierto,
  usuario,
  claveFormulario,
  alCerrar,
  alGuardar,
}) {
  const esEdicion = Boolean(usuario);

  return (
    <Modal
      abierto={abierto}
      alCerrar={alCerrar}
      titulo={esEdicion ? "Editar usuario" : "Nuevo usuario"}
      descripcion={
        esEdicion
          ? "Actualice los datos y el rol de este usuario."
          : "Registre un usuario del sistema y asígnele su rol."
      }
    >
      <FormularioUsuario
        key={claveFormulario}
        usuario={usuario}
        alCerrar={alCerrar}
        alGuardar={alGuardar}
      />
    </Modal>
  );
}
