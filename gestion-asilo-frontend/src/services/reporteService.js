import { api } from "./api";

/**
 * HU-24 (YS-152). El reporte de costos por paciente y cita entre dos fechas
 * ("AAAA-MM-DD"); sin `internoId`, de todos los pacientes.
 */
export async function obtenerReporteCostos({ internoId, desde, hasta }) {
  const { data } = await api.get("/reportes/costos-por-cita", {
    params: { internoId: internoId || undefined, desde, hasta },
  });
  return data;
}

/**
 * HU-26 (YS-158). El reporte de cobros: los pagos recibidos entre dos fechas
 * ("AAAA-MM-DD"), con las cuentas que cubren; sin `internoId`, de todos.
 */
export async function obtenerReporteCobros({ internoId, desde, hasta }, signal) {
  const { data } = await api.get("/reportes/cobros", {
    params: { internoId: internoId || undefined, desde, hasta },
    signal,
  });
  return data;
}

/** HU-27 (YS-161). Los pagos a la fundación (RF-20a) entre dos fechas ("AAAA-MM-DD"). */
export async function obtenerReportePagosFundacion({ desde, hasta }) {
  const { data } = await api.get("/reportes/pagos-fundacion", { params: { desde, hasta } });
  return data;
}

/** HU-27 (YS-161). Las entradas (RF-20b) entre dos fechas: las donaciones y los cobros a familiares. */
export async function obtenerReporteEntradas({ desde, hasta }) {
  const { data } = await api.get("/reportes/entradas", { params: { desde, hasta } });
  return data;
}

/**
 * HU-28 (YS-164). Los exámenes realizados a un paciente (RF-21a) entre dos
 * fechas del resultado ("AAAA-MM-DD"). Los permisos del análisis médico.
 */
export async function obtenerReporteExamenes({ internoId, desde, hasta }) {
  const { data } = await api.get("/reportes/examenes", { params: { internoId, desde, hasta } });
  return data;
}

/** HU-28 (YS-164). Los medicamentos entregados a un paciente (RF-21b) entre dos fechas de entrega. */
export async function obtenerReporteMedicamentos({ internoId, desde, hasta }) {
  const { data } = await api.get("/reportes/medicamentos", { params: { internoId, desde, hasta } });
  return data;
}

/**
 * HU-25 (YS-155). El reporte de análisis médico de un paciente: sus datos, su
 * ficha y su historial. El especialista, solo del paciente de su visita activa.
 */
export async function obtenerReporteAnalisisMedico(internoId) {
  const { data } = await api.get("/reportes/analisis-medico", { params: { internoId } });
  return data;
}
