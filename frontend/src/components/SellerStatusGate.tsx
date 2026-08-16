import Link from "next/link";
import type { Seller } from "@/lib/api";
import { SellerNav } from "@/components/SellerNav";

// Shared across every /seller/* page: unregistered and non-ACTIVE sellers see the
// same messaging everywhere rather than each page reimplementing it slightly
// differently.
export function SellerStatusGate({ seller, children }: { seller: Seller | null; children: React.ReactNode }) {
  if (!seller) {
    return (
      <div className="mx-auto w-full max-w-2xl flex-1 px-6 py-10">
        <h1 className="mb-4 text-2xl font-semibold tracking-tight">Sell on Cloth Shop</h1>
        <p className="mb-6 text-foreground/60">
          You don&apos;t have a seller account yet — register to start listing products.
        </p>
        <Link
          href="/seller/onboarding"
          className="inline-block rounded-full bg-accent px-6 py-3 text-sm font-semibold text-accent-foreground hover:bg-accent-hover"
        >
          Register as a seller
        </Link>
      </div>
    );
  }

  if (seller.status !== "ACTIVE") {
    return (
      <div className="mx-auto w-full max-w-2xl flex-1 px-6 py-10">
        <h1 className="mb-4 text-2xl font-semibold tracking-tight">Application {seller.status.toLowerCase()}</h1>
        <p className="text-foreground/60">
          {seller.status === "PENDING"
            ? "Your seller application is awaiting admin approval. Check back soon."
            : "Your seller account is suspended. Contact support for details."}
        </p>
      </div>
    );
  }

  return (
    <div className="mx-auto w-full max-w-4xl flex-1 px-6 py-10">
      <SellerNav />
      {children}
    </div>
  );
}
