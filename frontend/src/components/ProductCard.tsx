import Link from "next/link";
import type { Product } from "@/lib/types";

export function ProductCard({ product }: { product: Product }) {
  const totalStock = product.variants.reduce((sum, v) => sum + v.availableQuantity, 0);
  const colors = [...new Set(product.variants.map((v) => v.color))];

  return (
    <Link
      href={`/products/${product.id}`}
      className="group flex flex-col gap-2 rounded-lg border border-zinc-200 p-4 transition-colors hover:border-zinc-400 dark:border-zinc-800 dark:hover:border-zinc-600"
    >
      <span className="text-xs uppercase tracking-wide text-zinc-500">{product.categoryName}</span>
      <span className="font-medium">{product.name}</span>
      <span className="text-sm text-zinc-500">{colors.join(" · ")}</span>
      <span className="mt-auto flex items-center justify-between pt-2">
        <span className="font-semibold">${product.price.toFixed(2)}</span>
        <span className="text-xs text-zinc-500">{totalStock > 0 ? `${totalStock} in stock` : "Out of stock"}</span>
      </span>
    </Link>
  );
}
