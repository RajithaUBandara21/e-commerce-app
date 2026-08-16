"use client";

import { useState, useTransition } from "react";
import { useSession } from "next-auth/react";
import { submitReviewAction } from "@/lib/actions";

export function ReviewForm({ productId }: { productId: number }) {
  const { status } = useSession();
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);
  const [isPending, startTransition] = useTransition();

  if (status !== "authenticated") {
    return <p className="text-sm text-foreground/60">Sign in to write a review.</p>;
  }

  if (submitted) {
    return <p className="text-sm text-foreground/60">Thanks — your review has been posted.</p>;
  }

  return (
    <form
      className="flex flex-col gap-3 rounded-2xl border border-border-subtle p-4"
      onSubmit={(e) => {
        e.preventDefault();
        setError(null);
        startTransition(async () => {
          try {
            await submitReviewAction(productId, rating, comment);
            setSubmitted(true);
          } catch {
            setError("Couldn't post your review — you may need to have purchased this product first, or you've already reviewed it.");
          }
        });
      }}
    >
      <label className="flex items-center gap-2 text-sm font-medium">
        Your rating
        <select
          value={rating}
          onChange={(e) => setRating(Number(e.target.value))}
          className="rounded-full border border-border-subtle px-2 py-1 text-sm"
        >
          {[5, 4, 3, 2, 1].map((n) => (
            <option key={n} value={n}>
              {n} star{n === 1 ? "" : "s"}
            </option>
          ))}
        </select>
      </label>
      <textarea
        value={comment}
        onChange={(e) => setComment(e.target.value)}
        placeholder="Share your thoughts on this product (optional)"
        maxLength={2000}
        rows={3}
        className="rounded-xl border border-border-subtle p-3 text-sm"
      />
      {error && <p className="text-sm text-red-600">{error}</p>}
      <button
        type="submit"
        disabled={isPending}
        className="self-start rounded-full bg-accent px-4 py-1.5 text-xs font-semibold text-accent-foreground disabled:opacity-50"
      >
        {isPending ? "Submitting…" : "Submit review"}
      </button>
    </form>
  );
}
