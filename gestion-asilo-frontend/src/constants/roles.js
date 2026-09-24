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
