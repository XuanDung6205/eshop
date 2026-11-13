export class ApiError extends Error {
  constructor(status, data) {
    super(data?.message || `Không thể hoàn thành yêu cầu (${status}). Vui lòng thử lại.`);
    this.status = status;
    this.code = data?.code;
    this.fieldErrors = data?.fieldErrors || {};
  }
}

export function createApiClient(fetcher = (...args) => fetch(...args)) {
  let csrf;
  let csrfPromise;
  async function request(path, { method = 'GET', body, signal, form = false } = {}) {
    const headers = { Accept: 'application/json' };
    if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
      const token = csrf || await refreshCsrf();
      headers[token.headerName] = token.token;
    }
    if (body !== undefined) headers['Content-Type'] = form ? 'application/x-www-form-urlencoded;charset=UTF-8' : 'application/json';
    let response;
    try {
      response = await fetcher(`/api${path}`, { method, credentials: 'include', headers, signal, ...(body !== undefined ? { body: form ? new URLSearchParams(body).toString() : JSON.stringify(body) } : {}) });
    } catch (error) {
      if (error.name === 'AbortError') throw error;
      throw new ApiError(0, { code: 'NETWORK_ERROR', message: 'Chưa kết nối được máy chủ. Kiểm tra kết nối và thử lại.' });
    }
    const data = response.status === 204 ? null : await response.json().catch(() => null);
    if (!response.ok) {
      if (response.status === 403) csrf = undefined;
      throw new ApiError(response.status, data);
    }
    return data;
  }
  async function refreshCsrf() {
    if (!csrfPromise) csrfPromise = request('/csrf').then(value => { csrf = value; return value; }).finally(() => { csrfPromise = undefined; });
    return csrfPromise;
  }
  async function authenticate(path, body) {
    const session = await request(path, { method: 'POST', body, form: !!body });
    csrf = undefined;
    // Refresh failure must not misreport a successful login/logout as a failed one.
    // The next mutation will fetch it again and surface any persistent error.
    await refreshCsrf().catch(() => {});
    return session;
  }
  return {
    request,
    login: (username, password) => authenticate('/session/login', { username, password }),
    logout: () => authenticate('/session/logout'),
    mergeCart: () => request('/cart/merge', { method: 'POST' }),
  };
}
