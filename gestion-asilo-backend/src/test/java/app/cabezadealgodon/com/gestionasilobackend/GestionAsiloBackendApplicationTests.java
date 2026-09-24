package app.cabezadealgodon.com.gestionasilobackend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("Arranque de la aplicación")
class GestionAsiloBackendApplicationTests extends PruebaConBaseDeDatos {

	@Test
	@DisplayName("El contexto de Spring se levanta con todas sus piezas")
	void contextLoads() {
	}

}
