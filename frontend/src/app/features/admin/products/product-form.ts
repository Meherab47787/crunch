import { AbstractControl, FormArray, FormControl, FormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { ComboComponent, ProductDetail, ProductRequest, ProductType, Variant } from '../../../core/models/api.models';
import { SLUG_PATTERN, blankToNull } from '../shared/form-utils';

export type VariantForm = FormGroup<{
  id: FormControl<number | null>;
  name: FormControl<string>;
  priceDelta: FormControl<number>;
  defaultVariant: FormControl<boolean>;
  available: FormControl<boolean>;
}>;

export type ChoiceForm = FormGroup<{
  productId: FormControl<number | null>;
  extraPrice: FormControl<number>;
  defaultChoice: FormControl<boolean>;
}>;

export type ComboComponentForm = FormGroup<{
  id: FormControl<number | null>;
  name: FormControl<string>;
  quantity: FormControl<number>;
  choices: FormArray<ChoiceForm>;
}>;

export type ProductForm = ReturnType<typeof buildProductForm>;

export const minItems = (n: number): ValidatorFn => (c: AbstractControl): ValidationErrors | null =>
  c instanceof FormArray && c.length < n ? { minItems: { min: n } } : null;

export function buildProductForm() {
  return new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(120)] }),
    slug: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(140), Validators.pattern(SLUG_PATTERN)] }),
    categoryId: new FormControl<number | null>(null, { validators: [Validators.required] }),
    productType: new FormControl<ProductType>('STANDARD', { nonNullable: true }),
    description: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(1000)] }),
    ingredients: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(1000)] }),
    allergens: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(300)] }),
    basePrice: new FormControl<number>(0, { nonNullable: true, validators: [Validators.required, Validators.min(0), Validators.max(10000)] }),
    imageUrl: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(500)] }),
    calories: new FormControl<number | null>(null, { validators: [Validators.min(0)] }),
    prepMinutes: new FormControl<number>(10, { nonNullable: true, validators: [Validators.required, Validators.min(0), Validators.max(240)] }),
    sortOrder: new FormControl<number>(0, { nonNullable: true }),
    vegetarian: new FormControl(false, { nonNullable: true }),
    spicy: new FormControl(false, { nonNullable: true }),
    available: new FormControl(true, { nonNullable: true }),
    visible: new FormControl(true, { nonNullable: true }),
    featured: new FormControl(false, { nonNullable: true }),
    bestseller: new FormControl(false, { nonNullable: true }),
    newArrival: new FormControl(false, { nonNullable: true }),
    galleryImages: new FormArray<FormControl<string>>([]),
    variants: new FormArray<VariantForm>([]),
    customizationGroupIds: new FormControl<number[]>([], { nonNullable: true }),
    comboComponents: new FormArray<ComboComponentForm>([]),
  });
}

export function galleryControl(url = ''): FormControl<string> {
  return new FormControl(url, { nonNullable: true, validators: [Validators.required, Validators.maxLength(500)] });
}

export function variantForm(v?: Partial<Variant>): VariantForm {
  return new FormGroup({
    id: new FormControl<number | null>(v?.id ?? null),
    name: new FormControl(v?.name ?? '', { nonNullable: true, validators: [Validators.required, Validators.maxLength(80)] }),
    priceDelta: new FormControl(v?.priceDelta ?? 0, { nonNullable: true, validators: [Validators.required, Validators.min(-1000), Validators.max(1000)] }),
    defaultVariant: new FormControl(v?.defaultVariant ?? false, { nonNullable: true }),
    available: new FormControl(v?.available ?? true, { nonNullable: true }),
  });
}

export function choiceForm(c?: { productId: number | null; extraPrice: number; defaultChoice: boolean }): ChoiceForm {
  return new FormGroup({
    productId: new FormControl<number | null>(c?.productId ?? null, { validators: [Validators.required] }),
    extraPrice: new FormControl(c?.extraPrice ?? 0, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
    defaultChoice: new FormControl(c?.defaultChoice ?? false, { nonNullable: true }),
  });
}

export function comboComponentForm(c?: ComboComponent): ComboComponentForm {
  return new FormGroup({
    id: new FormControl<number | null>(c?.id ?? null),
    name: new FormControl(c?.name ?? '', { nonNullable: true, validators: [Validators.required, Validators.maxLength(80)] }),
    quantity: new FormControl(c?.quantity ?? 1, { nonNullable: true, validators: [Validators.required, Validators.min(1), Validators.max(20)] }),
    choices: new FormArray<ChoiceForm>(
      (c?.choices ?? []).map((ch) => choiceForm({ productId: ch.productId, extraPrice: ch.extraPrice, defaultChoice: ch.defaultChoice })),
      { validators: [minItems(1)] },
    ),
  });
}

export function patchProductForm(form: ProductForm, d: ProductDetail): void {
  const s = d.summary;
  form.reset({
    name: s.name,
    slug: s.slug,
    categoryId: s.categoryId,
    productType: s.productType,
    description: s.description ?? '',
    ingredients: d.ingredients ?? '',
    allergens: s.allergens ?? '',
    basePrice: s.basePrice,
    imageUrl: s.imageUrl ?? '',
    calories: s.calories,
    prepMinutes: d.prepMinutes,
    sortOrder: d.sortOrder,
    vegetarian: s.vegetarian,
    spicy: s.spicy,
    available: s.available,
    visible: s.visible,
    featured: s.featured,
    bestseller: s.bestseller,
    newArrival: s.newArrival,
    customizationGroupIds: d.groups.map((g) => g.id),
  });
  form.controls.galleryImages.clear();
  // the first image is normally the main imageUrl; keep only extra gallery images
  d.images.filter((u) => u !== s.imageUrl).forEach((u) => form.controls.galleryImages.push(galleryControl(u)));
  form.controls.variants.clear();
  [...d.variants].sort((a, b) => a.sortOrder - b.sortOrder).forEach((v) => form.controls.variants.push(variantForm(v)));
  form.controls.comboComponents.clear();
  [...d.comboComponents].sort((a, b) => a.sortOrder - b.sortOrder).forEach((c) => form.controls.comboComponents.push(comboComponentForm(c)));
  form.markAsPristine();
}

export function toProductRequest(form: ProductForm): ProductRequest {
  const v = form.getRawValue();
  const combo = v.productType === 'COMBO';
  return {
    categoryId: v.categoryId!,
    name: v.name.trim(),
    slug: blankToNull(v.slug),
    description: blankToNull(v.description),
    ingredients: blankToNull(v.ingredients),
    allergens: blankToNull(v.allergens),
    basePrice: Number(v.basePrice),
    imageUrl: blankToNull(v.imageUrl),
    productType: v.productType,
    vegetarian: v.vegetarian,
    spicy: v.spicy,
    calories: v.calories == null || (v.calories as unknown) === '' ? null : Number(v.calories),
    prepMinutes: Number(v.prepMinutes),
    available: v.available,
    visible: v.visible,
    featured: v.featured,
    bestseller: v.bestseller,
    newArrival: v.newArrival,
    sortOrder: Number(v.sortOrder) || 0,
    variants: v.variants.map((x, i) => ({
      id: x.id,
      name: x.name.trim(),
      priceDelta: Number(x.priceDelta),
      defaultVariant: x.defaultVariant,
      available: x.available,
      sortOrder: i,
    })),
    customizationGroupIds: v.customizationGroupIds,
    comboComponents: combo
      ? v.comboComponents.map((c, i) => ({
          id: c.id,
          name: c.name.trim(),
          quantity: Number(c.quantity),
          sortOrder: i,
          choices: c.choices.map((ch) => ({ productId: ch.productId!, extraPrice: Number(ch.extraPrice), defaultChoice: ch.defaultChoice })),
        }))
      : [],
    galleryImages: v.galleryImages.map((u) => u.trim()).filter(Boolean),
  };
}
