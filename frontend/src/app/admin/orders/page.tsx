import { auth } from "@/auth";
import { getAllOrders } from "@/lib/api";
import type { Order, OrderStatus } from "@/lib/types";
import { AdminGate } from "@/components/AdminGate";

export const dynamic = "force-dynamic";

const STATUS_STYLES: Record<OrderStatus, string> = {
  PENDING_PAYMENT: "bg-surface-muted text-foreground/60",
  CONFIRMED: "bg-accent-soft text-accent",
  PAYMENT_FAILED: "bg-red-100 text-red-700",
  CANCELLED: "bg-red-100 text-red-700",
  SHIPPED: "bg-blue-100 text-blue-700",
  DELIVERED: "bg-green-100 text-green-700",
  REFUNDED: "bg-surface-muted text-foreground/60",
};

export default async function AdminOrdersPage() {
  const session = await auth();
  if (!session?.userId || !session.accessToken) {
    return null;
  }

  const isAdmin = session.roles?.includes("admin") ?? false;

  return (
    <AdminGate isAdmin={isAdmin}>
      <OrdersContent accessToken={session.accessToken} />
    </AdminGate>
  );
}

async function OrdersContent({ accessToken }: { accessToken: string }) {
  let orders: Order[];
  let loadFailed = false;
  try {
    orders = await getAllOrders(accessToken);
  } catch (error) {
    console.error("Failed to load orders:", error);
    loadFailed = true;
    orders = [];
  }

  return (
    <>
      <h1 className="mb-8 text-2xl font-semibold tracking-tight">All orders</h1>

      {loadFailed ? (
        <p className="text-foreground/60">Couldn&apos;t load orders right now — try refreshing.</p>
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
                  {order.paymentMethode} · ${order.amount.toFixed(2)} · customer {order.customerId}
                </p>
              </div>
              <span className={`w-fit rounded-full px-3 py-1 text-xs font-semibold ${STATUS_STYLES[order.status]}`}>
                {order.status.replace("_", " ")}
              </span>
            </div>
          ))}
        </div>
      )}
    </>
  );
}
