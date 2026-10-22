package app.cabezadealgodon.com.gestionasilobackend.controllers;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteAnalisisMedicoResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteCobrosResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteCostosResponse;
import app.cabezadealgodon.com.gestionasilobackend.security.MedicoGeneralOEspecialista;
import app.cabezadealgodon.com.gestionasilobackend.security.SoloAdministradorAsilo;
import app.cabezadealgodon.com.gestionasilobackend.security.UsuarioAutenticado;
import app.cabezadealgodon.com.gestionasilobackend.services.ReporteCobrosService;
import app.cabezadealgodon.com.gestionasilobackend.services.ReporteService;

/**
 * HU-24 a HU-28. Los reportes del sistema (RF-17 a RF-21b). Cada uno declara
 * su rol: los financieros son del Administrador del Asilo, y los clínicos
 * (HU-25, HU-28), de los médicos (RNF-03).
 */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;
    private final ReporteCobrosService cobrosService;

    public ReporteController(ReporteService reporteService, ReporteCobrosService cobrosService) {
        this.reporteService = reporteService;
        this.cobrosService = cobrosService;
    }

    /**
     * HU-24 (YS-151). Los costos de cada cita por paciente (RF-17), entre dos
     * fechas de la cita, las dos incluidas. Sin {@code internoId}, de todos los
     * pacientes. Sin fechas, del primer día del mes en curso a hoy.
     */
    @GetMapping("/costos-por-cita")
    @SoloAdministradorAsilo
    public ReporteCostosResponse costosPorCita(@RequestParam(required = false) Long internoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate hoy = LocalDate.now();
        return reporteService.costosPorCita(internoId, desde == null ? hoy.withDayOfMonth(1) : desde,
                hasta == null ? hoy : hasta);
    }

    /**
     * HU-25 (YS-154). El análisis médico de un paciente (RF-18): su ficha, el
     * motivo de reclusión, sus visitas con los diagnósticos y los
     * medicamentos recetados. El especialista, solo del paciente de su visita
     * activa (lo revisa el servicio).
     */
    @GetMapping("/analisis-medico")
    @MedicoGeneralOEspecialista
    public ReporteAnalisisMedicoResponse analisisMedico(@RequestParam Long internoId,
            @AuthenticationPrincipal UsuarioAutenticado medico) {
        return reporteService.analisisMedico(internoId, medico.getUsuario());
    }

    /**
     * HU-26 (YS-157). Los cobros a familiares (RF-19): los pagos recibidos
     * entre dos fechas, las dos incluidas, con las cuentas que cubren y sus
     * gastos médicos por tipo. Sin {@code internoId}, de todos los pacientes.
     * Sin fechas, del primer día del mes en curso a hoy.
     */
    @GetMapping("/cobros")
    @SoloAdministradorAsilo
    public ReporteCobrosResponse cobros(@RequestParam(required = false) Long internoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate hoy = LocalDate.now();
        return cobrosService.cobros(internoId, desde == null ? hoy.withDayOfMonth(1) : desde,
                hasta == null ? hoy : hasta);
    }
}
