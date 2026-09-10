import isotipo from "../../assets/isotipo/isotipo-primario.svg";

/**
 * Bloque compacto de marca para el menú lateral: isotipo más el nombre corto.
 * El manual pide el logotipo horizontal cuando hay espacio, pero el ancho del
 * menú no alcanza para la línea "ASILO DE ANCIANOS" sin encogerla por debajo
 * del mínimo legible, así que aquí se usa la versión reducida.
 */
export function Logotipo() {
  return (
    <span className="flex items-center gap-3">
      <img src={isotipo} alt="" aria-hidden="true" className="size-9" />
      <span className="text-h3 font-bold text-primario">Cabeza de Algodón</span>
      <span className="sr-only">Asilo de Ancianos Cabeza de Algodón</span>
    </span>
  );
}
