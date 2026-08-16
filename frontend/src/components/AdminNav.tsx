"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const TABS = [
  { href: "/admin/categories", label: "Categories" },
  { href: "/admin/sellers", label: "Sellers" },
  { href: "/admin/orders", label: "Orders" },
];

// Not live until Phase 13 stands up the container — the link is here now so the
// dashboard doesn't need a follow-up change once it is.
const GRAFANA_URL = process.env.NEXT_PUBLIC_GRAFANA_URL ?? "http://localhost:4000";

export function AdminNav() {
  const pathname = usePathname();

  return (
    <nav className="mb-8 flex items-center justify-between border-b border-border-subtle">
      <div className="flex gap-1">
        {TABS.map((tab) => {
          const active = pathname.startsWith(tab.href);
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
      </div>
      <a href={GRAFANA_URL} target="_blank" rel="noopener noreferrer" className="pb-2 text-sm text-foreground/60 hover:text-accent">
        Grafana →
      </a>
    </nav>
  );
}
