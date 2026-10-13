package app.cabezadealgodon.com.gestionasilobackend.services;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.models.CargoServicio;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoServicio;
import app.cabezadealgodon.com.gestionasilobackend.models.VisitaMedica;
import app.cabezadealgodon.com.gestionasilobackend.repository.CargoServicioRepository;

/**
 * YS-105. El único lugar donde nace un cargo para la cuenta del familiar
 * (RF-12). Lo usan la visita (consulta, HU-09), el laboratorio (HU-14) y la
 * farmacia (HU-17), así que la regla del descuento se aplica en un solo sitio.
 *
 * <p>
 * Por ahora el descuento es cero: las políticas de descuento llegan con HU-19,
 * que lo calculará aquí mismo (el {@code CalculadoraDescuento} del diseño) sin
 * que cambie ninguno de los que llaman.
 */
@Service
public class CargoService {

    private final CargoServicioRepository cargoRepository;

    public CargoService(CargoServicioRepository cargoRepository) {
        this.cargoRepository = cargoRepository;
    }

    /**
     * Registra el cargo dentro de la transacción de quien lo pide: si la
     * operación que lo origina falla, el cargo tampoco queda.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public CargoServicio registrar(VisitaMedica visita, TipoServicio tipo, BigDecimal montoOriginal) {
        BigDecimal descuento = BigDecimal.ZERO;

        CargoServicio cargo = new CargoServicio();
        cargo.setTipo(tipo);
        cargo.setInterno(visita.getInterno());
        cargo.setVisita(visita);
        cargo.setMontoOriginal(montoOriginal);
        cargo.setDescuento(descuento);
        // Nunca menor que cero, aunque un descuento fijo supere al monto (HU-19).
        cargo.setMontoNeto(montoOriginal.subtract(descuento).max(BigDecimal.ZERO));
        cargo.setFecha(LocalDate.now());
        return cargoRepository.save(cargo);
    }
}
