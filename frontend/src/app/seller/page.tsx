import Link from "next/link";
import { auth } from "@/auth";
import { getMyOrderLines, getMyPayouts, getMyProducts, getMySellerProfile } from "@/lib/api";
import { SellerStatusGate } from "@/components/SellerStatusGate";

export const dynamic = "force-dynamic";

export default async function SellerOverviewPage() {
  const session = await auth();
  if (!session?.userId || !session.accessToken) {
    return null;
  }

  const seller = await getMySellerProfile(session.accessToken);

  return (
    <SellerStatusGate seller={seller}>
      <OverviewContent sellerId={session.userId} accessToken={session.accessToken} />
    </SellerStatusGate>
  );
}

async function OverviewContent({ sellerId, accessToken }: { sellerId: string; accessToken: string }) {
  const [products, orderLines, payouts] = await Promise.all([
    getMyProducts(sellerId).catch(() => []),
    getMyOrderLines(accessToken).catch(() => []),
    getMyPayouts(accessToken).catch(() => []),
  ]);

  const distinctOrders = new Set(orderLines.map((line) => line.orderId)).size;
  const pendingFulfillment = orderLines.filter((line) => line.status === "PENDING").length;
  const totalEarned = payouts.filter((p) => p.status === "PAID").reduce((sum, p) => sum + p.netAmount, 0);
  const pendingPayout = payouts.filter((p) => p.status === "PENDING").reduce((sum, p) => sum + p.netAmount, 0);

  const cards = [
    { label: "Products listed", value: products.length.toString() },
    { label: "Orders", value: distinctOrders.toString() },
    { label: "Awaiting fulfillment", value: pendingFulfillment.toString(), href: "/seller/orders" },
    { label: "Paid out to date", value: `$${totalEarned.toFixed(2)}` },
    { label: "Pending payout", value: `$${pendingPayout.toFixed(2)}`, href: "/seller/payouts" },
  ];

  return (
    <>
      <h1 className="mb-8 text-2xl font-semibold tracking-tight">Overview</h1>

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
        {cards.map((card) => {
          const content = (
            <>
              <p className="text-xs uppercase tracking-wide text-foreground/50">{card.label}</p>
              <p className="text-2xl font-semibold">{card.value}</p>
            </>
          );
          return card.href ? (
            <Link
              key={card.label}
              href={card.href}
              className="rounded-2xl border border-border-subtle p-4 transition-colors hover:border-accent"
            >
              {content}
            </Link>
          ) : (
            <div key={card.label} className="rounded-2xl border border-border-subtle p-4">
              {content}
            </div>
          );
        })}
      </div>

      {pendingFulfillment > 0 && (
        <p className="mt-8 text-sm text-foreground/60">
          You have {pendingFulfillment} order{pendingFulfillment === 1 ? "" : "s"} waiting to ship —{" "}
          <Link href="/seller/orders" className="text-accent hover:underline">
            fulfill them now
          </Link>
          .
        </p>
      )}
    </>
  );
}
