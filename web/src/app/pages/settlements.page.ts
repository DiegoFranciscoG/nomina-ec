import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, errorMessage } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { formatMoney, label } from '../core/format';
import { Employee, Settlement, SettlementReason } from '../core/models';

@Component({
  selector: 'app-settlements',
  imports: [FormsModule],
  template: `
    <h1>Liquidación de haberes</h1>
    <p class="lead">Cálculo básico al terminar la relación laboral: décimos proporcionales, vacaciones, desahucio (Art. 185) e indemnización (Art. 188).</p>
    @if (error()) {
      <div class="alert error">{{ error() }}</div>
    }
    @if (message()) {
      <div class="alert success">{{ message() }}</div>
    }

    <section class="card">
      <form class="form-grid" (ngSubmit)="preview()">
        <label>Empleado
          <select name="employee" [(ngModel)]="request.employeeId" required>
            @for (e of activeEmployees(); track e.id) { <option [ngValue]="e.id">{{ e.lastNames }} {{ e.firstNames }}</option> }
          </select>
        </label>
        <label>Fecha de salida<input name="date" type="date" [(ngModel)]="request.terminationDate" required /></label>
        <label>Motivo
          <select name="reason" [(ngModel)]="request.reason">
            @for (r of reasons; track r) { <option [value]="r">{{ label(r) }}</option> }
          </select>
        </label>
        <label>Días de sueldo pendientes<input name="pending" type="number" min="0" max="30" [(ngModel)]="request.pendingSalaryDays" /></label>
        <label>Vacaciones no gozadas (días)<input name="vac" type="number" min="0" max="120" [(ngModel)]="request.unusedVacationDays" /></label>
        <div class="row" style="align-self: end"><button type="submit" [disabled]="busy()">Calcular</button></div>
      </form>
    </section>

    @if (result(); as s) {
      <section class="card">
        <div class="row spread">
          <h2>{{ s.employeeName }} · {{ label(s.reason) }}</h2>
          <div class="row">
            <button type="button" class="secondary" (click)="pdf()">PDF</button>
            @if (auth.isAdmin() && s.id === null) {
              <button type="button" class="danger" (click)="register()" [disabled]="busy()">Registrar y terminar contrato</button>
            }
          </div>
        </div>
        <p class="muted small">Ingreso {{ s.startDate }} · salida {{ s.terminationDate }} · {{ s.serviceDays }} días ({{ s.serviceYears }} años) ·
          última remuneración {{ money(s.lastSalary) }}</p>
        <table>
          <tbody>
            @for (i of s.items; track i.code) {
              <tr><td>{{ i.description }}</td><td class="num" [style.color]="i.amount < 0 ? 'var(--danger)' : null">{{ money(i.amount) }}</td></tr>
            }
          </tbody>
          <tfoot><tr><td>Total a pagar</td><td class="num">{{ money(s.netTotal) }}</td></tr></tfoot>
        </table>
      </section>
    }
  `,
})
export class SettlementsPage implements OnInit {
  private readonly api = inject(ApiService);
  protected readonly auth = inject(AuthService);

  protected readonly employees = signal<Employee[]>([]);
  protected readonly result = signal<Settlement | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly message = signal('');
  protected readonly activeEmployees = computed(() => this.employees().filter((e) => e.active));
  protected readonly reasons: SettlementReason[] = ['RESIGNATION', 'MUTUAL_AGREEMENT', 'UNJUSTIFIED_DISMISSAL', 'JUSTIFIED_DISMISSAL'];

  protected request = {
    employeeId: null as number | null,
    terminationDate: new Date().toISOString().slice(0, 10),
    reason: 'RESIGNATION' as SettlementReason,
    pendingSalaryDays: 0,
    unusedVacationDays: 0,
  };

  protected readonly money = formatMoney;
  protected readonly label = label;

  async ngOnInit(): Promise<void> {
    try {
      this.employees.set(await this.api.employees());
      this.request.employeeId = this.activeEmployees()[0]?.id ?? null;
    } catch (e) {
      this.error.set(errorMessage(e));
    }
  }

  protected async preview(): Promise<void> {
    await this.run(async () => this.result.set(await this.api.previewSettlement(this.request)));
  }

  protected async pdf(): Promise<void> {
    await this.run(() => this.api.downloadPdf('/settlements/preview.pdf', `liquidacion-${this.request.employeeId}.pdf`, this.request));
  }

  protected async register(): Promise<void> {
    if (!confirm('¿Registrar la liquidación? El contrato quedará terminado.')) {
      return;
    }
    await this.run(async () => {
      this.result.set(await this.api.registerSettlement(this.request));
      this.message.set('Liquidación registrada y contrato terminado.');
      this.employees.set(await this.api.employees());
    });
  }

  private async run(action: () => Promise<unknown>): Promise<void> {
    this.busy.set(true);
    this.error.set('');
    try {
      await action();
    } catch (e) {
      this.error.set(errorMessage(e));
    } finally {
      this.busy.set(false);
    }
  }
}
