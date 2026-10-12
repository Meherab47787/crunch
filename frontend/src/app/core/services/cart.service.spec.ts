import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CartLine, CartService, lineKey } from './cart.service';

function line(overrides: Partial<Omit<CartLine, 'key'>> = {}): Omit<CartLine, 'key'> {
  return {
    productId: 1,
    variantId: 6,
    optionIds: [3, 15],
    comboSelections: [],
    quantity: 1,
    notes: null,
    slug: 'pepperoni-pizza',
    name: 'Pepperoni',
    imageUrl: null,
    categoryName: 'Pizza',
    summary: [],
    displayUnitPrice: 14.49,
    ...overrides,
  };
}

describe('CartService', () => {
  let cart: CartService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    cart = TestBed.inject(CartService);
    http = TestBed.inject(HttpTestingController);
  });

  it('builds the same key regardless of option order', () => {
    expect(lineKey(line({ optionIds: [15, 3] }))).toBe(lineKey(line({ optionIds: [3, 15] })));
  });

  it('merges identical configurations and keeps different customizations apart', () => {
    cart.add(line());
    cart.add(line({ quantity: 2 }));
    cart.add(line({ optionIds: [3] }));
    expect(cart.lines().length).toBe(2);
    expect(cart.lines()[0].quantity).toBe(3);
    expect(cart.itemCount()).toBe(4);
  });

  it('caps quantity at 50 and removes lines set to zero', () => {
    cart.add(line({ quantity: 49 }));
    cart.add(line({ quantity: 5 }));
    expect(cart.lines()[0].quantity).toBe(50);
    cart.setQuantity(cart.lines()[0].key, 0);
    expect(cart.isEmpty()).toBe(true);
  });

  it('restores a removed line in place with undo', () => {
    cart.add(line({ productId: 1 }));
    cart.add(line({ productId: 2 }));
    const first = cart.lines()[0].key;
    cart.remove(first);
    cart.undoRemove();
    expect(cart.lines()[0].key).toBe(first);
  });

  it('replaces an edited line, merging into an existing identical line', () => {
    cart.add(line({ optionIds: [3] }));
    cart.add(line({ optionIds: [3, 15] }));
    const firstKey = cart.lines()[0].key;
    cart.replace(firstKey, line({ optionIds: [15, 3], quantity: 2 }));
    expect(cart.lines().length).toBe(1);
    expect(cart.lines()[0].quantity).toBe(3);
  });

  it('persists to localStorage', () => {
    cart.add(line());
    cart.applyCoupon(' welcome10 ');
    TestBed.tick();
    const saved = JSON.parse(localStorage.getItem('cc.cart.v1')!);
    expect(saved.lines.length).toBe(1);
    expect(saved.couponCode).toBe('WELCOME10');
  });

  it('asks the server for a quote and never sends prices', async () => {
    cart.add(line());
    TestBed.tick();
    await new Promise((r) => setTimeout(r, 300));
    const req = http.expectOne('/api/cart/quote');
    expect(req.request.body.items[0]).toEqual({
      productId: 1, variantId: 6, optionIds: [3, 15], comboSelections: [], quantity: 1, notes: null,
    });
    expect(JSON.stringify(req.request.body)).not.toContain('14.49');
    req.flush({ lines: [], subtotal: 14.49, total: 15.65, valid: true, issues: [] });
    expect(cart.quote()?.total).toBe(15.65);
  });
});
