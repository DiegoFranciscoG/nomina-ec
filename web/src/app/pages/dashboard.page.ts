import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService, errorMessage } from '../core/api.service';
import { formatMoney, label, MONTHS, periodLabel } from '../core/format';
import { Employee, LegalParameter, PayrollSheet } from '../core/models';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  template: `
    <h1>Panel</h1>
    <p class="lead">Resumen de la nómina del año con los parámetros legales vigentes.</p>
    @if (error()) {
      <div class="alert error">{{ error() }}</div>
    }

    <div class="grid cols-4">
      <div class="kpi"><small>Empleados activos</small><strong>{{ activeEmployees() }}</strong></div>
      <div class="kpi"><small>Neto a pagar · {{ lastLabel() }}</small><strong>{{ money(last()?.totals?.netPay) }}</strong></div>
      <div class="kpi"><small>Costo empleador · {{ lastLabel() }}</small><strong>{{ money(last()?.totals?.employerCost) }}</strong></div>
      <div class="kpi"><small>Costo empleador acumulado {{ year }}</small><strong>{{ money(yearCost()) }}</strong></div>
    </div>

    <div class="grid cols-2" style="margin-top: 18px">
      <section class="card">
        <h2>Costo mensual del empleador</h2>
        @if (loading()) {
          <p class="muted">Cargando planillas…</p>
        } @else if (sheets().length === 0) {
          <p class="muted">Aún no hay periodos calculados. <a routerLink="/periods">Crear periodo</a></p>
        } @else {
          <div class="bar-chart" role="img" [attr.aria-label]="'Costo del empleador por mes'">
            @for (s of sheets(); track s.period.id) {
              <div class="bar" [title]="money(s.totals.employerCost)">
                <small>{{ short(s.totals.employerCost) }}</small>
                <span class="fill" [style.height.%]="(s.totals.employerCost / maxCost()) * 85"></span>
                <span>{{ monthShort(s.period.month) }}</span>
              </div>
            }
          </div>
        }
      </section>

      <section class="card">
        <h2>Parámetros vigentes clave</h2>
        <table>
          <tbody>
            @for (p of keyParameters(); track p.id) {
              <tr>
                <td>{{ p.description }}<div class="muted small">{{ p.legalBasis }}</div></td>
                <td class="num"><strong>{{ p.code === 'SBU' || p.code === 'BASIC_FAMILY_BASKET' ? money(p.value) : pct(p.value) }}</strong></td>
              </tr>
            }
          </tbody>
        </table>
        <p class="small"><a routerLink="/parameters">Ver todos los parámetros, su historial y la auditoría →</a></p>
      </section>
    </div>

    @if (last(); as l) {
      <section class="card">
        <div class="row spread">
          <h2>Último periodo: {{ lastLabel() }} <span class="badge {{ l.period.status }}">{{ label(l.period.status) }}</span></h2>
          <a routerLink="/periods/{{ l.period.id }}">Abrir periodo →</a>
        </div>
        <div class="grid cols-4">
          <div><h3>Aporte personal IESS</h3>{{ money(l.totals.iessPersonal) }}</div>
          <div><h3>Aporte patronal IESS</h3>{{ money(l.totals.iessEmployer) }}</div>
          <div><h3>Retenciones IR</h3>{{ money(l.totals.incomeTax) }}</div>
          <div><h3>Provisiones</h3>{{ money(l.totals.provisions) }}</div>
        </div>
      </section>
    }
  `,
})
export class DashboardPage implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly year = new Date().getFullYear();
  protected readonly employees = signal<Employee[]>([]);
  protected readonly sheets = signal<PayrollSheet[]>([]);
  protected readonly parameters = signal<LegalParameter[]>([]);
  protected readonly error = signal('');
  protected readonly loading = signal(true);

  protected readonly activeEmployees = computed(() => this.employees().filter((e) => e.active).length);
  protected readonly last = computed(() => this.sheets().at(-1) ?? null);
  protected readonly lastLabel = computed(() => {
    const l = this.last();
    return l ? periodLabel(l.period.year, l.period.month) : '—';
  });
  protected readonly yearCost = computed(() => this.sheets().reduce((sum, s) => sum + s.totals.employerCost, 0));
  protected readonly maxCost = computed(() => Math.max(1, ...this.sheets().map((s) => s.totals.employerCost)));
  protected readonly keyParameters = computed(() => {
    const codes = ['SBU', 'IESS_PERSONAL_RATE', 'IESS_EMPLOYER_RATE', 'RESERVE_FUND_RATE', 'BASIC_FAMILY_BASKET', 'PERSONAL_EXPENSE_REBATE_RATE'];
    return codes
      .map((c) => this.parameters().find((p) => p.code === c && p.inForce))
      .filter((p): p is LegalParameter => !!p);
  });

  protected readonly money = formatMoney;
  protected readonly label = label;

  async ngOnInit(): Promise<void> {
    try {
      const [employees, periods, parameters] = await Promise.all([
        this.api.employees(),
        this.api.periods(),
        this.api.parameters(),
      ]);
      this.employees.set(employees);
      this.parameters.set(parameters);
      const calculated = periods
        .filter((p) => p.year === this.year && p.status !== 'OPEN')
        .sort((a, b) => a.month - b.month);
      this.sheets.set(await Promise.all(calculated.map((p) => this.api.sheet(p.id))));
    } catch (e) {
      this.error.set(errorMessage(e));
    } finally {
      this.loading.set(false);
    }
  }

  protected pct(value: number): string {
    return `${(value * 100).toLocaleString('es-EC', { maximumFractionDigits: 2 })} %`;
  }

  protected short(value: number): string {
    return `$${(value / 1000).toLocaleString('es-EC', { maximumFractionDigits: 1 })}k`;
  }

  protected monthShort(month: number): string {
    return MONTHS[month - 1].slice(0, 3);
  }
}
