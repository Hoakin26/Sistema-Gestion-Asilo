package app.cabezadealgodon.com.gestionasilobackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import jakarta.servlet.MultipartConfigElement;

/**
 * YS-117. El tamaño máximo de los archivos subidos, el mismo en todos los
 * entornos: 10 MB por archivo (decidido el 15/10, YS-174).
 *
 * <p>
 * Spring Boot lo toma de {@code spring.servlet.multipart.*}, y sin esas líneas
 * usa 1 MB. {@code application-prod.properties} las tiene, pero el
 * {@code application.properties} de desarrollo no se versiona y no las trae:
 * en desarrollo, la foto de un resultado se rechazaba antes de llegar al
 * servicio. Este bean lee las mismas propiedades, con los 10 MB como valor por
 * defecto, así que producción las sigue pudiendo cambiar.
 */
@Configuration
public class ArchivosConfig {

    @Bean
    public MultipartConfigElement multipartConfigElement(
            @Value("${spring.servlet.multipart.max-file-size:10MB}") String porArchivo,
            @Value("${spring.servlet.multipart.max-request-size:11MB}") String porPeticion) {
        // Sin carpeta propia y sin umbral: el archivo se guarda en el almacén
        // del sistema (AlmacenArchivos), no donde lo deja el contenedor.
        return new MultipartConfigElement("", DataSize.parse(porArchivo).toBytes(),
                DataSize.parse(porPeticion).toBytes(), 0);
    }
}
