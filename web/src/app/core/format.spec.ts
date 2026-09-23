import { describe, expect, it } from 'vitest';
import { formatMoney, formatParameter, label, periodLabel } from './format';

describe('format', () => {
  it('formats money in US dollars with two decimals', () => {
    expect(formatMoney(1300.27)).toContain('1.300,27');
    expect(formatMoney(null)).toContain('0,00');
  });

  it('shows rates as percentages and SBU as money', () => {
    expect(formatParameter('IESS_PERSONAL_RATE', 0.0945)).toContain('9,45');
    expect(formatParameter('SBU', 482)).toContain('482,00');
    expect(formatParameter('MONTHLY_HOURS_BASE', 240)).toBe('240');
  });

  it('builds period labels and translates enum keys', () => {
    expect(periodLabel(2026, 9)).toBe('Septiembre 2026');
    expect(label('ACCUMULATED')).toBe('Acumulado');
    expect(label('UNKNOWN_KEY')).toBe('UNKNOWN_KEY');
    expect(label(null)).toBe('');
  });
});
