"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { getProducts } from "@/lib/api";
import { useWishlist } from "@/lib/wishlist";
import { ProductCard } from "@/components/ProductCard";

export default function WishlistPage() {
  const { ids } = useWishlist();
  const { data: products, isLoading, isError } = useQuery({
    queryKey: ["products"],
    queryFn: getProducts,
  });

  const wishlisted = products?.filter((product) => ids.includes(product.id)) ?? [];

  return (
    <div className="mx-auto w-full max-w-6xl flex-1 px-6 py-10">
      <h1 className="mb-8 text-2xl font-semibold tracking-tight">Wishlist</h1>

      {isLoading ? (
        <p className="text-foreground/60">Loading…</p>
      ) : isError ? (
        <p className="text-foreground/60">Couldn&apos;t load your wishlist right now — try refreshing.</p>
      ) : ids.length === 0 ? (
        <p className="text-foreground/60">
          Nothing saved yet.{" "}
          <Link href="/" className="text-accent hover:underline">
            Browse the catalog
          </Link>{" "}
          and tap the heart on anything you like.
        </p>
      ) : (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
          {wishlisted.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      )}
    </div>
  );
}
