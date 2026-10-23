package app.cabezadealgodon.com.gestionasilobackend.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.PoliticaDescuentoRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.PoliticaDescuentoResponse;
import app.cabezadealgodon.com.gestionasilobackend.exception.CampoInvalidoException;
import app.cabezadealgodon.com.gestionasilobackend.models.PoliticaDescuento;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoDescuento;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoServicio;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.repository.PoliticaDescuentoRepository;

/**
 * YS-136. Los descuentos de la fundación (HU-19): los define el Administrador
 * de la Fundación, porque el descuento lo da la fundación, y los consulta el
 * Administrador del Asilo, que los ve aplicados en la cuenta del familiar.
 */
@Service
public class PoliticaDescuentoService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final PoliticaDescuentoRepository politicaRepository;

    public PoliticaDescuentoService(PoliticaDescuentoRepository politicaRepository) {
        this.politicaRepository = politicaRepository;
    }

    /** Todas las versiones, por servicio y de la más reciente a la más antigua, marcando la que rige hoy. */
    @Transactional(readOnly = true)
    public List<PoliticaDescuentoResponse> politicas() {
        List<PoliticaDescuento> todas = politicaRepository.findAllByOrderByTipoServicioAscVigenteDesdeDesc();
        LocalDate hoy = LocalDate.now();
        return todas.stream()
                .map(politica -> respuesta(politica,
                        politica == CalculadoraDescuento.vigenteEn(politica.getTipoServicio(), hoy, todas)))
                .toList();
    }

    /**
     * Define el descuento de {@code tipo} desde {@code vigenteDesde}. Si ya
     * había uno desde esa misma fecha, lo corrige; si no, agrega una versión
     * nueva, y las anteriores siguen rigiendo para los servicios de antes.
     *
     * <p>
     * La fecha puede ser pasada: así una política "desde el 1 de octubre"
     * cubre los cargos de octubre ya creados, que la toman al consolidar el
     * mes (HU-18). Se pidió así el 20/10.
     */
    @Transactional
    public PoliticaDescuentoResponse definir(TipoServicio tipo, PoliticaDescuentoRequest request, Usuario usuario) {
        if (request.tipoDescuento() == TipoDescuento.PORCENTAJE && request.valor().compareTo(CIEN) > 0) {
            throw new CampoInvalidoException("valor", "Un porcentaje no puede pasar de 100");
        }
        PoliticaDescuento politica = politicaRepository.findByTipoServicioAndVigenteDesde(tipo, request.vigenteDesde())
                .orElseGet(() -> {
                    PoliticaDescuento nueva = new PoliticaDescuento();
                    nueva.setTipoServicio(tipo);
                    nueva.setVigenteDesde(request.vigenteDesde());
                    return nueva;
                });
        politica.setTipoDescuento(request.tipoDescuento());
        politica.setValor(request.valor().setScale(2));
        politica.setRegistradaPor(usuario);
        PoliticaDescuento guardada = politicaRepository.saveAndFlush(politica);

        List<PoliticaDescuento> delServicio = politicaRepository.findByTipoServicioOrderByVigenteDesdeDesc(tipo);
        PoliticaDescuento vigente = CalculadoraDescuento.vigenteEn(tipo, LocalDate.now(), delServicio);
        return respuesta(guardada, vigente != null && vigente.getId().equals(guardada.getId()));
    }

    private static PoliticaDescuentoResponse respuesta(PoliticaDescuento politica, boolean vigente) {
        return new PoliticaDescuentoResponse(
                politica.getId(),
                politica.getTipoServicio(),
                politica.getTipoDescuento(),
                politica.getValor(),
                politica.getVigenteDesde(),
                vigente,
                politica.getRegistradaPor().getNombreCompleto(),
                politica.getFechaRegistro());
    }
}
