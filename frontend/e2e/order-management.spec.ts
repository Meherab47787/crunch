import { APIRequestContext, Page, expect, test } from '@playwright/test';

const ADMIN = { email: process.env['E2E_ADMIN_EMAIL'] ?? '', password: process.env['E2E_ADMIN_PASSWORD'] ?? '' };
const KITCHEN = { email: process.env['E2E_KITCHEN_EMAIL'] ?? '', password: process.env['E2E_KITCHEN_PASSWORD'] ?? '' };

test.skip(!ADMIN.email || !KITCHEN.email, 'Set E2E_ADMIN_* and E2E_KITCHEN_* to run staff tests');

/** Places a pickup order through the public API, as the storefront would. */
async function placeOrder(request: APIRequestContext) {
  const product = await (await request.get('/api/products/classic-smash-burger')).json();
  const res = await request.post('/api/orders', {
    headers: { 'Idempotency-Key': `e2e-${Date.now()}-${Math.random().toString(36).slice(2)}` },
    data: {
      items: [{ productId: product.summary.id, variantId: null, optionIds: [product.groups[0].options[1].id], comboSelections: [], quantity: 2, notes: 'no onions' }],
      couponCode: null,
      fulfillmentType: 'PICKUP',
      contact: { name: 'Kitchen Flow', email: 'kitchen-flow@example.com', phone: '+1 555 0177' },
      address: null,
      deliveryInstructions: null,
      specialInstructions: null,
      allergyNotes: 'Peanut allergy',
      paymentMethod: 'CASH',
    },
  });
  expect(res.status()).toBe(201);
  return (await res.json()) as { id: number; orderNumber: string; trackingToken: string };
}

async function signIn(page: Page, who: { email: string; password: string }) {
  await page.goto('/login');
  await page.getByLabel('Email').fill(who.email);
  await page.getByLabel('Password', { exact: true }).fill(who.password);
  await page.getByRole('button', { name: /^sign in$/i }).click();
}

test('kitchen and admin move an order through its lifecycle', async ({ page, request, browser }) => {
  const order = await placeOrder(request);

  // Kitchen: ticket shows customization + allergy, then confirm and start cooking.
  await signIn(page, KITCHEN);
  await expect(page).toHaveURL(/\/kitchen/);
  const ticket = page.getByRole('article', { name: new RegExp(`Order ${order.orderNumber}`) });
  await expect(ticket).toBeVisible();
  await expect(ticket.getByText(/double patty/i)).toBeVisible();
  await expect(ticket.getByText(/peanut allergy/i)).toBeVisible();
  await ticket.locator('footer button').last().click(); // PENDING -> CONFIRMED
  await expect(page.getByRole('article', { name: new RegExp(`Order ${order.orderNumber}`) }).locator('footer button').last()).toBeEnabled();
  await page.getByRole('article', { name: new RegExp(`Order ${order.orderNumber}`) }).locator('footer button').last().click(); // -> PREPARING

  // Admin: find the order and finish it.
  const adminCtx = await browser.newContext();
  const admin = await adminCtx.newPage();
  await signIn(admin, ADMIN);
  await expect(admin).toHaveURL(/\/admin/);
  await admin.goto(`/admin/orders?q=${order.orderNumber}`);
  await admin.getByRole('link', { name: order.orderNumber }).click();
  await expect(admin.getByText(/peanut allergy/i).first()).toBeVisible();
  await admin.getByRole('button', { name: /mark as ready/i }).click();
  await admin.getByRole('button', { name: /mark as delivered/i }).click();
  await expect(admin.getByRole('button', { name: /mark as/i })).toHaveCount(0);

  // Customer tracking page reflects the final state.
  const guest = await (await browser.newContext()).newPage();
  await guest.goto(`/orders/${order.orderNumber}?token=${order.trackingToken}`);
  await expect(guest.getByRole('heading', { name: /collected/i })).toBeVisible();
  await adminCtx.close();
});

test('kitchen can move a ticket back to the previous column', async ({ page, request }) => {
  const order = await placeOrder(request);
  await signIn(page, KITCHEN);
  await expect(page).toHaveURL(/\/kitchen/);

  const ticket = () => page.getByRole('article', { name: new RegExp(`Order ${order.orderNumber}`) });

  // a brand-new ticket has nowhere to go back to
  await expect(ticket()).toBeVisible();
  await expect(ticket().getByRole('button', { name: /move order .* back/i })).toHaveCount(0);

  await ticket().locator('footer button').last().click(); // New -> Confirmed
  const back = ticket().getByRole('button', { name: new RegExp(`Move order ${order.orderNumber} back to New`) });
  await expect(back).toBeVisible();

  await back.click(); // Confirmed -> New
  await expect(ticket().getByRole('button', { name: /move order .* back/i })).toHaveCount(0);
  await expect(ticket().getByRole('button', { name: /accept order/i })).toBeVisible();

  // the server agrees: the guest tracking page shows the order as just received again
  const guest = await request.get(`/api/orders/track/${order.orderNumber}`, { params: { token: order.trackingToken } });
  const body = await guest.json();
  expect(body.status).toBe('PENDING');
  expect(body.history.map((h: { toStatus: string }) => h.toStatus)).toEqual(['PENDING', 'CONFIRMED', 'PENDING']);
});

test('kitchen staff cannot open the admin dashboard', async ({ page }) => {
  await signIn(page, KITCHEN);
  await expect(page).toHaveURL(/\/kitchen/);
  await page.goto('/admin');
  await expect(page).not.toHaveURL(/\/admin/);
});
