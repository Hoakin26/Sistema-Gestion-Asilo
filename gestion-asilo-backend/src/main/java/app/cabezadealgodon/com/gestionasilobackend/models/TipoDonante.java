package app.cabezadealgodon.com.gestionasilobackend.models;

/**
 * YS-145. Quién dona (HU-22, RF-15): los cuatro tipos del enunciado. La
 * revisión de requerimientos v1.1 (15/10) separó la empresa internacional de
 * la nacional. El reporte de entradas (RF-20b) suma las donaciones por tipo.
 */
public enum TipoDonante {
    EMPRESA_INTERNACIONAL,
    EMPRESA_NACIONAL,
    GOBIERNO,
    PARTICULAR
}
