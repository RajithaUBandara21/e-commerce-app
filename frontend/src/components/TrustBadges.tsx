import { Headset, RotateCcw, ShieldCheck, Truck } from "lucide-react";

const BADGES = [
  { icon: Truck, title: "Free shipping", detail: "On orders over $50" },
  { icon: RotateCcw, title: "Easy returns", detail: "30-day return window" },
  { icon: ShieldCheck, title: "Secure checkout", detail: "Encrypted payments via Stripe" },
  { icon: Headset, title: "Real support", detail: "We reply within a day" },
];

export function TrustBadges() {
  return (
    <div className="border-y border-border-subtle bg-surface-muted">
      <div className="mx-auto grid max-w-6xl grid-cols-2 gap-6 px-6 py-8 sm:grid-cols-4">
        {BADGES.map(({ icon: Icon, title, detail }) => (
          <div key={title} className="flex items-center gap-3">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-accent-soft text-accent">
              <Icon className="h-5 w-5" />
            </div>
            <div className="min-w-0">
              <p className="text-sm font-semibold leading-tight">{title}</p>
              <p className="truncate text-xs text-foreground/50">{detail}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
