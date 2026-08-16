import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { getProduct } from "@/lib/api";
import { VariantPicker } from "@/components/VariantPicker";
import { ProductArt } from "@/components/ProductArt";
import { StarRating } from "@/components/StarRating";
import { ReviewSection } from "@/components/ReviewSection";
import { SITE_URL } from "@/lib/seo";

type Props = { params: Promise<{ id: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id } = await params;
  const productId = Number(id);
  if (Number.isNaN(productId)) return {};

  const product = await getProduct(productId).catch(() => null);
  if (!product) return {};

  const image = product.imageUrls[0];

  return {
    title: product.name,
    description: product.description || `${product.name} — ${product.categoryName} at $${product.price.toFixed(2)}.`,
    alternates: { canonical: `${SITE_URL}/products/${product.id}` },
    openGraph: {
      type: "website",
      title: product.name,
      description: product.description || undefined,
      images: image ? [{ url: image }] : undefined,
    },
  };
}

export default async function ProductPage({ params }: Props) {
  const { id } = await params;
  const productId = Number(id);
  if (Number.isNaN(productId)) notFound();

  const product = await getProduct(productId).catch(() => null);
  if (!product) notFound();

  const totalStock = product.variants.reduce((sum, v) => sum + v.availableQuantity, 0);
  const jsonLd = {
    "@context": "https://schema.org",
    "@type": "Product",
    name: product.name,
    description: product.description,
    category: product.categoryName,
    image: product.imageUrls,
    ...(product.averageRating !== null && product.reviewCount > 0
      ? {
          aggregateRating: {
            "@type": "AggregateRating",
            ratingValue: product.averageRating,
            reviewCount: product.reviewCount,
          },
        }
      : {}),
    offers: {
      "@type": "Offer",
      url: `${SITE_URL}/products/${product.id}`,
      priceCurrency: "USD",
      price: product.price,
      availability: totalStock > 0 ? "https://schema.org/InStock" : "https://schema.org/OutOfStock",
    },
  };

  return (
    <div className="mx-auto flex w-full max-w-4xl flex-1 flex-col gap-10 px-6 py-10">
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }} />
      <div className="grid gap-10 sm:grid-cols-2">
        {product.imageUrls.length > 0 ? (
          // eslint-disable-next-line @next/next/no-img-element -- see ProductCard's note
          <img
            src={product.imageUrls[0]}
            alt={product.name}
            className="aspect-square w-full rounded-2xl object-cover"
          />
        ) : (
          <ProductArt seed={`${product.id}-${product.name}`} className="aspect-square w-full rounded-2xl" />
        )}

        <div className="flex flex-col gap-4">
          <div>
            <span className="text-xs uppercase tracking-wide text-foreground/50">{product.categoryName}</span>
            <h1 className="text-2xl font-semibold tracking-tight">{product.name}</h1>
          </div>
          {product.reviewCount > 0 && product.averageRating !== null && (
            <div className="flex items-center gap-2">
              <StarRating rating={product.averageRating} />
              <span className="text-sm text-foreground/60">
                {product.averageRating.toFixed(1)} ({product.reviewCount} review{product.reviewCount === 1 ? "" : "s"})
              </span>
            </div>
          )}
          <p className="text-xl font-semibold">${product.price.toFixed(2)}</p>
          <p className="text-foreground/60">{product.description}</p>
          <VariantPicker variants={product.variants} />
        </div>
      </div>

      <ReviewSection productId={product.id} />
    </div>
  );
}
