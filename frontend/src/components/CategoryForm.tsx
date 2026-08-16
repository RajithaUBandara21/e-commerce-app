"use client";

import { useState, useTransition } from "react";
import { createCategoryAction } from "@/lib/actions";

export function CategoryForm() {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  return (
    <form
      className="flex flex-wrap items-end gap-3 rounded-2xl border border-border-subtle p-4"
      onSubmit={(e) => {
        e.preventDefault();
        if (!name.trim()) return;
        setError(null);
        startTransition(async () => {
          try {
            await createCategoryAction(name, description);
            setName("");
            setDescription("");
          } catch {
            setError("Couldn't create the category — try again.");
          }
        });
      }}
    >
      <label className="flex flex-col gap-1 text-sm">
        Name
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
          className="rounded-full border border-border-subtle px-3 py-1.5 text-sm"
        />
      </label>
      <label className="flex flex-1 flex-col gap-1 text-sm">
        Description
        <input
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          className="rounded-full border border-border-subtle px-3 py-1.5 text-sm"
        />
      </label>
      <button
        type="submit"
        disabled={isPending}
        className="rounded-full bg-accent px-4 py-1.5 text-xs font-semibold text-accent-foreground disabled:opacity-50"
      >
        {isPending ? "Adding…" : "Add category"}
      </button>
      {error && <p className="w-full text-sm text-red-600">{error}</p>}
    </form>
  );
}
