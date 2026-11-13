export function formatMoney(value) {
  if (typeof value !== 'string' || !/^-?\d+(\.\d+)?$/.test(value)) return '—';
  const [integer, fraction = ''] = value.split('.');
  return `${integer.replace(/\B(?=(\d{3})+(?!\d))/g, '.')}${/[1-9]/.test(fraction) ? `,${fraction}` : ''} ₫`;
}
export function toLocalInput(value) {
  if (!value) return '';
  const date = new Date(value);
  const pad = (number, width = 2) => String(number).padStart(width, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}.${pad(date.getMilliseconds(), 3)}`;
}
export function toUtc(value, originalUtc) {
  // A repeated local hour during a DST transition has two possible UTC instants.
  // Keep the saved instant if the user has not changed the wall-clock value.
  if (originalUtc && toLocalInput(value) === toLocalInput(originalUtc)) return originalUtc;
  return new Date(value).toISOString();
}
export function formatDate(value) { return new Date(value).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' }); }
