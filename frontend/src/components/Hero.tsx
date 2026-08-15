import Link from "next/link";
import type { Product } from "@/lib/types";
import { ProductArt } from "@/components/ProductArt";

export function Hero({ featured }: { featured: Product[] }) {
  const cards = featured.slice(0, 2);

  return (
    <section className="relative overflow-hidden bg-surface-muted">
      <div className="mx-auto grid max-w-6xl items-center gap-10 px-6 py-16 lg:grid-cols-2 lg:py-24">
        <div className="flex flex-col items-start gap-6">
          <span className="rounded-full bg-accent-soft px-3 py-1 text-xs font-semibold uppercase tracking-wide text-accent">
            New season
          </span>
          <h1 className="text-4xl font-bold leading-tight tracking-tight sm:text-5xl">
            Everyday clothing, <span className="text-accent">made to last</span>
          </h1>
          <p className="max-w-md text-foreground/60">
            Considered basics and statement pieces from a catalog that changes every week — shop the full
            collection below.
          </p>
          <Link
            href="#categories"
            className="rounded-full bg-accent px-6 py-3 text-sm font-semibold text-accent-foreground transition-colors hover:bg-accent-hover"
          >
            Shop the collection
          </Link>
        </div>

        <div className="relative mx-auto flex h-72 w-full max-w-sm items-center justify-center sm:h-96">
          <div className="absolute h-64 w-64 rounded-full bg-accent-soft sm:h-80 sm:w-80" aria-hidden="true" />

          {cards.length > 0 && (
            <Link
              href={`/products/${cards[0].id}`}
              className="absolute left-0 top-4 w-36 rounded-2xl border border-border-subtle bg-background p-2 shadow-lg sm:w-44"
            >
              <ProductArt seed={`${cards[0].id}-${cards[0].name}`} className="aspect-square w-full rounded-xl" />
              <p className="mt-2 truncate text-xs font-medium">{cards[0].name}</p>
              <p className="text-sm font-semibold">${cards[0].price.toFixed(2)}</p>
            </Link>
          )}

          {cards.length > 1 && (
            <Link
              href={`/products/${cards[1].id}`}
              className="absolute bottom-2 right-0 w-36 rounded-2xl border border-border-subtle bg-background p-2 shadow-lg sm:w-44"
            >
              <ProductArt seed={`${cards[1].id}-${cards[1].name}`} className="aspect-square w-full rounded-xl" />
              <p className="mt-2 truncate text-xs font-medium">{cards[1].name}</p>
              <p className="text-sm font-semibold">${cards[1].price.toFixed(2)}</p>
            </Link>
          )}
        </div>
      </div>
    </section>
  );
}
