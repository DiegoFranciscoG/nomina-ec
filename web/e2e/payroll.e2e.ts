import { expect, test } from '@playwright/test';

const user = process.env['E2E_USER'] ?? 'demo';
const password = process.env['E2E_PASSWORD'] ?? '';
const screenshots = process.env['E2E_SCREENSHOTS'];

test.beforeEach(async ({ page }) => {
  test.skip(!password, 'Define E2E_PASSWORD con la contraseña del usuario demo');
  await page.goto('/login');
  await page.getByLabel('Usuario').fill(user);
  await page.getByLabel('Contraseña').fill(password);
  await page.getByRole('button', { name: 'Ingresar' }).click();
  await expect(page.getByRole('heading', { name: 'Panel' })).toBeVisible();
});

test('dashboard shows the year payroll and key legal parameters', async ({ page }) => {
  await expect(page.getByText('Costo mensual del empleador')).toBeVisible();
  await expect(page.locator('.bar-chart .bar').first()).toBeVisible();
  await expect(page.getByText('Salario básico unificado mensual (USD)')).toBeVisible();
  if (screenshots) {
    await page.screenshot({ path: `${screenshots}/dashboard.png` });
  }
});

test('payroll sheet of the open period lists payslips and opens a payslip', async ({ page }) => {
  await page.getByRole('link', { name: 'Nómina' }).click();
  await page.locator('tbody tr').first().click();
  await expect(page.getByRole('heading', { name: 'Planilla del periodo' })).toBeVisible();
  await expect(page.locator('tfoot')).toContainText('Totales');
  if (screenshots) {
    await page.screenshot({ path: `${screenshots}/planilla.png`, fullPage: true });
  }
  await page.locator('section.card tbody tr.clickable').first().click();
  await expect(page.getByText('Neto a recibir')).toBeVisible();
  if (screenshots) {
    await page.screenshot({ path: `${screenshots}/rol.png` });
  }
});

test('hiring cost simulator returns first and second year costs', async ({ page }) => {
  await page.getByRole('link', { name: 'Simulador de contratación' }).click();
  await page.getByLabel('Sueldo mensual (USD)').fill('482');
  await page.getByRole('button', { name: 'Calcular' }).click();
  await expect(page.getByRole('heading', { name: 'Primer año' })).toBeVisible();
  await expect(page.locator('.kpi').first()).toContainText('640,98');
  if (screenshots) {
    await page.screenshot({ path: `${screenshots}/simulador.png`, fullPage: true });
  }
});

test('legal parameters show validity, source and audit tab', async ({ page }) => {
  await page.getByRole('link', { name: 'Parámetros legales' }).click();
  await expect(page.getByRole('cell', { name: /^SBU/ })).toBeVisible();
  if (screenshots) {
    await page.screenshot({ path: `${screenshots}/parametros.png` });
  }
  await page.getByRole('button', { name: 'Tabla IR y gastos personales' }).click();
  await expect(page.getByText('Impuesto a la renta 2026')).toBeVisible();
});
