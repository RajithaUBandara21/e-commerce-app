import type { MetadataRoute } from "next";
import { SITE_URL } from "@/lib/seo";

export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      allow: "/",
      // Everything auth-gated in proxy.ts, plus API routes — nothing for a
      // crawler to index behind sign-in.
      disallow: ["/cart", "/checkout", "/account", "/orders", "/seller", "/admin", "/api", "/wishlist"],
    },
    sitemap: `${SITE_URL}/sitemap.xml`,
  };
}
