import { useCallback, useMemo, useRef, useState } from "react";
import { CircleCheck, TriangleAlert, X } from "lucide-react";

import { ToastContext } from "./toastContext";

const DURACION_MS = 5000;

const TIPOS = {
  exito: { Icono: CircleCheck, clases: "bg-exito-tenue text-exito" },
  error: { Icono: TriangleAlert, clases: "bg-error-tenue text-error" },
};

export function ToastProvider({ children }) {
  const [avisos, setAvisos] = useState([]);
  const siguienteId = useRef(0);

  const descartar = useCallback((id) => {
    setAvisos((previos) => previos.filter((aviso) => aviso.id !== id));
  }, []);

  const mostrar = useCallback(
    (mensaje, tipo = "exito") => {
      const id = siguienteId.current++;
      setAvisos((previos) => [...previos, { id, mensaje, tipo }]);
      setTimeout(() => descartar(id), DURACION_MS);
    },
    [descartar],
  );

  const valor = useMemo(() => ({ mostrar }), [mostrar]);

  return (
    <ToastContext value={valor}>
      {children}

      {/*
        aria-live="polite" y no "assertive": la confirmación de una acción que
        el usuario acaba de hacer no debe interrumpir lo que esté leyendo.
      */}
      <div
        role="status"
        aria-live="polite"
        className="pointer-events-none fixed inset-x-0 bottom-0 z-50 flex flex-col items-center gap-3 px-4 py-6 sm:items-end sm:px-6"
      >
        {avisos.map(({ id, mensaje, tipo }) => {
          const { Icono, clases } = TIPOS[tipo];
          return (
            <div
              key={id}
              className={`entrada-aviso pointer-events-auto flex max-w-md items-start gap-3 rounded-tarjeta px-4 py-3 font-medium shadow-menu ${clases}`}
            >
              <Icono aria-hidden="true" size={20} className="mt-0.5 shrink-0" />
              <span className="text-auxiliar">{mensaje}</span>
              <button
                type="button"
                onClick={() => descartar(id)}
                aria-label="Descartar aviso"
                /* 44px de área táctil como exige el manual; los márgenes
                   negativos la extienden sobre el relleno del aviso para que
                   el botón no se vea desproporcionado (YS-80). */
                className="-my-2 -mr-2 ml-2 flex size-11 shrink-0 items-center justify-center rounded-campo hover:bg-superficie/50"
              >
                <X aria-hidden="true" size={16} />
              </button>
            </div>
          );
        })}
      </div>
    </ToastContext>
  );
}
