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
 * HU-25 (YS-155). El reporte de análisis médico de un paciente: sus datos, su
 * ficha y su historial. El especialista, solo del paciente de su visita activa.
 */
export async function obtenerReporteAnalisisMedico(internoId) {
  const { data } = await api.get("/reportes/analisis-medico", { params: { internoId } });
  return data;
}
