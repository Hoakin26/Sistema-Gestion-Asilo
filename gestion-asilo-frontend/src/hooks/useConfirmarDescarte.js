import { useCallback, useRef, useState } from "react";

/**
 * Cerrar un formulario con datos a medio escribir los perdería sin aviso. Este
 * hook concentra la regla que ya usaba el formulario de usuarios, para que los
 * formularios de internos no tengan que repetirla.
 *
 * Devuelve `marcarCambios` (lo llama el formulario en cada tecla),
 * `olvidarCambios` (al guardar con éxito), `intentarCerrar` (pide confirmación
 * si hubo cambios) y las props listas para el `ConfirmDialog`.
 *
 * Los cambios van en un ref y no en estado: solo los consulta el cierre, así
 * que marcarlos no necesita provocar un renderizado en cada tecla.
 */
export function useConfirmarDescarte(alCerrar) {
  const refCambios = useRef(false);
  const [confirmando, setConfirmando] = useState(false);

  const marcarCambios = useCallback(() => {
    refCambios.current = true;
  }, []);

  const olvidarCambios = useCallback(() => {
    refCambios.current = false;
  }, []);

  const intentarCerrar = () => {
    if (refCambios.current) {
      setConfirmando(true);
      return;
    }
    alCerrar();
  };

  const dialogo = {
    abierto: confirmando,
    titulo: "¿Descartar los cambios?",
    mensaje: "Escribió datos que todavía no se han guardado. Si cierra ahora, se perderán.",
    textoConfirmar: "Descartar",
    textoCancelar: "Seguir editando",
    varianteConfirmar: "peligro",
    alConfirmar: () => {
      refCambios.current = false;
      setConfirmando(false);
      alCerrar();
    },
    alCancelar: () => setConfirmando(false),
  };

  return { marcarCambios, olvidarCambios, intentarCerrar, dialogo };
}
