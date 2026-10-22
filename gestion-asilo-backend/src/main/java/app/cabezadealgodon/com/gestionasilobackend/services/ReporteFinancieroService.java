package app.cabezadealgodon.com.gestionasilobackend.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteEntradasResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteEntradasResponse.CobroRecibido;
import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteEntradasResponse.DonacionRecibida;
import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteEntradasResponse.TotalPorDonante;
import app.cabezadealgodon.com.gestionasilobackend.dto.ReportePagosFundacionResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.ReportePagosFundacionResponse.PagoAFundacion;
import app.cabezadealgodon.com.gestionasilobackend.dto.ReportePagosFundacionResponse.TotalPorConcepto;
import app.cabezadealgodon.com.gestionasilobackend.dto.ResumenPersona;
import app.cabezadealgodon.com.gestionasilobackend.exception.CampoInvalidoException;
import app.cabezadealgodon.com.gestionasilobackend.models.Donacion;
import app.cabezadealgodon.com.gestionasilobackend.models.Interno;
import app.cabezadealgodon.com.gestionasilobackend.models.Pago;
import app.cabezadealgodon.com.gestionasilobackend.models.PagoFundacion;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoDonante;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoServicio;
import app.cabezadealgodon.com.gestionasilobackend.repository.DonacionRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.PagoFundacionRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.PagoRepository;

/**
 * YS-160. Los reportes financieros (HU-27): los pagos a la fundación (RF-20a)
 * y las entradas (RF-20b). Decidido el 22/10: dos reportes y no uno
 * consolidado, como los separó la revisión v1.1. Los dos van por la fecha de
 * cada movimiento y dejan fuera lo anulado.
 */
@Service
public class ReporteFinancieroService {

    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2);

    private final PagoFundacionRepository pagoFundacionRepository;
    private final DonacionRepository donacionRepository;
    private final PagoRepository pagoRepository;

    public ReporteFinancieroService(PagoFundacionRepository pagoFundacionRepository,
            DonacionRepository donacionRepository, PagoRepository pagoRepository) {
        this.pagoFundacionRepository = pagoFundacionRepository;
        this.donacionRepository = donacionRepository;
        this.pagoRepository = pagoRepository;
    }

    /**
     * HU-27 (RF-20a). Los pagos que el asilo le hizo a la fundación entre
     * {@code desde} y {@code hasta}, las dos incluidas, del más antiguo al más
     * reciente, con el total y lo pagado por concepto.
     */
    @Transactional(readOnly = true)
    public ReportePagosFundacionResponse pagosAFundacion(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        List<PagoFundacion> pagos = pagoFundacionRepository.realizadosEntre(desde, hasta);
        List<TotalPorConcepto> porConcepto = Arrays.stream(TipoServicio.values())
                .map(concepto -> {
                    List<PagoFundacion> deEste = pagos.stream()
                            .filter(pago -> pago.getCuenta().getTipoServicio() == concepto)
                            .toList();
                    return new TotalPorConcepto(concepto, deEste.size(), sumar(deEste, PagoFundacion::getMonto));
                })
                .toList();
        return new ReportePagosFundacionResponse(desde, hasta,
                pagos.stream().map(ReporteFinancieroService::pagoAFundacion).toList(), pagos.size(),
                sumar(pagos, PagoFundacion::getMonto), porConcepto);
    }

    /**
     * HU-27 (RF-20b). Lo que entró entre {@code desde} y {@code hasta}, las dos
     * incluidas: las donaciones, también por tipo de donante, y los cobros a
     * familiares, cada uno con su total, y el total de las entradas.
     */
    @Transactional(readOnly = true)
    public ReporteEntradasResponse entradas(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        List<Donacion> donaciones = donacionRepository.recibidasEntre(desde, hasta);
        List<Pago> cobros = pagoRepository.cobradosEntre(desde, hasta);

        List<TotalPorDonante> porTipoDonante = Arrays.stream(TipoDonante.values())
                .map(tipo -> {
                    List<Donacion> deEste = donaciones.stream()
                            .filter(donacion -> donacion.getTipoDonante() == tipo)
                            .toList();
                    return new TotalPorDonante(tipo, deEste.size(), sumar(deEste, Donacion::getMonto));
                })
                .toList();
        BigDecimal totalDonaciones = sumar(donaciones, Donacion::getMonto);
        BigDecimal totalCobros = sumar(cobros, Pago::getMonto);

        return new ReporteEntradasResponse(desde, hasta,
                donaciones.stream().map(ReporteFinancieroService::donacion).toList(), totalDonaciones,
                porTipoDonante, cobros.stream().map(ReporteFinancieroService::cobro).toList(), totalCobros,
                totalDonaciones.add(totalCobros));
    }

    private static void validarRango(LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            throw new CampoInvalidoException("hasta", "La fecha final no puede ser anterior a la inicial");
        }
    }

    private static PagoAFundacion pagoAFundacion(PagoFundacion pago) {
        return new PagoAFundacion(pago.getId(), pago.getFecha(), pago.getCuenta().getTipoServicio(),
                pago.getCuenta().getPeriodo(), pago.getFormaPago(), pago.getReferencia(), pago.getMonto());
    }

    private static DonacionRecibida donacion(Donacion donacion) {
        return new DonacionRecibida(donacion.getId(), donacion.getFecha(), donacion.getDonante(),
                donacion.getTipoDonante(), donacion.getMonto());
    }

    private static CobroRecibido cobro(Pago pago) {
        Interno interno = pago.getCuenta().getInterno();
        return new CobroRecibido(pago.getId(), pago.getFecha(),
                new ResumenPersona(interno.getId(), interno.getNombreCompleto()), pago.getCuenta().getPeriodo(),
                pago.getFormaPago(), pago.getMonto());
    }

    private static <T> BigDecimal sumar(List<T> elementos, Function<T, BigDecimal> monto) {
        return elementos.stream().map(monto).reduce(CERO, BigDecimal::add).setScale(2);
    }
}
