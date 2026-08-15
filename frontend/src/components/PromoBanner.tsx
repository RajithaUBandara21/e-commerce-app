import Link from "next/link";

export function PromoBanner() {
  return (
    <section className="mx-auto max-w-6xl px-6 py-14">
      <div className="flex flex-col items-start gap-4 rounded-2xl bg-black px-8 py-12 text-white sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-2xl font-semibold tracking-tight">Free shipping on orders over $50</h2>
          <p className="mt-2 text-white/70">No code needed — applied automatically at checkout.</p>
        </div>
        <Link
          href="#categories"
          className="shrink-0 rounded-full bg-accent px-6 py-3 text-sm font-semibold text-accent-foreground transition-colors hover:bg-accent-hover"
        >
          Start shopping
        </Link>
      </div>
    </section>
  );
}
