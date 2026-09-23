import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { HttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { firstValueFrom } from 'rxjs';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { errorMessage } from './api.service';
import { HttpErrorResponse } from '@angular/common/http';

describe('AuthService and interceptor', () => {
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([{ path: 'login', children: [] }]), provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('stores the session after login and exposes the role', async () => {
    const login = auth.login('admin', 'secret');
    http.expectOne('/api/auth/login').flush({ token: 'jwt-token', expiresInSeconds: 1800, username: 'admin', role: 'ADMIN' });
    await login;

    expect(auth.isLoggedIn()).toBe(true);
    expect(auth.isAdmin()).toBe(true);
    expect(auth.canOperate()).toBe(true);
    expect(auth.token).toBe('jwt-token');
  });

  it('adds the bearer token only to API calls', async () => {
    const login = auth.login('demo', 'secret');
    http.expectOne('/api/auth/login').flush({ token: 'abc', expiresInSeconds: 60, username: 'demo', role: 'PAYROLL' });
    await login;
    const client = TestBed.inject(HttpClient);

    const apiCall = firstValueFrom(client.get('/api/employees'));
    const apiRequest = http.expectOne('/api/employees');
    expect(apiRequest.request.headers.get('Authorization')).toBe('Bearer abc');
    apiRequest.flush([]);
    await apiCall;

    const external = firstValueFrom(client.get('https://example.org/data'));
    const externalRequest = http.expectOne('https://example.org/data');
    expect(externalRequest.request.headers.has('Authorization')).toBe(false);
    externalRequest.flush({});
    await external;
    expect(auth.isAdmin()).toBe(false);
  });

  it('logs out when the API answers 401', async () => {
    const login = auth.login('demo', 'secret');
    http.expectOne('/api/auth/login').flush({ token: 'abc', expiresInSeconds: 60, username: 'demo', role: 'PAYROLL' });
    await login;

    const call = firstValueFrom(TestBed.inject(HttpClient).get('/api/periods')).catch(() => null);
    http.expectOne('/api/periods').flush({ detail: 'Autenticación requerida' }, { status: 401, statusText: 'Unauthorized' });
    await call;
    expect(auth.isLoggedIn()).toBe(false);
  });

  it('turns problem details into readable messages', () => {
    const validation = new HttpErrorResponse({ status: 400, error: { errors: { idNumber: 'cédula ecuatoriana inválida' } } });
    expect(errorMessage(validation)).toBe('idNumber: cédula ecuatoriana inválida');
    const rule = new HttpErrorResponse({ status: 422, error: { detail: 'Las horas suplementarias superan el máximo' } });
    expect(errorMessage(rule)).toContain('horas suplementarias');
    expect(errorMessage(new HttpErrorResponse({ status: 0 }))).toContain('No se pudo conectar');
  });
});
