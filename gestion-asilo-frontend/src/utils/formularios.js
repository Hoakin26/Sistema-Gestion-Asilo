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
 *
 * YS-85. El campo se centra en vez de dejar que `focus()` lo desplace: el
 * navegador lo lleva al borde superior de la ventana, que es justo donde está
 * pegado el título del modal, y el campo quedaba tapado por él.
 */
export function enfocarPrimerError(formulario) {
  requestAnimationFrame(() => {
    const campo = formulario?.querySelector('[aria-invalid="true"]');
    if (!campo) return;
    campo.focus({ preventScroll: true });
    campo.scrollIntoView({ block: "center" });
  });
}
