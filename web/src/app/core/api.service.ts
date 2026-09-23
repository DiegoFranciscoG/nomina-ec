import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import { ConfigService } from './config.service';
import {
  AuditEntry,
  BenefitBalance,
  Employee,
  HiringCost,
  LegalParameter,
  Novelty,
  Page,
  PayrollSheet,
  Payslip,
  Period,
  Position,
  ProblemDetail,
  Settlement,
  TaxTables,
} from './models';

/** Typed access to the nomina-ec REST API. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(ConfigService);

  private url(path: string): string {
    return `${this.config.apiUrl}/api${path}`;
  }

  private get<T>(path: string): Promise<T> {
    return firstValueFrom(this.http.get<T>(this.url(path)));
  }

  private post<T>(path: string, body: unknown): Promise<T> {
    return firstValueFrom(this.http.post<T>(this.url(path), body));
  }

  private put<T>(path: string, body: unknown): Promise<T> {
    return firstValueFrom(this.http.put<T>(this.url(path), body));
  }

  // personnel
  employees = () => this.get<Employee[]>('/employees');
  positions = () => this.get<Position[]>('/positions');
  createEmployee = (body: unknown) => this.post<Employee>('/employees', body);
  updateContract = (id: number, body: unknown) => this.put<Employee>(`/employees/${id}/contract`, body);
  savePersonalExpenses = (id: number, fiscalYear: number, projectedAmount: number) =>
    this.put<void>(`/employees/${id}/personal-expenses`, { fiscalYear, projectedAmount });
  personalExpenses = (id: number, year: number) => this.get<number>(`/employees/${id}/personal-expenses?year=${year}`);
  benefits = (id: number, year: number) => this.get<BenefitBalance>(`/employees/${id}/benefits?year=${year}`);

  // payroll
  periods = () => this.get<Period[]>('/periods');
  period = (id: number) => this.get<Period>(`/periods/${id}`);
  createPeriod = (year: number, month: number) => this.post<Period>('/periods', { year, month });
  novelties = (periodId: number) => this.get<Novelty[]>(`/periods/${periodId}/novelties`);
  addNovelty = (periodId: number, body: unknown) => this.post<Novelty>(`/periods/${periodId}/novelties`, body);
  deleteNovelty = (periodId: number, noveltyId: number) =>
    firstValueFrom(this.http.delete<void>(this.url(`/periods/${periodId}/novelties/${noveltyId}`)));
  calculate = (periodId: number) => this.post<PayrollSheet>(`/periods/${periodId}/calculate`, null);
  close = (periodId: number) => this.post<Period>(`/periods/${periodId}/close`, null);
  sheet = (periodId: number) => this.get<PayrollSheet>(`/periods/${periodId}/payroll`);
  payslip = (id: number) => this.get<Payslip>(`/payslips/${id}`);

  // legal parameters
  parameters = () => this.get<LegalParameter[]>('/legal-parameters');
  createParameterVersion = (body: unknown) => this.post<LegalParameter>('/legal-parameters', body);
  audit = (page = 0, code = '') =>
    this.get<Page<AuditEntry>>(`/legal-parameters/audit?page=${page}&size=20${code ? `&code=${encodeURIComponent(code)}` : ''}`);
  taxTables = (year: number) => this.get<TaxTables>(`/legal-parameters/tax-tables/${year}`);

  // simulator and settlements
  hiringCost = (body: unknown) => this.post<HiringCost>('/simulations/hiring-cost', body);
  previewSettlement = (body: unknown) => this.post<Settlement>('/settlements/preview', body);
  registerSettlement = (body: unknown) => this.post<Settlement>('/settlements', body);

  /** Downloads a PDF through HttpClient (so the Bearer token is sent) and saves it. */
  async downloadPdf(path: string, filename: string, body?: unknown): Promise<void> {
    const request$: Observable<Blob> =
      body === undefined
        ? this.http.get(this.url(path), { responseType: 'blob' })
        : this.http.post(this.url(path), body, { responseType: 'blob' });
    const blob = await firstValueFrom(request$);
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = filename;
    link.click();
    setTimeout(() => URL.revokeObjectURL(link.href), 1000);
  }
}

/** Human-readable message from an API error (RFC 9457 problem detail). */
export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const problem = error.error as ProblemDetail | null;
    if (problem?.errors) {
      return Object.entries(problem.errors)
        .map(([field, message]) => `${field}: ${message}`)
        .join(' · ');
    }
    if (problem?.detail) {
      return problem.detail;
    }
    if (error.status === 0) {
      return 'No se pudo conectar con la API. Si está en el plan gratuito, puede tardar ~1 min en despertar.';
    }
    return `Error ${error.status}`;
  }
  return 'Error inesperado';
}
