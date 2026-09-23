import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService, errorMessage } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { formatMoney, label, periodLabel } from '../core/format';
import { Employee, Novelty, NoveltyType, PayrollSheet, Payslip, Period } from '../core/models';

@Component({
  selector: 'app-period-detail',
  imports: [FormsModule, RouterLink],
  template: `
    <p class="small"><a routerLink="/periods">← Periodos</a></p>
    @if (period(); as p) {
      <div class="row spread">
        <div>
          <h1>{{ title() }} <span class="badge {{ p.status }}">{{ label(p.status) }}</span></h1>
          <p class="lead">Los parámetros legales se toman con vigencia al último día del mes.</p>
        </div>
        <div class="row">
          @if (auth.canOperate() && p.status !== 'CLOSED') {
            <button type="button" (click)="calculate()" [disabled]="busy()">{{ p.status === 'CALCULATED' ? 'Recalcular' : 'Calcular roles' }}</button>
          }
          @if (auth.isAdmin() && p.status === 'CALCULATED') {
            <button type="button" class="success" (click)="close()" [disabled]="busy()">Cerrar periodo</button>
          }
          @if (sheet()?.payslips?.length) {
            <button type="button" class="secondary" (click)="downloadSheet()">Planilla PDF</button>
          }
        </div>
      </div>
    }
    @if (error()) {
      <div class="alert error">{{ error() }}</div>
    }
    @if (message()) {
      <div class="alert success">{{ message() }}</div>
    }

    <section class="card">
      <h2>Novedades del mes</h2>
      @if (auth.canOperate() && period()?.status !== 'CLOSED') {
        <form class="form-grid" (ngSubmit)="addNovelty()">
          <label>Empleado
            <select name="employee" [(ngModel)]="novelty.employeeId" required>
              @for (e of activeEmployees(); track e.id) { <option [ngValue]="e.id">{{ e.lastNames }} {{ e.firstNames }}</option> }
            </select>
          </label>
          <label>Tipo
            <select name="type" [(ngModel)]="novelty.noveltyType">
              @for (t of noveltyTypes; track t) { <option [value]="t">{{ label(t) }}</option> }
            </select>
          </label>
          @if (usesQuantity()) {
            <label>{{ novelty.noveltyType === 'ABSENCE_DAYS' ? 'Días' : 'Horas' }}<input name="qty" type="number" min="0" step="0.5" [(ngModel)]="novelty.quantity" /></label>
          } @else {
            <label>Monto (USD)<input name="amount" type="number" min="0" step="0.01" [(ngModel)]="novelty.amount" /></label>
          }
          <label>Detalle<input name="description" maxlength="255" [(ngModel)]="novelty.description" /></label>
          <div class="row" style="align-self: end"><button type="submit" [disabled]="busy()">Agregar</button></div>
        </form>
      }
      <div class="table-wrap" style="margin-top: 12px">
        <table>
          <thead><tr><th>Empleado</th><th>Novedad</th><th class="num">Cantidad</th><th class="num">Monto</th><th>Detalle</th><th></th></tr></thead>
          <tbody>
            @for (n of novelties(); track n.id) {
              <tr>
                <td>{{ n.employeeName }}</td>
                <td>{{ label(n.noveltyType) }}</td>
                <td class="num">{{ n.quantity || '' }}</td>
                <td class="num">{{ n.amount ? money(n.amount) : '' }}</td>
                <td class="small">{{ n.description }}</td>
                <td class="num">
                  @if (auth.canOperate() && period()?.status !== 'CLOSED') {
                    <button type="button" class="link" (click)="removeNovelty(n)">Quitar</button>
                  }
                </td>
              </tr>
            } @empty {
              <tr><td colspan="6" class="muted">Sin novedades.</td></tr>
            }
          </tbody>
        </table>
      </div>
    </section>

    @if (sheet(); as s) {
      <section class="card table-wrap">
        <h2>Planilla del periodo</h2>
        <table>
          <thead>
            <tr><th>Empleado</th><th>Cargo</th><th class="num">Días</th><th class="num">Base IESS</th><th class="num">Ingresos</th>
              <th class="num">Descuentos</th><th class="num">Neto</th><th class="num">Costo empleador</th><th></th></tr>
          </thead>
          <tbody>
            @for (r of s.payslips; track r.id) {
              <tr class="clickable" (click)="openPayslip(r.id)">
                <td>{{ r.employeeName }}</td>
                <td class="small">{{ r.positionName }}</td>
                <td class="num">{{ r.workedDays }}</td>
                <td class="num">{{ money(r.iessBase) }}</td>
                <td class="num">{{ money(r.totalIncome) }}</td>
                <td class="num">{{ money(r.totalDeductions) }}</td>
                <td class="num"><strong>{{ money(r.netPay) }}</strong></td>
                <td class="num">{{ money(r.employerCost) }}</td>
                <td class="num"><button type="button" class="link" (click)="downloadPayslip(r.id, r.employeeId); $event.stopPropagation()">PDF</button></td>
              </tr>
            } @empty {
              <tr><td colspan="9" class="muted">Calcule el periodo para generar los roles.</td></tr>
            }
          </tbody>
          @if (s.payslips.length) {
            <tfoot>
              <tr><td colspan="4">Totales</td><td class="num">{{ money(s.totals.totalIncome) }}</td><td class="num">{{ money(s.totals.totalDeductions) }}</td>
                <td class="num">{{ money(s.totals.netPay) }}</td><td class="num">{{ money(s.totals.employerCost) }}</td><td></td></tr>
            </tfoot>
          }
        </table>
        @if (s.payslips.length) {
          <p class="small muted">IESS personal {{ money(s.totals.iessPersonal) }} · IESS patronal {{ money(s.totals.iessEmployer) }} ·
            Retenciones IR {{ money(s.totals.incomeTax) }} · Provisiones {{ money(s.totals.provisions) }}</p>
        }
      </section>
    }

    @if (payslip(); as ps) {
      <div class="drawer-backdrop" (click)="payslip.set(null)"></div>
      <aside class="drawer" aria-label="Rol de pagos">
        <div class="row spread">
          <h2>Rol de pagos · {{ ps.employeeName }}</h2>
          <button type="button" class="ghost" (click)="payslip.set(null)">Cerrar</button>
        </div>
        <p class="muted small">{{ ps.positionName }} · {{ ps.idNumberMasked }} · {{ ps.workedDays }} días · parámetros al {{ ps.parametersSnapshot.effectiveDate }}</p>
        @for (group of groups; track group.type) {
          <h3 style="margin-top: 14px">{{ group.title }}</h3>
          <table>
            <tbody>
              @for (l of linesOf(ps, group.type); track l.conceptCode) {
                <tr><td>{{ l.conceptName }}@if (l.quantity) {<span class="muted small"> · {{ l.quantity }}</span>}</td><td class="num">{{ money(l.amount) }}</td></tr>
              }
            </tbody>
          </table>
        }
        <div class="grid cols-2" style="margin-top: 16px">
          <div class="kpi"><small>Neto a recibir</small><strong>{{ money(ps.netPay) }}</strong></div>
          <div class="kpi"><small>Costo del empleador</small><strong>{{ money(ps.employerCost) }}</strong></div>
        </div>
        <p class="small muted">Base gravada IR del mes {{ money(ps.incomeTaxBase) }} · proyección anual {{ money(ps.projectedAnnualTaxBase) }}</p>
      </aside>
    }
  `,
})
export class PeriodDetailPage implements OnInit {
  private readonly api = inject(ApiService);
  protected readonly auth = inject(AuthService);

  readonly id = input.required<string>();

  protected readonly period = signal<Period | null>(null);
  protected readonly sheet = signal<PayrollSheet | null>(null);
  protected readonly novelties = signal<Novelty[]>([]);
  protected readonly employees = signal<Employee[]>([]);
  protected readonly payslip = signal<Payslip | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly message = signal('');

  protected readonly activeEmployees = computed(() => this.employees().filter((e) => e.active));
  protected readonly title = computed(() => {
    const p = this.period();
    return p ? `Nómina de ${periodLabel(p.year, p.month)}` : '';
  });

  protected readonly noveltyTypes: NoveltyType[] = [
    'OVERTIME_SUPPLEMENTARY', 'OVERTIME_EXTRAORDINARY', 'ABSENCE_DAYS', 'BONUS', 'ADVANCE', 'OTHER_DEDUCTION',
  ];
  protected readonly groups = [
    { type: 'INCOME', title: 'Ingresos' },
    { type: 'DEDUCTION', title: 'Descuentos' },
    { type: 'EMPLOYER_CONTRIBUTION', title: 'Aportes patronales' },
    { type: 'PROVISION', title: 'Provisiones' },
  ];
  protected novelty = { employeeId: null as number | null, noveltyType: 'OVERTIME_SUPPLEMENTARY' as NoveltyType, quantity: 0, amount: 0, description: '' };

  protected readonly money = formatMoney;
  protected readonly label = label;

  private get periodId(): number {
    return Number(this.id());
  }

  async ngOnInit(): Promise<void> {
    await this.refresh();
    try {
      this.employees.set(await this.api.employees());
      this.novelty.employeeId = this.activeEmployees()[0]?.id ?? null;
    } catch (e) {
      this.error.set(errorMessage(e));
    }
  }

  private async refresh(): Promise<void> {
    try {
      const [period, novelties, sheet] = await Promise.all([
        this.api.period(this.periodId),
        this.api.novelties(this.periodId),
        this.api.sheet(this.periodId),
      ]);
      this.period.set(period);
      this.novelties.set(novelties);
      this.sheet.set(sheet);
    } catch (e) {
      this.error.set(errorMessage(e));
    }
  }

  protected usesQuantity(): boolean {
    return ['OVERTIME_SUPPLEMENTARY', 'OVERTIME_EXTRAORDINARY', 'ABSENCE_DAYS'].includes(this.novelty.noveltyType);
  }

  protected async addNovelty(): Promise<void> {
    const n = this.novelty;
    await this.run(() =>
      this.api.addNovelty(this.periodId, {
        employeeId: n.employeeId,
        noveltyType: n.noveltyType,
        quantity: this.usesQuantity() ? n.quantity : null,
        amount: this.usesQuantity() ? null : n.amount,
        description: n.description || null,
      }),
      'Novedad registrada. Recalcule el periodo para aplicarla.',
    );
  }

  protected async removeNovelty(n: Novelty): Promise<void> {
    await this.run(() => this.api.deleteNovelty(this.periodId, n.id), 'Novedad eliminada.');
  }

  protected async calculate(): Promise<void> {
    await this.run(() => this.api.calculate(this.periodId), 'Roles calculados con los parámetros vigentes.');
  }

  protected async close(): Promise<void> {
    if (!confirm('¿Cerrar el periodo? Los roles quedarán inmutables.')) {
      return;
    }
    await this.run(() => this.api.close(this.periodId), 'Periodo cerrado.');
  }

  protected async openPayslip(id: number): Promise<void> {
    try {
      this.payslip.set(await this.api.payslip(id));
    } catch (e) {
      this.error.set(errorMessage(e));
    }
  }

  protected linesOf(p: Payslip, type: string) {
    return p.lines.filter((l) => l.conceptType === type);
  }

  protected async downloadPayslip(id: number, employeeId: number): Promise<void> {
    const p = this.period();
    await this.safe(() => this.api.downloadPdf(`/payslips/${id}/pdf`, `rol-${p?.year}-${p?.month}-${employeeId}.pdf`));
  }

  protected async downloadSheet(): Promise<void> {
    const p = this.period();
    await this.safe(() => this.api.downloadPdf(`/periods/${this.periodId}/payroll.pdf`, `planilla-${p?.year}-${p?.month}.pdf`));
  }

  private async run(action: () => Promise<unknown>, success: string): Promise<void> {
    this.busy.set(true);
    this.error.set('');
    this.message.set('');
    try {
      await action();
      this.message.set(success);
      await this.refresh();
    } catch (e) {
      this.error.set(errorMessage(e));
    } finally {
      this.busy.set(false);
    }
  }

  private async safe(action: () => Promise<void>): Promise<void> {
    try {
      await action();
    } catch (e) {
      this.error.set(errorMessage(e));
    }
  }
}
