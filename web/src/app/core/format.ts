const money = new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD', minimumFractionDigits: 2 });
const percent = new Intl.NumberFormat('es-EC', { style: 'percent', maximumFractionDigits: 2 });

export const MONTHS = [
  'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre',
];

export function formatMoney(value: number | null | undefined): string {
  return money.format(value ?? 0);
}

export function formatPercent(value: number | null | undefined): string {
  return percent.format(value ?? 0);
}

export function periodLabel(year: number, month: number): string {
  return `${MONTHS[month - 1]} ${year}`;
}

/** Rates are stored as fractions (0.0945); amounts as plain numbers. */
export function formatParameter(code: string, value: number): string {
  if (code.endsWith('_RATE') || code.endsWith('_SURCHARGE') || code.startsWith('PROFIT_SHARING')) {
    return formatPercent(value);
  }
  if (code === 'SBU' || code === 'BASIC_FAMILY_BASKET') {
    return formatMoney(value);
  }
  return String(value);
}

export const LABELS: Record<string, string> = {
  MONTHLY: 'Mensualizado',
  ACCUMULATED: 'Acumulado',
  COSTA_GALAPAGOS: 'Costa / Galápagos',
  SIERRA_AMAZONIA: 'Sierra / Amazonía',
  INDEFINITE: 'Indefinido',
  EVENTUAL: 'Eventual',
  OCCASIONAL: 'Ocasional',
  SEASONAL: 'De temporada',
  PER_PROJECT: 'Por obra',
  OPEN: 'Abierto',
  CALCULATED: 'Calculado',
  CLOSED: 'Cerrado',
  OVERTIME_SUPPLEMENTARY: 'Horas suplementarias (50 %)',
  OVERTIME_EXTRAORDINARY: 'Horas extraordinarias (100 %)',
  ABSENCE_DAYS: 'Faltas (días)',
  ADVANCE: 'Anticipo',
  BONUS: 'Bono / comisión',
  OTHER_DEDUCTION: 'Otro descuento',
  RESIGNATION: 'Renuncia (desahucio del trabajador)',
  MUTUAL_AGREEMENT: 'Mutuo acuerdo',
  UNJUSTIFIED_DISMISSAL: 'Despido intempestivo',
  JUSTIFIED_DISMISSAL: 'Visto bueno (despido justificado)',
  CREATE: 'Nueva versión',
  CLOSE_VALIDITY: 'Cierre de vigencia',
};

export function label(key: string | null | undefined): string {
  return key ? (LABELS[key] ?? key) : '';
}
