export const checkoutApi = api => ({
  addresses: signal => api.request('/checkout/addresses', { signal }),
  quote: body => api.request('/checkout/quote', { method: 'POST', body }),
  place: attempt => api.request('/checkout/orders', { method: 'POST', body: attempt.body, headers: { 'Idempotency-Key': attempt.key } }),
});

const storageKey = userId => `nep-nha.checkout.pending.${userId}`;
export function readAttempt(userId) {
  try {
    const attempt = JSON.parse(sessionStorage.getItem(storageKey(userId)));
    return attempt && /^[A-Za-z0-9_-]{8,64}$/.test(attempt.key) && attempt.body?.quoteFingerprint && attempt.quote?.quoteFingerprint ? attempt : null;
  } catch { return null; }
}
export function saveAttempt(userId, attempt) {
  try { sessionStorage.setItem(storageKey(userId), JSON.stringify(attempt)); }
  catch { throw new Error('Trình duyệt chưa cho phép lưu yêu cầu trong phiên. Hãy cho phép bộ nhớ phiên trước khi đặt hàng để có thể thử lại an toàn.'); }
}
export function clearAttempt(userId) { try { sessionStorage.removeItem(storageKey(userId)); } catch { /* Retaining a completed key is safe: the server replays the same order. */ } }
