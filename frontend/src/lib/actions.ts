"use server";

import { revalidatePath } from "next/cache";
import { auth } from "@/auth";
import * as api from "@/lib/api";

async function requireSession() {
  const session = await auth();
  if (!session?.userId || !session.accessToken) {
    throw new Error("Sign in required");
  }
  return { userId: session.userId, accessToken: session.accessToken };
}

export async function addToCartAction(variantId: number, quantity: number) {
  const { userId, accessToken } = await requireSession();
  await api.addCartItem(userId, variantId, quantity, accessToken);
  revalidatePath("/cart");
}

export async function updateCartItemAction(variantId: number, quantity: number) {
  const { userId, accessToken } = await requireSession();
  await api.setCartItemQuantity(userId, variantId, quantity, accessToken);
  revalidatePath("/cart");
}

export async function removeCartItemAction(variantId: number) {
  const { userId, accessToken } = await requireSession();
  await api.removeCartItem(userId, variantId, accessToken);
  revalidatePath("/cart");
}

export async function checkoutAction(body: api.CheckoutRequest) {
  const { userId, accessToken } = await requireSession();
  const orderId = await api.checkout(userId, body, accessToken);
  revalidatePath("/cart");
  return orderId;
}

export async function refundOrderAction(orderId: number) {
  const { accessToken } = await requireSession();
  await api.refundOrder(orderId, accessToken);
  revalidatePath("/orders");
}

export async function getImageUploadUrlAction(productId: number, contentType: string) {
  const { accessToken } = await requireSession();
  return api.getImageUploadUrl(productId, contentType, accessToken);
}

export async function registerProductImageAction(productId: number, objectKey: string) {
  const { accessToken } = await requireSession();
  await api.registerProductImage(productId, objectKey, accessToken);
  revalidatePath("/seller/products");
}

export async function registerSellerAction(body: api.SellerRegistrationRequest) {
  const { accessToken } = await requireSession();
  await api.registerSeller(body, accessToken);
  revalidatePath("/seller/products");
}

export async function submitReviewAction(productId: number, rating: number, comment: string) {
  const { accessToken } = await requireSession();
  await api.createReview(productId, { rating, comment: comment.trim() || undefined }, accessToken);
  revalidatePath(`/products/${productId}`);
}

export async function deleteReviewAction(productId: number, reviewId: number) {
  const { accessToken } = await requireSession();
  await api.deleteReview(productId, reviewId, accessToken);
  revalidatePath(`/products/${productId}`);
}

export async function createCategoryAction(name: string, description: string) {
  const { accessToken } = await requireSession();
  await api.createCategory({ name, description: description.trim() || undefined }, accessToken);
  revalidatePath("/admin/categories");
}

export async function updateCategoryAction(id: number, name: string, description: string) {
  const { accessToken } = await requireSession();
  await api.updateCategory(id, { name, description: description.trim() || undefined }, accessToken);
  revalidatePath("/admin/categories");
}

export async function deleteCategoryAction(id: number) {
  const { accessToken } = await requireSession();
  await api.deleteCategory(id, accessToken);
  revalidatePath("/admin/categories");
}

export async function updateSellerStatusAction(id: number, status: api.Seller["status"]) {
  const { accessToken } = await requireSession();
  await api.updateSellerStatus(id, status, accessToken);
  revalidatePath("/admin/sellers");
}

export async function fulfillOrderLineAction(
  orderLineId: number,
  status: "SHIPPED" | "DELIVERED",
  trackingNumber: string,
) {
  const { accessToken } = await requireSession();
  await api.updateOrderLineFulfillment(orderLineId, { status, trackingNumber: trackingNumber.trim() || undefined }, accessToken);
  revalidatePath("/seller/orders");
  revalidatePath("/seller");
}
