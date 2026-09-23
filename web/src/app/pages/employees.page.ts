import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, errorMessage } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { formatMoney, label } from '../core/format';
import { BenefitBalance, ContractType, Employee, PaymentMode, Position, Region } from '../core/models';

interface EmployeeForm {
  idNumber: string;
  firstNames: string;
  lastNames: string;
  email: string;
  familyDependents: number;
  catastrophicCondition: boolean;
  positionId: number | null;
  contractType: ContractType;
  weeklyHours: number;
  monthlySalary: number;
  startDate: string;
  region: Region;
  thirteenthMode: PaymentMode;
  fourteenthMode: PaymentMode;
  reserveFundMode: PaymentMode;
}

@Component({
  selector: 'app-employees',
  imports: [FormsModule],
  template: `
    <div class="row spread">
      <div>
        <h1>Personal</h1>
        <p class="lead">Empleados, contratos y modalidad de pago de décimos y fondos de reserva.</p>
      </div>
      @if (auth.canOperate()) {
        <button type="button" (click)="startCreate()">+ Nuevo empleado</button>
      }
    </div>
    @if (error()) {
      <div class="alert error">{{ error() }}</div>
    }
    @if (message()) {
      <div class="alert success">{{ message() }}</div>
    }

    <section class="card table-wrap">
      <table>
        <thead>
          <tr>
            <th>Empleado</th><th>Cédula</th><th>Cargo</th><th>Región</th><th class="num">Sueldo</th>
            <th>13.º</th><th>14.º</th><th>F. reserva</th><th>Estado</th>
          </tr>
        </thead>
        <tbody>
          @for (e of employees(); track e.id) {
            <tr class="clickable" (click)="open(e)">
              <td><strong>{{ e.lastNames }}</strong> {{ e.firstNames }}<div class="muted small">{{ e.email }}</div></td>
              <td>{{ e.idNumberMasked }}</td>
              <td>{{ e.contract?.positionName }}</td>
              <td>{{ label(e.contract?.region) }}</td>
              <td class="num">{{ money(e.contract?.monthlySalary) }}</td>
              <td>{{ label(e.contract?.thirteenthMode) }}</td>
              <td>{{ label(e.contract?.fourteenthMode) }}</td>
              <td>{{ label(e.contract?.reserveFundMode) }}</td>
              <td><span class="badge" [class.CLOSED]="e.active">{{ e.active ? 'Activo' : 'Salida' }}</span></td>
            </tr>
          } @empty {
            <tr><td colspan="9" class="muted">Sin empleados registrados.</td></tr>
          }
        </tbody>
      </table>
    </section>

    @if (creating()) {
      <div class="drawer-backdrop" (click)="creating.set(false)"></div>
      <aside class="drawer" aria-label="Nuevo empleado">
        <h2>Nuevo empleado</h2>
        <p class="muted small">Use solo datos ficticios en la demo. La cédula se valida con el dígito verificador y se muestra enmascarada.</p>
        <form (ngSubmit)="create()" class="form-grid">
          <label>Cédula<input name="idNumber" [(ngModel)]="form.idNumber" maxlength="10" pattern="\\d{10}" required /></label>
          <label>Nombres<input name="firstNames" [(ngModel)]="form.firstNames" maxlength="80" required /></label>
          <label>Apellidos<input name="lastNames" [(ngModel)]="form.lastNames" maxlength="80" required /></label>
          <label>Correo<input name="email" type="email" [(ngModel)]="form.email" maxlength="120" /></label>
          <label>Cargas familiares<input name="familyDependents" type="number" min="0" max="20" [(ngModel)]="form.familyDependents" /></label>
          <label class="check"><input name="catastrophic" type="checkbox" [(ngModel)]="form.catastrophicCondition" /> Discapacidad / enfermedad catastrófica</label>
          <label>Cargo
            <select name="positionId" [(ngModel)]="form.positionId" required>
              @for (p of positions(); track p.id) { <option [ngValue]="p.id">{{ p.name }}</option> }
            </select>
          </label>
          <label>Tipo de contrato
            <select name="contractType" [(ngModel)]="form.contractType">
              @for (t of contractTypes; track t) { <option [value]="t">{{ label(t) }}</option> }
            </select>
          </label>
          <label>Horas semanales<input name="weeklyHours" type="number" min="1" max="40" [(ngModel)]="form.weeklyHours" /></label>
          <label>Sueldo mensual (USD)<input name="monthlySalary" type="number" min="1" step="0.01" [(ngModel)]="form.monthlySalary" required /></label>
          <label>Fecha de ingreso<input name="startDate" type="date" [(ngModel)]="form.startDate" required /></label>
          <label>Región (décimo cuarto)
            <select name="region" [(ngModel)]="form.region">
              <option value="SIERRA_AMAZONIA">Sierra / Amazonía</option>
              <option value="COSTA_GALAPAGOS">Costa / Galápagos</option>
            </select>
          </label>
          <label>Décimo tercero
            <select name="thirteenthMode" [(ngModel)]="form.thirteenthMode">
              <option value="MONTHLY">Mensualizado</option><option value="ACCUMULATED">Acumulado</option>
            </select>
          </label>
          <label>Décimo cuarto
            <select name="fourteenthMode" [(ngModel)]="form.fourteenthMode">
              <option value="MONTHLY">Mensualizado</option><option value="ACCUMULATED">Acumulado</option>
            </select>
          </label>
          <label>Fondos de reserva
            <select name="reserveFundMode" [(ngModel)]="form.reserveFundMode">
              <option value="MONTHLY">Mensualizado</option><option value="ACCUMULATED">Acumulado en IESS</option>
            </select>
          </label>
          <div class="row" style="grid-column: 1 / -1">
            <button type="submit" [disabled]="saving()">Guardar</button>
            <button type="button" class="ghost" (click)="creating.set(false)">Cancelar</button>
          </div>
        </form>
      </aside>
    }

    @if (selected(); as e) {
      <div class="drawer-backdrop" (click)="selected.set(null)"></div>
      <aside class="drawer" aria-label="Detalle del empleado">
        <div class="row spread">
          <h2>{{ e.lastNames }} {{ e.firstNames }}</h2>
          <button type="button" class="ghost" (click)="selected.set(null)">Cerrar</button>
        </div>
        <p class="muted small">{{ e.contract?.positionName }} · ingreso {{ e.contract?.startDate }} · {{ label(e.contract?.contractType) }} ·
          {{ e.contract?.weeklyHours }} h/semana · {{ e.familyDependents }} cargas</p>

        @if (balance(); as b) {
          <h3>Décimos y fondos de reserva {{ year }}</h3>
          <table>
            <thead><tr><th>Beneficio</th><th class="num">Pagado en rol</th><th class="num">Acumulado (provisión)</th></tr></thead>
            <tbody>
              <tr><td>Décimo tercero</td><td class="num">{{ money(b.thirteenthPaid) }}</td><td class="num">{{ money(b.thirteenthAccumulated) }}</td></tr>
              <tr><td>Décimo cuarto</td><td class="num">{{ money(b.fourteenthPaid) }}</td><td class="num">{{ money(b.fourteenthAccumulated) }}</td></tr>
              <tr><td>Fondos de reserva</td><td class="num">{{ money(b.reserveFundPaid) }}</td><td class="num">{{ money(b.reserveFundAccumulated) }}</td></tr>
              <tr><td>Provisión de vacaciones</td><td class="num">—</td><td class="num">{{ money(b.vacationProvision) }}</td></tr>
            </tbody>
          </table>
        }

        @if (auth.canOperate() && e.active && e.contract) {
          <h3 style="margin-top: 18px">Contrato</h3>
          <form class="form-grid" (ngSubmit)="saveContract(e)">
            <label>Sueldo (USD)<input name="salary" type="number" step="0.01" [(ngModel)]="contractEdit.monthlySalary" /></label>
            <label>Horas semanales<input name="hours" type="number" min="1" max="40" [(ngModel)]="contractEdit.weeklyHours" /></label>
            <label>13.º<select name="t" [(ngModel)]="contractEdit.thirteenthMode"><option value="MONTHLY">Mensualizado</option><option value="ACCUMULATED">Acumulado</option></select></label>
            <label>14.º<select name="f" [(ngModel)]="contractEdit.fourteenthMode"><option value="MONTHLY">Mensualizado</option><option value="ACCUMULATED">Acumulado</option></select></label>
            <label>F. reserva<select name="r" [(ngModel)]="contractEdit.reserveFundMode"><option value="MONTHLY">Mensualizado</option><option value="ACCUMULATED">Acumulado</option></select></label>
            <div class="row" style="align-self: end"><button type="submit" [disabled]="saving()">Actualizar contrato</button></div>
          </form>

          <h3 style="margin-top: 18px">Proyección de gastos personales {{ year }} (SRI)</h3>
          <form class="row" (ngSubmit)="saveExpenses(e)">
            <label>Monto anual proyectado (USD)<input name="gp" type="number" min="0" step="0.01" [(ngModel)]="expenses" /></label>
            <button type="submit" style="align-self: end" [disabled]="saving()">Guardar</button>
          </form>
          <p class="muted small">Rebaja del IR = 18 % del menor entre este monto y el límite en canastas básicas según cargas familiares.</p>
        }
      </aside>
    }
  `,
})
export class EmployeesPage implements OnInit {
  private readonly api = inject(ApiService);
  protected readonly auth = inject(AuthService);

  protected readonly year = new Date().getFullYear();
  protected readonly employees = signal<Employee[]>([]);
  protected readonly positions = signal<Position[]>([]);
  protected readonly creating = signal(false);
  protected readonly selected = signal<Employee | null>(null);
  protected readonly balance = signal<BenefitBalance | null>(null);
  protected readonly saving = signal(false);
  protected readonly error = signal('');
  protected readonly message = signal('');
  protected readonly contractTypes: ContractType[] = ['INDEFINITE', 'EVENTUAL', 'OCCASIONAL', 'SEASONAL', 'PER_PROJECT'];

  protected form: EmployeeForm = EmployeesPage.emptyForm();
  protected contractEdit = { monthlySalary: 0, weeklyHours: 40, thirteenthMode: 'MONTHLY', fourteenthMode: 'MONTHLY', reserveFundMode: 'MONTHLY' };
  protected expenses = 0;

  protected readonly money = formatMoney;
  protected readonly label = label;

  async ngOnInit(): Promise<void> {
    await this.reload();
    try {
      this.positions.set(await this.api.positions());
    } catch (e) {
      this.error.set(errorMessage(e));
    }
  }

  private async reload(): Promise<void> {
    try {
      this.employees.set(await this.api.employees());
    } catch (e) {
      this.error.set(errorMessage(e));
    }
  }

  protected startCreate(): void {
    this.form = EmployeesPage.emptyForm();
    this.form.positionId = this.positions()[0]?.id ?? null;
    this.creating.set(true);
  }

  protected async create(): Promise<void> {
    this.saving.set(true);
    this.error.set('');
    const f = this.form;
    try {
      await this.api.createEmployee({
        idNumber: f.idNumber,
        firstNames: f.firstNames,
        lastNames: f.lastNames,
        email: f.email || null,
        familyDependents: f.familyDependents,
        catastrophicCondition: f.catastrophicCondition,
        contract: {
          positionId: f.positionId,
          contractType: f.contractType,
          weeklyHours: f.weeklyHours,
          monthlySalary: f.monthlySalary,
          startDate: f.startDate,
          region: f.region,
          thirteenthMode: f.thirteenthMode,
          fourteenthMode: f.fourteenthMode,
          reserveFundMode: f.reserveFundMode,
        },
      });
      this.creating.set(false);
      this.message.set('Empleado registrado.');
      await this.reload();
    } catch (e) {
      this.error.set(errorMessage(e));
    } finally {
      this.saving.set(false);
    }
  }

  protected async open(e: Employee): Promise<void> {
    this.selected.set(e);
    this.balance.set(null);
    if (e.contract) {
      this.contractEdit = {
        monthlySalary: e.contract.monthlySalary,
        weeklyHours: e.contract.weeklyHours,
        thirteenthMode: e.contract.thirteenthMode,
        fourteenthMode: e.contract.fourteenthMode,
        reserveFundMode: e.contract.reserveFundMode,
      };
    }
    try {
      const [balance, expenses] = await Promise.all([this.api.benefits(e.id, this.year), this.api.personalExpenses(e.id, this.year)]);
      this.balance.set(balance);
      this.expenses = expenses;
    } catch (err) {
      this.error.set(errorMessage(err));
    }
  }

  protected async saveContract(e: Employee): Promise<void> {
    await this.run(async () => {
      const updated = await this.api.updateContract(e.id, this.contractEdit);
      this.selected.set(updated);
      this.message.set('Contrato actualizado. Recalcule los periodos abiertos para aplicarlo.');
    });
  }

  protected async saveExpenses(e: Employee): Promise<void> {
    await this.run(async () => {
      await this.api.savePersonalExpenses(e.id, this.year, this.expenses);
      this.message.set('Proyección de gastos personales guardada.');
    });
  }

  private async run(action: () => Promise<void>): Promise<void> {
    this.saving.set(true);
    this.error.set('');
    this.message.set('');
    try {
      await action();
      await this.reload();
    } catch (e) {
      this.error.set(errorMessage(e));
    } finally {
      this.saving.set(false);
    }
  }

  private static emptyForm(): EmployeeForm {
    return {
      idNumber: '',
      firstNames: '',
      lastNames: '',
      email: '',
      familyDependents: 0,
      catastrophicCondition: false,
      positionId: null,
      contractType: 'INDEFINITE',
      weeklyHours: 40,
      monthlySalary: 482,
      startDate: new Date().toISOString().slice(0, 10),
      region: 'SIERRA_AMAZONIA',
      thirteenthMode: 'MONTHLY',
      fourteenthMode: 'MONTHLY',
      reserveFundMode: 'MONTHLY',
    };
  }
}
