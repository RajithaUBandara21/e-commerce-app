import { getProducts } from "@/lib/api";
import type { Product } from "@/lib/types";
import { ProductCard } from "@/components/ProductCard";
import { Hero } from "@/components/Hero";
import { TrustBadges } from "@/components/TrustBadges";
import { CategoryGrid } from "@/components/CategoryGrid";
import { PromoBanner } from "@/components/PromoBanner";

// Stock/price are live data — never prerender this at build time.
export const dynamic = "force-dynamic";

export default async function HomePage({ searchParams }: PageProps<"/">) {
  const { q, category } = await searchParams;
  const query = typeof q === "string" ? q.trim().toLowerCase() : "";
  const categoryId = typeof category === "string" ? Number(category) : undefined;

  let products: Product[];
  let loadFailed = false;
  try {
    products = await getProducts();
  } catch (error) {
    // A downstream outage (api-gateway/product-service down or still
    // starting) shouldn't crash the whole page with a raw stack trace —
    // degrade to a friendly message instead.
    console.error("Failed to load catalog:", error);
    loadFailed = true;
    products = [];
  }

  // Higher id == created more recently, so this is a real "New Arrivals"
  // ordering, not an arbitrary one — the backend has no createdAt field.
  const newArrivals = [...products].sort((a, b) => b.id - a.id);

  const filtered = newArrivals.filter((product) => {
    const matchesQuery = query ? product.name.toLowerCase().includes(query) : true;
    const matchesCategory = categoryId ? product.categoryId === categoryId : true;
    return matchesQuery && matchesCategory;
  });

  const isFiltering = query.length > 0 || categoryId !== undefined;

  return (
    <div className="flex-1">
      {loadFailed ? (
        <div className="mx-auto w-full max-w-5xl px-6 py-10">
          <p className="text-foreground/60">
            Couldn&apos;t load the catalog right now — the backend may still be starting up. Try refreshing in a
            moment.
          </p>
        </div>
      ) : (
        <>
          {!isFiltering && <Hero featured={newArrivals} />}
          {!isFiltering && <TrustBadges />}
          {!isFiltering && <CategoryGrid products={products} />}

          <section className="mx-auto max-w-6xl px-6 py-14">
            <h2 className="mb-6 text-2xl font-semibold tracking-tight">
              {isFiltering ? "Search results" : "New Arrivals"}
            </h2>
            {filtered.length === 0 ? (
              <p className="text-foreground/60">No products match your search.</p>
            ) : (
              <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
                {(isFiltering ? filtered : filtered.slice(0, 8)).map((product) => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </div>
            )}
          </section>

          {!isFiltering && <PromoBanner />}
        </>
      )}
    </div>
  );
}
