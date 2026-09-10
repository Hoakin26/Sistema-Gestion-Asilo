package app.cabezadealgodon.com.gestionasilobackend.config;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import app.cabezadealgodon.com.gestionasilobackend.models.Rol;
import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;
import app.cabezadealgodon.com.gestionasilobackend.repository.RolRepository;

/**
 * Inserta los 7 roles fijos del sistema (ACT-01 a ACT-07) si la tabla
 * "rol" está vacía. Los roles no son gestionables por el usuario final,
 * por eso se siembran al arrancar la aplicación en vez de exponer un
 * endpoint para crearlos.
 */
@Component
public class RolSeeder implements CommandLineRunner {

    private static final Map<RolNombre, String> DESCRIPCIONES = new EnumMap<>(RolNombre.class);
    static {
        DESCRIPCIONES.put(RolNombre.MEDICO_GENERAL, "Médico General");
        DESCRIPCIONES.put(RolNombre.ADMINISTRADOR_FUNDACION, "Administrador de la Fundación");
        DESCRIPCIONES.put(RolNombre.MEDICO_ESPECIALISTA, "Médico Especialista");
        DESCRIPCIONES.put(RolNombre.ADMINISTRADOR_ASILO, "Administrador del Asilo");
        DESCRIPCIONES.put(RolNombre.ENFERMERIA, "Enfermería");
        DESCRIPCIONES.put(RolNombre.LABORATORIO, "Laboratorio");
        DESCRIPCIONES.put(RolNombre.FARMACIA, "Farmacia");
    }

    private final RolRepository rolRepository;

    public RolSeeder(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    @Override
    public void run(String... args) {
        if (rolRepository.count() > 0) {
            return;
        }
        for (RolNombre nombre : RolNombre.values()) {
            Rol rol = new Rol();
            rol.setNombre(nombre);
            rol.setDescripcion(DESCRIPCIONES.get(nombre));
            rolRepository.save(rol);
        }
    }
}
