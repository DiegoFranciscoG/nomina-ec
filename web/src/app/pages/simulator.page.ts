import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, errorMessage } from '../core/api.service';
import { formatMoney, formatPercent } from '../core/format';
import { HiringCost, HiringScenario, PaymentMode } from '../core/models';

interface Slice {
  key: keyof HiringScenario;
  label: string;
  color: string;
}

@Component({
  selector: 'app-simulator',
  imports: [FormsModule],
  template: `
    <h1>¿Cuánto cuesta contratar a alguien?</h1>
    <p class="lead">Costo real mensual y anual para el empleador con los parámetros vigentes: aportes patronales, décimos, vacaciones y fondos de reserva.</p>
    @if (error()) {
      <div class="alert error">{{ error() }}</div>
    }

    <section class="card">
      <form class="form-grid" (ngSubmit)="simulate()">
        <label>Sueldo mensual (USD)<input name="salary" type="number" min="1" step="0.01" [(ngModel)]="salary" required /></label>
        <label>Horas semanales<input name="hours" type="number" min="1" max="40" [(ngModel)]="weeklyHours" /></label>
        <label>Décimo tercero
          <select name="t" [(ngModel)]="thirteenthMode"><option value="MONTHLY">Mensualizado</option><option value="ACCUMULATED">Acumulado</option></select>
        </label>
        <label>Décimo cuarto
          <select name="f" [(ngModel)]="fourteenthMode"><option value="MONTHLY">Mensualizado</option><option value="ACCUMULATED">Acumulado</option></select>
        </label>
        <div class="row" style="align-self: end"><button type="submit" [disabled]="loading()">Calcular</button></div>
      </form>
    </section>

    @if (result(); as r) {
      @if (r.belowMinimumWage) {
        <div class="alert error">El sueldo es menor al SBU vigente ({{ money(r.sbu) }}) proporcional a la jornada.</div>
      }
      <div class="grid cols-2">
        @for (s of scenarios(); track s.title) {
          <section class="card">
            <h2>{{ s.title }}</h2>
            <div class="grid cols-2">
              <div class="kpi"><small>Costo mensual</small><strong>{{ money(s.data.monthlyCost) }}</strong></div>
              <div class="kpi"><small>Costo anual</small><strong>{{ money(s.data.annualCost) }}</strong></div>
            </div>
            <p class="small muted" style="margin: 10px 0">Cada dólar de sueldo cuesta {{ money(s.data.costOverSalary) }} ({{ pct(s.data.costOverSalary - 1) }} adicional).</p>
            <div class="stack" aria-hidden="true">
              @for (slice of slices; track slice.key) {
                <span [style.width.%]="share(s.data, slice.key)" [style.background]="slice.color"></span>
              }
            </div>
            <div class="legend">
              @for (slice of slices; track slice.key) {
                <span><i [style.background]="slice.color"></i>{{ slice.label }} {{ money(value(s.data, slice.key)) }}</span>
              }
            </div>
            <h3 style="margin-top: 14px">El trabajador recibe</h3>
            <table>
              <tbody>
                <tr><td>Aporte personal IESS</td><td class="num">−{{ money(s.data.employeeIess) }}</td></tr>
                <tr><td>Retención IR (sin gastos personales)</td><td class="num">−{{ money(s.data.employeeIncomeTax) }}</td></tr>
                <tr><td><strong>Neto mensual en el rol</strong></td><td class="num"><strong>{{ money(s.data.employeeNetMonthly) }}</strong></td></tr>
              </tbody>
            </table>
          </section>
        }
      </div>
    }
  `,
})
export class SimulatorPage implements OnInit {
  private readonly api = inject(ApiService);

  protected salary = 800;
  protected weeklyHours = 40;
  protected thirteenthMode: PaymentMode = 'MONTHLY';
  protected fourteenthMode: PaymentMode = 'MONTHLY';
  protected readonly result = signal<HiringCost | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal('');

  protected readonly slices: Slice[] = [
    { key: 'salary', label: 'Sueldo', color: '#1f4e79' },
    { key: 'employerIess', label: 'IESS patronal', color: '#2e75b6' },
    { key: 'iece', label: 'IECE', color: '#6fa8dc' },
    { key: 'secap', label: 'SECAP', color: '#9fc5e8' },
    { key: 'thirteenth', label: '13.º', color: '#0f8a5f' },
    { key: 'fourteenth', label: '14.º', color: '#5cb88a' },
    { key: 'vacation', label: 'Vacaciones', color: '#b7791f' },
    { key: 'reserveFund', label: 'Fondo de reserva', color: '#c0392b' },
  ];

  protected readonly scenarios = computed(() => {
    const r = this.result();
    return r
      ? [
          { title: 'Primer año', data: r.firstYear },
          { title: 'Desde el segundo año (con fondos de reserva)', data: r.fromSecondYear },
        ]
      : [];
  });

  protected readonly money = formatMoney;
  protected readonly pct = formatPercent;

  async ngOnInit(): Promise<void> {
    await this.simulate();
  }

  protected value(s: HiringScenario, key: keyof HiringScenario): number {
    return s[key];
  }

  protected share(s: HiringScenario, key: keyof HiringScenario): number {
    return (s[key] / s.monthlyCost) * 100;
  }

  protected async simulate(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      this.result.set(
        await this.api.hiringCost({
          monthlySalary: this.salary,
          weeklyHours: this.weeklyHours,
          thirteenthMode: this.thirteenthMode,
          fourteenthMode: this.fourteenthMode,
        }),
      );
    } catch (e) {
      this.error.set(errorMessage(e));
    } finally {
      this.loading.set(false);
    }
  }
}
