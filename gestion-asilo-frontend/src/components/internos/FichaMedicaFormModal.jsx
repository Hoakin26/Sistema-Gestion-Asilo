import { useRef, useState } from "react";
import { Plus, Trash2 } from "lucide-react";

import { useConfirmarDescarte } from "../../hooks/useConfirmarDescarte";
import { erroresPorCampo, mensajeDeError } from "../../services/api";
import { actualizarFichaMedica } from "../../services/internoService";
import { hoyISO } from "../../utils/fechas";
import { enfocarPrimerError, opcional } from "../../utils/formularios";
import { Alert } from "../ui/Alert";
import { Button, IconButton } from "../ui/Button";
import { ConfirmDialog } from "../ui/ConfirmDialog";
import { FormField } from "../ui/FormField";
import { Input, Select, Textarea } from "../ui/Input";
import { Modal } from "../ui/Modal";

const TIPOS_SANGRE = ["O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-"];

/*
 * Cada fila de las listas necesita una key estable: el índice no sirve, porque
 * al quitar una fila del medio React reutilizaría los campos de la siguiente y
 * lo escrito saltaría de una fila a otra. No se usa crypto.randomUUID porque
 * el navegador solo lo ofrece en HTTPS, y la tableta entra a la red local por
 * HTTP.
 */
let ultimaClave = 0;
function nuevaClave() {
  ultimaClave += 1;
  return ultimaClave;
}

function padecimientoVacio() {
  return { clave: nuevaClave(), nombre: "", fechaDiagnostico: "", observaciones: "" };
}

function medicamentoVacio() {
  return { clave: nuevaClave(), nombre: "", dosis: "", frecuencia: "" };
}

function valoresDe(ficha) {
  return {
    tipoSangre: ficha.tipoSangre ?? "",
    alergias: ficha.alergias ?? "",
    motivoIngreso: ficha.motivoIngreso ?? "",
    psicopatologias: ficha.psicopatologias.map((p) => ({
      clave: nuevaClave(),
      nombre: p.nombre,
      fechaDiagnostico: p.fechaDiagnostico ?? "",
      observaciones: p.observaciones ?? "",
    })),
    medicamentosHabituales: ficha.medicamentosHabituales.map((m) => ({
      clave: nuevaClave(),
      nombre: m.nombre,
      dosis: m.dosis,
      frecuencia: m.frecuencia,
    })),
  };
}

/**
 * Mismas reglas y mensajes que el backend (YS-58). Las claves de error de las
 * listas siguen su formato, "medicamentosHabituales[1].dosis": así un error
 * del servidor señala la fila correcta igual que uno detectado aquí.
 */
function validar(valores) {
  const errores = {};
  const hoy = hoyISO();

  if (valores.alergias.trim().length > 500) {
    errores.alergias = "Las alergias no pueden exceder 500 caracteres";
  }
  if (valores.motivoIngreso.trim().length > 500) {
    errores.motivoIngreso = "El motivo de ingreso no puede exceder 500 caracteres";
  }

  valores.psicopatologias.forEach((p, i) => {
    const base = `psicopatologias[${i}]`;
    if (!p.nombre.trim()) errores[`${base}.nombre`] = "El nombre del padecimiento es obligatorio";
    if (p.fechaDiagnostico && p.fechaDiagnostico > hoy) {
      errores[`${base}.fechaDiagnostico`] = "La fecha de diagnóstico no puede estar en el futuro";
    }
  });

  valores.medicamentosHabituales.forEach((m, i) => {
    const base = `medicamentosHabituales[${i}]`;
    if (!m.nombre.trim()) errores[`${base}.nombre`] = "El nombre del medicamento es obligatorio";
    if (!m.dosis.trim()) errores[`${base}.dosis`] = "La dosis es obligatoria";
    if (!m.frecuencia.trim()) errores[`${base}.frecuencia`] = "La frecuencia es obligatoria";
  });

  return errores;
}

/* Quita los errores de una lista: al borrar una fila los índices se corren. */
function sinErroresDe(errores, lista) {
  return Object.fromEntries(
    Object.entries(errores).filter(([clave]) => !clave.startsWith(`${lista}[`)),
  );
}

function FormularioFicha({ internoId, ficha, alCambiar, alCerrar, alGuardar }) {
  const refFormulario = useRef(null);
  const [valores, setValores] = useState(() => valoresDe(ficha));
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState(null);
  const [guardando, setGuardando] = useState(false);
  // La fila recién agregada recibe el foco: quien pulsó "Agregar" va a escribir ahí.
  const [claveNueva, setClaveNueva] = useState(null);

  const cambiar = (campo) => (evento) => {
    alCambiar();
    setValores((previos) => ({ ...previos, [campo]: evento.target.value }));
    setErrores((previos) => ({ ...previos, [campo]: undefined }));
  };

  const cambiarFila = (lista, indice, campo) => (evento) => {
    alCambiar();
    const valor = evento.target.value;
    setValores((previos) => ({
      ...previos,
      [lista]: previos[lista].map((fila, i) => (i === indice ? { ...fila, [campo]: valor } : fila)),
    }));
    setErrores((previos) => ({ ...previos, [`${lista}[${indice}].${campo}`]: undefined }));
  };

  const agregar = (lista, crear) => {
    alCambiar();
    const fila = crear();
    setClaveNueva(fila.clave);
    setValores((previos) => ({ ...previos, [lista]: [...previos[lista], fila] }));
  };

  const quitar = (lista, indice) => {
    alCambiar();
    setValores((previos) => ({
      ...previos,
      [lista]: previos[lista].filter((_, i) => i !== indice),
    }));
    setErrores((previos) => sinErroresDe(previos, lista));
  };

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

    try {
      const guardada = await actualizarFichaMedica(internoId, {
        tipoSangre: valores.tipoSangre || null,
        alergias: opcional(valores.alergias),
        motivoIngreso: opcional(valores.motivoIngreso),
        psicopatologias: valores.psicopatologias.map((p) => ({
          nombre: p.nombre.trim(),
          fechaDiagnostico: p.fechaDiagnostico || null,
          observaciones: opcional(p.observaciones),
        })),
        medicamentosHabituales: valores.medicamentosHabituales.map((m) => ({
          nombre: m.nombre.trim(),
          dosis: m.dosis.trim(),
          frecuencia: m.frecuencia.trim(),
        })),
      });
      alGuardar(guardada);
    } catch (error) {
      const porCampo = erroresPorCampo(error);
      if (porCampo) {
        fallar(porCampo);
      } else {
        setErrorGeneral(mensajeDeError(error, "No se pudo guardar la ficha médica"));
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
          <legend className="mb-4 font-semibold text-texto">Datos generales</legend>
          <div className="grid gap-6 sm:grid-cols-2">
            <FormField etiqueta="Tipo de sangre" error={errores.tipoSangre}>
              {(campo) => (
                <Select {...campo} value={valores.tipoSangre} onChange={cambiar("tipoSangre")}>
                  <option value="">No se conoce</option>
                  {TIPOS_SANGRE.map((tipo) => (
                    <option key={tipo} value={tipo}>
                      {tipo}
                    </option>
                  ))}
                </Select>
              )}
            </FormField>

            <div className="sm:col-span-2">
              <FormField
                etiqueta="Alergias"
                error={errores.alergias}
                ayuda="Medicamentos, alimentos o materiales. Déjelo vacío si no tiene."
              >
                {(campo) => (
                  <Textarea
                    {...campo}
                    value={valores.alergias}
                    onChange={cambiar("alergias")}
                    maxLength={500}
                    placeholder="Penicilina, mariscos…"
                  />
                )}
              </FormField>
            </div>

            <div className="sm:col-span-2">
              <FormField etiqueta="Motivo de ingreso" error={errores.motivoIngreso}>
                {(campo) => (
                  <Textarea
                    {...campo}
                    value={valores.motivoIngreso}
                    onChange={cambiar("motivoIngreso")}
                    maxLength={500}
                    placeholder="Por qué ingresó al asilo y en qué condición llegó"
                  />
                )}
              </FormField>
            </div>
          </div>
        </fieldset>

        <fieldset>
          <legend className="mb-4 font-semibold text-texto">Psicopatologías</legend>

          {valores.psicopatologias.length === 0 ? (
            <p className="text-texto-secundario">No hay padecimientos registrados.</p>
          ) : (
            <ul className="flex flex-col gap-4">
              {valores.psicopatologias.map((p, i) => (
                <li key={p.clave} className="rounded-tarjeta border border-divisor p-4">
                  <div className="flex items-center justify-between gap-4">
                    <p className="font-medium text-texto tabular-nums">Padecimiento {i + 1}</p>
                    <IconButton
                      type="button"
                      etiqueta={`Quitar el padecimiento ${i + 1}${p.nombre ? `, ${p.nombre}` : ""}`}
                      onClick={() => quitar("psicopatologias", i)}
                    >
                      <Trash2 aria-hidden="true" size={20} />
                    </IconButton>
                  </div>

                  <div className="mt-2 grid gap-4 sm:grid-cols-2">
                    <FormField
                      etiqueta="Nombre"
                      obligatorio
                      error={errores[`psicopatologias[${i}].nombre`]}
                    >
                      {(campo) => (
                        <Input
                          {...campo}
                          value={p.nombre}
                          onChange={cambiarFila("psicopatologias", i, "nombre")}
                          autoFocus={p.clave === claveNueva}
                          maxLength={150}
                          placeholder="Demencia tipo Alzheimer"
                        />
                      )}
                    </FormField>

                    <FormField
                      etiqueta="Fecha de diagnóstico"
                      error={errores[`psicopatologias[${i}].fechaDiagnostico`]}
                      ayuda="Opcional."
                    >
                      {(campo) => (
                        <Input
                          {...campo}
                          type="date"
                          value={p.fechaDiagnostico}
                          onChange={cambiarFila("psicopatologias", i, "fechaDiagnostico")}
                          min="1900-01-01"
                          max={hoy}
                          className="tabular-nums slashed-zero"
                        />
                      )}
                    </FormField>

                    <div className="sm:col-span-2">
                      <FormField
                        etiqueta="Observaciones"
                        error={errores[`psicopatologias[${i}].observaciones`]}
                      >
                        {(campo) => (
                          <Textarea
                            {...campo}
                            rows={2}
                            value={p.observaciones}
                            onChange={cambiarFila("psicopatologias", i, "observaciones")}
                            maxLength={500}
                          />
                        )}
                      </FormField>
                    </div>
                  </div>
                </li>
              ))}
            </ul>
          )}

          <Button
            type="button"
            variante="secundario"
            className="mt-4"
            onClick={() => agregar("psicopatologias", padecimientoVacio)}
          >
            <Plus aria-hidden="true" size={20} />
            Agregar padecimiento
          </Button>
        </fieldset>

        <fieldset>
          <legend className="mb-4 font-semibold text-texto">Medicamentos habituales</legend>

          {valores.medicamentosHabituales.length === 0 ? (
            <p className="text-texto-secundario">No toma medicamentos de forma permanente.</p>
          ) : (
            <ul className="flex flex-col gap-4">
              {valores.medicamentosHabituales.map((m, i) => (
                <li key={m.clave} className="rounded-tarjeta border border-divisor p-4">
                  <div className="flex items-center justify-between gap-4">
                    <p className="font-medium text-texto tabular-nums">Medicamento {i + 1}</p>
                    <IconButton
                      type="button"
                      etiqueta={`Quitar el medicamento ${i + 1}${m.nombre ? `, ${m.nombre}` : ""}`}
                      onClick={() => quitar("medicamentosHabituales", i)}
                    >
                      <Trash2 aria-hidden="true" size={20} />
                    </IconButton>
                  </div>

                  <div className="mt-2 grid gap-4 sm:grid-cols-2">
                    <div className="sm:col-span-2">
                      <FormField
                        etiqueta="Nombre"
                        obligatorio
                        error={errores[`medicamentosHabituales[${i}].nombre`]}
                      >
                        {(campo) => (
                          <Input
                            {...campo}
                            value={m.nombre}
                            onChange={cambiarFila("medicamentosHabituales", i, "nombre")}
                            autoFocus={m.clave === claveNueva}
                            maxLength={150}
                            placeholder="Donepezilo"
                          />
                        )}
                      </FormField>
                    </div>

                    <FormField
                      etiqueta="Dosis"
                      obligatorio
                      error={errores[`medicamentosHabituales[${i}].dosis`]}
                    >
                      {(campo) => (
                        <Input
                          {...campo}
                          value={m.dosis}
                          onChange={cambiarFila("medicamentosHabituales", i, "dosis")}
                          maxLength={100}
                          placeholder="10 mg"
                          className="tabular-nums slashed-zero"
                        />
                      )}
                    </FormField>

                    <FormField
                      etiqueta="Frecuencia"
                      obligatorio
                      error={errores[`medicamentosHabituales[${i}].frecuencia`]}
                    >
                      {(campo) => (
                        <Input
                          {...campo}
                          value={m.frecuencia}
                          onChange={cambiarFila("medicamentosHabituales", i, "frecuencia")}
                          maxLength={100}
                          placeholder="Cada 24 horas"
                        />
                      )}
                    </FormField>
                  </div>
                </li>
              ))}
            </ul>
          )}

          <Button
            type="button"
            variante="secundario"
            className="mt-4"
            onClick={() => agregar("medicamentosHabituales", medicamentoVacio)}
          >
            <Plus aria-hidden="true" size={20} />
            Agregar medicamento
          </Button>
        </fieldset>
      </div>

      <div className="sticky bottom-0 flex flex-wrap justify-end gap-3 border-t border-divisor bg-superficie px-4 py-4 sm:px-6">
        <Button type="button" variante="secundario" onClick={alCerrar}>
          Cancelar
        </Button>
        <Button type="submit" cargando={guardando}>
          Guardar ficha médica
        </Button>
      </div>
    </form>
  );
}

/**
 * YS-60. La ficha médica del interno: tipo de sangre, alergias, motivo de
 * ingreso, psicopatologías y medicamentos habituales (RF-01). Solo la abre el
 * Médico General, que es el único rol al que el backend le permite escribirla.
 *
 * Recibe la ficha ya cargada en vez de pedirla: el perfil la acaba de leer
 * para mostrarla, y pedirla otra vez solo agregaría una espera.
 */
export function FichaMedicaFormModal({
  abierto,
  internoId,
  ficha,
  claveFormulario,
  alCerrar,
  alGuardar,
}) {
  const { marcarCambios, olvidarCambios, intentarCerrar, dialogo } =
    useConfirmarDescarte(alCerrar);

  return (
    <>
      <Modal
        abierto={abierto}
        alCerrar={intentarCerrar}
        ancho="amplio"
        titulo="Ficha médica"
        descripcion={ficha ? `Datos clínicos de ${ficha.nombreInterno}.` : undefined}
      >
        {ficha && (
          <FormularioFicha
            key={claveFormulario}
            internoId={internoId}
            ficha={ficha}
            alCambiar={marcarCambios}
            alCerrar={intentarCerrar}
            alGuardar={(guardada) => {
              olvidarCambios();
              alGuardar(guardada);
            }}
          />
        )}
      </Modal>

      <ConfirmDialog {...dialogo} />
    </>
  );
}
