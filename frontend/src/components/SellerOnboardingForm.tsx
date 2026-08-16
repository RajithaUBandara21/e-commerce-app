"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { registerSellerAction } from "@/lib/actions";

export function SellerOnboardingForm() {
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(formData: FormData) {
    setError(null);
    setIsSubmitting(true);
    try {
      await registerSellerAction({
        businessName: String(formData.get("businessName") ?? ""),
        businessEmail: String(formData.get("businessEmail") ?? ""),
        description: String(formData.get("description") ?? "") || undefined,
      });
      router.push("/seller/products");
    } catch {
      setError("Registration failed — check your details and try again.");
      setIsSubmitting(false);
    }
  }

  return (
    <form action={handleSubmit} className="flex flex-col gap-4">
      <label className="flex flex-col gap-1 text-sm font-medium">
        Business name
        <input
          name="businessName"
          required
          className="rounded-lg border border-border-subtle px-3 py-2 text-sm outline-none focus:border-accent"
        />
      </label>

      <label className="flex flex-col gap-1 text-sm font-medium">
        Business email
        <input
          type="email"
          name="businessEmail"
          required
          className="rounded-lg border border-border-subtle px-3 py-2 text-sm outline-none focus:border-accent"
        />
      </label>

      <label className="flex flex-col gap-1 text-sm font-medium">
        Description <span className="font-normal text-foreground/50">(optional)</span>
        <textarea
          name="description"
          rows={3}
          className="rounded-lg border border-border-subtle px-3 py-2 text-sm outline-none focus:border-accent"
        />
      </label>

      {error && <p className="text-sm text-red-600">{error}</p>}

      <button
        type="submit"
        disabled={isSubmitting}
        className="mt-2 rounded-full bg-accent px-6 py-3 text-sm font-semibold text-accent-foreground hover:bg-accent-hover disabled:opacity-50"
      >
        {isSubmitting ? "Submitting…" : "Submit application"}
      </button>
    </form>
  );
}
