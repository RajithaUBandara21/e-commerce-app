import type { Cart, Order, Product } from "@/lib/types";

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
};

export function checkout(userId: string, body: CheckoutRequest, accessToken: string): Promise<number> {
  return request<number>(`/api/v1/cart/${userId}/checkout`, {
    method: "POST",
    body: JSON.stringify(body),
    accessToken,
    headers: { "Idempotency-Key": body.reference },
  });
}

// order-service's GET /api/v1/orders returns every order unfiltered — not
// customer-scoped — so there is deliberately no "list my orders" call here.
// Only fetch-by-id is exposed, for the single order the frontend just created.
export function getOrder(orderId: number, accessToken: string): Promise<Order> {
  return request<Order>(`/api/v1/orders/${orderId}`, { accessToken, cache: "no-store" });
}
