package app.cabezadealgodon.com.gestionasilobackend.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.DonacionRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.DonacionResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.HistoricoDonacionesResponse;
import app.cabezadealgodon.com.gestionasilobackend.exception.CampoInvalidoException;
import app.cabezadealgodon.com.gestionasilobackend.exception.ConflictoException;
import app.cabezadealgodon.com.gestionasilobackend.exception.RecursoNoEncontradoException;
import app.cabezadealgodon.com.gestionasilobackend.models.Donacion;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoDonante;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.repository.DonacionRepository;

/**
 * YS-145. Las donaciones monetarias al asilo (HU-22, RF-15): su registro, con
 * el tipo de donante, y su histórico filtrado por tipo y por fechas, con lo que
 * suman. Una mal registrada se anula con su motivo, como los pagos (YS-176).
 */
@Service
public class DonacionService {

    /** Sin fechas, todas: desde antes de que existiera el asilo hasta muy adelante. */
    private static final LocalDate SIN_INICIO = LocalDate.of(1900, 1, 1);
    private static final LocalDate SIN_FIN = LocalDate.of(3000, 1, 1);

    private final DonacionRepository donacionRepository;

    public DonacionService(DonacionRepository donacionRepository) {
        this.donacionRepository = donacionRepository;
    }

    /** Registra una donación. Las reglas de cada campo las revisa {@code DonacionRequest}. */
    @Transactional
    public DonacionResponse registrar(DonacionRequest request, Usuario usuario) {
        Donacion donacion = new Donacion();
        donacion.setMonto(request.monto().setScale(2));
        donacion.setFecha(request.fecha());
        donacion.setDonante(request.donante().strip());
        donacion.setTipoDonante(request.tipoDonante());
        donacion.setRegistradaPor(usuario);
        return respuesta(donacionRepository.saveAndFlush(donacion));
    }

    /**
     * El histórico: las donaciones de {@code tipos} entre {@code desde} y
     * {@code hasta}, las dos incluidas, de la más reciente a la más antigua.
     * Sin tipos, de todos; sin fechas, de cualquier fecha. Los totales no
     * cuentan las anuladas.
     */
    @Transactional(readOnly = true)
    public HistoricoDonacionesResponse historico(LocalDate desde, LocalDate hasta, Collection<TipoDonante> tipos) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new CampoInvalidoException("hasta", "La fecha final no puede ser anterior a la inicial");
        }
        List<Donacion> donaciones = donacionRepository.historico(
                desde == null ? SIN_INICIO : desde,
                hasta == null ? SIN_FIN : hasta,
                tipos == null || tipos.isEmpty() ? Arrays.asList(TipoDonante.values()) : tipos);

        List<Donacion> vigentes = donaciones.stream().filter(donacion -> !donacion.isAnulada()).toList();
        List<HistoricoDonacionesResponse.TotalPorTipo> porTipo = Arrays.stream(TipoDonante.values())
                .map(tipo -> {
                    List<Donacion> delTipo = vigentes.stream()
                            .filter(donacion -> donacion.getTipoDonante() == tipo)
                            .toList();
                    return new HistoricoDonacionesResponse.TotalPorTipo(tipo, delTipo.size(), suma(delTipo));
                })
                .toList();
        return new HistoricoDonacionesResponse(donaciones.stream().map(DonacionService::respuesta).toList(),
                suma(vigentes), porTipo);
    }

    /**
     * Anula una donación mal registrada. No se borra: queda en el histórico con
     * el motivo, quién la anuló y cuándo, y deja de contar en los totales. Una
     * ya anulada no se vuelve a anular (409).
     */
    @Transactional
    public DonacionResponse anular(Long id, String motivo, Usuario usuario) {
        Donacion donacion = donacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una donación con id " + id));
        if (donacion.isAnulada()) {
            throw new ConflictoException("Esta donación ya está anulada");
        }
        donacion.anular(motivo.strip(), usuario, LocalDateTime.now());
        return respuesta(donacionRepository.saveAndFlush(donacion));
    }

    private static BigDecimal suma(List<Donacion> donaciones) {
        return donaciones.stream().map(Donacion::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
    }

    private static DonacionResponse respuesta(Donacion donacion) {
        return new DonacionResponse(
                donacion.getId(),
                donacion.getMonto(),
                donacion.getFecha(),
                donacion.getDonante(),
                donacion.getTipoDonante(),
                donacion.getRegistradaPor().getNombreCompleto(),
                donacion.getFechaRegistro(),
                donacion.isAnulada(),
                donacion.getMotivoAnulacion(),
                donacion.getAnuladaPor() == null ? null : donacion.getAnuladaPor().getNombreCompleto(),
                donacion.getFechaAnulacion());
    }
}
