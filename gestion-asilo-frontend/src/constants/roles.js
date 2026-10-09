/** Los 7 actores del sistema (ACT-01 a ACT-07), en el orden del CLAUDE.md. */
export const ROLES = [
  { valor: "MEDICO_GENERAL", etiqueta: "Médico General" },
  { valor: "ADMINISTRADOR_FUNDACION", etiqueta: "Administrador de la Fundación" },
  { valor: "MEDICO_ESPECIALISTA", etiqueta: "Médico Especialista" },
  { valor: "ADMINISTRADOR_ASILO", etiqueta: "Administrador del Asilo" },
  { valor: "ENFERMERIA", etiqueta: "Enfermería" },
  { valor: "LABORATORIO", etiqueta: "Laboratorio" },
  { valor: "FARMACIA", etiqueta: "Farmacia" },
];

export function etiquetaDeRol(valor) {
  return ROLES.find((rol) => rol.valor === valor)?.etiqueta ?? valor;
}

/*
 * YS-61. Quién lee la ficha médica: la misma lista que la anotación
 * @PersonalClinico del backend (YS-58). El Administrador del Asilo queda fuera
 * por el RNF-03. La pantalla la usa para no pedir ni mostrar lo que el backend
 * de todos modos le negaría; la autoridad sigue siendo el backend.
 */
const ROLES_CLINICOS = ["MEDICO_GENERAL", "MEDICO_ESPECIALISTA", "ENFERMERIA"];

export function leeDatosClinicos(rol) {
  return ROLES_CLINICOS.includes(rol);
}

/** HU-01. Solo el Médico General registra y edita internos (@SoloMedicoGeneral). */
export function editaInternos(rol) {
  return rol === "MEDICO_GENERAL";
}

/** HU-03. Solo el Médico General refiere a especialistas (RF-03). */
export function creaReferencias(rol) {
  return rol === "MEDICO_GENERAL";
}
