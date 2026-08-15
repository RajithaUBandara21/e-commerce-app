"use client";

import Link from "next/link";
import { Heart } from "lucide-react";
import type { Product } from "@/lib/types";
import { ProductArt } from "@/components/ProductArt";
import { useWishlist } from "@/lib/wishlist";

export function ProductCard({ product }: { product: Product }) {
  const totalStock = product.variants.reduce((sum, v) => sum + v.availableQuantity, 0);
  const colors = [...new Set(product.variants.map((v) => v.color))];
  const { isWishlisted, toggle } = useWishlist();
  const wishlisted = isWishlisted(product.id);

  return (
    <div className="group relative flex flex-col overflow-hidden rounded-2xl border border-border-subtle bg-background transition-shadow hover:shadow-lg">
      <button
        type="button"
        onClick={(e) => {
          e.preventDefault();
          toggle(product.id);
        }}
        aria-label={wishlisted ? "Remove from wishlist" : "Add to wishlist"}
        aria-pressed={wishlisted}
        className="absolute right-3 top-3 z-10 flex h-8 w-8 items-center justify-center rounded-full bg-background/90 shadow-sm backdrop-blur transition-transform hover:scale-105"
      >
        <Heart className={`h-4 w-4 ${wishlisted ? "fill-accent text-accent" : "text-foreground"}`} />
      </button>

      <Link href={`/products/${product.id}`} className="flex flex-1 flex-col">
        <ProductArt seed={`${product.id}-${product.name}`} className="aspect-4/5 w-full" />

        <div className="flex flex-1 flex-col gap-1 p-4">
          <span className="text-xs uppercase tracking-wide text-foreground/50">{product.categoryName}</span>
          <span className="font-medium leading-snug">{product.name}</span>
          {colors.length > 0 && <span className="text-sm text-foreground/50">{colors.join(" · ")}</span>}
          <span className="mt-auto flex items-center justify-between pt-3">
            <span className="text-lg font-semibold">${product.price.toFixed(2)}</span>
            <span className={`text-xs ${totalStock > 0 ? "text-foreground/50" : "text-red-600"}`}>
              {totalStock > 0 ? `${totalStock} in stock` : "Out of stock"}
            </span>
          </span>
        </div>
      </Link>
    </div>
  );
}
