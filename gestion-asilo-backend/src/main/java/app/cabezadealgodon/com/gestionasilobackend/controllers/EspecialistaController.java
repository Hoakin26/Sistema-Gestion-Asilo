package app.cabezadealgodon.com.gestionasilobackend.controllers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import app.cabezadealgodon.com.gestionasilobackend.dto.EspecialistaRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.EspecialistaResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.ResumenPersona;
import app.cabezadealgodon.com.gestionasilobackend.security.SoloAdministradorFundacion;
import app.cabezadealgodon.com.gestionasilobackend.services.EspecialistaService;
import jakarta.validation.Valid;

/**
 * HU-06 (YS-167). Los especialistas de la fundación: su perfil, su horario y
 * sus horas libres.
 *
 * <p>
 * Todo es de la Fundación, así que la anotación va en la clase: los
 * especialistas los registra ella y las citas las asigna ella (RF-05).
 */
@RestController
@RequestMapping("/api/especialistas")
@SoloAdministradorFundacion
public class EspecialistaController {

    private final EspecialistaService especialistaService;

    public EspecialistaController(EspecialistaService especialistaService) {
        this.especialistaService = especialistaService;
    }

    /** Con {@code especialidadId}, solo los activos de esa especialidad. */
    @GetMapping
    public List<EspecialistaResponse> listar(@RequestParam(required = false) Long especialidadId) {
        return especialistaService.listar(especialidadId);
    }

    @GetMapping("/usuarios-disponibles")
    public List<ResumenPersona> usuariosDisponibles() {
        return especialistaService.usuariosDisponibles();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EspecialistaResponse registrar(@Valid @RequestBody EspecialistaRequest request) {
        return especialistaService.registrar(request);
    }

    @PutMapping("/{id}")
    public EspecialistaResponse actualizar(@PathVariable Long id, @Valid @RequestBody EspecialistaRequest request) {
        return especialistaService.actualizar(id, request);
    }

    @GetMapping("/{id}/disponibilidad")
    public List<LocalTime> disponibilidad(@PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return especialistaService.disponibilidad(id, fecha);
    }
}
