package app.cabezadealgodon.com.gestionasilobackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.models.Familiar;
import app.cabezadealgodon.com.gestionasilobackend.models.FichaMedica;
import app.cabezadealgodon.com.gestionasilobackend.models.Interno;
import app.cabezadealgodon.com.gestionasilobackend.models.MedicamentoHabitual;
import app.cabezadealgodon.com.gestionasilobackend.models.Psicopatologia;
import app.cabezadealgodon.com.gestionasilobackend.repository.FamiliarRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.FichaMedicaRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.InternoRepository;

/**
 * YS-57. Comprueba que el modelo del interno se persiste como se diseñó y, muy
 * en particular, que los datos clínicos quedan separados de los personales
 * (RNF-03).
 */
@SpringBootTest
@DisplayName("Modelo del interno (YS-57)")
class InternoTest extends PruebaConBaseDeDatos {

    @Autowired
    private InternoRepository internoRepository;

    @Autowired
    private FichaMedicaRepository fichaMedicaRepository;

    @Autowired
    private FamiliarRepository familiarRepository;

    @Test
    @DisplayName("Un interno se guarda con su familiar responsable")
    void seGuardaConSuFamiliar() {
        Interno interno = internoRepository.save(nuevoInterno("2345678901234"));

        Interno leido = internoRepository.findById(interno.getId()).orElseThrow();
        assertThat(leido.getNombreCompleto()).isEqualTo("Rosa Elena Morales");
        assertThat(leido.getFamiliarResponsable().getNombreCompleto()).isEqualTo("Carmen Morales");
        assertThat(leido.isActivo()).isTrue();
        // La cuota se compara con isEqualByComparingTo y no con isEqualTo:
        // PostgreSQL devuelve la escala de la columna, así que 850.00 y 850
        // son el mismo número pero no el mismo objeto BigDecimal.
        assertThat(leido.getCuotaMensual()).isEqualByComparingTo("850.00");
    }

    @Test
    @DisplayName("No se admiten dos internos con el mismo DPI")
    void elDpiEsUnico() {
        internoRepository.save(nuevoInterno("3456789012345"));

        assertThatThrownBy(() -> internoRepository.saveAndFlush(nuevoInterno("3456789012345")))
                .isInstanceOf(Exception.class);
    }

    /**
     * Transaccional a propósito, y no porque la prueba lo necesite para
     * funcionar: las dos listas de la ficha son perezosas, así que solo se
     * pueden leer con la sesión abierta. Es exactamente la condición en la que
     * trabajará {@code InternoService.obtenerFicha} (YS-58), marcado
     * {@code @Transactional(readOnly = true)} y mapeando al DTO dentro de la
     * transacción.
     *
     * <p>
     * La alternativa habría sido traerlas de golpe con un {@code @EntityGraph},
     * pero cargar dos colecciones en la misma consulta choca con el
     * {@code MultipleBagFetchException} de Hibernate, y para una ficha con unas
     * pocas filas no compensa: son dos consultas más, no un problema N+1.
     */
    @Test
    @Transactional
    @DisplayName("La ficha médica guarda varias psicopatologías y varios medicamentos")
    void laFichaGuardaSusListas() {
        Interno interno = internoRepository.save(nuevoInterno("4567890123456"));

        FichaMedica ficha = new FichaMedica();
        ficha.setInterno(interno);
        ficha.setTipoSangre("O+");
        ficha.setAlergias("Penicilina");
        ficha.setMotivoIngreso("Deterioro cognitivo progresivo");
        ficha.agregarPsicopatologia(psicopatologia("Demencia senil"));
        ficha.agregarPsicopatologia(psicopatologia("Depresión mayor"));
        ficha.agregarMedicamento(medicamento("Donepezilo"));
        fichaMedicaRepository.save(ficha);

        FichaMedica leida = fichaMedicaRepository.findByInternoId(interno.getId()).orElseThrow();
        assertThat(leida.getPsicopatologias()).hasSize(2);
        assertThat(leida.getMedicamentosHabituales()).hasSize(1);
        // Listas y no un campo de texto: es lo que permite contarlas y
        // mostrarlas por separado, que es lo que pide HU-01.
        assertThat(leida.getPsicopatologias())
                .extracting(Psicopatologia::getNombre)
                .containsExactlyInAnyOrder("Demencia senil", "Depresión mayor");
    }

    /**
     * El corazón del RNF-03. No se comprueba comparando un DTO, sino que la
     * entidad no tiene por dónde llegar a lo clínico: si alguien agregara un
     * campo `fichaMedica` a `Interno`, esta prueba falla y obliga a justificarlo.
     */
    @Test
    @DisplayName("Desde el interno no hay ningún camino hacia sus datos clínicos")
    void elInternoNoAlcanzaLoClinico() {
        List<String> camposClinicos = List.of(
                "fichaMedica", "psicopatologias", "medicamentosHabituales",
                "diagnosticos", "alergias", "tipoSangre");

        List<String> encontrados = java.util.Arrays.stream(Interno.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .filter(camposClinicos::contains)
                .toList();

        assertThat(encontrados)
                .as("Interno no debe exponer datos clínicos (RNF-03): "
                        + "el Administrador del Asilo no puede leerlos")
                .isEmpty();
    }

    @Test
    @DisplayName("La búsqueda encuentra al interno por nombre y por DPI")
    void laBusquedaEncuentraPorNombreYPorDpi() {
        internoRepository.save(nuevoInterno("5678901234567"));

        assertThat(internoRepository
                .findByNombreCompletoContainingIgnoreCaseOrDpiContaining(
                        "rosa elena", "---", org.springframework.data.domain.Pageable.unpaged()))
                .isNotEmpty();

        assertThat(internoRepository
                .findByNombreCompletoContainingIgnoreCaseOrDpiContaining(
                        "---", "5678901234567", org.springframework.data.domain.Pageable.unpaged()))
                .isNotEmpty();
    }

    // ---- apoyo ----

    private Interno nuevoInterno(String dpi) {
        Familiar familiar = new Familiar();
        familiar.setNombreCompleto("Carmen Morales");
        familiar.setParentesco("Hija");
        familiar.setTelefono("50212345678");
        familiarRepository.save(familiar);

        Interno interno = new Interno();
        interno.setNombreCompleto("Rosa Elena Morales");
        interno.setDpi(dpi);
        interno.setFechaNacimiento(LocalDate.of(1941, 3, 17));
        interno.setFechaIngreso(LocalDate.of(2024, 8, 1));
        interno.setCuotaMensual(new BigDecimal("850.00"));
        interno.setFamiliarResponsable(familiar);
        return interno;
    }

    private Psicopatologia psicopatologia(String nombre) {
        Psicopatologia psicopatologia = new Psicopatologia();
        psicopatologia.setNombre(nombre);
        psicopatologia.setFechaDiagnostico(LocalDate.of(2024, 9, 10));
        return psicopatologia;
    }

    private MedicamentoHabitual medicamento(String nombre) {
        MedicamentoHabitual medicamento = new MedicamentoHabitual();
        medicamento.setNombre(nombre);
        medicamento.setDosis("10 mg");
        medicamento.setFrecuencia("Cada 24 horas");
        return medicamento;
    }
}
