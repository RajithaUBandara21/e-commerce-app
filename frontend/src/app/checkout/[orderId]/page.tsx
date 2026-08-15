import Link from "next/link";
import { notFound } from "next/navigation";
import { auth } from "@/auth";
import { getOrder } from "@/lib/api";

const STATUS_COPY: Record<string, string> = {
  PENDING_PAYMENT: "We've received your order and are reserving your items.",
  CONFIRMED: "Payment confirmed — your order is on its way to fulfillment.",
  PAYMENT_FAILED: "Payment didn't go through. No items were reserved for long — you can try again.",
  CANCELLED: "This order was cancelled — one or more items were out of stock.",
  SHIPPED: "Your order has shipped.",
  DELIVERED: "Your order was delivered.",
  REFUNDED: "This order was refunded.",
};

export default async function CheckoutStatusPage({ params }: { params: Promise<{ orderId: string }> }) {
  const { orderId } = await params;
  const session = await auth();
  if (!session?.accessToken) return null;

  const order = await getOrder(Number(orderId), session.accessToken).catch(() => null);
  if (!order) notFound();

  return (
    <div className="mx-auto flex w-full max-w-lg flex-1 flex-col gap-4 px-6 py-16 text-center">
      <h1 className="text-2xl font-semibold tracking-tight">Order {order.reference}</h1>
      <p className="text-sm uppercase tracking-wide text-zinc-500">{order.status}</p>
      <p className="text-zinc-600 dark:text-zinc-400">
        {STATUS_COPY[order.status] ?? "We'll email you as this order updates."}
      </p>
      <p className="text-sm text-zinc-500">
        Status here is a snapshot from when the page loaded — the saga updates it asynchronously, so refresh to see
        the latest.
      </p>
      <Link href="/" className="mt-4 text-sm underline">
        Continue shopping
      </Link>
    </div>
  );
}
