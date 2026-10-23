import { Input } from "./Input";

/**
 * YS-121. El `CurrencyInput` del sistema de diseño: un monto en quetzales, con
 * la "Q" delante y las cifras alineadas a la derecha y con ancho fijo
 * (`tabular-nums`), como en las tablas de montos. Salió de la tarifa de
 * consulta (YS-169) para usarse también en el costo del laboratorio (HU-14).
 *
 * Es un campo de texto y no `type="number"`: el número de JavaScript puede
 * convertir 350.10 en 350.0999…, y el navegador deja escribir "1e3". El valor
 * viaja como texto y el backend lo lee como `BigDecimal`. `inputMode` abre el
 * teclado numérico con punto en la tableta.
 *
 * Se usa con el render-prop de `FormField`; el ancho lo decide quien lo usa.
 */
export function CurrencyInput({ className = "", ...props }) {
  return (
    <div className={`relative ${className}`}>
      <span
        aria-hidden="true"
        className="pointer-events-none absolute top-1/2 left-4 -translate-y-1/2 text-texto-secundario"
      >
        Q
      </span>
      <Input inputMode="decimal" autoComplete="off" className="pl-10 text-right tabular-nums slashed-zero" {...props} />
    </div>
  );
}
