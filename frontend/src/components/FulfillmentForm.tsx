"use client";

import { useState, useTransition } from "react";
import type { OrderLine } from "@/lib/types";
import { fulfillOrderLineAction } from "@/lib/actions";

const NEXT_STATUS: Record<OrderLine["status"], "SHIPPED" | "DELIVERED" | null> = {
  PENDING: "SHIPPED",
  SHIPPED: "DELIVERED",
  DELIVERED: null,
};

export function FulfillmentForm({ line }: { line: OrderLine }) {
  const [trackingNumber, setTrackingNumber] = useState(line.trackingNumber ?? "");
  const [error, setError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  const nextStatus = NEXT_STATUS[line.status];
  if (!nextStatus) {
    return <span className="text-xs font-medium text-foreground/50">Delivered</span>;
  }

  return (
    <form
      className="flex items-center gap-2"
      onSubmit={(e) => {
        e.preventDefault();
        setError(null);
        startTransition(async () => {
          try {
            await fulfillOrderLineAction(line.id, nextStatus, trackingNumber);
          } catch {
            setError("Couldn't update this order line — try again.");
          }
        });
      }}
    >
      {nextStatus === "SHIPPED" && (
        <input
          type="text"
          value={trackingNumber}
          onChange={(e) => setTrackingNumber(e.target.value)}
          placeholder="Tracking number (optional)"
          className="w-44 rounded-full border border-border-subtle px-3 py-1.5 text-xs"
        />
      )}
      <button
        type="submit"
        disabled={isPending}
        className="shrink-0 rounded-full border border-border-subtle px-4 py-1.5 text-xs font-semibold hover:border-accent hover:text-accent disabled:opacity-50"
      >
        {isPending ? "Updating…" : `Mark ${nextStatus.toLowerCase()}`}
      </button>
      {error && <span className="text-xs text-red-600">{error}</span>}
    </form>
  );
}
