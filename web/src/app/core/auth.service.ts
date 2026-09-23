import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ConfigService } from './config.service';

export type Role = 'ADMIN' | 'PAYROLL' | 'VIEWER';

export interface Session {
  token: string;
  username: string;
  role: Role;
  expiresAt: number;
}

interface LoginResponse {
  token: string;
  expiresInSeconds: number;
  username: string;
  role: Role;
}

const STORAGE_KEY = 'nomina-ec.session';

/**
 * Short-lived JWT kept in sessionStorage (cleared when the tab closes). The API does not use
 * cookies, so there is no CSRF surface; the CSP and Angular's escaping mitigate XSS.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(ConfigService);
  private readonly router = inject(Router);

  private readonly session = signal<Session | null>(AuthService.restore());

  readonly user = computed(() => this.session());
  readonly isLoggedIn = computed(() => {
    const s = this.session();
    return s !== null && s.expiresAt > Date.now();
  });
  readonly canOperate = computed(() => ['ADMIN', 'PAYROLL'].includes(this.session()?.role ?? ''));
  readonly isAdmin = computed(() => this.session()?.role === 'ADMIN');

  get token(): string | null {
    const s = this.session();
    return s && s.expiresAt > Date.now() ? s.token : null;
  }

  async login(username: string, password: string): Promise<void> {
    const response = await firstValueFrom(
      this.http.post<LoginResponse>(`${this.config.apiUrl}/api/auth/login`, { username, password }),
    );
    const session: Session = {
      token: response.token,
      username: response.username,
      role: response.role,
      expiresAt: Date.now() + response.expiresInSeconds * 1000,
    };
    this.session.set(session);
    AuthService.persist(session);
  }

  logout(redirect = true): void {
    this.session.set(null);
    AuthService.persist(null);
    if (redirect) {
      void this.router.navigate(['/login']);
    }
  }

  private static restore(): Session | null {
    try {
      const raw = sessionStorage.getItem(STORAGE_KEY);
      if (!raw) {
        return null;
      }
      const session = JSON.parse(raw) as Session;
      return session.expiresAt > Date.now() ? session : null;
    } catch {
      return null;
    }
  }

  private static persist(session: Session | null): void {
    try {
      if (session) {
        sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
      } else {
        sessionStorage.removeItem(STORAGE_KEY);
      }
    } catch {
      // storage unavailable (private mode): keep the session in memory only
    }
  }
}
