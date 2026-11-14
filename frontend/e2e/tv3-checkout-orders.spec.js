import { test, expect } from '@playwright/test';

test.beforeEach(async ({ request }) => {
  const response = await request.get('/api/commerce/runtime');
  expect(response.ok()).toBeTruthy();
  const runtime = await response.json();
  expect(runtime.application).toBe('tv3-commerce');
  expect(runtime.isolatedData, 'Checkout E2E may only mutate the named isolated development/test database').toBe(true);
});

test('real customer quotes, places, views and cancels an order with responsive snapshots', async ({ page }) => {
  test.skip(!process.env.TV3_DEMO_PASSWORD, 'Provide the isolated demo account password through the environment.');
  const errors = []; page.on('pageerror', error => errors.push(error.message));
  let createdId;
  try {
    await page.setViewportSize({ width: 1440, height: 1000 });
    await page.goto('/checkout');
    await expect(page.getByRole('heading', { name: 'Đăng nhập để tiếp tục', exact: true })).toBeVisible();
    await page.getByRole('main').getByRole('link', { name: 'Đăng nhập', exact: true }).click();
    await page.getByLabel('Tên đăng nhập', { exact: true }).fill(process.env.TV3_CHECKOUT_USER || 'tv3.customer');
    await page.getByLabel('Mật khẩu', { exact: true }).fill(process.env.TV3_DEMO_PASSWORD);
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
    await expect(page).toHaveURL(/\/checkout$/);
    await page.getByRole('link', { name: 'Trở về giỏ hàng', exact: true }).click();
    await expect(page.getByRole('region', { name: 'Sản phẩm trong giỏ', exact: true })).toBeVisible();
    if (await page.getByRole('button', { name: 'Xóa tất cả', exact: true }).isVisible()) {
      await page.getByRole('button', { name: 'Xóa tất cả', exact: true }).click();
      await page.getByRole('button', { name: 'Đồng ý xóa hết', exact: true }).click();
      await expect(page.getByRole('heading', { name: 'Giỏ hàng đang chờ bạn', exact: true })).toBeVisible();
    }
    await page.getByRole('button', { name: 'Thêm Bàn trà Oslo vào giỏ', exact: true }).click();
    await expect(page.getByRole('region', { name: 'Sản phẩm trong giỏ', exact: true }).getByRole('heading', { name: 'Bàn trà Oslo', exact: true })).toBeVisible();
    await page.getByRole('link', { name: 'Tiếp tục đặt hàng', exact: true }).click();
    await page.getByLabel('Nhập địa chỉ mới', { exact: true }).check();
    for (const [label, value] of [['Họ tên người nhận', 'Khách kiểm thử TV3'], ['Số điện thoại', '0900000000'], ['Địa chỉ', '12 Đường Kiểm Thử'], ['Phường / xã', 'Phường 1'], ['Quận / huyện', 'Quận 3'], ['Tỉnh / thành phố', 'Hồ Chí Minh']]) await page.getByLabel(label, { exact: true }).fill(value);
    await page.getByLabel('Ghi chú cho cửa hàng', { exact: true }).fill('E2E TV3 isolated order; cancel after verification.');
    await page.getByRole('button', { name: 'Xem trước đơn hàng', exact: true }).click();
    await expect(page.getByTestId('checkout-total')).toContainText('₫');
    await expect(page.getByRole('button', { name: 'Đặt hàng', exact: true })).toBeDisabled();
    await page.screenshot({ path: 'test-results/tv3-checkout-desktop.png', fullPage: true });
    await page.setViewportSize({ width: 390, height: 844 });
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
    await page.screenshot({ path: 'test-results/tv3-checkout-mobile.png', fullPage: true });
    await page.getByLabel('Tôi đã kiểm tra thông tin và tổng tiền.', { exact: true }).check();
    const created = page.waitForResponse(response => response.url().endsWith('/api/checkout/orders') && response.request().method() === 'POST');
    await page.getByRole('button', { name: 'Đặt hàng', exact: true }).click();
    const response = await created;
    expect(response.status()).toBe(201);
    const detail = await response.json(); createdId = detail.id;
    expect(response.request().headers()['idempotency-key']).toMatch(/^[A-Za-z0-9_-]{8,64}$/);
    await expect(page).toHaveURL(new RegExp(`/orders/${createdId}$`));
    await expect(page.getByRole('heading', { name: detail.orderNumber, exact: true })).toBeVisible();
    await expect(page.getByRole('region', { name: 'Thanh toán', exact: true })).toContainText('Đang chờ');
    await expect(page.getByText('Khách kiểm thử TV3', { exact: true })).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
    await page.screenshot({ path: 'test-results/tv3-order-mobile.png', fullPage: true });
    await page.getByRole('button', { name: 'Hủy đơn hàng', exact: true }).click();
    await page.getByLabel('Lý do hủy đơn', { exact: true }).fill('Hoàn tất kiểm thử TV3 trên dữ liệu cô lập');
    await page.getByRole('button', { name: 'Xác nhận hủy đơn', exact: true }).click();
    await expect(page.getByText('Đã hủy đơn hàng. Xem thông tin thanh toán và yêu cầu hoàn tiền bên dưới.', { exact: true })).toBeVisible();
    createdId = undefined;
    await page.setViewportSize({ width: 1440, height: 1000 });
    await page.screenshot({ path: 'test-results/tv3-order-desktop.png', fullPage: true });
    await page.getByRole('link', { name: '← Tất cả đơn hàng', exact: true }).click();
    await page.getByLabel('Trạng thái đơn hàng', { exact: true }).selectOption('CANCELLED');
    await expect(page.getByRole('link', { name: `Xem đơn ${detail.orderNumber}`, exact: true })).toBeVisible();
    expect(errors).toEqual([]);
  } finally {
    // Only release this test's own inventory/coupon holds; preserve order history.
    if (createdId) {
      const csrfResponse = await page.request.get('/api/csrf');
      const csrf = await csrfResponse.json();
      const cleanup = await page.request.post(`/api/orders/${createdId}/cancel`, { data: { reason: 'Dọn yêu cầu giữ hàng sau kiểm thử TV3' }, headers: { [csrf.headerName]: csrf.token } });
      expect(cleanup.ok(), 'Release this E2E order through the real cancellation API').toBeTruthy();
    }
  }
});
