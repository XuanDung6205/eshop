export const orderApi = api => ({
  list: (page = 0, status = '', signal) => api.request(`/orders?${new URLSearchParams({ page, size: 10, status })}`, { signal }),
  detail: (id, signal) => api.request(`/orders/${encodeURIComponent(id)}`, { signal }),
  cancel: (id, reason) => api.request(`/orders/${encodeURIComponent(id)}/cancel`, { method: 'POST', body: { reason } }),
});
