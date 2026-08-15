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
