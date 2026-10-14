package app.cabezadealgodon.com.gestionasilobackend.models;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * YS-111. Un examen del catálogo de laboratorio, el que el especialista
 * elige al ordenar (HU-11, RF-08).
 *
 * <p>
 * Lleva un costo de referencia porque RF-08 pide registrar el costo para el
 * asilo al asignar el examen: la orden lo copia al crearse. El costo real del
 * examen realizado lo registra después el laboratorio (HU-14).
 */
@Entity
@Table(name = "examen_laboratorio")
@Getter
@Setter
@NoArgsConstructor
public class ExamenLaboratorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String nombre;

    @Column(name = "costo_referencia", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoReferencia;

    /** Como las especialidades: un examen que se deja de ofrecer se desactiva, no se borra. */
    @Column(nullable = false)
    private boolean activo = true;
}
