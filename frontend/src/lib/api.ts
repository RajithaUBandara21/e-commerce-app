import type { Category, Cart, Order, OrderLine, OrderLineStatus, Product, Review } from "@/lib/types";

const GATEWAY_URL = process.env.NEXT_PUBLIC_API_GATEWAY_URL ?? "http://localhost:8222";

class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
  ) {
    super(message);
  }
}

async function request<T>(
  path: string,
  init?: RequestInit & { accessToken?: string },
): Promise<T> {
  const headers = new Headers(init?.headers);
  headers.set("Content-Type", "application/json");
  if (init?.accessToken) {
    headers.set("Authorization", `Bearer ${init.accessToken}`);
  }

  const response = await fetch(`${GATEWAY_URL}${path}`, {
    ...init,
    headers,
  });

  if (!response.ok) {
    throw new ApiError(`${init?.method ?? "GET"} ${path} failed with ${response.status}`, response.status);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

// Catalog is public — permitted through the gateway without a token.
export function getProducts(): Promise<Product[]> {
  return request<Product[]>("/api/v1/products");
}

export function getProduct(id: number): Promise<Product> {
  return request<Product>(`/api/v1/products/${id}`);
}

// A seller's own catalog — still the public GET endpoint, just filtered.
export function getMyProducts(sellerId: string): Promise<Product[]> {
  return request<Product[]>(`/api/v1/products?sellerId=${encodeURIComponent(sellerId)}`, { cache: "no-store" });
}

export type ProductImageUploadUrl = { uploadUrl: string; objectKey: string };

export function getImageUploadUrl(productId: number, contentType: string, accessToken: string): Promise<ProductImageUploadUrl> {
  return request<ProductImageUploadUrl>(
    `/api/v1/products/${productId}/images/upload-url?contentType=${encodeURIComponent(contentType)}`,
    { method: "POST", accessToken },
  );
}

export function registerProductImage(productId: number, objectKey: string, accessToken: string): Promise<void> {
  return request(`/api/v1/products/${productId}/images`, {
    method: "POST",
    body: JSON.stringify({ objectKey }),
    accessToken,
  });
}

// Reviews are public reads (same as the product catalog); writes are gated on
// verified purchase server-side, not on a role.
export function getProductReviews(productId: number): Promise<Review[]> {
  return request<Review[]>(`/api/v1/products/${productId}/reviews`, { cache: "no-store" });
}

export function createReview(
  productId: number,
  body: { rating: number; comment?: string },
  accessToken: string,
): Promise<Review> {
  return request<Review>(`/api/v1/products/${productId}/reviews`, {
    method: "POST",
    body: JSON.stringify(body),
    accessToken,
  });
}

export function deleteReview(productId: number, reviewId: number, accessToken: string): Promise<void> {
  return request(`/api/v1/products/${productId}/reviews/${reviewId}`, {
    method: "DELETE",
    accessToken,
  });
}

// Everything below requires the caller's Keycloak access token.

export function getCart(userId: string, accessToken: string): Promise<Cart> {
  return request<Cart>(`/api/v1/cart/${userId}`, { accessToken, cache: "no-store" });
}

export function addCartItem(
  userId: string,
  variantId: number,
  quantity: number,
  accessToken: string,
): Promise<void> {
  return request(`/api/v1/cart/${userId}/items`, {
    method: "POST",
    body: JSON.stringify({ variantId, quantity }),
    accessToken,
  });
}

export function setCartItemQuantity(
  userId: string,
  variantId: number,
  quantity: number,
  accessToken: string,
): Promise<void> {
  return request(`/api/v1/cart/${userId}/items/${variantId}`, {
    method: "PUT",
    body: JSON.stringify({ quantity }),
    accessToken,
  });
}

export function removeCartItem(userId: string, variantId: number, accessToken: string): Promise<void> {
  return request(`/api/v1/cart/${userId}/items/${variantId}`, {
    method: "DELETE",
    accessToken,
  });
}

export type CheckoutRequest = {
  reference: string;
  totalAmount: number;
  paymentMethode: string;
  stripePaymentMethodId?: string;
  couponCode?: string;
};

export function checkout(userId: string, body: CheckoutRequest, accessToken: string): Promise<number> {
  return request<number>(`/api/v1/cart/${userId}/checkout`, {
    method: "POST",
    body: JSON.stringify(body),
    accessToken,
    headers: { "Idempotency-Key": body.reference },
  });
}

export function getOrder(orderId: number, accessToken: string): Promise<Order> {
  return request<Order>(`/api/v1/orders/${orderId}`, { accessToken, cache: "no-store" });
}

// order-service's GET /api/v1/orders (unfiltered, every customer's orders) is
// admin-only at the gateway now — this hits the customer-scoped /mine endpoint
// instead, which resolves the caller's own orders from their JWT.
export function getMyOrders(accessToken: string): Promise<Order[]> {
  return request<Order[]>(`/api/v1/orders/mine`, { accessToken, cache: "no-store" });
}

export function refundOrder(orderId: number, accessToken: string): Promise<Order> {
  return request<Order>(`/api/v1/orders/${orderId}/refund`, { method: "POST", accessToken });
}

// A seller's own order lines across every order — the "orders to fulfill" view.
export function getMyOrderLines(accessToken: string): Promise<OrderLine[]> {
  return request<OrderLine[]>(`/api/v1/order-lines/mine`, { accessToken, cache: "no-store" });
}

export function updateOrderLineFulfillment(
  orderLineId: number,
  body: { status: OrderLineStatus; trackingNumber?: string },
  accessToken: string,
): Promise<OrderLine> {
  return request<OrderLine>(`/api/v1/order-lines/${orderLineId}/fulfillment`, {
    method: "PATCH",
    body: JSON.stringify(body),
    accessToken,
  });
}

export type SellerRegistrationRequest = {
  businessName: string;
  businessEmail: string;
  description?: string;
};

export type Seller = {
  id: number;
  keycloakUserId: string;
  businessName: string;
  businessEmail: string;
  description: string | null;
  status: "PENDING" | "ACTIVE" | "SUSPENDED";
  stripeAccountId: string | null;
  chargesEnabled: boolean;
  payoutsEnabled: boolean;
};

export function registerSeller(body: SellerRegistrationRequest, accessToken: string): Promise<Seller> {
  return request<Seller>("/api/v1/sellers/register", {
    method: "POST",
    body: JSON.stringify(body),
    accessToken,
  });
}

export type SellerPayout = {
  id: number;
  sellerId: string;
  orderReference: string;
  grossAmount: number;
  commissionAmount: number;
  netAmount: number;
  status: "PENDING" | "PAID" | "FAILED";
  stripeTransferId: string | null;
  failureReason: string | null;
  createdDate: string;
};

export function getMyPayouts(accessToken: string): Promise<SellerPayout[]> {
  return request<SellerPayout[]>("/api/v1/payouts", { accessToken, cache: "no-store" });
}

// 404 just means "hasn't registered as a seller yet" — not an error worth
// surfacing. Anything else (network failure, 500) still propagates.
export async function getMySellerProfile(accessToken: string): Promise<Seller | null> {
  try {
    return await request<Seller>("/api/v1/sellers/me", { accessToken, cache: "no-store" });
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      return null;
    }
    throw error;
  }
}

// --- Admin ---

export function getCategories(): Promise<Category[]> {
  return request<Category[]>("/api/v1/categories", { cache: "no-store" });
}

export function createCategory(body: { name: string; description?: string }, accessToken: string): Promise<number> {
  return request<number>("/api/v1/categories", { method: "POST", body: JSON.stringify(body), accessToken });
}

export function updateCategory(
  id: number,
  body: { name: string; description?: string },
  accessToken: string,
): Promise<void> {
  return request(`/api/v1/categories/${id}`, { method: "PUT", body: JSON.stringify(body), accessToken });
}

export function deleteCategory(id: number, accessToken: string): Promise<void> {
  return request(`/api/v1/categories/${id}`, { method: "DELETE", accessToken });
}

export function getAllSellers(accessToken: string): Promise<Seller[]> {
  return request<Seller[]>("/api/v1/sellers", { accessToken, cache: "no-store" });
}

export function updateSellerStatus(
  id: number,
  status: Seller["status"],
  accessToken: string,
): Promise<Seller> {
  return request<Seller>(`/api/v1/sellers/${id}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
    accessToken,
  });
}

export function getAllOrders(accessToken: string): Promise<Order[]> {
  return request<Order[]>("/api/v1/orders", { accessToken, cache: "no-store" });
}
