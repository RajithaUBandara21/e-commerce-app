import Link from "next/link";
import type { Product } from "@/lib/types";
import { ProductArt } from "@/components/ProductArt";

// Categories are derived from the products the backend actually returned —
// there's no category-listing endpoint yet (a known gap), so this is the
// only source of truth available without inventing category names.
export function CategoryGrid({ products }: { products: Product[] }) {
  const categories = new Map<number, { name: string; count: number }>();
  for (const product of products) {
    const existing = categories.get(product.categoryId);
    if (existing) {
      existing.count += 1;
    } else {
      categories.set(product.categoryId, { name: product.categoryName, count: 1 });
    }
  }

  if (categories.size === 0) return null;

  return (
    <section id="categories" className="mx-auto max-w-6xl px-6 py-14">
      <h2 className="mb-6 text-2xl font-semibold tracking-tight">Shop by Category</h2>
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
        {[...categories.entries()].map(([categoryId, { name, count }]) => (
          <Link
            key={categoryId}
            href={`/?category=${categoryId}`}
            className="group relative flex aspect-square flex-col justify-end overflow-hidden rounded-2xl"
          >
            <ProductArt seed={name} className="absolute inset-0 h-full w-full transition-transform group-hover:scale-105" />
            <div className="relative bg-gradient-to-t from-black/60 to-transparent p-4">
              <p className="font-semibold text-white">{name}</p>
              <p className="text-xs text-white/80">
                {count} {count === 1 ? "item" : "items"}
              </p>
            </div>
          </Link>
        ))}
      </div>
    </section>
  );
}
