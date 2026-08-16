"use client";

import { useTransition } from "react";
import type { Seller } from "@/lib/api";
import { updateSellerStatusAction } from "@/lib/actions";

const STATUS_STYLES: Record<Seller["status"], string> = {
  PENDING: "bg-surface-muted text-foreground/60",
  ACTIVE: "bg-green-100 text-green-700",
  SUSPENDED: "bg-red-100 text-red-700",
};

export function SellerModerationRow({ seller }: { seller: Seller }) {
  const [isPending, startTransition] = useTransition();

  function setStatus(status: Seller["status"]) {
    startTransition(async () => {
      await updateSellerStatusAction(seller.id, status);
    });
  }

  return (
    <div className="flex flex-col gap-3 rounded-2xl border border-border-subtle p-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <p className="font-medium">{seller.businessName}</p>
        <p className="text-sm text-foreground/60">{seller.businessEmail}</p>
      </div>
      <div className="flex items-center gap-3">
        <span className={`rounded-full px-3 py-1 text-xs font-semibold ${STATUS_STYLES[seller.status]}`}>
          {seller.status}
        </span>
        {seller.status !== "ACTIVE" && (
          <button
            type="button"
            disabled={isPending}
            onClick={() => setStatus("ACTIVE")}
            className="rounded-full border border-border-subtle px-4 py-1.5 text-xs font-semibold hover:border-accent hover:text-accent disabled:opacity-50"
          >
            {seller.status === "PENDING" ? "Approve" : "Reactivate"}
          </button>
        )}
        {seller.status !== "SUSPENDED" && (
          <button
            type="button"
            disabled={isPending}
            onClick={() => {
              if (!confirm(`Suspend ${seller.businessName}? Their seller role is revoked immediately.`)) return;
              setStatus("SUSPENDED");
            }}
            className="rounded-full border border-border-subtle px-4 py-1.5 text-xs font-semibold text-red-600 hover:border-red-600 disabled:opacity-50"
          >
            Suspend
          </button>
        )}
      </div>
    </div>
  );
}
