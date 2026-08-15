import { notFound } from "next/navigation";
import { getProduct } from "@/lib/api";
import { VariantPicker } from "@/components/VariantPicker";

export default async function ProductPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const productId = Number(id);
  if (Number.isNaN(productId)) notFound();

  const product = await getProduct(productId).catch(() => null);
  if (!product) notFound();

  return (
    <div className="mx-auto grid w-full max-w-4xl flex-1 gap-10 px-6 py-10 sm:grid-cols-2">
      <div className="aspect-square rounded-lg bg-zinc-100 dark:bg-zinc-900" />

      <div className="flex flex-col gap-4">
        <div>
          <span className="text-xs uppercase tracking-wide text-zinc-500">{product.categoryName}</span>
          <h1 className="text-2xl font-semibold tracking-tight">{product.name}</h1>
        </div>
        <p className="text-xl font-semibold">${product.price.toFixed(2)}</p>
        <p className="text-zinc-600 dark:text-zinc-400">{product.description}</p>
        <VariantPicker variants={product.variants} />
      </div>
    </div>
  );
}
