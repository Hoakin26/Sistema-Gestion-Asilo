/**
 * YS-152. Abre la impresión del navegador para guardar un reporte como PDF
 * (HU-24 a HU-28). `nombre` es el título de la página mientras tanto: es el
 * nombre que el navegador le propone al archivo.
 */
export function exportarPdf(nombre) {
  const anterior = document.title;
  document.title = nombre;
  const restaurar = () => {
    document.title = anterior;
    window.removeEventListener("afterprint", restaurar);
  };
  window.addEventListener("afterprint", restaurar);
  window.print();
}
