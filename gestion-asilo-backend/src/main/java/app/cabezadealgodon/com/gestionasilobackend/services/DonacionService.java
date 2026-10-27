package app.cabezadealgodon.com.gestionasilobackend.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
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
import app.cabezadealgodon.com.gestionasilobackend.repository.DonacionRepository.TotalPorTipo;

/**
 * YS-145. Las donaciones monetarias al asilo (HU-22, RF-15): su registro, con
 * el tipo de donante, y su histórico filtrado por tipo y por fechas, con lo que
 * suman. Una mal registrada se anula con su motivo, como los pagos (YS-176).
 */
@Service
public class DonacionService {

    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2);
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
     *
     * <p>
     * OBS-01. Las donaciones, por páginas; los totales, de todas las del
     * filtro, sumados por la base.
     */
    @Transactional(readOnly = true)
    public HistoricoDonacionesResponse historico(LocalDate desde, LocalDate hasta, Collection<TipoDonante> tipos,
            Pageable paginado) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new CampoInvalidoException("hasta", "La fecha final no puede ser anterior a la inicial");
        }
        LocalDate inicio = desde == null ? SIN_INICIO : desde;
        LocalDate fin = hasta == null ? SIN_FIN : hasta;
        Collection<TipoDonante> deTipo = tipos == null || tipos.isEmpty() ? Arrays.asList(TipoDonante.values()) : tipos;

        Map<TipoDonante, TotalPorTipo> sumados = new EnumMap<>(TipoDonante.class);
        donacionRepository.totalesDelHistorico(inicio, fin, deTipo).forEach(total -> sumados.put(total.getTipo(), total));
        List<HistoricoDonacionesResponse.TotalPorTipo> porTipo = Arrays.stream(TipoDonante.values())
                .map(tipo -> {
                    TotalPorTipo delTipo = sumados.get(tipo);
                    return delTipo == null ? new HistoricoDonacionesResponse.TotalPorTipo(tipo, 0, CERO)
                            : new HistoricoDonacionesResponse.TotalPorTipo(tipo, Math.toIntExact(delTipo.getCantidad()),
                                    delTipo.getTotal().setScale(2));
                })
                .toList();
        return new HistoricoDonacionesResponse(
                donacionRepository.historico(inicio, fin, deTipo, paginado).map(DonacionService::respuesta),
                porTipo.stream().map(HistoricoDonacionesResponse.TotalPorTipo::total).reduce(CERO, BigDecimal::add),
                porTipo);
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
