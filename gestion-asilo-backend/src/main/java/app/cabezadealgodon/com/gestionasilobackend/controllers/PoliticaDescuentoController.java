package app.cabezadealgodon.com.gestionasilobackend.controllers;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.cabezadealgodon.com.gestionasilobackend.dto.PoliticaDescuentoRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.PoliticaDescuentoResponse;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoServicio;
import app.cabezadealgodon.com.gestionasilobackend.security.AsiloOFundacion;
import app.cabezadealgodon.com.gestionasilobackend.security.SoloAdministradorFundacion;
import app.cabezadealgodon.com.gestionasilobackend.security.UsuarioAutenticado;
import app.cabezadealgodon.com.gestionasilobackend.services.PoliticaDescuentoService;
import jakarta.validation.Valid;

/**
 * HU-19 (YS-136). Los descuentos de la fundación por tipo de servicio
 * (RF-12): la fundación los define y el asilo los consulta. Ningún rol clínico
 * tiene por qué verlos.
 */
@RestController
@RequestMapping("/api/politicas-descuento")
public class PoliticaDescuentoController {

    private final PoliticaDescuentoService politicaService;

    public PoliticaDescuentoController(PoliticaDescuentoService politicaService) {
        this.politicaService = politicaService;
    }

    /** Todas las versiones, marcando la que rige hoy en cada servicio. */
    @GetMapping
    @AsiloOFundacion
    public List<PoliticaDescuentoResponse> politicas() {
        return politicaService.politicas();
    }

    /** Define el descuento de un servicio desde una fecha; la misma fecha otra vez lo corrige. */
    @PutMapping("/{tipoServicio}")
    @SoloAdministradorFundacion
    public PoliticaDescuentoResponse definir(@PathVariable TipoServicio tipoServicio,
            @Valid @RequestBody PoliticaDescuentoRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return politicaService.definir(tipoServicio, request, usuario.getUsuario());
    }
}
