import { useId, useRef, useState } from "react";
import { CircleCheck, Clock, Pencil, Pill, Plus, Trash2 } from "lucide-react";

import { useToast } from "../../hooks/useToast";
import { erroresPorCampo, mensajeDeError } from "../../services/api";
import { corregirReceta, registrarReceta } from "../../services/visitaService";
import { formatearFechaHora } from "../../utils/fechas";
import { enfocarPrimerError } from "../../utils/formularios";
import { Alert } from "../ui/Alert";
import { Button, IconButton } from "../ui/Button";
import { FormField } from "../ui/FormField";
import { Input, Textarea } from "../ui/Input";
import { StatusBadge } from "../ui/StatusBadge";
import { Barra } from "../ui/TablaSkeleton";

const TARJETA = "rounded-tarjeta border border-divisor bg-superficie p-4 shadow-tarjeta sm:p-6";

/*
 * Cada fila necesita una key estable, como en la ficha médica: con el índice,
 * al quitar una fila del medio lo escrito saltaría a la siguiente.
 */
let ultimaClave = 0;
function nuevaClave() {
  ultimaClave += 1;
  return ultimaClave;
}

/*
 * Los textos de ejemplo de los campos llevan "Ej.:". Sin eso, una fila recién
 * agregada, vacía, se leía igual que una ya llena: "Losartán · 50 mg · 30" en
 * gris pasaba por un medicamento escrito.
 */
function filaVacia() {
  return { clave: nuevaClave(), medicamento: "", dosis: "", cantidad: "", formaAplicacion: "" };
}

function valoresDe(receta) {
  return {
    indicacionesGenerales: receta?.indicacionesGenerales ?? "",
    medicamentos: receta
      ? receta.medicamentos.map((m) => ({
          clave: nuevaClave(),
          medicamento: m.medicamento,
          dosis: m.dosis,
          cantidad: String(m.cantidad),
          formaAplicacion: m.formaAplicacion,
        }))
      : [filaVacia()],
  };
}

/** Mismas reglas y mensajes que el backend (MedicamentoRecetaRequest), por fila. */
function validar({ indicacionesGenerales, medicamentos }) {
  const errores = {};
  if (indicacionesGenerales.trim().length > 1000) {
    errores.indicacionesGenerales = "Las indicaciones no pueden exceder 1000 caracteres";
  }
  if (medicamentos.length === 0) errores.medicamentos = "Agregue al menos un medicamento";
  medicamentos.forEach((m, i) => {
    const base = `medicamentos[${i}]`;
    if (!m.medicamento.trim()) errores[`${base}.medicamento`] = "El nombre del medicamento es obligatorio";
    if (!m.dosis.trim()) errores[`${base}.dosis`] = "La dosis es obligatoria";
    const cantidad = Number(m.cantidad);
    if (!m.cantidad.trim()) errores[`${base}.cantidad`] = "La cantidad es obligatoria";
    else if (!Number.isInteger(cantidad) || cantidad < 1) {
      errores[`${base}.cantidad`] = "La cantidad tiene que ser al menos 1";
    } else if (cantidad > 999) errores[`${base}.cantidad`] = "La cantidad no puede pasar de 999";
    if (!m.formaAplicacion.trim()) {
      errores[`${base}.formaAplicacion`] = "El tiempo o la forma de aplicación es obligatorio";
    }
  });
  return errores;
}

/**
 * Los medicamentos de una receta, para leer: en la visita y en los antecedentes.
 * HU-16 (YS-127): con `conEntrega`, la farmacia ve además si cada uno ya se
 * entregó y cuándo, o si sigue pendiente.
 */
export function ListaMedicamentos({ medicamentos, conEntrega = false }) {
  return (
    <ul className="divide-y divide-divisor">
      {medicamentos.map((m) => (
        <li key={m.id} className="py-3">
          <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
            <span className="font-medium text-texto">{m.medicamento}</span>
            <span className="text-texto-secundario tabular-nums slashed-zero">
              {m.dosis} · {m.cantidad} {m.cantidad === 1 ? "unidad" : "unidades"}
            </span>
          </div>
          <p className="mt-1 text-texto">{m.formaAplicacion}</p>
          {conEntrega &&
            (m.entregado ? (
              <p className="mt-1 flex items-center gap-2 text-auxiliar text-exito">
                <CircleCheck aria-hidden="true" size={16} />
                <span>
                  Entregado el{" "}
                  <span className="tabular-nums slashed-zero">{formatearFechaHora(m.fechaEntrega)}</span>
                </span>
              </p>
            ) : (
              <p className="mt-1 flex items-center gap-2 text-auxiliar text-advertencia">
                <Clock aria-hidden="true" size={16} />
                Pendiente de entregar
              </p>
            ))}
        </li>
      ))}
    </ul>
  );
}

function FormularioReceta({ visita, receta, alCancelar, alGuardar }) {
  const { mostrar } = useToast();
  const refFormulario = useRef(null);
  const [valores, setValores] = useState(() => valoresDe(receta));
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState(null);
  const [guardando, setGuardando] = useState(false);
  // La fila recién agregada recibe el foco: quien pulsó "Agregar" va a escribir ahí.
  const [claveNueva, setClaveNueva] = useState(null);

  const cambiarFila = (indice, campo) => (evento) => {
    const valor = evento.target.value;
    setValores((previos) => ({
      ...previos,
      medicamentos: previos.medicamentos.map((fila, i) => (i === indice ? { ...fila, [campo]: valor } : fila)),
    }));
    setErrores((previos) => ({ ...previos, [`medicamentos[${indice}].${campo}`]: undefined }));
  };

  const agregar = () => {
    const fila = filaVacia();
    setClaveNueva(fila.clave);
    setValores((previos) => ({ ...previos, medicamentos: [...previos.medicamentos, fila] }));
    setErrores((previos) => ({ ...previos, medicamentos: undefined }));
  };

  // Al quitar una fila, las de abajo cambian de índice: un error guardado
  // como "medicamentos[2]" quedaría señalando otra fila. Se limpian todos.
  const quitar = (indice) => {
    setValores((previos) => ({
      ...previos,
      medicamentos: previos.medicamentos.filter((_, i) => i !== indice),
    }));
    setErrores((previos) =>
      Object.fromEntries(Object.entries(previos).filter(([clave]) => !clave.startsWith("medicamentos["))),
    );
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
    const datos = {
      indicacionesGenerales: valores.indicacionesGenerales.trim() || null,
      medicamentos: valores.medicamentos.map((m) => ({
        medicamento: m.medicamento.trim(),
        dosis: m.dosis.trim(),
        cantidad: Number(m.cantidad),
        formaAplicacion: m.formaAplicacion.trim(),
      })),
    };
    try {
      const guardada = receta
        ? await corregirReceta(visita.id, datos)
        : await registrarReceta(visita.id, datos);
      mostrar(receta ? "Se guardaron los cambios de la receta." : "La receta quedó registrada para la farmacia.");
      alGuardar(guardada);
    } catch (fallo) {
      const porCampo = erroresPorCampo(fallo);
      if (porCampo) fallar(porCampo);
      else setErrorGeneral(mensajeDeError(fallo, "No se pudo guardar la receta"));
    } finally {
      setGuardando(false);
    }
  };

  return (
    <form ref={refFormulario} onSubmit={enviar} noValidate className="mt-6 flex flex-col gap-6">
      {errorGeneral && <Alert>{errorGeneral}</Alert>}

      <fieldset>
        <legend className="mb-4 font-semibold text-texto">Medicamentos</legend>

        {valores.medicamentos.length === 0 ? (
          <p className="text-texto-secundario">Agregue al menos un medicamento.</p>
        ) : (
          <ul className="flex flex-col gap-4">
            {valores.medicamentos.map((m, i) => {
              const base = `medicamentos[${i}]`;
              return (
                <li key={m.clave} className="rounded-tarjeta border border-divisor p-4">
                  <div className="flex items-center justify-between gap-4">
                    <p className="font-medium text-texto tabular-nums">Medicamento {i + 1}</p>
                    <IconButton
                      type="button"
                      etiqueta={`Quitar el medicamento ${i + 1}${m.medicamento ? `, ${m.medicamento}` : ""}`}
                      onClick={() => quitar(i)}
                    >
                      <Trash2 aria-hidden="true" size={20} />
                    </IconButton>
                  </div>

                  <div className="mt-2 grid gap-4 sm:grid-cols-2">
                    <div className="sm:col-span-2">
                      <FormField etiqueta="Medicamento" obligatorio error={errores[`${base}.medicamento`]}>
                        {(campo) => (
                          <Input
                            {...campo}
                            value={m.medicamento}
                            onChange={cambiarFila(i, "medicamento")}
                            autoFocus={m.clave === claveNueva}
                            maxLength={150}
                            placeholder="Ej.: Losartán"
                          />
                        )}
                      </FormField>
                    </div>

                    <FormField
                      etiqueta="Dosis"
                      obligatorio
                      error={errores[`${base}.dosis`]}
                      ayuda="Lo que se toma cada vez."
                    >
                      {(campo) => (
                        <Input
                          {...campo}
                          value={m.dosis}
                          onChange={cambiarFila(i, "dosis")}
                          maxLength={100}
                          placeholder="Ej.: 50 mg"
                          className="tabular-nums slashed-zero"
                        />
                      )}
                    </FormField>

                    <FormField
                      etiqueta="Cantidad"
                      obligatorio
                      error={errores[`${base}.cantidad`]}
                      ayuda="Unidades que entrega la farmacia."
                    >
                      {(campo) => (
                        <Input
                          {...campo}
                          type="number"
                          inputMode="numeric"
                          min={1}
                          max={999}
                          step={1}
                          value={m.cantidad}
                          onChange={cambiarFila(i, "cantidad")}
                          placeholder="Ej.: 30"
                          className="tabular-nums slashed-zero"
                        />
                      )}
                    </FormField>

                    <div className="sm:col-span-2">
                      <FormField
                        etiqueta="Tiempo o forma de aplicación"
                        obligatorio
                        error={errores[`${base}.formaAplicacion`]}
                      >
                        {(campo) => (
                          <Input
                            {...campo}
                            value={m.formaAplicacion}
                            onChange={cambiarFila(i, "formaAplicacion")}
                            maxLength={200}
                            placeholder="Ej.: cada 24 horas por 30 días, vía oral"
                          />
                        )}
                      </FormField>
                    </div>
                  </div>
                </li>
              );
            })}
          </ul>
        )}

        {errores.medicamentos && (
          <div className="mt-4">
            <Alert>{errores.medicamentos}</Alert>
          </div>
        )}

        <Button type="button" variante="secundario" className="mt-4" onClick={agregar}>
          <Plus aria-hidden="true" size={20} />
          Agregar medicamento
        </Button>
      </fieldset>

      <FormField etiqueta="Indicaciones generales" error={errores.indicacionesGenerales}>
        {(campo) => (
          <Textarea
            {...campo}
            rows={2}
            maxLength={1000}
            value={valores.indicacionesGenerales}
            onChange={(evento) => {
              setValores((previos) => ({ ...previos, indicacionesGenerales: evento.target.value }));
              setErrores((previos) => ({ ...previos, indicacionesGenerales: undefined }));
            }}
            placeholder="Lo que vale para toda la receta: tomar con alimentos, suspender si…"
          />
        )}
      </FormField>

      <div className="flex flex-wrap justify-end gap-3">
        <Button type="button" variante="secundario" onClick={alCancelar} disabled={guardando}>
          Cancelar
        </Button>
        <Button type="submit" cargando={guardando}>
          {receta ? "Guardar cambios" : "Registrar receta"}
        </Button>
      </div>
    </form>
  );
}

/**
 * HU-10 (YS-109). La receta de la visita: los medicamentos con su dosis,
 * cantidad y tiempo o forma de aplicación (RF-10).
 *
 * Una por visita. Se escribe con "Agregar receta"; ya registrada, se lee y
 * se puede corregir mientras la visita siga activa y la farmacia no haya
 * entregado nada. Al registrarla, la farmacia ya la ve pendiente de despacho.
 *
 * - `receta`: `undefined` mientras carga, `null` si la visita no tiene.
 */
export function RecetaVisita({ visita, receta, error, alGuardar }) {
  const idTitulo = useId();
  const [editando, setEditando] = useState(false);

  const editable =
    visita.estado === "ACTIVA" && (receta === null || receta?.estado === "PENDIENTE_DESPACHO");

  let contenido;
  if (error) {
    contenido = (
      <div className="mt-4">
        <Alert>{error}</Alert>
      </div>
    );
  } else if (receta === undefined) {
    contenido = (
      <div role="status" className="mt-4 flex flex-col gap-2">
        <span className="sr-only">Cargando la receta…</span>
        <Barra className="w-48" />
        <Barra className="w-64" />
      </div>
    );
  } else if (editando && editable) {
    contenido = (
      <FormularioReceta
        visita={visita}
        receta={receta}
        alCancelar={() => setEditando(false)}
        alGuardar={(guardada) => {
          setEditando(false);
          alGuardar(guardada);
        }}
      />
    );
  } else if (receta === null) {
    contenido = (
      <div className="mt-4 flex flex-col items-start gap-4">
        <p className="text-texto-secundario">
          {editable ? "Esta visita todavía no tiene receta." : "En esta visita no se emitió receta."}
        </p>
        {editable && (
          <Button variante="secundario" onClick={() => setEditando(true)}>
            <Plus aria-hidden="true" size={20} />
            Agregar receta
          </Button>
        )}
      </div>
    );
  } else {
    contenido = (
      <div className="mt-4 flex flex-col gap-4">
        <div className="flex flex-wrap items-center gap-3">
          <StatusBadge estado={receta.estado} />
          <span className="text-auxiliar text-texto-secundario tabular-nums slashed-zero">
            Emitida el {formatearFechaHora(receta.fechaEmision)}
          </span>
        </div>
        <ListaMedicamentos medicamentos={receta.medicamentos} />
        {receta.indicacionesGenerales && (
          <div>
            <h3 className="text-auxiliar text-texto-secundario">Indicaciones generales</h3>
            <p className="mt-1 whitespace-pre-line text-texto">{receta.indicacionesGenerales}</p>
          </div>
        )}
        {editable && (
          <div>
            <Button variante="secundario" onClick={() => setEditando(true)}>
              <Pencil aria-hidden="true" size={20} />
              Corregir receta
            </Button>
          </div>
        )}
      </div>
    );
  }

  return (
    <section aria-labelledby={idTitulo} className={TARJETA}>
      <div className="flex items-center gap-3">
        <Pill aria-hidden="true" size={20} className="text-texto-secundario" />
        <h2 id={idTitulo} className="text-h3 font-semibold">
          Receta
        </h2>
      </div>
      {contenido}
    </section>
  );
}
