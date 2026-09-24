import { createContext } from "react";

/* En archivo aparte del proveedor para no romper el Fast Refresh de Vite. */
export const ToastContext = createContext(null);
