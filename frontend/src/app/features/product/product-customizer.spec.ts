import { TestBed } from '@angular/core/testing';
import { ProductDetail } from '../../core/models/api.models';
import { ProductCustomizer } from './product-customizer';

const pizza: ProductDetail = {
  summary: {
    id: 2, slug: 'pepperoni-pizza', name: 'Pepperoni', description: null, categoryId: 1, categoryName: 'Pizza',
    categorySlug: 'pizza', basePrice: 11.49, fromPrice: 11.49, salePrice: null, promotionName: null, imageUrl: null,
    productType: 'STANDARD', vegetarian: false, spicy: false, allergens: null, calories: null, available: true,
    visible: true, featured: false, bestseller: false, newArrival: false, ratingAvg: 0, ratingCount: 0, soldCount: 0,
    customizable: true,
  },
  ingredients: null, prepMinutes: 10, sortOrder: 0, images: [],
  variants: [
    { id: 5, name: 'Small', priceDelta: 0, defaultVariant: false, available: true, sortOrder: 1 },
    { id: 7, name: 'Large', priceDelta: 6, defaultVariant: true, available: true, sortOrder: 2 },
  ],
  groups: [
    { id: 1, name: 'Crust', description: null, selectionType: 'SINGLE', minSelect: 1, maxSelect: 1, required: true,
      options: [
        { id: 1, name: 'Classic', priceDelta: 0, defaultOption: true, available: true, sortOrder: 1 },
        { id: 3, name: 'Stuffed', priceDelta: 2.5, defaultOption: false, available: true, sortOrder: 2 },
      ] },
    { id: 2, name: 'Toppings', description: null, selectionType: 'MULTIPLE', minSelect: 0, maxSelect: 2, required: false,
      options: [
        { id: 5, name: 'Pepperoni', priceDelta: 1.5, defaultOption: false, available: true, sortOrder: 1 },
        { id: 6, name: 'Mushrooms', priceDelta: 1, defaultOption: false, available: true, sortOrder: 2 },
        { id: 7, name: 'Onions', priceDelta: 0.75, defaultOption: false, available: true, sortOrder: 3 },
      ] },
  ],
  comboComponents: [],
  promotion: { id: 1, name: 'Pizza week', discountType: 'PERCENT', discountValue: 20 },
};

describe('ProductCustomizer', () => {
  let c: ProductCustomizer;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [ProductCustomizer] });
    c = TestBed.inject(ProductCustomizer);
    c.init(pizza);
  });

  it('starts from the default size and default options', () => {
    expect(c.variantId()).toBe(7);
    expect(c.selectedFor(1)).toEqual([1]);
    expect(c.unitPrice()).toBe(17.49);
  });

  it('adds option prices and applies the live promotion', () => {
    c.toggleOption(pizza.groups[0], 3);
    c.toggleOption(pizza.groups[1], 6);
    expect(c.unitPrice()).toBe(20.99);
    expect(c.discountPerUnit()).toBe(4.2);
    c.quantity.set(2);
    expect(c.lineTotal()).toBe(33.58);
  });

  it('enforces the max for multi-select groups', () => {
    const toppings = pizza.groups[1];
    c.toggleOption(toppings, 5);
    c.toggleOption(toppings, 6);
    c.toggleOption(toppings, 7);
    expect(c.selectedFor(2)).toEqual([5, 6]);
  });

  it('does not let a required single group be cleared', () => {
    c.toggleOption(pizza.groups[0], 1);
    expect(c.selectedFor(1)).toEqual([1]);
    expect(c.problems()).toEqual([]);
  });

  it('produces a cart line with ids, readable summary and preview price', () => {
    c.toggleOption(pizza.groups[1], 5);
    const line = c.toCartLine();
    expect(line.variantId).toBe(7);
    expect(line.optionIds).toEqual([1, 5]);
    expect(line.summary).toEqual(['Large', 'Classic', '+ Pepperoni']);
    expect(line.displayUnitPrice).toBe(15.19);
  });
});
