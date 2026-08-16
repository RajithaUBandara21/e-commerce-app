import type { MetadataRoute } from "next";
import { getProducts } from "@/lib/api";
import { SITE_URL } from "@/lib/seo";

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const products = await getProducts().catch(() => []);

  return [
    {
      url: SITE_URL,
      changeFrequency: "daily",
      priority: 1,
    },
    ...products.map((product) => ({
      url: `${SITE_URL}/products/${product.id}`,
      changeFrequency: "daily" as const,
      priority: 0.8,
    })),
  ];
}
