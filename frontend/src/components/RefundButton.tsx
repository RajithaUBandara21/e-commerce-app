"use client";

import { useTransition } from "react";
import { refundOrderAction } from "@/lib/actions";

export function RefundButton({ orderId }: { orderId: number }) {
  const [isPending, startTransition] = useTransition();

  return (
    <button
      type="button"
      disabled={isPending}
      onClick={() => {
        if (!confirm("Refund this order? This cannot be undone.")) return;
        startTransition(async () => {
          await refundOrderAction(orderId);
        });
      }}
      className="rounded-full border border-border-subtle px-4 py-1.5 text-xs font-semibold hover:border-accent hover:text-accent disabled:opacity-50"
    >
      {isPending ? "Refunding…" : "Request refund"}
    </button>
  );
}
