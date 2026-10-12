import { expect, test } from '@playwright/test';

test.describe('customer ordering', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      if (!sessionStorage.getItem('e2e-init')) {
        localStorage.clear();
        sessionStorage.setItem('e2e-init', '1');
      }
    });
  });

  test('guest customizes a pizza, checks out for pickup and tracks the order', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('heading', { name: /hot, crispy/i })).toBeVisible();

    await page.goto('/menu/item/pepperoni-pizza');
    await expect(page.getByRole('heading', { level: 1, name: 'Pepperoni Classic' })).toBeVisible();
    await page.getByText('Large 14"').click();
    await page.getByText('Cheese-Stuffed Crust').click();
    await page.getByText('Mushrooms').click();
    await page.getByRole('button', { name: /add to bag/i }).click();
    await expect(page.getByText(/added to your bag/i)).toBeVisible();

    await page.goto('/cart');
    await expect(page.getByText('Large 14"', { exact: true })).toBeVisible();
    await expect(page.getByText('Pizza Week: 20% Off')).toBeVisible(); // automatic promotion from the server quote

    await page.getByRole('link', { name: /go to checkout/i }).click();
    await page.getByRole('radio', { name: /^pickup/i }).click();
    await page.getByLabel('Full name').fill('E2E Guest');
    await page.getByLabel('Email').fill(`e2e+${Date.now()}@example.com`);
    await page.getByLabel('Phone').fill('+1 555 0142');
    await page.getByLabel(/allergies/i).fill('Sesame');
    await page.getByRole('button', { name: /place order/i }).click();

    await expect(page.getByRole('heading', { name: /your order is in/i })).toBeVisible();
    await expect(page.getByText(/order CC\d{6}-/i)).toBeVisible();
    const view = page.locator('app-order-view');
    await expect(view.getByText('Pepperoni Classic')).toBeVisible();
    await expect(view.getByText(/cheese-stuffed crust/i)).toBeVisible();
    // the bag is emptied after a successful order
    await expect(page.getByRole('button', { name: /open bag, 0 items/i })).toBeVisible();
  });

  test('menu search and filters narrow results', async ({ page }) => {
    await page.goto('/menu');
    await page.getByPlaceholder(/search pizza/i).fill('wings');
    await expect(page).toHaveURL(/q=wings/);
    await expect(page.getByRole('heading', { name: 'Chicken Wings' })).toBeVisible();

    await page.goto('/menu/pizza?vegetarian=true');
    await expect(page.getByRole('heading', { name: 'Margherita' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Pepperoni Classic' })).toHaveCount(0);
  });

  test('delivery checkout validates the address before submitting', async ({ page }) => {
    await page.goto('/menu/item/classic-smash-burger');
    await page.getByRole('button', { name: /increase quantity/i }).click();
    await page.getByRole('button', { name: /add to bag/i }).click();
    await page.goto('/checkout');
    await page.getByRole('radio', { name: /^delivery/i }).click();
    await page.getByLabel('Full name').fill('E2E Delivery');
    await page.getByLabel('Email').fill('e2e-delivery@example.com');
    await page.getByLabel('Phone').fill('+1 555 0143');
    await page.getByRole('button', { name: /place order/i }).click();
    await expect(page.getByText('Please enter your street address.')).toBeVisible();
    await expect(page).toHaveURL(/\/checkout/);
  });

  test('a customer can register, apply a coupon and see the order in their history', async ({ page }) => {
    const email = `e2e-${Date.now()}@example.com`;
    await page.goto('/register');
    await page.getByLabel('Full name').fill('E2E Customer');
    await page.getByLabel('Email').fill(email);
    await page.getByLabel('Password').fill('E2ePassword1');
    await page.getByRole('button', { name: /create account/i }).click();
    await expect(page).toHaveURL(/\/menu/);

    await page.goto('/menu/item/burger-combo');
    await page.getByRole('button', { name: /add to bag/i }).click();
    await page.goto('/menu/item/family-bucket');
    await page.getByRole('button', { name: /add to bag/i }).click();

    await page.goto('/cart');
    await page.getByPlaceholder('Coupon code').fill('combo15');
    await page.getByRole('button', { name: 'Apply' }).click();
    await expect(page.getByText(/coupon applied/i)).toBeVisible();

    await page.getByRole('link', { name: /go to checkout/i }).click();
    await page.getByRole('radio', { name: /^pickup/i }).click();
    await page.getByLabel('Phone').fill('+1 555 0144');
    await page.getByRole('button', { name: /place order/i }).click();
    await expect(page.getByRole('heading', { name: /your order is in/i })).toBeVisible();

    await page.goto('/account/orders');
    await expect(page.getByText(/1× Burger & Fries Combo, 1× Family Chicken Bucket/)).toBeVisible();
  });
});
