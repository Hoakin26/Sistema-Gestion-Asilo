package app.cabezadealgodon.com.gestionasilobackend.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import app.cabezadealgodon.com.gestionasilobackend.models.Cita;
import app.cabezadealgodon.com.gestionasilobackend.models.HorarioAtencion;
import app.cabezadealgodon.com.gestionasilobackend.models.MedicoEspecialista;

/**
 * YS-167 y YS-91. Las cuentas de la agenda de un especialista: en qué horas
 * se puede dar una cita y cuáles ya están tomadas.
 *
 * <p>
 * Vive aparte porque la usan dos servicios con la misma regla: el que muestra
 * las horas libres y el que asigna la cita. Si cada uno hiciera su cuenta, la
 * pantalla podría ofrecer una hora que la asignación después rechaza.
 */
final class Agenda {

    private Agenda() {
    }

    /**
     * Las horas en que empieza una cita según el horario del día: desde la
     * hora de inicio, de una duración en una duración, mientras la cita
     * completa termine antes del fin. De 8:00 a 10:00 con citas de 30 minutos:
     * 8:00, 8:30, 9:00 y 9:30.
     */
    static List<LocalTime> franjas(MedicoEspecialista especialista, LocalDate fecha) {
        List<LocalTime> franjas = new ArrayList<>();
        HorarioAtencion horario = especialista.horarioDe(fecha.getDayOfWeek()).orElse(null);
        if (horario == null) return franjas;

        int duracion = especialista.getDuracionCitaMinutos();
        LocalTime inicio = horario.getHoraInicio();
        while (true) {
            LocalTime fin = inicio.plusMinutes(duracion);
            // LocalTime da la vuelta a medianoche: si el fin "retrocede", ya
            // se pasó del día y no hay más franjas.
            if (fin.isBefore(inicio) || fin.isAfter(horario.getHoraFin())) break;
            franjas.add(inicio);
            inicio = fin;
        }
        return franjas;
    }

    /** Si una cita en ese momento se cruzaría con alguna de las ya dadas. */
    static boolean ocupada(LocalDateTime inicio, int duracionMinutos, List<Cita> citas) {
        LocalDateTime fin = inicio.plusMinutes(duracionMinutos);
        return citas.stream()
                .anyMatch(cita -> cita.getFechaHora().isBefore(fin) && cita.fechaHoraFin().isAfter(inicio));
    }
}
