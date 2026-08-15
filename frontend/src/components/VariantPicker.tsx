"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { useSession } from "next-auth/react";
import type { ProductVariant } from "@/lib/types";
import { addToCartAction } from "@/lib/actions";

export function VariantPicker({ variants }: { variants: ProductVariant[] }) {
  const { status } = useSession();
  const router = useRouter();
  const [isPending, startTransition] = useTransition();

  const sizes = [...new Set(variants.map((v) => v.size))];
  const colors = [...new Set(variants.map((v) => v.color))];

  const [size, setSize] = useState(sizes[0]);
  const [color, setColor] = useState(colors[0]);
  const [message, setMessage] = useState<string | null>(null);

  const selected = variants.find((v) => v.size === size && v.color === color);

  function handleAddToCart() {
    if (status !== "authenticated") {
      router.push("/cart"); // proxy.ts redirects unauthenticated visits to sign-in
      return;
    }
    if (!selected || selected.availableQuantity <= 0) return;

    startTransition(async () => {
      await addToCartAction(selected.id, 1);
      setMessage("Added to cart.");
    });
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-2">
        <span className="text-sm font-medium">Size</span>
        <div className="flex gap-2">
          {sizes.map((s) => (
            <button
              key={s}
              onClick={() => setSize(s)}
              className={`rounded border px-3 py-1.5 text-sm ${
                s === size ? "border-foreground" : "border-zinc-300 dark:border-zinc-700"
              }`}
            >
              {s}
            </button>
          ))}
        </div>
      </div>

      <div className="flex flex-col gap-2">
        <span className="text-sm font-medium">Color</span>
        <div className="flex gap-2">
          {colors.map((c) => (
            <button
              key={c}
              onClick={() => setColor(c)}
              className={`rounded border px-3 py-1.5 text-sm ${
                c === color ? "border-foreground" : "border-zinc-300 dark:border-zinc-700"
              }`}
            >
              {c}
            </button>
          ))}
        </div>
      </div>

      <p className="text-sm text-zinc-500">
        {selected
          ? selected.availableQuantity > 0
            ? `${selected.availableQuantity} in stock (SKU ${selected.sku})`
            : "Out of stock in this size/color"
          : "This size/color combination isn't available"}
      </p>

      <button
        onClick={handleAddToCart}
        disabled={!selected || selected.availableQuantity <= 0 || isPending}
        className="rounded-full bg-foreground px-5 py-2.5 text-background disabled:opacity-40"
      >
        {isPending ? "Adding…" : status === "authenticated" ? "Add to cart" : "Sign in to add to cart"}
      </button>

      {message && <p className="text-sm text-green-600">{message}</p>}
    </div>
  );
}
