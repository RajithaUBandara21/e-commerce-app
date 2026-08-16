// Mirrors of backend DTOs consumed through api-gateway. Kept in one file since
// they're small and change together with the services that own them.

export type ProductVariant = {
  id: number;
  sku: string;
  size: string;
  color: string;
  availableQuantity: number;
};

export type Product = {
  id: number;
  name: string;
  description: string;
  price: number;
  sellerId: string;
  categoryId: number;
  categoryName: string;
  categoryDescription: string;
  variants: ProductVariant[];
  imageUrls: string[];
  averageRating: number | null;
  reviewCount: number;
};

export type Review = {
  id: number;
  productId: number;
  customerId: string;
  rating: number;
  comment: string | null;
  createdDate: string;
};

export type CartItem = {
  variantId: number;
  quantity: number;
};

export type Cart = {
  userId: string;
  items: CartItem[];
};

export type PaymentMethod = "PAYPAL" | "CREDIT_CARD" | "VISA" | "MASTERCARD" | "BITCOIN";

export type OrderStatus =
  | "PENDING_PAYMENT"
  | "CONFIRMED"
  | "PAYMENT_FAILED"
  | "CANCELLED"
  | "SHIPPED"
  | "DELIVERED"
  | "REFUNDED";

export type Order = {
  id: number;
  reference: string;
  amount: number;
  paymentMethode: PaymentMethod;
  status: OrderStatus;
  customerId: string;
};

export type OrderLineStatus = "PENDING" | "SHIPPED" | "DELIVERED";

export type OrderLine = {
  id: number;
  orderId: number;
  variantId: number;
  quantity: number;
  sellerId: string;
  status: OrderLineStatus;
  trackingNumber: string | null;
};

export type Category = {
  id: number;
  name: string;
  description: string | null;
  productCount: number;
};
