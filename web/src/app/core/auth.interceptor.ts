import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { ConfigService } from './config.service';

/** Adds the Bearer token only to calls to our API and logs out on 401. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const config = inject(ConfigService);
  const token = auth.token;
  const isApi = req.url.startsWith(`${config.apiUrl}/api/`);
  const request = token && isApi ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && isApi && !req.url.endsWith('/api/auth/login')) {
        auth.logout();
      }
      return throwError(() => error);
    }),
  );
};
