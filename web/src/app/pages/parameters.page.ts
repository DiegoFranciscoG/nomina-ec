import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, errorMessage } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { formatMoney, formatParameter, label } from '../core/format';
import { AuditEntry, LegalParameter, TaxTables } from '../core/models';

type Tab = 'current' | 'audit' | 'tax';

@Component({
  selector: 'app-parameters',
  imports: [FormsModule],
  template: `
    <h1>Parámetros legales</h1>
    <p class="lead">Cada valor tiene vigencia (desde / hasta) y fuente oficial. Cambiar un valor crea una versión nueva: no hay que recompilar
      y los periodos cerrados no se alteran.</p>
    @if (error()) {
      <div class="alert error">{{ error() }}</div>
    }
    @if (message()) {
      <div class="alert success">{{ message() }}</div>
    }

    <div class="tabs" role="tablist">
      <button type="button" [class.active]="tab() === 'current'" (click)="tab.set('current')">Vigentes e historial</button>
      <button type="button" [class.active]="tab() === 'audit'" (click)="loadAudit(0)">Auditoría de cambios</button>
      <button type="button" [class.active]="tab() === 'tax'" (click)="loadTax()">Tabla IR y gastos personales</button>
    </div>

    @switch (tab()) {
      @case ('current') {
        @if (auth.isAdmin()) {
          <section class="card">
            <h2>Nueva versión de un parámetro</h2>
            <form class="form-grid" (ngSubmit)="save()">
              <label>Código
                <select name="code" [(ngModel)]="draft.code" (ngModelChange)="prefill()">
                  @for (c of codes(); track c) { <option [value]="c">{{ c }}</option> }
                </select>
              </label>
              <label>Valor (tasas en fracción: 0.0945)<input name="value" type="number" step="any" [(ngModel)]="draft.value" required /></label>
              <label>Vigente desde<input name="validFrom" type="date" [(ngModel)]="draft.validFrom" required /></label>
              <label>Base legal<input name="legalBasis" maxlength="255" [(ngModel)]="draft.legalBasis" required /></label>
              <label style="grid-column: span 2">URL de la fuente (https)<input name="sourceUrl" type="url" maxlength="500" [(ngModel)]="draft.sourceUrl" required /></label>
              <label style="grid-column: 1 / -1">Motivo del cambio (queda en la auditoría)<input name="reason" minlength="10" maxlength="500" [(ngModel)]="draft.reason" required /></label>
              <div class="row"><button type="submit" [disabled]="saving()">Registrar versión</button></div>
            </form>
          </section>
        }
        <section class="card table-wrap">
          <table>
            <thead><tr><th>Parámetro</th><th class="num">Valor</th><th>Desde</th><th>Hasta</th><th>Base legal / fuente</th><th>Estado</th></tr></thead>
            <tbody>
              @for (p of parameters(); track p.id) {
                <tr [style.opacity]="p.inForce ? 1 : 0.7">
                  <td><strong>{{ p.code }}</strong><div class="muted small">{{ p.description }}</div></td>
                  <td class="num"><strong>{{ fmt(p.code, p.value) }}</strong></td>
                  <td class="nowrap">{{ p.validFrom }}</td>
                  <td class="nowrap">{{ p.validTo ?? '—' }}</td>
                  <td class="small">{{ p.legalBasis }}<br /><a [href]="p.sourceUrl" target="_blank" rel="noopener noreferrer">fuente</a> · {{ p.createdBy }}</td>
                  <td>
                    @if (p.inForce) { <span class="badge in-force">Vigente</span> }
                    @else if (isFuture(p)) { <span class="badge CALCULATED">Futuro</span> }
                    @else { <span class="badge">Histórico</span> }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </section>
      }
      @case ('audit') {
        <section class="card table-wrap">
          <table>
            <thead><tr><th>Fecha</th><th>Usuario</th><th>Parámetro</th><th>Acción</th><th>Antes</th><th>Después</th><th>Motivo</th></tr></thead>
            <tbody>
              @for (a of audit(); track a.id) {
                <tr>
                  <td class="small">{{ a.changedAt.slice(0, 19).replace('T', ' ') }}</td>
                  <td>{{ a.changedBy }}</td>
                  <td><strong>{{ a.code }}</strong></td>
                  <td>{{ label(a.action) }}</td>
                  <td class="small">{{ describe(a.oldValue) }}</td>
                  <td class="small">{{ describe(a.newValue) }}</td>
                  <td class="small">{{ a.reason }}</td>
                </tr>
              } @empty {
                <tr><td colspan="7" class="muted">Aún no hay cambios registrados (los valores iniciales vienen de la migración con su fuente).</td></tr>
              }
            </tbody>
          </table>
          <div class="row" style="margin-top: 10px">
            <button type="button" class="ghost" [disabled]="auditPage() === 0" (click)="loadAudit(auditPage() - 1)">Anterior</button>
            <span class="small muted">Página {{ auditPage() + 1 }} de {{ auditPages() || 1 }}</span>
            <button type="button" class="ghost" [disabled]="auditPage() + 1 >= auditPages()" (click)="loadAudit(auditPage() + 1)">Siguiente</button>
          </div>
        </section>
      }
      @case ('tax') {
        @if (tax(); as t) {
          <div class="grid cols-2">
            <section class="card table-wrap">
              <h2>Impuesto a la renta {{ t.fiscalYear }} (personas naturales)</h2>
              <table>
                <thead><tr><th class="num">Fracción básica</th><th class="num">Exceso hasta</th><th class="num">Impuesto FB</th><th class="num">% excedente</th></tr></thead>
                <tbody>
                  @for (b of t.brackets; track b.lowerBound) {
                    <tr><td class="num">{{ money(b.lowerBound) }}</td><td class="num">{{ b.upperBound === null ? 'En adelante' : money(b.upperBound) }}</td>
                      <td class="num">{{ money(b.baseTax) }}</td><td class="num">{{ (b.marginalRate * 100).toFixed(0) }} %</td></tr>
                  }
                </tbody>
              </table>
              <p class="small muted">Fuente: SRI, Resolución NAC-DGERCGC25-00000043.</p>
            </section>
            <section class="card table-wrap">
              <h2>Gastos personales por cargas familiares</h2>
              <table>
                <thead><tr><th>Cargas</th><th class="num">Canastas</th><th class="num">Tope de gastos</th><th class="num">Rebaja máxima</th></tr></thead>
                <tbody>
                  @for (c of t.personalExpenseCaps; track c.familyDependents) {
                    <tr><td>{{ c.familyDependents === 5 ? '5 o más' : c.familyDependents }}</td><td class="num">{{ c.basketMultiplier }}</td>
                      <td class="num">{{ money(c.maxAmount) }}</td><td class="num">{{ money(c.maxRebate) }}</td></tr>
                  }
                </tbody>
              </table>
              <p class="small muted">Rebaja = 18 % del menor entre gastos proyectados y el tope. Canasta básica enero 2026 (INEC).</p>
            </section>
          </div>
        }
      }
    }
  `,
})
export class ParametersPage implements OnInit {
  private readonly api = inject(ApiService);
  protected readonly auth = inject(AuthService);

  protected readonly tab = signal<Tab>('current');
  protected readonly parameters = signal<LegalParameter[]>([]);
  protected readonly audit = signal<AuditEntry[]>([]);
  protected readonly auditPage = signal(0);
  protected readonly auditPages = signal(0);
  protected readonly tax = signal<TaxTables | null>(null);
  protected readonly saving = signal(false);
  protected readonly error = signal('');
  protected readonly message = signal('');
  protected readonly codes = computed(() => [...new Set(this.parameters().map((p) => p.code))].sort());

  protected draft = { code: 'SBU', value: 0, validFrom: '', legalBasis: '', sourceUrl: '', reason: '' };

  protected readonly fmt = formatParameter;
  protected readonly money = formatMoney;
  protected readonly label = label;
  private readonly today = new Date().toISOString().slice(0, 10);

  async ngOnInit(): Promise<void> {
    await this.reload();
    this.prefill();
  }

  private async reload(): Promise<void> {
    try {
      this.parameters.set(await this.api.parameters());
    } catch (e) {
      this.error.set(errorMessage(e));
    }
  }

  protected prefill(): void {
    const current = this.parameters().find((p) => p.code === this.draft.code && p.validTo === null);
    if (current) {
      this.draft = { ...this.draft, value: current.value, legalBasis: current.legalBasis, sourceUrl: current.sourceUrl };
    }
  }

  protected isFuture(p: LegalParameter): boolean {
    return p.validFrom > this.today;
  }

  protected describe(value: Record<string, string | null> | null): string {
    if (!value) {
      return '—';
    }
    return `${value['value']} (${value['validFrom']} → ${value['validTo'] ?? '∞'})`;
  }

  protected async save(): Promise<void> {
    this.saving.set(true);
    this.error.set('');
    this.message.set('');
    try {
      const created = await this.api.createParameterVersion(this.draft);
      this.message.set(`Versión registrada: ${created.code} = ${created.value} desde ${created.validFrom}. Auditoría actualizada.`);
      this.draft.reason = '';
      await this.reload();
    } catch (e) {
      this.error.set(errorMessage(e));
    } finally {
      this.saving.set(false);
    }
  }

  protected async loadAudit(page: number): Promise<void> {
    this.tab.set('audit');
    try {
      const result = await this.api.audit(page);
      this.audit.set(result.content);
      this.auditPage.set(result.number);
      this.auditPages.set(result.totalPages);
    } catch (e) {
      this.error.set(errorMessage(e));
    }
  }

  protected async loadTax(): Promise<void> {
    this.tab.set('tax');
    try {
      this.tax.set(await this.api.taxTables(new Date().getFullYear()));
    } catch (e) {
      this.error.set(errorMessage(e));
    }
  }
}
