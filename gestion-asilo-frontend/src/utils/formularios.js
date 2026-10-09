/** Un texto opcional vacío viaja como null, no como "": "" no es "sin dato". */
export function opcional(texto) {
  const limpio = texto.trim();
  return limpio || null;
}

/**
 * Lleva el foco al primer campo con error. En los formularios largos el error
 * puede quedar fuera de vista, y en tableta el usuario no tiene cómo saber
 * que el botón no hizo nada por un campo que no ve.
 *
 * Espera al siguiente cuadro porque los campos se marcan como inválidos en el
 * renderizado que provoca el propio error, que todavía no ocurrió.
 */
export function enfocarPrimerError(formulario) {
  requestAnimationFrame(() => {
    formulario?.querySelector('[aria-invalid="true"]')?.focus();
  });
}
