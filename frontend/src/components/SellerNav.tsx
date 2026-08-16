"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const TABS = [
  { href: "/seller", label: "Overview" },
  { href: "/seller/products", label: "Products" },
  { href: "/seller/orders", label: "Orders" },
  { href: "/seller/payouts", label: "Payouts" },
];

export function SellerNav() {
  const pathname = usePathname();

  return (
    <nav className="mb-8 flex gap-1 border-b border-border-subtle">
      {TABS.map((tab) => {
        const active = tab.href === "/seller" ? pathname === "/seller" : pathname.startsWith(tab.href);
        return (
          <Link
            key={tab.href}
            href={tab.href}
            className={`border-b-2 px-4 py-2 text-sm font-medium ${
              active ? "border-accent text-accent" : "border-transparent text-foreground/60 hover:text-foreground"
            }`}
          >
            {tab.label}
          </Link>
        );
      })}
    </nav>
  );
}
