import {
  CalendarCheck,
  CalendarClock,
  CircleDollarSign,
  ClipboardList,
  FileCheck,
  FileText,
  FlaskConical,
  Gift,
  HeartPulse,
  LayoutDashboard,
  PackageOpen,
  Pill,
  Receipt,
  Settings,
  Stethoscope,
  UserRound,
  Users,
  Wallet,
} from "lucide-react";

/**
 * YS-81. Catálogo de los módulos del sistema y qué le toca a cada rol.
 *
 * Un catálogo y un mapa, en vez de siete listas completas: así agregar un
 * módulo o cambiarle el ícono es un solo cambio, y no siete que se desincronizan.
 *
 * `ruta: null` significa "todavía no construido". Esos módulos sí se muestran,
 * deshabilitados, porque le dan al usuario la medida del alcance del sistema.
 * Es distinto de un módulo que no aparece: el gris dice "llega después" y la
 * ausencia dice "no es de su rol".
 */
const CATALOGO = {
  dashboard: { etiqueta: "Dashboard", Icono: LayoutDashboard, ruta: "/dashboard" },
  usuarios: { etiqueta: "Usuarios y roles", Icono: Users, ruta: "/usuarios" },

  internos: { etiqueta: "Internos", Icono: UserRound, ruta: "/internos" },
  // HU-02 (YS-66). "/historial-medico/15" es el historial de un interno.
  historialMedico: { etiqueta: "Historial médico", Icono: ClipboardList, ruta: "/historial-medico" },
  solicitudesReferencia: {
    etiqueta: "Solicitudes de referencia",
    Icono: FileText,
    ruta: "/solicitudes-referencia",
  },

  solicitudesEntrantes: {
    etiqueta: "Solicitudes entrantes",
    Icono: FileText,
    ruta: "/solicitudes-entrantes",
  },
  agendaAsignacion: { etiqueta: "Agenda y asignación", Icono: CalendarClock, ruta: null },
  medicosDisponibilidad: {
    etiqueta: "Médicos y disponibilidad",
    Icono: Stethoscope,
    ruta: "/medicos-disponibilidad",
  },

  miAgenda: { etiqueta: "Mi agenda del día", Icono: CalendarCheck, ruta: "/mi-agenda" },
  // YS-100. "/visitas/15" es la pantalla de una visita: entra con este módulo.
  visitasMedicas: { etiqueta: "Visitas médicas", Icono: HeartPulse, ruta: "/visitas" },
  ordenesLaboratorio: { etiqueta: "Órdenes de laboratorio", Icono: FlaskConical, ruta: null },
  recetas: { etiqueta: "Recetas", Icono: Pill, ruta: null },

  solicitudesAsignadas: { etiqueta: "Solicitudes asignadas", Icono: ClipboardList, ruta: null },
  citas: { etiqueta: "Citas", Icono: CalendarCheck, ruta: null },

  // HU-12 (YS-115). La bandeja del laboratorio.
  ordenesPendientes: {
    etiqueta: "Órdenes pendientes",
    Icono: FlaskConical,
    ruta: "/ordenes-pendientes",
  },
  // HU-13 (YS-118). Los resultados que el laboratorio ya entregó (decidido el 19/10).
  registroResultados: {
    etiqueta: "Registro de resultados",
    Icono: FileCheck,
    ruta: "/registro-resultados",
  },

  // HU-15 (YS-124). La bandeja de la farmacia.
  recetasPendientes: { etiqueta: "Recetas pendientes", Icono: Pill, ruta: "/recetas-pendientes" },
  // HU-16 (YS-127). Las recetas que la farmacia ya despachó (decidido el 19/10).
  despachoMedicamentos: {
    etiqueta: "Despacho de medicamentos",
    Icono: PackageOpen,
    ruta: "/despacho-medicamentos",
  },

  /*
   * Etiqueta distinta de "Internos" a propósito (RNF-03): esta vista nunca
   * muestra psicopatologías, diagnósticos ni medicamentos, solo datos
   * personales, familiar responsable y estado de cuenta.
   *
   * YS-61. Misma ruta que la vista clínica: la pantalla decide qué mostrar
   * según el rol, y el backend decide qué devolver. Dos rutas distintas
   * habrían duplicado la pantalla sin agregar ninguna protección.
   */
  internosAdministrativo: {
    etiqueta: "Internos (vista administrativa)",
    Icono: UserRound,
    ruta: "/internos",
  },
  // HU-18 (YS-134). La cuenta mensual de cada familiar.
  cuentasPorCobrar: { etiqueta: "Cuentas por cobrar", Icono: Receipt, ruta: "/cuentas-por-cobrar" },
  // HU-20 (YS-140). Lo que el asilo le debe a la fundación.
  cuentasPorPagar: { etiqueta: "Cuentas por pagar", Icono: Wallet, ruta: "/cuentas-por-pagar" },
  // HU-22 (YS-146). Las donaciones monetarias al asilo.
  donaciones: { etiqueta: "Donaciones", Icono: Gift, ruta: "/donaciones" },
  // HU-23 (YS-149). Los gastos operativos del asilo.
  gastos: { etiqueta: "Gastos", Icono: CircleDollarSign, ruta: "/gastos" },
  // HU-24 (YS-152). La entrada a los reportes; cada uno se suma con su historia.
  reportes: { etiqueta: "Reportes", Icono: FileText, ruta: "/reportes" },
  configuracion: { etiqueta: "Configuración", Icono: Settings, ruta: null },
};

/** La matriz de menús por rol del diseño (Notion, "Estilo"). */
const MENU_POR_ROL = {
  MEDICO_GENERAL: ["dashboard", "internos", "historialMedico", "solicitudesReferencia"],
  ADMINISTRADOR_FUNDACION: [
    "dashboard",
    "solicitudesEntrantes",
    "agendaAsignacion",
    "medicosDisponibilidad",
  ],
  /*
   * HU-02. Sin "Historial médico", aunque la matriz del diseño lo traía: por
   * HU-08 el especialista ve los antecedentes solo durante su visita activa, y
   * ahí ya los tiene. Un módulo aparte no tendría nada que mostrarle.
   */
  MEDICO_ESPECIALISTA: ["dashboard", "miAgenda", "visitasMedicas", "ordenesLaboratorio", "recetas"],
  ADMINISTRADOR_ASILO: [
    "dashboard",
    "usuarios",
    "internosAdministrativo",
    "cuentasPorCobrar",
    "cuentasPorPagar",
    "donaciones",
    "gastos",
    "reportes",
    "configuracion",
  ],
  ENFERMERIA: ["dashboard", "internos", "solicitudesAsignadas", "citas"],
  LABORATORIO: ["dashboard", "ordenesPendientes", "registroResultados"],
  FARMACIA: ["dashboard", "recetasPendientes", "despachoMedicamentos"],
};

/**
 * Los módulos del rol. Un rol desconocido recibe solo el Dashboard, nunca el
 * menú completo: si algún día llega un rol que este archivo no contempla, el
 * error debe ser de menos y no de más.
 */
export function modulosDeRol(rol) {
  return (MENU_POR_ROL[rol] ?? ["dashboard"]).map((clave) => CATALOGO[clave]);
}

/**
 * Si el rol puede entrar a una ruta. Lo resuelve con la misma matriz que arma
 * el menú, para que el guardia de las rutas y lo que se muestra no puedan
 * discrepar: ocultar una opción no protege nada si la URL escrita a mano abre
 * igual, y protegerla sin ocultarla deja al usuario tocando puertas cerradas.
 *
 * Esto no reemplaza al backend, que es la autoridad (YS-53). Solo evita que el
 * usuario llegue a una pantalla que no le va a responder.
 *
 * YS-61. Una ruta hija hereda el permiso del módulo: quien entra a
 * "/internos" entra también a "/internos/15". Se compara con la barra para que
 * un módulo "/internos" no abra por accidente un "/internosX".
 */
export function puedeEntrar(rol, ruta) {
  return modulosDeRol(rol).some(
    (modulo) => modulo.ruta && (ruta === modulo.ruta || ruta.startsWith(`${modulo.ruta}/`)),
  );
}
