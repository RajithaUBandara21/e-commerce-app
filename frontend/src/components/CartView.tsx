"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { removeCartItemAction, updateCartItemAction, checkoutAction } from "@/lib/actions";

export type CartLine = {
  variantId: number;
  quantity: number;
  productName: string;
  size: string;
  color: string;
  price: number;
};

export function CartView({ lines }: { lines: CartLine[] }) {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);

  const total = lines.reduce((sum, line) => sum + line.price * line.quantity, 0);

  function handleQuantityChange(variantId: number, quantity: number) {
    startTransition(async () => {
      await updateCartItemAction(variantId, quantity);
    });
  }

  function handleRemove(variantId: number) {
    startTransition(async () => {
      await removeCartItemAction(variantId);
    });
  }

  function handleCheckout() {
    setError(null);
    startTransition(async () => {
      try {
        const orderId = await checkoutAction({
          reference: `web-${Date.now()}`,
          totalAmount: total,
          paymentMethode: "CREDIT_CARD",
        });
        router.push(`/checkout/${orderId}`);
      } catch {
        setError("Checkout failed — please try again.");
      }
    });
  }

  if (lines.length === 0) {
    return <p className="text-zinc-500">Your cart is empty.</p>;
  }

  return (
    <div className="flex flex-col gap-6">
      <ul className="flex flex-col divide-y divide-zinc-200 dark:divide-zinc-800">
        {lines.map((line) => (
          <li key={line.variantId} className="flex items-center justify-between gap-4 py-4">
            <div>
              <p className="font-medium">{line.productName}</p>
              <p className="text-sm text-zinc-500">
                {line.size} · {line.color} · ${line.price.toFixed(2)}
              </p>
            </div>
            <div className="flex items-center gap-3">
              <input
                type="number"
                min={0}
                value={line.quantity}
                disabled={isPending}
                onChange={(e) => handleQuantityChange(line.variantId, Number(e.target.value))}
                className="w-16 rounded border border-zinc-300 px-2 py-1 text-sm dark:border-zinc-700"
              />
              <button
                onClick={() => handleRemove(line.variantId)}
                disabled={isPending}
                className="text-sm text-zinc-500 underline"
              >
                Remove
              </button>
            </div>
          </li>
        ))}
      </ul>

      <div className="flex items-center justify-between border-t border-zinc-200 pt-4 dark:border-zinc-800">
        <span className="text-lg font-semibold">Total: ${total.toFixed(2)}</span>
        <button
          onClick={handleCheckout}
          disabled={isPending}
          className="rounded-full bg-foreground px-5 py-2.5 text-background disabled:opacity-40"
        >
          {isPending ? "Placing order…" : "Checkout"}
        </button>
      </div>

      {error && <p className="text-sm text-red-600">{error}</p>}
    </div>
  );
}
