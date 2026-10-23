package app.cabezadealgodon.com.gestionasilobackend.models;

/**
 * YS-148. En qué gasta el asilo (HU-23, RF-16). RF-16 nombra la luz y el agua
 * "entre otros"; decidido el 21/10, se agregan los egresos comunes de un asilo
 * para que el filtro por tipo agrupe de verdad. Lo que no cae en ninguno va como
 * {@link #OTRO}, con su descripción obligatoria.
 */
public enum TipoGasto {
    LUZ,
    AGUA,
    TELEFONO_INTERNET,
    GAS,
    ALIMENTACION,
    LIMPIEZA,
    MANTENIMIENTO,
    OTRO
}
