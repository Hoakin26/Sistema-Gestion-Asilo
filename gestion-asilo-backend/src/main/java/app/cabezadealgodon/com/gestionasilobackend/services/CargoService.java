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
 * YS-136. El descuento de la fundación (HU-19) se calcula aquí, con la
 * {@link CalculadoraDescuento} del diseño, y ninguno de los que llaman tuvo
 * que cambiar. Es la política vigente el día del servicio; la consolidación
 * del mes (HU-18) la vuelve a calcular con la misma regla, por si la fundación
 * definió después una política que ya regía en esa fecha.
 */
@Service
public class CargoService {

    private final CargoServicioRepository cargoRepository;
    private final CalculadoraDescuento calculadora;

    public CargoService(CargoServicioRepository cargoRepository, CalculadoraDescuento calculadora) {
        this.cargoRepository = cargoRepository;
        this.calculadora = calculadora;
    }

    /**
     * Registra el cargo dentro de la transacción de quien lo pide: si la
     * operación que lo origina falla, el cargo tampoco queda.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public CargoServicio registrar(VisitaMedica visita, TipoServicio tipo, BigDecimal montoOriginal) {
        LocalDate hoy = LocalDate.now();
        // Nunca menor que cero, aunque un descuento fijo supere al monto (HU-19).
        DesgloseCosto desglose = calculadora.calcular(tipo, montoOriginal, hoy);

        CargoServicio cargo = new CargoServicio();
        cargo.setTipo(tipo);
        cargo.setInterno(visita.getInterno());
        cargo.setVisita(visita);
        cargo.setMontoOriginal(desglose.montoOriginal());
        cargo.setDescuento(desglose.descuento());
        cargo.setMontoNeto(desglose.montoNeto());
        cargo.setFecha(hoy);
        return cargoRepository.save(cargo);
    }
}
