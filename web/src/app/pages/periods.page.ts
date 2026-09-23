import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService, errorMessage } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { label, MONTHS, periodLabel } from '../core/format';
import { Period } from '../core/models';

@Component({
  selector: 'app-periods',
  imports: [FormsModule],
  template: `
    <h1>Nómina</h1>
    <p class="lead">Periodos mensuales: registre novedades, calcule, revise la planilla y cierre. Un periodo cerrado es inmutable.</p>
    @if (error()) {
      <div class="alert error">{{ error() }}</div>
    }

    @if (auth.canOperate()) {
      <section class="card">
        <form class="row" (ngSubmit)="create()">
          <label>Año<input name="year" type="number" min="2026" max="2100" [(ngModel)]="year" /></label>
          <label>Mes
            <select name="month" [(ngModel)]="month">
              @for (m of months; track $index) { <option [ngValue]="$index + 1">{{ m }}</option> }
            </select>
          </label>
          <button type="submit" style="align-self: end" [disabled]="saving()">Crear periodo</button>
        </form>
      </section>
    }

    <section class="card table-wrap">
      <table>
        <thead><tr><th>Periodo</th><th>Estado</th><th class="num">Roles</th><th>Calculado</th><th>Cerrado por</th></tr></thead>
        <tbody>
          @for (p of periods(); track p.id) {
            <tr class="clickable" (click)="openPeriod(p)">
              <td><strong>{{ name(p) }}</strong></td>
              <td><span class="badge {{ p.status }}">{{ label(p.status) }}</span></td>
              <td class="num">{{ p.payslipCount }}</td>
              <td class="small">{{ p.calculatedAt ? (p.calculatedAt.slice(0, 16).replace('T', ' ')) : '—' }}</td>
              <td class="small">{{ p.closedBy ?? '—' }}</td>
            </tr>
          } @empty {
            <tr><td colspan="5" class="muted">No hay periodos.</td></tr>
          }
        </tbody>
      </table>
    </section>
  `,
})
export class PeriodsPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);

  protected readonly periods = signal<Period[]>([]);
  protected readonly saving = signal(false);
  protected readonly error = signal('');
  protected readonly months = MONTHS;
  protected year = new Date().getFullYear();
  protected month = new Date().getMonth() + 1;
  protected readonly label = label;

  async ngOnInit(): Promise<void> {
    try {
      this.periods.set(await this.api.periods());
    } catch (e) {
      this.error.set(errorMessage(e));
    }
  }

  protected name(p: Period): string {
    return periodLabel(p.year, p.month);
  }

  protected openPeriod(p: Period): void {
    void this.router.navigate(['/periods', p.id]);
  }

  protected async create(): Promise<void> {
    this.saving.set(true);
    this.error.set('');
    try {
      const period = await this.api.createPeriod(this.year, this.month);
      this.openPeriod(period);
    } catch (e) {
      this.error.set(errorMessage(e));
    } finally {
      this.saving.set(false);
    }
  }
}
