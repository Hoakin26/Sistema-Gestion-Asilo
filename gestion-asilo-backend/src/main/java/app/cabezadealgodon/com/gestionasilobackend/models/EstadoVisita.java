package app.cabezadealgodon.com.gestionasilobackend.models;

/**
 * YS-98. En qué va la visita médica. Mientras está activa el especialista
 * registra el diagnóstico, la receta y los exámenes (HU-09 a HU-11); al
 * finalizarla, la solicitud pasa a Atendida (HU-09).
 */
public enum EstadoVisita {
    ACTIVA,
    FINALIZADA
}
