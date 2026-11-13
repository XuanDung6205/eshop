export const cartApi = api => ({
  get: options => api.request('/cart', options),
  add: (variantId, quantity = 1) => api.request('/cart/items', { method: 'POST', body: { variantId, quantity } }),
  update: (id, quantity) => api.request(`/cart/items/${id}`, { method: 'PATCH', body: { quantity } }),
  remove: id => api.request(`/cart/items/${id}`, { method: 'DELETE' }),
  clear: () => api.request('/cart/items', { method: 'DELETE' }),
  catalog: (query, page = 0, signal) => api.request(`/commerce/catalog/variants?${new URLSearchParams({ query, page, size: 12 })}`, { signal }),
});
