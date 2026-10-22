import { useEffect, useState } from "react";
import { X } from "lucide-react";

import { useAuth } from "../../hooks/useAuth";
import { mensajeDeError } from "../../services/api";
import { listarInternos } from "../../services/internoService";
import { listarVisitas } from "../../services/visitaService";
import { formatearDpi } from "../../utils/formato";
import { Button } from "../ui/Button";
import { FormField } from "../ui/FormField";
import { Select } from "../ui/Input";
import { SearchAutocomplete } from "../ui/SearchAutocomplete";

/* El Médico General busca entre todos los internos, también los que ya egresaron. */
async function buscarInternos(termino) {
  const pagina = await listarInternos({ busqueda: termino });
  return pagina.content.slice(0, 8).map((interno) => ({
    clave: interno.id,
    titulo: interno.nombreCompleto,
    subtitulo: `DPI ${formatearDpi(interno.dpi)}${interno.activo ? "" : " · Egresado"}`,
    interno,
  }));
}

/**
 * YS-164. El paciente de un reporte clínico (HU-25 y HU-28). Salió del de
 * análisis médico para que los dos elijan igual. El Médico General lo busca
 * entre todos los internos; el especialista lo elige entre los de sus visitas
 * activas, los únicos de los que el backend le deja generar el reporte
 * (decidido el 21/10).
 *
 * `paciente` es `{ id, nombreCompleto }` o `null`; `alElegir` recibe el nuevo.
 */
export function PacienteMedico({ paciente, alElegir, error }) {
  const { usuario } = useAuth();
  const esEspecialista = usuario?.rol === "MEDICO_ESPECIALISTA";
  // El especialista: los pacientes de sus visitas activas.
  const [enAtencion, setEnAtencion] = useState(null);
  const [errorCarga, setErrorCarga] = useState(null);

  useEffect(() => {
    if (!esEspecialista) return undefined;
    let vigente = true;
    listarVisitas("ACTIVA")
      .then((visitas) => {
        if (!vigente) return;
        const unicos = new Map();
        visitas.forEach((visita) => unicos.set(visita.paciente.id, visita.paciente));
        setEnAtencion([...unicos.values()]);
      })
      .catch((fallo) => {
        if (!vigente) return;
        setEnAtencion([]);
        setErrorCarga(mensajeDeError(fallo, "No se pudieron cargar sus visitas en curso"));
      });
    return () => {
      vigente = false;
    };
  }, [esEspecialista]);

  const sinVisitas = esEspecialista && enAtencion?.length === 0 && !errorCarga;

  return (
    // El ancho del campo, como en los demás reportes; la ayuda se ajusta a él (YS-182).
    <div className="w-full sm:w-80">
      <FormField
        etiqueta="Paciente"
        obligatorio
        error={error ?? errorCarga}
        ayuda={
          sinVisitas
            ? "No tiene visitas en curso: el reporte se genera del paciente que está atendiendo, mientras su visita siga activa."
            : undefined
        }
      >
        {(campo) =>
          esEspecialista ? (
            <Select
              {...campo}
              value={paciente?.id ?? ""}
              onChange={(evento) =>
                alElegir(enAtencion?.find((elegido) => String(elegido.id) === evento.target.value) ?? null)
              }
              disabled={!enAtencion || enAtencion.length === 0}
            >
              <option value="">{enAtencion === null ? "Cargando sus visitas…" : "Elija…"}</option>
              {enAtencion?.map((enVisita) => (
                <option key={enVisita.id} value={enVisita.id}>
                  {enVisita.nombreCompleto}
                </option>
              ))}
            </Select>
          ) : paciente ? (
            <div
              id={campo.id}
              className="flex min-h-11 items-center justify-between gap-4 rounded-campo border border-texto-secundario bg-fondo px-4 py-2"
            >
              <p className="font-medium text-texto">{paciente.nombreCompleto}</p>
              <Button
                type="button"
                variante="texto"
                aria-label={`Cambiar el paciente elegido, ${paciente.nombreCompleto}`}
                onClick={() => alElegir(null)}
              >
                <X aria-hidden="true" size={20} />
                Cambiar
              </Button>
            </div>
          ) : (
            <SearchAutocomplete
              {...campo}
              flotante
              buscar={buscarInternos}
              alElegir={(opcion) => alElegir(opcion.interno)}
              placeholder="Escriba el nombre o el DPI…"
              etiquetaLista="Internos que coinciden"
            />
          )
        }
      </FormField>
    </div>
  );
}
