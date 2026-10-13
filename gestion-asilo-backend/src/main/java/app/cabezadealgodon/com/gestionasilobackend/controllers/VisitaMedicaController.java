package app.cabezadealgodon.com.gestionasilobackend.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import app.cabezadealgodon.com.gestionasilobackend.dto.CitaAgendaResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.DiagnosticoRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.HistorialMedicoResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.VisitaMedicaResponse;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoVisita;
import app.cabezadealgodon.com.gestionasilobackend.security.SoloMedicoEspecialista;
import app.cabezadealgodon.com.gestionasilobackend.security.UsuarioAutenticado;
import app.cabezadealgodon.com.gestionasilobackend.services.VisitaMedicaService;
import jakarta.validation.Valid;

/**
 * HU-07 (YS-99). La agenda del especialista y sus visitas médicas.
 *
 * <p>
 * Todo el controlador es del Médico Especialista: la visita es un dato
 * clínico y el Administrador del Asilo no la lee (RNF-03). Que la cita o la
 * visita sea de quien la pide lo comprueba el servicio.
 */
@RestController
@RequestMapping("/api")
@SoloMedicoEspecialista
public class VisitaMedicaController {

    private final VisitaMedicaService visitaService;

    public VisitaMedicaController(VisitaMedicaService visitaService) {
        this.visitaService = visitaService;
    }

    /** Sin fecha, la de hoy: es lo que el especialista abre al llegar. */
    @GetMapping("/citas/mi-agenda")
    public List<CitaAgendaResponse> agenda(@AuthenticationPrincipal UsuarioAutenticado medico,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return visitaService.agenda(medico.getUsuario(), fecha == null ? LocalDate.now() : fecha);
    }

    @PostMapping("/citas/{citaId}/visita")
    @ResponseStatus(HttpStatus.CREATED)
    public VisitaMedicaResponse iniciar(@PathVariable Long citaId,
            @AuthenticationPrincipal UsuarioAutenticado medico) {
        return visitaService.iniciar(citaId, medico.getUsuario());
    }

    @GetMapping("/visitas")
    public List<VisitaMedicaResponse> listar(@AuthenticationPrincipal UsuarioAutenticado medico,
            @RequestParam(required = false) EstadoVisita estado) {
        return visitaService.listar(medico.getUsuario(), estado);
    }

    @GetMapping("/visitas/{id}")
    public VisitaMedicaResponse detalle(@PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado medico) {
        return visitaService.detalle(id, medico.getUsuario());
    }

    /** HU-09 (YS-105). El diagnóstico es obligatorio; las observaciones, no. */
    @PutMapping("/visitas/{id}/diagnostico")
    public VisitaMedicaResponse registrarDiagnostico(@PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado medico,
            @Valid @RequestBody DiagnosticoRequest request) {
        return visitaService.registrarDiagnostico(id, request, medico.getUsuario());
    }

    /** HU-09 (YS-105). La solicitud pasa a Atendida y se registra el cargo de la consulta. */
    @PatchMapping("/visitas/{id}/finalizar")
    public VisitaMedicaResponse finalizar(@PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado medico) {
        return visitaService.finalizar(id, medico.getUsuario());
    }

    /** HU-08 (YS-102). Los antecedentes del paciente, solo durante la visita activa. */
    @GetMapping("/visitas/{id}/historial")
    public HistorialMedicoResponse historial(@PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado medico) {
        return visitaService.historial(id, medico.getUsuario());
    }
}
