import { useState } from "react";

import { useAuth } from "../../hooks/useAuth";
import { useToast } from "../../hooks/useToast";
import { erroresPorCampo, mensajeDeError } from "../../services/api";
import { Alert } from "../ui/Alert";
import { Button } from "../ui/Button";
import { FormField } from "../ui/FormField";
import { InputPassword } from "../ui/Input";
import { Modal } from "../ui/Modal";

/**
 * Las reglas replican las del backend (YS-42/YS-69) para avisar antes de gastar
 * una petición, pero el backend sigue siendo la autoridad: si rechaza algo, sus
 * mensajes se pintan sobre estos campos.
 */
function validar(valores) {
  const errores = {};

  if (!valores.passwordActual) {
    errores.passwordActual = "La contraseña actual es obligatoria";
  }

  if (!valores.passwordNueva) {
    errores.passwordNueva = "La contraseña nueva es obligatoria";
  } else if (valores.passwordNueva.length < 8 || valores.passwordNueva.length > 64) {
    errores.passwordNueva = "La contraseña debe tener entre 8 y 64 caracteres";
  } else if (valores.passwordNueva === valores.passwordActual) {
    errores.passwordNueva = "La contraseña nueva debe ser distinta de la actual";
  }

  return errores;
}

function Formulario({ obligatorio, alCerrar }) {
  const { cambiarPasswordPropia, logout } = useAuth();
  const { mostrar } = useToast();

  const [valores, setValores] = useState({ passwordActual: "", passwordNueva: "" });
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

    const encontrados = validar(valores);
    if (Object.keys(encontrados).length > 0) {
      setErrores(encontrados);
      return;
    }

    setGuardando(true);
    setErrorGeneral(null);

    try {
      await cambiarPasswordPropia(valores.passwordActual, valores.passwordNueva);
      mostrar("Su contraseña se cambió. Use la nueva la próxima vez que ingrese.");
      alCerrar();
    } catch (fallo) {
      const porCampo = erroresPorCampo(fallo);
      if (porCampo) {
        setErrores(porCampo);
      } else {
        setErrorGeneral(mensajeDeError(fallo, "No se pudo cambiar la contraseña"));
      }
      setGuardando(false);
    }
  };

  return (
    <form onSubmit={enviar} noValidate>
      <div className="flex flex-col gap-6 px-4 py-6 sm:px-6">
        {errorGeneral && <Alert>{errorGeneral}</Alert>}

        {obligatorio && (
          /*
           * Dice el motivo y no solo la orden. "Debe cambiarla" a secas se lee
           * como un trámite; saber que otra persona conoce esa contraseña es lo
           * que hace entender por qué no se puede saltar (RNF-04).
           */
          <Alert tipo="info" rol="status">
            La contraseña con la que acaba de ingresar se la asignó otra persona, que
            sigue conociéndola. Elija una propia para continuar.
          </Alert>
        )}

        <FormField etiqueta="Contraseña actual" obligatorio error={errores.passwordActual}>
          {(campo) => (
            <InputPassword
              {...campo}
              value={valores.passwordActual}
              onChange={cambiar("passwordActual")}
              placeholder={obligatorio ? "La que le asignaron" : "Su contraseña de ahora"}
              autoComplete="current-password"
              maxLength={64}
            />
          )}
        </FormField>

        <FormField
          etiqueta="Contraseña nueva"
          obligatorio
          error={errores.passwordNueva}
          ayuda="Entre 8 y 64 caracteres. Use el ojo para verificar lo que escribió."
        >
          {(campo) => (
            <InputPassword
              {...campo}
              value={valores.passwordNueva}
              onChange={cambiar("passwordNueva")}
              placeholder="Mínimo 8 caracteres"
              autoComplete="new-password"
              maxLength={64}
            />
          )}
        </FormField>

        {/*
          Se avisa, no se pregunta. Cerrar las demás sesiones es lo que hace que
          el cambio sirva de algo cuando alguien más conocía la contraseña, así
          que ofrecerlo como opción sería una pregunta cuya respuesta correcta
          es siempre la misma.
        */}
        <p className="text-auxiliar text-texto-secundario">
          Al cambiarla se cerrarán sus sesiones abiertas en otros dispositivos.
        </p>
      </div>

      <div className="sticky bottom-0 flex flex-wrap justify-end gap-3 border-t border-divisor bg-superficie px-4 py-4 sm:px-6">
        {obligatorio ? (
          /*
           * La única salida de la ventana forzada. Sin ella quedaría encerrado
           * quien no pueda cambiarla en este momento.
           */
          <Button type="button" variante="secundario" onClick={logout}>
            Cerrar sesión
          </Button>
        ) : (
          <Button type="button" variante="secundario" onClick={alCerrar}>
            Descartar
          </Button>
        )}

        <Button type="submit" cargando={guardando}>
          Cambiar contraseña
        </Button>
      </div>
    </form>
  );
}

/**
 * YS-75. Una sola ventana para los dos caminos: el cambio obligatorio tras
 * recibir una contraseña asignada (HU-32) y el voluntario, que desde YS-69
 * existía en la API pero no tenía dónde usarse.
 *
 * Se mantienen juntos a propósito. Son el mismo formulario, contra el mismo
 * endpoint y con las mismas reglas; separarlos garantizaría que con el tiempo
 * uno corrija un detalle que el otro no.
 *
 * Es una ventana flotante y no una pantalla propia porque como pantalla se
 * parecía demasiado al login —misma tarjeta centrada sobre el fondo vacío— y no
 * se percibía el cambio. Flotando sobre la aplicación queda claro dónde está
 * uno, y en el caso obligatorio el fondo inerte del `<dialog>` comunica solo lo
 * que hace falta: está adentro, pero con algo pendiente.
 *
 * `clave` la cambia quien la abre para remontar el formulario limpio: sin eso,
 * descartar y volver a abrir mostraría lo escrito la vez anterior.
 */
export function CambiarPasswordModal({ abierto, obligatorio, clave, alCerrar }) {
  return (
    <Modal
      abierto={abierto}
      alCerrar={alCerrar}
      forzado={obligatorio}
      titulo={obligatorio ? "Cambie su contraseña" : "Cambiar contraseña"}
      descripcion={
        obligatorio
          ? "Es necesario antes de usar el sistema."
          : "Elija una contraseña nueva para su cuenta."
      }
    >
      <Formulario key={clave} obligatorio={obligatorio} alCerrar={alCerrar} />
    </Modal>
  );
}
