import { useState } from "react";
import { Navigate, useLocation } from "react-router-dom";

import logo from "../assets/isotipo/horizontal-primario.svg";
import { Alert } from "../components/ui/Alert";
import { Button } from "../components/ui/Button";
import { FormField } from "../components/ui/FormField";
import { Input, InputPassword } from "../components/ui/Input";
import { useAuth } from "../hooks/useAuth";
import { mensajeDeError } from "../services/api";

export function LoginPage() {
  const { login, autenticado, sesionTerminada, descartarAvisoSesion } = useAuth();
  const ubicacion = useLocation();
  const [correo, setCorreo] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(null);
  const [entrando, setEntrando] = useState(false);
  const [ayudaVisible, setAyudaVisible] = useState(false);

  // La dejó anotada RutaPrivada al expulsarlo; si entró al login por su cuenta,
  // no hay ninguna y va a la pantalla inicial.
  const destino = ubicacion.state?.desde ?? "/usuarios";

  if (autenticado) {
    return <Navigate to={destino} replace />;
  }

  const enviar = async (evento) => {
    evento.preventDefault();
    setError(null);
    // El aviso ya cumplió su función: sin esto quedaría junto a un eventual
    // error de credenciales, diciendo dos cosas distintas a la vez.
    descartarAvisoSesion();
    setEntrando(true);

    try {
      await login(correo.trim(), password);
    } catch (fallo) {
      setError(mensajeDeError(fallo, "No se pudo iniciar sesión"));
      setEntrando(false);
    }
  };

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-8 bg-fondo px-6 py-12">
      <img src={logo} alt="Asilo de Ancianos Cabeza de Algodón" className="h-16" />

      <div className="w-full max-w-md rounded-tarjeta border border-divisor bg-superficie p-8 shadow-tarjeta">
        <h1 className="text-h2 font-semibold">Iniciar sesión</h1>
        <p className="mt-2 text-texto-secundario">
          Ingrese sus credenciales institucionales para acceder al sistema.
        </p>

        <form onSubmit={enviar} noValidate className="mt-8 flex flex-col gap-6">
          {/* YS-70. El mensaje no dice "venció": el mismo 401 llega por
              expiración, por cierre de sesión en otro dispositivo o porque
              desactivaron al usuario, y el backend no los distingue a propósito. */}
          {sesionTerminada && (
            <Alert tipo="info" rol="status">
              Su sesión terminó. Vuelva a iniciar sesión para continuar.
            </Alert>
          )}

          {error && <Alert>{error}</Alert>}

          <FormField etiqueta="Correo electrónico">
            {(campo) => (
              <Input
                {...campo}
                type="email"
                value={correo}
                onChange={(evento) => setCorreo(evento.target.value)}
                placeholder="jgarcia@cabezadealgodon.org.gt"
                autoComplete="username"
                required
              />
            )}
          </FormField>

          <div className="flex flex-col gap-2">
            <FormField etiqueta="Contraseña">
              {(campo) => (
                <InputPassword
                  {...campo}
                  value={password}
                  onChange={(evento) => setPassword(evento.target.value)}
                  placeholder="Ingrese su contraseña"
                  autoComplete="current-password"
                  required
                />
              )}
            </FormField>

            <button
              type="button"
              onClick={() => setAyudaVisible((valor) => !valor)}
              aria-expanded={ayudaVisible}
              className="self-end text-auxiliar font-medium text-primario hover:text-primario-oscuro hover:underline"
            >
              ¿Olvidó su contraseña?
            </button>

            {/* No hay recuperación automática de contraseña en el sistema, así
                que el enlace explica la vía real en vez de no llevar a nada. */}
            {ayudaVisible && (
              <p className="entrada-error rounded-tarjeta bg-info-tenue px-4 py-3 text-auxiliar text-info">
                Solicite el restablecimiento al Administrador del Asilo: él puede
                asignarle una contraseña nueva desde el módulo de usuarios.
              </p>
            )}
          </div>

          <Button type="submit" cargando={entrando} className="w-full">
            Iniciar sesión
          </Button>
        </form>
      </div>

      <p className="text-auxiliar text-texto-secundario">
        Asilo de Ancianos Cabeza de Algodón · Sistema de gestión v1.0
      </p>
    </div>
  );
}
