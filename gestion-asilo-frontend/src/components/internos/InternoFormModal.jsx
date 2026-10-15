import { useRef, useState } from "react";

import { useConfirmarDescarte } from "../../hooks/useConfirmarDescarte";
import { erroresPorCampo, mensajeDeError } from "../../services/api";
import { actualizarInterno, registrarInterno } from "../../services/internoService";
import { hoyISO } from "../../utils/fechas";
import { enfocarPrimerError, opcional } from "../../utils/formularios";
import { Alert } from "../ui/Alert";
import { Button } from "../ui/Button";
import { ConfirmDialog } from "../ui/ConfirmDialog";
import { FormField } from "../ui/FormField";
import { Input } from "../ui/Input";
import { Modal } from "../ui/Modal";

/*
 * Las claves son las mismas rutas con que el backend reporta sus errores
 * ("familiarResponsable.telefono"). Así un rechazo del servidor cae solo en su
 * campo, sin una tabla que traduzca nombres de un lado al otro.
 */
const FAMILIAR = "familiarResponsable.";

const VACIO = {
  nombreCompleto: "",
  dpi: "",
  fechaNacimiento: "",
  fechaIngreso: "",
  [`${FAMILIAR}nombreCompleto`]: "",
  [`${FAMILIAR}parentesco`]: "",
  [`${FAMILIAR}telefono`]: "",
  [`${FAMILIAR}correo`]: "",
  [`${FAMILIAR}direccion`]: "",
};

function valoresDe(interno) {
  if (!interno) return VACIO;
  const familiar = interno.familiarResponsable;
  return {
    nombreCompleto: interno.nombreCompleto,
    dpi: interno.dpi,
    fechaNacimiento: interno.fechaNacimiento,
    fechaIngreso: interno.fechaIngreso,
    [`${FAMILIAR}nombreCompleto`]: familiar.nombreCompleto,
    [`${FAMILIAR}parentesco`]: familiar.parentesco,
    [`${FAMILIAR}telefono`]: familiar.telefono,
    [`${FAMILIAR}correo`]: familiar.correo ?? "",
    [`${FAMILIAR}direccion`]: familiar.direccion ?? "",
  };
}

/* Se aceptan espacios al escribirlo, como viene impreso en el documento. */
function soloDigitos(texto) {
  return texto.replace(/\s/g, "");
}

/**
 * Las reglas replican las del backend (YS-59) para avisar antes de gastar una
 * petición, con los mismos mensajes. El backend sigue siendo la autoridad: si
 * rechaza algo, sus errores se pintan sobre estos mismos campos.
 */
function validar(valores) {
  const errores = {};
  const hoy = hoyISO();

  const exigir = (campo, mensaje, maximo, mensajeMaximo) => {
    const texto = valores[campo].trim();
    if (!texto) errores[campo] = mensaje;
    else if (texto.length > maximo) errores[campo] = mensajeMaximo;
  };

  exigir(
    "nombreCompleto",
    "El nombre completo es obligatorio",
    150,
    "El nombre completo no puede exceder 150 caracteres",
  );

  const dpi = soloDigitos(valores.dpi);
  if (!dpi) errores.dpi = "El DPI es obligatorio";
  else if (!/^\d{13}$/.test(dpi)) errores.dpi = "El DPI debe tener exactamente 13 dígitos";

  // Las fechas "AAAA-MM-DD" se comparan bien como texto.
  if (!valores.fechaNacimiento) {
    errores.fechaNacimiento = "La fecha de nacimiento es obligatoria";
  } else if (valores.fechaNacimiento > hoy) {
    errores.fechaNacimiento = "La fecha de nacimiento no puede estar en el futuro";
  }

  if (!valores.fechaIngreso) {
    errores.fechaIngreso = "La fecha de ingreso es obligatoria";
  } else if (valores.fechaIngreso > hoy) {
    errores.fechaIngreso = "La fecha de ingreso no puede estar en el futuro";
  } else if (valores.fechaNacimiento && valores.fechaIngreso < valores.fechaNacimiento) {
    errores.fechaIngreso = "La fecha de ingreso no puede ser anterior a la fecha de nacimiento";
  }

  exigir(
    `${FAMILIAR}nombreCompleto`,
    "El nombre del familiar es obligatorio",
    150,
    "El nombre no puede exceder 150 caracteres",
  );
  exigir(
    `${FAMILIAR}parentesco`,
    "El parentesco es obligatorio",
    50,
    "El parentesco no puede exceder 50 caracteres",
  );
  exigir(
    `${FAMILIAR}telefono`,
    "El teléfono del familiar es obligatorio",
    20,
    "El teléfono no puede exceder 20 caracteres",
  );

  const correo = valores[`${FAMILIAR}correo`].trim();
  if (correo && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo)) {
    errores[`${FAMILIAR}correo`] = "El correo no tiene un formato válido";
  }

  return errores;
}

/**
 * Montado con una key por apertura: su estado nace de las props y no hace
 * falta un efecto que lo resincronice (mismo patrón que el de usuarios).
 */
function FormularioInterno({ interno, alCambiar, alCerrar, alGuardar }) {
  const esEdicion = Boolean(interno);
  const refFormulario = useRef(null);
  const [valores, setValores] = useState(() => valoresDe(interno));
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState(null);
  const [guardando, setGuardando] = useState(false);

  /* Valor, cambio y error de un campo, para no repetir las tres cosas en cada uno. */
  const enlazar = (campo) => ({
    value: valores[campo],
    onChange: (evento) => {
      alCambiar();
      setValores((previos) => ({ ...previos, [campo]: evento.target.value }));
      // Limpiar el error al corregir evita que el mensaje contradiga lo escrito.
      setErrores((previos) => ({ ...previos, [campo]: undefined }));
    },
  });

  const fallar = (encontrados) => {
    setErrores(encontrados);
    enfocarPrimerError(refFormulario.current);
  };

  const enviar = async (evento) => {
    evento.preventDefault();

    const encontrados = validar(valores);
    if (Object.keys(encontrados).length > 0) {
      fallar(encontrados);
      return;
    }

    setGuardando(true);
    setErrorGeneral(null);

    const datos = {
      nombreCompleto: valores.nombreCompleto.trim(),
      dpi: soloDigitos(valores.dpi),
      fechaNacimiento: valores.fechaNacimiento,
      fechaIngreso: valores.fechaIngreso,
      /*
       * RNF-02. La cuota es un dato financiero que no le compete al Médico
       * General, así que el formulario no la muestra. Pero el PUT reemplaza el
       * expediente completo: al editar se reenvía la que ya estaba guardada,
       * porque mandarla vacía la borraría. La fija el Administrador del Asilo
       * por su propia vía (YS-132).
       */
      cuotaMensual: interno?.cuotaMensual ?? null,
      familiarResponsable: {
        nombreCompleto: valores[`${FAMILIAR}nombreCompleto`].trim(),
        parentesco: valores[`${FAMILIAR}parentesco`].trim(),
        telefono: valores[`${FAMILIAR}telefono`].trim(),
        correo: opcional(valores[`${FAMILIAR}correo`]),
        direccion: opcional(valores[`${FAMILIAR}direccion`]),
      },
    };

    try {
      const guardado = esEdicion
        ? await actualizarInterno(interno.id, datos)
        : await registrarInterno(datos);
      alGuardar(guardado);
    } catch (error) {
      const porCampo = erroresPorCampo(error);
      if (porCampo) {
        fallar(porCampo);
      } else if (error.response?.status === 409) {
        // El único conflicto posible aquí es el DPI repetido.
        fallar({ dpi: mensajeDeError(error) });
      } else {
        setErrorGeneral(mensajeDeError(error, "No se pudo guardar el interno"));
      }
    } finally {
      setGuardando(false);
    }
  };

  const hoy = hoyISO();

  return (
    <form ref={refFormulario} onSubmit={enviar} noValidate>
      <div className="flex flex-col gap-8 px-4 py-6 sm:px-6">
        {errorGeneral && <Alert>{errorGeneral}</Alert>}

        <fieldset>
          <legend className="mb-4 font-semibold text-texto">Datos personales</legend>
          <div className="grid gap-6 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <FormField etiqueta="Nombre completo" obligatorio error={errores.nombreCompleto}>
                {(campo) => (
                  /* autoComplete="off": es el nombre de otra persona, y el
                     navegador ofrecería el del usuario que está registrando. */
                  <Input
                    {...campo}
                    {...enlazar("nombreCompleto")}
                    autoComplete="off"
                    maxLength={150}
                    placeholder="María Elena López Hernández"
                  />
                )}
              </FormField>
            </div>

            <div className="sm:col-span-2">
              <FormField
                etiqueta="DPI"
                obligatorio
                error={errores.dpi}
                ayuda="Los 13 dígitos del Documento Personal de Identificación."
              >
                {(campo) => (
                  <Input
                    {...campo}
                    {...enlazar("dpi")}
                    inputMode="numeric"
                    autoComplete="off"
                    maxLength={15}
                    placeholder="1234 56789 0101"
                    className="tabular-nums slashed-zero"
                  />
                )}
              </FormField>
            </div>

            <FormField
              etiqueta="Fecha de nacimiento"
              obligatorio
              error={errores.fechaNacimiento}
            >
              {(campo) => (
                <Input
                  {...campo}
                  {...enlazar("fechaNacimiento")}
                  type="date"
                  min="1900-01-01"
                  max={hoy}
                  className="tabular-nums slashed-zero"
                />
              )}
            </FormField>

            <FormField etiqueta="Fecha de ingreso" obligatorio error={errores.fechaIngreso}>
              {(campo) => (
                <Input
                  {...campo}
                  {...enlazar("fechaIngreso")}
                  type="date"
                  min={valores.fechaNacimiento || "1900-01-01"}
                  max={hoy}
                  className="tabular-nums slashed-zero"
                />
              )}
            </FormField>
          </div>
        </fieldset>

        <fieldset>
          <legend className="mb-4 font-semibold text-texto">Familiar responsable</legend>
          <div className="grid gap-6 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <FormField
                etiqueta="Nombre completo"
                obligatorio
                error={errores[`${FAMILIAR}nombreCompleto`]}
              >
                {(campo) => (
                  <Input
                    {...campo}
                    {...enlazar(`${FAMILIAR}nombreCompleto`)}
                    autoComplete="off"
                    maxLength={150}
                    placeholder="Carlos Alberto López Ruiz"
                  />
                )}
              </FormField>
            </div>

            <FormField
              etiqueta="Parentesco"
              obligatorio
              error={errores[`${FAMILIAR}parentesco`]}
            >
              {(campo) => (
                <Input
                  {...campo}
                  {...enlazar(`${FAMILIAR}parentesco`)}
                  autoComplete="off"
                  maxLength={50}
                  placeholder="Hijo, sobrina, cónyuge…"
                />
              )}
            </FormField>

            <FormField etiqueta="Teléfono" obligatorio error={errores[`${FAMILIAR}telefono`]}>
              {(campo) => (
                <Input
                  {...campo}
                  {...enlazar(`${FAMILIAR}telefono`)}
                  type="tel"
                  inputMode="tel"
                  autoComplete="off"
                  maxLength={20}
                  placeholder="5555 1234"
                  className="tabular-nums slashed-zero"
                />
              )}
            </FormField>

            <div className="sm:col-span-2">
              <FormField
                etiqueta="Correo electrónico"
                error={errores[`${FAMILIAR}correo`]}
                ayuda="Opcional. Aquí le llegarán los avisos de las citas médicas del interno."
              >
                {(campo) => (
                  <Input
                    {...campo}
                    {...enlazar(`${FAMILIAR}correo`)}
                    type="email"
                    autoComplete="off"
                    maxLength={150}
                    placeholder="nombre@ejemplo.com"
                  />
                )}
              </FormField>
            </div>

            <div className="sm:col-span-2">
              <FormField
                etiqueta="Dirección"
                error={errores[`${FAMILIAR}direccion`]}
                ayuda="Opcional."
              >
                {(campo) => (
                  <Input
                    {...campo}
                    {...enlazar(`${FAMILIAR}direccion`)}
                    autoComplete="off"
                    maxLength={200}
                    placeholder="4.ª calle 3-21, zona 1, Mazatenango"
                  />
                )}
              </FormField>
            </div>
          </div>
        </fieldset>
      </div>

      {/* Pegado abajo y con wrap, como el de usuarios (YS-80). */}
      <div className="sticky bottom-0 flex flex-wrap justify-end gap-3 border-t border-divisor bg-superficie px-4 py-4 sm:px-6">
        <Button type="button" variante="secundario" onClick={alCerrar}>
          Cancelar
        </Button>
        <Button type="submit" cargando={guardando}>
          {esEdicion ? "Guardar cambios" : "Registrar interno"}
        </Button>
      </div>
    </form>
  );
}

/**
 * YS-60. Alta y edición de los datos personales del interno y de su familiar
 * responsable. La ficha médica no va aquí: se completa después desde el
 * perfil, con su propia ventana y su propio endpoint (hallazgo 7 de
 * docs/trazabilidad-requerimientos.md). Así ninguna de las dos operaciones
 * puede quedar a medias por culpa de la otra.
 *
 * `alGuardar` recibe el interno tal como lo devolvió el backend, con su id:
 * quien abre la ventana decide adónde llevar al usuario después.
 */
export function InternoFormModal({ abierto, interno, claveFormulario, alCerrar, alGuardar }) {
  const esEdicion = Boolean(interno);
  const { marcarCambios, olvidarCambios, intentarCerrar, dialogo } =
    useConfirmarDescarte(alCerrar);

  return (
    <>
      <Modal
        abierto={abierto}
        alCerrar={intentarCerrar}
        titulo={esEdicion ? "Editar datos del interno" : "Nuevo interno"}
        descripcion={
          esEdicion
            ? "Actualice los datos personales y los del familiar responsable."
            : "Registre los datos personales y el familiar responsable. La ficha médica se completa después, desde el perfil."
        }
      >
        <FormularioInterno
          key={claveFormulario}
          interno={interno}
          alCambiar={marcarCambios}
          alCerrar={intentarCerrar}
          alGuardar={(guardado) => {
            olvidarCambios();
            alGuardar(guardado);
          }}
        />
      </Modal>

      <ConfirmDialog {...dialogo} />
    </>
  );
}
