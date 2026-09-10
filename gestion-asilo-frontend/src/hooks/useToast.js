import { use } from "react";

import { ToastContext } from "../context/toastContext";

export function useToast() {
  const contexto = use(ToastContext);
  if (!contexto) {
    throw new Error("useToast debe usarse dentro de ToastProvider");
  }
  return contexto;
}
