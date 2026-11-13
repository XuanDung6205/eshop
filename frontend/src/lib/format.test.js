import { expect, it } from 'vitest';
import { formatMoney, toLocalInput, toUtc } from './format';
it('formats large decimal strings without floating point rounding', () => {
  expect(formatMoney('12345678901234567890.12')).toBe('12.345.678.901.234.567.890,12 ₫');
  expect(formatMoney('1250000.00')).toBe('1.250.000 ₫');
});
it('round trips UTC instants through local datetime inputs without dropping seconds or milliseconds', () => {
  expect(toUtc(toLocalInput('2026-11-13T08:15:31.125Z'))).toBe('2026-11-13T08:15:31.125Z');
});
it('preserves the original UTC representation when an edited local datetime has not changed', () => {
  expect(toUtc(toLocalInput('2026-11-01T06:30:00Z'), '2026-11-01T06:30:00Z')).toBe('2026-11-01T06:30:00Z');
  expect(toUtc(toLocalInput('2026-11-13T08:15:31.125Z'), '2026-11-13T07:15:31.125Z')).toBe('2026-11-13T08:15:31.125Z');
});
