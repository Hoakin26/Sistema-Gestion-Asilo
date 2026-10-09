package app.cabezadealgodon.com.gestionasilobackend.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import app.cabezadealgodon.com.gestionasilobackend.models.Especialidad;
import app.cabezadealgodon.com.gestionasilobackend.repository.EspecialidadRepository;

/**
 * YS-83. El catálogo inicial de especialidades de la fundación.
 *
 * <p>
 * Es una lista base con las especialidades más frecuentes en adultos mayores,
 * elegida el 09/10 mientras se confirma la lista real de la fundación.
 * Cambiarla es cambiar esta constante.
 *
 * <p>
 * A diferencia de {@link RolSeeder}, que solo siembra con la tabla vacía, este
 * agrega cada especialidad que falte. Así, agregar una a la lista la incorpora
 * en la siguiente arrancada aunque la base ya tenga otras. Quitarla de la lista
 * no la borra: puede haber solicitudes que la usen.
 */
@Component
@Order(3)
public class EspecialidadSeeder implements CommandLineRunner {

    static final List<String> ESPECIALIDADES = List.of(
            "Cardiología",
            "Geriatría",
            "Medicina Interna",
            "Neurología",
            "Oftalmología",
            "Psiquiatría",
            "Traumatología y Ortopedia",
            "Urología");

    private final EspecialidadRepository especialidadRepository;

    public EspecialidadSeeder(EspecialidadRepository especialidadRepository) {
        this.especialidadRepository = especialidadRepository;
    }

    @Override
    public void run(String... args) {
        for (String nombre : ESPECIALIDADES) {
            if (!especialidadRepository.existsByNombre(nombre)) {
                Especialidad especialidad = new Especialidad();
                especialidad.setNombre(nombre);
                especialidadRepository.save(especialidad);
            }
        }
    }
}
