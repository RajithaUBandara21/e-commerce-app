import { auth } from "@/auth";
import { getMyOrders } from "@/lib/api";
import type { Order, OrderStatus } from "@/lib/types";
import { RefundButton } from "@/components/RefundButton";

export const dynamic = "force-dynamic";

const REFUNDABLE = new Set<OrderStatus>(["CONFIRMED", "SHIPPED", "DELIVERED"]);

const STATUS_STYLES: Record<OrderStatus, string> = {
  PENDING_PAYMENT: "bg-surface-muted text-foreground/60",
  CONFIRMED: "bg-accent-soft text-accent",
  PAYMENT_FAILED: "bg-red-100 text-red-700",
  CANCELLED: "bg-red-100 text-red-700",
  SHIPPED: "bg-blue-100 text-blue-700",
  DELIVERED: "bg-green-100 text-green-700",
  REFUNDED: "bg-surface-muted text-foreground/60",
};

export default async function OrdersPage() {
  const session = await auth();
  if (!session?.userId || !session.accessToken) {
    return null;
  }

  let orders: Order[];
  let loadFailed = false;
  try {
    orders = await getMyOrders(session.accessToken);
  } catch (error) {
    console.error("Failed to load orders:", error);
    loadFailed = true;
    orders = [];
  }

  return (
    <div className="mx-auto w-full max-w-3xl flex-1 px-6 py-10">
      <h1 className="mb-8 text-2xl font-semibold tracking-tight">Your orders</h1>

      {loadFailed ? (
        <p className="text-foreground/60">Couldn&apos;t load your orders right now — try refreshing.</p>
      ) : orders.length === 0 ? (
        <p className="text-foreground/60">No orders yet.</p>
      ) : (
        <div className="flex flex-col gap-3">
          {orders.map((order) => (
            <div
              key={order.id}
              className="flex flex-col gap-3 rounded-2xl border border-border-subtle p-4 sm:flex-row sm:items-center sm:justify-between"
            >
              <div>
                <p className="font-medium">{order.reference}</p>
                <p className="text-sm text-foreground/60">
                  {order.paymentMethode} · ${order.amount.toFixed(2)}
                </p>
              </div>
              <div className="flex items-center gap-3">
                <span className={`rounded-full px-3 py-1 text-xs font-semibold ${STATUS_STYLES[order.status]}`}>
                  {order.status.replace("_", " ")}
                </span>
                {REFUNDABLE.has(order.status) && <RefundButton orderId={order.id} />}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
