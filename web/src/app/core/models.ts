export type PaymentMode = 'MONTHLY' | 'ACCUMULATED';
export type Region = 'COSTA_GALAPAGOS' | 'SIERRA_AMAZONIA';
export type ContractType = 'INDEFINITE' | 'EVENTUAL' | 'OCCASIONAL' | 'SEASONAL' | 'PER_PROJECT';
export type PeriodStatus = 'OPEN' | 'CALCULATED' | 'CLOSED';
export type NoveltyType =
  | 'OVERTIME_SUPPLEMENTARY'
  | 'OVERTIME_EXTRAORDINARY'
  | 'ABSENCE_DAYS'
  | 'ADVANCE'
  | 'BONUS'
  | 'OTHER_DEDUCTION';
export type SettlementReason = 'RESIGNATION' | 'MUTUAL_AGREEMENT' | 'UNJUSTIFIED_DISMISSAL' | 'JUSTIFIED_DISMISSAL';

export interface Position {
  id: number;
  code: string;
  name: string;
}

export interface Contract {
  id: number;
  positionId: number;
  positionName: string;
  contractType: ContractType;
  weeklyHours: number;
  monthlySalary: number;
  startDate: string;
  endDate: string | null;
  region: Region;
  thirteenthMode: PaymentMode;
  fourteenthMode: PaymentMode;
  reserveFundMode: PaymentMode;
  status: 'ACTIVE' | 'TERMINATED';
}

export interface Employee {
  id: number;
  idNumberMasked: string;
  firstNames: string;
  lastNames: string;
  email: string | null;
  familyDependents: number;
  catastrophicCondition: boolean;
  active: boolean;
  contract: Contract | null;
}

export interface BenefitBalance {
  year: number;
  thirteenthAccumulated: number;
  thirteenthPaid: number;
  fourteenthAccumulated: number;
  fourteenthPaid: number;
  reserveFundAccumulated: number;
  reserveFundPaid: number;
  vacationProvision: number;
}

export interface Period {
  id: number;
  year: number;
  month: number;
  status: PeriodStatus;
  calculatedAt: string | null;
  closedAt: string | null;
  closedBy: string | null;
  payslipCount: number;
}

export interface Novelty {
  id: number;
  employeeId: number;
  employeeName: string;
  noveltyType: NoveltyType;
  quantity: number;
  amount: number;
  description: string | null;
}

export interface PayslipSummary {
  id: number;
  employeeId: number;
  employeeName: string;
  positionName: string;
  workedDays: number;
  baseSalary: number;
  iessBase: number;
  totalIncome: number;
  totalDeductions: number;
  netPay: number;
  employerCost: number;
}

export interface Totals {
  totalIncome: number;
  totalDeductions: number;
  netPay: number;
  iessPersonal: number;
  iessEmployer: number;
  incomeTax: number;
  provisions: number;
  employerCost: number;
}

export interface PayrollSheet {
  period: Period;
  payslips: PayslipSummary[];
  totals: Totals;
}

export interface PayslipLine {
  conceptCode: string;
  conceptName: string;
  conceptType: 'INCOME' | 'DEDUCTION' | 'PROVISION' | 'EMPLOYER_CONTRIBUTION';
  quantity: number | null;
  rate: number | null;
  amount: number;
}

export interface Payslip {
  id: number;
  year: number;
  month: number;
  employeeName: string;
  idNumberMasked: string;
  positionName: string;
  workedDays: number;
  iessBase: number;
  incomeTaxBase: number;
  projectedAnnualTaxBase: number;
  lines: PayslipLine[];
  totalIncome: number;
  totalDeductions: number;
  netPay: number;
  employerCost: number;
  parametersSnapshot: { effectiveDate: string; parameters: Record<string, number> };
}

export interface LegalParameter {
  id: number;
  code: string;
  value: number;
  validFrom: string;
  validTo: string | null;
  description: string;
  legalBasis: string;
  sourceUrl: string;
  createdBy: string;
  inForce: boolean;
}

export interface AuditEntry {
  id: number;
  code: string;
  action: 'CREATE' | 'CLOSE_VALIDITY';
  oldValue: Record<string, string | null> | null;
  newValue: Record<string, string | null>;
  reason: string;
  changedBy: string;
  changedAt: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  number: number;
  totalPages: number;
}

export interface TaxTables {
  fiscalYear: number;
  brackets: { lowerBound: number; upperBound: number | null; baseTax: number; marginalRate: number }[];
  personalExpenseCaps: { familyDependents: number; basketMultiplier: number; maxAmount: number; maxRebate: number }[];
}

export interface HiringScenario {
  salary: number;
  employerIess: number;
  iece: number;
  secap: number;
  thirteenth: number;
  fourteenth: number;
  reserveFund: number;
  vacation: number;
  monthlyCost: number;
  annualCost: number;
  costOverSalary: number;
  employeeIess: number;
  employeeIncomeTax: number;
  employeeNetMonthly: number;
}

export interface HiringCost {
  sbu: number;
  belowMinimumWage: boolean;
  firstYear: HiringScenario;
  fromSecondYear: HiringScenario;
}

export interface SettlementItem {
  code: string;
  description: string;
  amount: number;
}

export interface Settlement {
  id: number | null;
  employeeId: number;
  employeeName: string;
  startDate: string;
  terminationDate: string;
  reason: SettlementReason;
  serviceDays: number;
  serviceYears: number;
  lastSalary: number;
  items: SettlementItem[];
  totalIncome: number;
  totalDeductions: number;
  netTotal: number;
}

export interface ProblemDetail {
  title?: string;
  status?: number;
  detail?: string;
  errors?: Record<string, string>;
}
