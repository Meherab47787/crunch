// Mirrors the backend DTOs (com.crustcrunch.dto). Money values arrive as JSON numbers.

export type Role = 'CUSTOMER' | 'ADMIN' | 'KITCHEN_STAFF';
export type ProductType = 'STANDARD' | 'COMBO';
export type SelectionType = 'SINGLE' | 'MULTIPLE';
export type DiscountType = 'PERCENT' | 'FIXED';
export type FulfillmentType = 'DELIVERY' | 'PICKUP';
export type PaymentMethod = 'CASH' | 'ONLINE';
export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED';
export type CustomizationKind = 'OPTION' | 'COMBO_CHOICE';
export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'PREPARING'
  | 'READY_FOR_PICKUP'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'CANCELLED';

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string>;
}

// ---------- auth / users ----------
export interface User {
  id: number;
  email: string;
  fullName: string;
  phone: string | null;
  role: Role;
  enabled: boolean;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  expiresInSeconds: number;
  user: User;
}

export interface Address {
  id: number;
  label: string;
  line1: string;
  line2: string | null;
  city: string;
  postalCode: string | null;
  instructions: string | null;
  defaultAddress: boolean;
}

export type AddressRequest = Omit<Address, 'id'>;

// ---------- menu ----------
export interface Category {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  imageUrl: string | null;
  icon: string | null;
  sortOrder: number;
  active: boolean;
  productCount: number;
}

export interface ProductSummary {
  id: number;
  slug: string;
  name: string;
  description: string | null;
  categoryId: number;
  categoryName: string;
  categorySlug: string;
  basePrice: number;
  fromPrice: number;
  salePrice: number | null;
  promotionName: string | null;
  imageUrl: string | null;
  productType: ProductType;
  vegetarian: boolean;
  spicy: boolean;
  allergens: string | null;
  calories: number | null;
  available: boolean;
  visible: boolean;
  featured: boolean;
  bestseller: boolean;
  newArrival: boolean;
  ratingAvg: number;
  ratingCount: number;
  soldCount: number;
  customizable: boolean;
}

export interface Variant {
  id: number;
  name: string;
  priceDelta: number;
  defaultVariant: boolean;
  available: boolean;
  sortOrder: number;
}

export interface CustomizationOption {
  id: number;
  name: string;
  priceDelta: number;
  defaultOption: boolean;
  available: boolean;
  sortOrder: number;
}

export interface CustomizationGroup {
  id: number;
  name: string;
  description: string | null;
  selectionType: SelectionType;
  minSelect: number;
  maxSelect: number;
  required: boolean;
  options: CustomizationOption[];
}

export interface ComboChoice {
  productId: number;
  name: string;
  slug: string;
  imageUrl: string | null;
  extraPrice: number;
  defaultChoice: boolean;
  available: boolean;
}

export interface ComboComponent {
  id: number;
  name: string;
  quantity: number;
  sortOrder: number;
  choices: ComboChoice[];
}

export interface PromotionBadge {
  id: number;
  name: string;
  discountType: DiscountType;
  discountValue: number;
}

export interface ProductDetail {
  summary: ProductSummary;
  ingredients: string | null;
  prepMinutes: number;
  sortOrder: number;
  images: string[];
  variants: Variant[];
  groups: CustomizationGroup[];
  comboComponents: ComboComponent[];
  promotion: PromotionBadge | null;
}

export interface Review {
  id: number;
  authorName: string;
  rating: number;
  comment: string | null;
  productId: number;
  productName: string;
  productSlug: string;
  createdAt: string;
}

export interface Promotion {
  id: number;
  name: string;
  description: string | null;
  imageUrl: string | null;
  discountType: DiscountType;
  discountValue: number;
  categoryId: number | null;
  categoryName: string | null;
  categorySlug: string | null;
  productId: number | null;
  productName: string | null;
  productSlug: string | null;
  startsAt: string | null;
  endsAt: string | null;
  active: boolean;
  live: boolean;
}

export interface HoursEntry {
  days: string;
  open: string;
  close: string;
}

export interface PublicSettings {
  restaurantName: string;
  tagline: string;
  phone: string;
  email: string;
  address: string;
  currency: string;
  hours: HoursEntry[];
  taxRate: number;
  deliveryFee: number;
  freeDeliveryThreshold: number;
  deliveryMinutes: number;
  pickupMinutes: number;
}

export interface ProductQuery {
  q?: string;
  category?: string;
  minPrice?: number;
  maxPrice?: number;
  vegetarian?: boolean;
  spicy?: boolean;
  available?: boolean;
  allergenFree?: string;
  featured?: boolean;
  bestseller?: boolean;
  newArrival?: boolean;
  type?: ProductType;
  sort?: string;
  page?: number;
  size?: number;
}

// ---------- cart / quote ----------
export interface ComboSelection {
  componentId: number;
  productId: number;
}

/** What the server needs to price a line — ids only, never prices. */
export interface CartLineRequest {
  productId: number;
  variantId: number | null;
  optionIds: number[];
  comboSelections: ComboSelection[];
  quantity: number;
  notes: string | null;
}

export interface SelectionLine {
  kind: CustomizationKind;
  groupName: string;
  optionName: string;
  priceDelta: number;
}

export interface QuoteLine {
  index: number;
  productId: number;
  productName: string | null;
  productSlug: string | null;
  imageUrl: string | null;
  categoryName: string | null;
  variantName: string | null;
  selections: SelectionLine[];
  unitPrice: number;
  quantity: number;
  lineSubtotal: number;
  promotionDiscount: number;
  promotionName: string | null;
  lineTotal: number;
  valid: boolean;
  error: string | null;
}

export interface Quote {
  lines: QuoteLine[];
  subtotal: number;
  promotionDiscount: number;
  couponDiscount: number;
  discountTotal: number;
  deliveryFee: number;
  tax: number;
  total: number;
  couponCode: string | null;
  couponApplied: boolean;
  couponMessage: string | null;
  freeDeliveryThreshold: number | null;
  issues: string[];
  valid: boolean;
}

// ---------- orders ----------
export interface ContactInfo {
  name: string;
  email: string;
  phone: string;
}

export interface DeliveryAddress {
  line1: string;
  line2: string | null;
  city: string;
  postalCode: string | null;
}

export interface CheckoutRequest {
  items: CartLineRequest[];
  couponCode: string | null;
  fulfillmentType: FulfillmentType;
  contact: ContactInfo;
  address: DeliveryAddress | null;
  deliveryInstructions: string | null;
  specialInstructions: string | null;
  allergyNotes: string | null;
  paymentMethod: PaymentMethod;
}

export interface OrderItemCustomization {
  kind: CustomizationKind;
  groupName: string;
  optionName: string;
  priceDelta: number;
}

export interface OrderItem {
  id: number;
  productId: number | null;
  productName: string;
  categoryName: string;
  variantName: string | null;
  basePrice: number;
  variantPriceDelta: number;
  unitPrice: number;
  quantity: number;
  discount: number;
  lineTotal: number;
  notes: string | null;
  customizations: OrderItemCustomization[];
}

export interface StatusHistory {
  fromStatus: OrderStatus | null;
  toStatus: OrderStatus;
  changedBy: string | null;
  note: string | null;
  createdAt: string;
}

export interface Order {
  id: number;
  orderNumber: string;
  trackingToken: string | null;
  status: OrderStatus;
  nextStatus: OrderStatus | null;
  /** Where the kitchen can send the order back to (null once it has left the kitchen). */
  previousStatus: OrderStatus | null;
  fulfillmentType: FulfillmentType;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  contact: ContactInfo;
  address: DeliveryAddress | null;
  deliveryInstructions: string | null;
  specialInstructions: string | null;
  allergyNotes: string | null;
  items: OrderItem[];
  subtotal: number;
  discountTotal: number;
  deliveryFee: number;
  tax: number;
  total: number;
  couponCode: string | null;
  estimatedReadyAt: string | null;
  cancelReason: string | null;
  createdAt: string;
  updatedAt: string;
  history: StatusHistory[];
  cancellableByCustomer: boolean;
  customerId: number | null;
  reviewedProductIds: number[];
}

export interface OrderSummary {
  id: number;
  orderNumber: string;
  status: OrderStatus;
  fulfillmentType: FulfillmentType;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  total: number;
  itemCount: number;
  itemsPreview: string;
  contactName: string;
  createdAt: string;
}

export interface ReorderLine {
  request: CartLineRequest;
  name: string;
  slug: string;
  imageUrl: string | null;
  categoryName: string;
  summary: string[];
  unitPrice: number;
}

export interface ReorderResponse {
  items: ReorderLine[];
  skipped: string[];
}

// ---------- admin ----------
export interface CategoryRequest {
  name: string;
  slug: string | null;
  description: string | null;
  imageUrl: string | null;
  icon: string | null;
  sortOrder: number;
  active: boolean;
}

export interface VariantRequest {
  id: number | null;
  name: string;
  priceDelta: number;
  defaultVariant: boolean;
  available: boolean;
  sortOrder: number;
}

export interface ComboChoiceRequest {
  productId: number;
  extraPrice: number;
  defaultChoice: boolean;
}

export interface ComboComponentRequest {
  id: number | null;
  name: string;
  quantity: number;
  sortOrder: number;
  choices: ComboChoiceRequest[];
}

export interface ProductRequest {
  categoryId: number;
  name: string;
  slug: string | null;
  description: string | null;
  ingredients: string | null;
  allergens: string | null;
  basePrice: number;
  imageUrl: string | null;
  productType: ProductType;
  vegetarian: boolean;
  spicy: boolean;
  calories: number | null;
  prepMinutes: number;
  available: boolean;
  visible: boolean;
  featured: boolean;
  bestseller: boolean;
  newArrival: boolean;
  sortOrder: number;
  variants: VariantRequest[];
  customizationGroupIds: number[];
  comboComponents: ComboComponentRequest[];
  galleryImages: string[];
}

export interface OptionRequest {
  id: number | null;
  name: string;
  priceDelta: number;
  defaultOption: boolean;
  available: boolean;
  sortOrder: number;
}

export interface GroupRequest {
  name: string;
  description: string | null;
  selectionType: SelectionType;
  minSelect: number;
  maxSelect: number;
  options: OptionRequest[];
}

export interface GroupAdmin {
  group: CustomizationGroup;
  usedByProducts: number;
}

export interface Coupon {
  id: number;
  code: string;
  description: string | null;
  discountType: DiscountType;
  discountValue: number;
  minOrderAmount: number;
  maxDiscountAmount: number | null;
  categoryId: number | null;
  categoryName: string | null;
  startsAt: string | null;
  expiresAt: string | null;
  maxRedemptions: number | null;
  perUserLimit: number | null;
  redemptionCount: number;
  active: boolean;
  createdAt: string;
}

export interface CouponRequest {
  code: string;
  description: string | null;
  discountType: DiscountType;
  discountValue: number;
  minOrderAmount: number;
  maxDiscountAmount: number | null;
  categoryId: number | null;
  startsAt: string | null;
  expiresAt: string | null;
  maxRedemptions: number | null;
  perUserLimit: number | null;
  active: boolean;
}

export interface PromotionRequest {
  name: string;
  description: string | null;
  imageUrl: string | null;
  discountType: DiscountType;
  discountValue: number;
  categoryId: number | null;
  productId: number | null;
  startsAt: string | null;
  endsAt: string | null;
  active: boolean;
}

export interface Customer {
  user: User;
  orderCount: number;
  totalSpent: number;
}

export interface StaffCreateRequest {
  email: string;
  fullName: string;
  password: string;
  role: Role;
  phone: string | null;
}

export interface SalesPoint {
  label: string;
  orders: number;
  revenue: number;
}

export interface DashboardStats {
  totalOrders: number;
  totalRevenue: number;
  averageOrderValue: number;
  todayOrders: number;
  todayRevenue: number;
  weekOrders: number;
  weekRevenue: number;
  monthOrders: number;
  monthRevenue: number;
  activeOrders: number;
  completedOrders: number;
  cancelledOrders: number;
  customers: number;
  statusCounts: Record<OrderStatus, number>;
  daily: SalesPoint[];
  weekly: SalesPoint[];
  monthly: SalesPoint[];
  topProducts: { productId: number | null; name: string; quantity: number; revenue: number }[];
  categorySales: { name: string; quantity: number; revenue: number }[];
  recentOrders: OrderSummary[];
}
