package app.cabezadealgodon.com.gestionasilobackend.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import app.cabezadealgodon.com.gestionasilobackend.models.PoliticaDescuento;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoDescuento;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoServicio;
import app.cabezadealgodon.com.gestionasilobackend.repository.PoliticaDescuentoRepository;

/**
 * YS-136. El descuento de la fundación sobre un cargo (HU-19, RF-12): toma la
 * política del servicio vigente en la fecha del cargo y la calcula con su
 * {@link ReglaDescuento}. Es el único lugar donde se resta un descuento; lo
 * usan el cargo al nacer ({@link CargoService}) y la consolidación del mes
 * ({@link CuentaFamiliarService}).
 *
 * <p>
 * El neto nunca es negativo (regla de negocio del diseño): un descuento mayor
 * que el costo se limita al costo, y el cargo queda en cero. El descuento que
 * se guarda es el que de verdad se aplicó, para que el desglose del estado de
 * cuenta cuadre: original menos descuento es el neto.
 */
@Component
public class CalculadoraDescuento {

    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2);

    private final PoliticaDescuentoRepository politicaRepository;
    private final Map<TipoDescuento, ReglaDescuento> reglas = new EnumMap<>(TipoDescuento.class);

    public CalculadoraDescuento(PoliticaDescuentoRepository politicaRepository, List<ReglaDescuento> reglas) {
        this.politicaRepository = politicaRepository;
        reglas.forEach(regla -> this.reglas.put(regla.tipo(), regla));
    }

    /** El desglose de un cargo de {@code tipo} dado en {@code fecha}. */
    public DesgloseCosto calcular(TipoServicio tipo, BigDecimal montoOriginal, LocalDate fecha) {
        return calcular(tipo, montoOriginal, fecha, politicaRepository.findByTipoServicioOrderByVigenteDesdeDesc(tipo));
    }

    /**
     * Lo mismo con las políticas ya leídas: la consolidación calcula todos los
     * cargos del mes con una sola consulta ({@link #politicas()}).
     */
    public DesgloseCosto calcular(TipoServicio tipo, BigDecimal montoOriginal, LocalDate fecha,
            List<PoliticaDescuento> politicas) {
        BigDecimal original = montoOriginal.setScale(2, RoundingMode.HALF_UP);
        PoliticaDescuento vigente = vigenteEn(tipo, fecha, politicas);
        if (vigente == null) {
            return new DesgloseCosto(original, CERO, original);
        }
        BigDecimal descuento = reglas.get(vigente.getTipoDescuento()).aplicar(original, vigente.getValor())
                .max(BigDecimal.ZERO)
                .min(original)
                .setScale(2, RoundingMode.HALF_UP);
        return new DesgloseCosto(original, descuento, original.subtract(descuento));
    }

    /** Todas las políticas, para calcular muchos cargos de una vez. */
    public List<PoliticaDescuento> politicas() {
        return politicaRepository.findAllByOrderByTipoServicioAscVigenteDesdeDesc();
    }

    /**
     * La política de {@code tipo} que rige en {@code fecha}: la de
     * {@code vigenteDesde} más reciente que no sea posterior. Ninguna si todavía
     * no empezaba ninguna.
     */
    static PoliticaDescuento vigenteEn(TipoServicio tipo, LocalDate fecha, List<PoliticaDescuento> politicas) {
        return politicas.stream()
                .filter(politica -> politica.getTipoServicio() == tipo && !politica.getVigenteDesde().isAfter(fecha))
                .max(Comparator.comparing(PoliticaDescuento::getVigenteDesde))
                .orElse(null);
    }
}
