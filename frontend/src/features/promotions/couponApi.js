export const couponApi = api => ({
  list: (query, page = 0, signal) => api.request(`/admin/coupons?${new URLSearchParams({ query, page, size: 20 })}`, { signal }),
  create: body => api.request('/admin/coupons', { method: 'POST', body }),
  update: (id, body) => api.request(`/admin/coupons/${id}`, { method: 'PATCH', body }),
  remove: id => api.request(`/admin/coupons/${id}`, { method: 'DELETE' }),
  preview: (id, merchandiseAmount) => api.request(`/admin/coupons/${id}/preview`, { method: 'POST', body: { merchandiseAmount } }),
});
