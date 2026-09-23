import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { ShellComponent } from './layout/shell.component';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login.page').then((m) => m.LoginPage), title: 'Ingresar · nomina-ec' },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', loadComponent: () => import('./pages/dashboard.page').then((m) => m.DashboardPage), title: 'Panel · nomina-ec' },
      { path: 'employees', loadComponent: () => import('./pages/employees.page').then((m) => m.EmployeesPage), title: 'Personal · nomina-ec' },
      { path: 'periods', loadComponent: () => import('./pages/periods.page').then((m) => m.PeriodsPage), title: 'Nómina · nomina-ec' },
      { path: 'periods/:id', loadComponent: () => import('./pages/period-detail.page').then((m) => m.PeriodDetailPage), title: 'Periodo · nomina-ec' },
      { path: 'parameters', loadComponent: () => import('./pages/parameters.page').then((m) => m.ParametersPage), title: 'Parámetros legales · nomina-ec' },
      { path: 'simulator', loadComponent: () => import('./pages/simulator.page').then((m) => m.SimulatorPage), title: 'Simulador · nomina-ec' },
      { path: 'settlements', loadComponent: () => import('./pages/settlements.page').then((m) => m.SettlementsPage), title: 'Liquidaciones · nomina-ec' },
    ],
  },
  { path: '**', redirectTo: '' },
];
