import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { errorMessage } from '../core/api.service';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  template: `
    <div class="login-page">
      <div class="login-card">
        <h1>nomina-ec</h1>
        <p class="muted small">Rol de pagos ecuatoriano 2026 · parámetros legales versionados por fecha</p>
        @if (error()) {
          <div class="alert error" role="alert">{{ error() }}</div>
        }
        <form (ngSubmit)="submit()">
          <label>Usuario
            <input name="username" [(ngModel)]="username" autocomplete="username" required maxlength="60" />
          </label>
          <label>Contraseña
            <input name="password" type="password" [(ngModel)]="password" autocomplete="current-password" required maxlength="100" />
          </label>
          <button type="submit" [disabled]="loading() || !username || !password">
            {{ loading() ? 'Ingresando…' : 'Ingresar' }}
          </button>
        </form>
        <p class="muted small">Datos de demostración ficticios. El primer ingreso puede tardar ~1 min si el servidor gratuito está dormido.</p>
      </div>
    </div>
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected username = '';
  protected password = '';
  protected readonly loading = signal(false);
  protected readonly error = signal('');

  async submit(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      await this.auth.login(this.username.trim(), this.password);
      await this.router.navigate(['/dashboard']);
    } catch (e) {
      this.error.set(errorMessage(e));
    } finally {
      this.loading.set(false);
      this.password = '';
    }
  }
}
