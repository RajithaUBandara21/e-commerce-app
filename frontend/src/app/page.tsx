import { getProducts } from "@/lib/api";
import { ProductCard } from "@/components/ProductCard";

// Stock/price are live data — never prerender this at build time.
export const dynamic = "force-dynamic";

export default async function HomePage() {
  const products = await getProducts();

  return (
    <div className="mx-auto w-full max-w-5xl flex-1 px-6 py-10">
      <h1 className="mb-8 text-2xl font-semibold tracking-tight">Catalog</h1>
      {products.length === 0 ? (
        <p className="text-zinc-500">No products yet.</p>
      ) : (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
          {products.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      )}
    </div>
  );
}
