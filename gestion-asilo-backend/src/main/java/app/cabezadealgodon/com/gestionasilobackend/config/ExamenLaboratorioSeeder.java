package app.cabezadealgodon.com.gestionasilobackend.config;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import app.cabezadealgodon.com.gestionasilobackend.models.ExamenLaboratorio;
import app.cabezadealgodon.com.gestionasilobackend.repository.ExamenLaboratorioRepository;

/**
 * YS-111. El catálogo inicial de exámenes de laboratorio, con su costo de
 * referencia en quetzales.
 *
 * <p>
 * Es una lista base de los exámenes más frecuentes en adultos mayores,
 * elegida el 13/10 mientras se confirma la lista real de la fundación. Los
 * costos son aproximados: el laboratorio registra el real de cada examen
 * realizado (HU-14). Cambiar la lista es cambiar esta constante.
 *
 * <p>
 * Igual que {@link EspecialidadSeeder}, agrega cada examen que falte y no
 * toca los que ya existen: un costo ajustado a mano en la base no se pisa en
 * la siguiente arrancada, y quitar un examen de la lista no lo borra.
 */
@Component
@Order(4)
public class ExamenLaboratorioSeeder implements CommandLineRunner {

    static final Map<String, String> EXAMENES = new LinkedHashMap<>();
    static {
        EXAMENES.put("Hematología completa", "75.00");
        EXAMENES.put("Glucosa en ayunas", "30.00");
        EXAMENES.put("Hemoglobina glicosilada (HbA1c)", "120.00");
        EXAMENES.put("Perfil lipídico", "150.00");
        EXAMENES.put("Creatinina", "45.00");
        EXAMENES.put("Nitrógeno de urea (BUN)", "45.00");
        EXAMENES.put("Ácido úrico", "45.00");
        EXAMENES.put("Electrolitos (sodio, potasio, cloro)", "150.00");
        EXAMENES.put("Pruebas de función hepática", "200.00");
        EXAMENES.put("Perfil tiroideo (TSH, T4 libre)", "175.00");
        EXAMENES.put("Examen general de orina", "35.00");
        EXAMENES.put("Urocultivo", "150.00");
        EXAMENES.put("Examen de heces", "35.00");
        EXAMENES.put("Vitamina B12", "250.00");
        EXAMENES.put("Proteína C reactiva", "100.00");
        EXAMENES.put("Tiempo de protrombina (INR)", "75.00");
    }

    private final ExamenLaboratorioRepository examenRepository;

    public ExamenLaboratorioSeeder(ExamenLaboratorioRepository examenRepository) {
        this.examenRepository = examenRepository;
    }

    @Override
    public void run(String... args) {
        EXAMENES.forEach((nombre, costo) -> {
            if (!examenRepository.existsByNombre(nombre)) {
                ExamenLaboratorio examen = new ExamenLaboratorio();
                examen.setNombre(nombre);
                examen.setCostoReferencia(new BigDecimal(costo));
                examenRepository.save(examen);
            }
        });
    }
}
