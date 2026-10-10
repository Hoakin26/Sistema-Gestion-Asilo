import { useId } from "react";
import { ClipboardList, Pencil, TriangleAlert } from "lucide-react";

import { formatearFecha } from "../../utils/fechas";
import { Alert } from "../ui/Alert";
import { Button } from "../ui/Button";

const TARJETA = "rounded-tarjeta border border-divisor bg-superficie p-4 shadow-tarjeta sm:p-6";

/** Un dato de la ficha. Lo que falta se dice con palabras, no con un hueco. */
function Dato({ etiqueta, children, vacio = "No registrado", ancho = false }) {
  // `false` también es vacío: es lo que deja un `condición && <span>` sin dato.
  const tieneValor = children != null && children !== "" && children !== false;
  return (
    <div className={ancho ? "sm:col-span-2" : undefined}>
      <dt className="text-auxiliar text-texto-secundario">{etiqueta}</dt>
      <dd className={`mt-1 ${tieneValor ? "text-texto" : "text-texto-secundario"}`}>
        {tieneValor ? children : vacio}
      </dd>
    </div>
  );
}

function fichaVacia(ficha) {
  return (
    !ficha.tipoSangre &&
    !ficha.alergias &&
    !ficha.motivoIngreso &&
    ficha.psicopatologias.length === 0 &&
    ficha.medicamentosHabituales.length === 0
  );
}

/**
 * La ficha médica en modo lectura. Solo se monta para el personal clínico: al
 * Administrador del Asilo no se le oculta, simplemente no existe para él
 * (RNF-03), igual que los módulos que no son de su rol (YS-81).
 *
 * YS-103. Salió del perfil del interno para usarse también en la visita
 * médica: el especialista ve los padecimientos junto a los antecedentes sin
 * salir de la consulta. Recibe la ficha ya cargada, o el error.
 */
export function FichaMedicaDetalle({ ficha, error, puedeEditar = false, alEditar }) {
  const idTitulo = useId();

  return (
    <section aria-labelledby={idTitulo} className={TARJETA}>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex items-center gap-3">
          <ClipboardList aria-hidden="true" size={20} className="text-texto-secundario" />
          <h2 id={idTitulo} className="text-h3 font-semibold">
            Ficha médica
          </h2>
        </div>
        {puedeEditar && ficha && !fichaVacia(ficha) && (
          <Button variante="secundario" onClick={alEditar}>
            <Pencil aria-hidden="true" size={20} />
            Editar ficha médica
          </Button>
        )}
      </div>

      {error ? (
        <div className="mt-4">
          <Alert>{error}</Alert>
        </div>
      ) : fichaVacia(ficha) ? (
        <div className="mt-4 flex flex-col items-center gap-4 rounded-tarjeta bg-fondo px-4 py-8 text-center">
          <p className="text-texto-secundario">
            {puedeEditar
              ? "La ficha médica todavía está vacía."
              : "El Médico General todavía no ha completado la ficha médica."}
          </p>
          {puedeEditar && <Button onClick={alEditar}>Completar ficha médica</Button>}
        </div>
      ) : (
        <div className="mt-6 flex flex-col gap-6">
          {/* Las alergias van primero y resaltadas: es lo que enfermería
              tiene que ver antes de administrar cualquier cosa. */}
          {ficha.alergias ? (
            <div className="flex items-start gap-2 rounded-tarjeta bg-advertencia-tenue px-4 py-3 text-advertencia">
              <TriangleAlert aria-hidden="true" size={20} className="shrink-0" />
              <p>
                <span className="font-semibold">Alergias: </span>
                {ficha.alergias}
              </p>
            </div>
          ) : (
            <p className="text-texto-secundario">Sin alergias registradas.</p>
          )}

          <dl className="grid gap-4 sm:grid-cols-2">
            <Dato etiqueta="Tipo de sangre" vacio="No se conoce">
              {ficha.tipoSangre}
            </Dato>
            <Dato etiqueta="Motivo de ingreso" ancho>
              {ficha.motivoIngreso}
            </Dato>
          </dl>

          <div>
            <h3 className="font-semibold text-texto">Psicopatologías</h3>
            {ficha.psicopatologias.length === 0 ? (
              <p className="mt-2 text-texto-secundario">No hay padecimientos registrados.</p>
            ) : (
              <ul className="mt-2 divide-y divide-divisor">
                {ficha.psicopatologias.map((p) => (
                  <li key={p.id} className="py-3">
                    <p className="font-medium text-texto">{p.nombre}</p>
                    {p.fechaDiagnostico && (
                      <p className="text-auxiliar text-texto-secundario tabular-nums slashed-zero">
                        Diagnosticado el {formatearFecha(p.fechaDiagnostico)}
                      </p>
                    )}
                    {p.observaciones && <p className="mt-1 text-texto">{p.observaciones}</p>}
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div>
            <h3 className="font-semibold text-texto">Medicamentos habituales</h3>
            {ficha.medicamentosHabituales.length === 0 ? (
              <p className="mt-2 text-texto-secundario">
                No toma medicamentos de forma permanente.
              </p>
            ) : (
              <ul className="mt-2 divide-y divide-divisor">
                {ficha.medicamentosHabituales.map((m) => (
                  <li key={m.id} className="flex flex-wrap justify-between gap-x-4 gap-y-1 py-3">
                    <span className="font-medium text-texto">{m.nombre}</span>
                    <span className="text-texto-secundario tabular-nums slashed-zero">
                      {m.dosis} · {m.frecuencia}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}
    </section>
  );
}
