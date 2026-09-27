import { useState } from "react";
import { Check, Copy, RefreshCw } from "lucide-react";

import { erroresPorCampo, mensajeDeError } from "../../services/api";
import { restablecerPasswordUsuario } from "../../services/usuarioService";
import { generarPassword } from "../../utils/password";
import { Alert } from "../ui/Alert";
import { Button } from "../ui/Button";
import { FormField } from "../ui/FormField";
import { Input } from "../ui/Input";
import { Modal } from "../ui/Modal";

function Formulario({ usuario, alCerrar, alExito }) {
  const [password, setPassword] = useState(generarPassword);
  const [error, setError] = useState(null);
  const [errorGeneral, setErrorGeneral] = useState(null);
  const [guardando, setGuardando] = useState(false);
  const [asignada, setAsignada] = useState(null);
  const [copiada, setCopiada] = useState(false);

  const enviar = async (evento) => {
    evento.preventDefault();

    // Mismas reglas que el backend (YS-42), para avisar sin gastar una petición.
    if (password.length < 8 || password.length > 64) {
      setError("La contraseña debe tener entre 8 y 64 caracteres");
      return;
    }

    setGuardando(true);
    setErrorGeneral(null);

    try {
      await restablecerPasswordUsuario(usuario.id, password);
      setAsignada(password);
      alExito(usuario.nombreCompleto);
    } catch (fallo) {
      const porCampo = erroresPorCampo(fallo);
      if (porCampo?.password) {
        setError(porCampo.password);
      } else {
        // Incluye el 403 de intentar restablecer la propia, que trae su
        // explicación desde el backend.
        setErrorGeneral(mensajeDeError(fallo, "No se pudo restablecer la contraseña"));
      }
    } finally {
      setGuardando(false);
    }
  };

  const copiar = async () => {
    try {
      await navigator.clipboard.writeText(asignada);
      setCopiada(true);
    } catch {
      setErrorGeneral(
        "No se pudo copiar automáticamente. Seleccione la contraseña y cópiela a mano.",
      );
    }
  };

  if (asignada) {
    return (
      <div className="flex flex-col gap-6 px-4 py-6 sm:px-6">
        <Alert tipo="exito" rol="status">
          {usuario.nombreCompleto} ya puede ingresar con esta contraseña.
        </Alert>

        <div className="flex flex-col gap-2">
          <p className="text-auxiliar text-texto-secundario">
            No vuelve a mostrarse: cópiela o anótela antes de cerrar.
          </p>
          <div className="flex flex-wrap items-center gap-3 rounded-tarjeta border border-texto-secundario px-4 py-3">
            {/* select-all: un clic la selecciona entera, sin arrastrar. */}
            <span className="grow text-h3 select-all">{asignada}</span>
            <Button type="button" variante="secundario" onClick={copiar}>
              {copiada ? (
                <Check aria-hidden="true" size={20} />
              ) : (
                <Copy aria-hidden="true" size={20} />
              )}
              {copiada ? "Copiada" : "Copiar"}
            </Button>
          </div>
        </div>

        {errorGeneral && <Alert>{errorGeneral}</Alert>}

        <div className="flex justify-end">
          <Button type="button" onClick={alCerrar}>
            Listo
          </Button>
        </div>
      </div>
    );
  }

  return (
    <form onSubmit={enviar} noValidate>
      <div className="flex flex-col gap-6 px-4 py-6 sm:px-6">
        {errorGeneral && <Alert>{errorGeneral}</Alert>}

        <Alert tipo="info" rol="status">
          {usuario.nombreCompleto} perderá las sesiones que tenga abiertas y
          deberá ingresar con la contraseña nueva.
        </Alert>

        <FormField
          etiqueta="Contraseña temporal"
          obligatorio
          error={error}
          ayuda="Entre 8 y 64 caracteres. Se muestra a la vista porque tendrá que dictarla o anotarla."
        >
          {(campo) => (
            <div className="flex flex-wrap gap-3">
              <Input
                {...campo}
                className="min-w-48 grow"
                value={password}
                onChange={(evento) => {
                  setPassword(evento.target.value);
                  setError(null);
                }}
                autoComplete="off"
                maxLength={64}
              />
              <Button
                type="button"
                variante="secundario"
                onClick={() => {
                  setPassword(generarPassword());
                  setError(null);
                }}
              >
                <RefreshCw aria-hidden="true" size={20} />
                Sugerir otra
              </Button>
            </div>
          )}
        </FormField>
      </div>

      <div className="sticky bottom-0 flex flex-wrap justify-end gap-3 border-t border-divisor bg-superficie px-4 py-4 sm:px-6">
        <Button type="button" variante="secundario" onClick={alCerrar}>
          Cancelar
        </Button>
        <Button type="submit" cargando={guardando}>
          Restablecer contraseña
        </Button>
      </div>
    </form>
  );
}

/**
 * YS-73. El formulario se remonta con una `clave` distinta en cada apertura
 * para que traiga una sugerencia nueva, y se mantiene montado al cerrar: si se
 * desmontara, el modal quedaría vacío durante su animación de salida.
 */
export function RestablecerPasswordModal({ abierto, usuario, clave, alCerrar, alExito }) {
  return (
    <Modal
      abierto={abierto}
      alCerrar={alCerrar}
      titulo="Restablecer contraseña"
      descripcion={
        usuario ? `Asigne una contraseña temporal a ${usuario.nombreCompleto}.` : ""
      }
    >
      {usuario && (
        <Formulario key={clave} usuario={usuario} alCerrar={alCerrar} alExito={alExito} />
      )}
    </Modal>
  );
}
