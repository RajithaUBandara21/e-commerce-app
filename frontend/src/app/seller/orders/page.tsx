import { auth } from "@/auth";
import { getMyOrderLines, getMySellerProfile } from "@/lib/api";
import type { OrderLine } from "@/lib/types";
import { SellerStatusGate } from "@/components/SellerStatusGate";
import { FulfillmentForm } from "@/components/FulfillmentForm";

export const dynamic = "force-dynamic";

const STATUS_STYLES: Record<OrderLine["status"], string> = {
  PENDING: "bg-surface-muted text-foreground/60",
  SHIPPED: "bg-blue-100 text-blue-700",
  DELIVERED: "bg-green-100 text-green-700",
};

export default async function SellerOrdersPage() {
  const session = await auth();
  if (!session?.userId || !session.accessToken) {
    return null;
  }

  const seller = await getMySellerProfile(session.accessToken);

  return (
    <SellerStatusGate seller={seller}>
      <OrdersContent accessToken={session.accessToken} />
    </SellerStatusGate>
  );
}

async function OrdersContent({ accessToken }: { accessToken: string }) {
  let lines: OrderLine[];
  let loadFailed = false;
  try {
    lines = await getMyOrderLines(accessToken);
  } catch (error) {
    console.error("Failed to load order lines:", error);
    loadFailed = true;
    lines = [];
  }

  // Pending fulfillment first — that's the actionable queue.
  const sorted = [...lines].sort((a, b) => a.status.localeCompare(b.status));

  return (
    <>
      <h1 className="mb-8 text-2xl font-semibold tracking-tight">Orders to fulfill</h1>

      {loadFailed ? (
        <p className="text-foreground/60">Couldn&apos;t load your orders right now — try refreshing.</p>
      ) : sorted.length === 0 ? (
        <p className="text-foreground/60">No orders yet.</p>
      ) : (
        <div className="flex flex-col gap-3">
          {sorted.map((line) => (
            <div
              key={line.id}
              className="flex flex-col gap-3 rounded-2xl border border-border-subtle p-4 sm:flex-row sm:items-center sm:justify-between"
            >
              <div>
                <p className="font-medium">
                  Order #{line.orderId} · Variant #{line.variantId}
                </p>
                <p className="text-sm text-foreground/60">
                  Qty {line.quantity}
                  {line.trackingNumber && ` · Tracking: ${line.trackingNumber}`}
                </p>
              </div>
              <div className="flex items-center gap-3">
                <span className={`rounded-full px-3 py-1 text-xs font-semibold ${STATUS_STYLES[line.status]}`}>
                  {line.status}
                </span>
                <FulfillmentForm line={line} />
              </div>
            </div>
          ))}
        </div>
      )}
    </>
  );
}
