import { useState } from "react";
import { Search } from "lucide-react";

import { Button } from "../ui/Button";
import { FormField } from "../ui/FormField";
import { Input, Select } from "../ui/Input";

/**
 * YS-149. Los filtros de un histórico financiero: un tipo (o todos) y un
 * rango de fechas. Salió de "Donaciones" (HU-22) para servir también a
 * "Gastos" (HU-23). Consulta al pulsar "Buscar", no en cada cambio:
 * `alBuscar({ tipo, desde, hasta })` recibe los filtros ya validados.
 *
 * `opciones` es `{ CLAVE: "Etiqueta" }`, en el orden en que se muestran.
 * `errores` son los que devolvió el backend, por si acaso.
 */
export function FiltrosHistorico({ etiquetaTipo, etiquetaTodos, opciones, cargando, errores = {}, alBuscar }) {
  const [tipo, setTipo] = useState("");
  const [desde, setDesde] = useState("");
  const [hasta, setHasta] = useState("");
  const [locales, setLocales] = useState({});

  const buscar = (evento) => {
    evento.preventDefault();
    const encontrados = {};
    if (desde && hasta && hasta < desde) encontrados.hasta = "La fecha final no puede ser anterior a la inicial";
    setLocales(encontrados);
    if (Object.keys(encontrados).length > 0) return;
    alBuscar({ tipo, desde, hasta });
  };

  const error = (campo) => locales[campo] ?? errores[campo];

  return (
    <form onSubmit={buscar} noValidate className="mt-6 flex flex-wrap items-start gap-4 sm:[&>button]:mt-8">
      <FormField etiqueta={etiquetaTipo} error={error("tipo")}>
        {(campo) => (
          <Select {...campo} value={tipo} onChange={(evento) => setTipo(evento.target.value)} className="sm:w-64">
            <option value="">{etiquetaTodos}</option>
            {Object.entries(opciones).map(([clave, etiqueta]) => (
              <option key={clave} value={clave}>
                {etiqueta}
              </option>
            ))}
          </Select>
        )}
      </FormField>
      <FormField etiqueta="Desde" error={error("desde")}>
        {(campo) => (
          <Input
            {...campo}
            type="date"
            value={desde}
            onChange={(evento) => setDesde(evento.target.value)}
            className="sm:w-48"
          />
        )}
      </FormField>
      <FormField etiqueta="Hasta" error={error("hasta")}>
        {(campo) => (
          <Input
            {...campo}
            type="date"
            value={hasta}
            onChange={(evento) => setHasta(evento.target.value)}
            className="sm:w-48"
          />
        )}
      </FormField>
      <Button type="submit" variante="secundario" cargando={cargando}>
        {!cargando && <Search aria-hidden="true" size={20} />}
        Buscar
      </Button>
    </form>
  );
}
