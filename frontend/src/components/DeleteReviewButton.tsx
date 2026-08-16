"use client";

import { useTransition } from "react";
import { deleteReviewAction } from "@/lib/actions";

export function DeleteReviewButton({ productId, reviewId }: { productId: number; reviewId: number }) {
  const [isPending, startTransition] = useTransition();

  return (
    <button
      type="button"
      disabled={isPending}
      onClick={() => {
        if (!confirm("Delete your review?")) return;
        startTransition(async () => {
          await deleteReviewAction(productId, reviewId);
        });
      }}
      className="text-xs font-medium text-foreground/50 hover:text-red-600 disabled:opacity-50"
    >
      {isPending ? "Deleting…" : "Delete"}
    </button>
  );
}
