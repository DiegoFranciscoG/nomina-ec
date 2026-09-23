import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="shell" [class.nav-open]="navOpen()">
      <aside class="sidebar">
        <div class="brand">
          <span class="brand-mark">₡</span>
          <div>
            <strong>nomina-ec</strong>
            <small>Rol de pagos 2026</small>
          </div>
        </div>
        <nav (click)="navOpen.set(false)">
          @for (item of nav; track item.path) {
            <a [routerLink]="item.path" routerLinkActive="active">{{ item.label }}</a>
          }
        </nav>
        <div class="user">
          <div>
            <strong>{{ auth.user()?.username }}</strong>
            <small>{{ roleLabel() }}</small>
          </div>
          <button class="ghost" type="button" (click)="auth.logout()">Salir</button>
        </div>
      </aside>
      <div class="content">
        <header class="topbar">
          <button class="ghost menu" type="button" aria-label="Menú" (click)="navOpen.set(!navOpen())">☰</button>
          <span>Parámetros legales versionados · SBU, IESS y SRI 2026</span>
        </header>
        <main>
          <router-outlet />
        </main>
      </div>
    </div>
  `,
})
export class ShellComponent {
  protected readonly auth = inject(AuthService);
  protected readonly navOpen = signal(false);
  protected readonly nav = [
    { path: '/dashboard', label: 'Panel' },
    { path: '/employees', label: 'Personal' },
    { path: '/periods', label: 'Nómina' },
    { path: '/parameters', label: 'Parámetros legales' },
    { path: '/simulator', label: 'Simulador de contratación' },
    { path: '/settlements', label: 'Liquidaciones' },
  ];

  protected roleLabel(): string {
    const labels: Record<string, string> = { ADMIN: 'Administrador', PAYROLL: 'Analista de nómina', VIEWER: 'Consulta' };
    return labels[this.auth.user()?.role ?? ''] ?? '';
  }
}
