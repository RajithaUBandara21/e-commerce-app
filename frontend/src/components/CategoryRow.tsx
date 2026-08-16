"use client";

import { useState, useTransition } from "react";
import type { Category } from "@/lib/types";
import { deleteCategoryAction, updateCategoryAction } from "@/lib/actions";

export function CategoryRow({ category }: { category: Category }) {
  const [isEditing, setIsEditing] = useState(false);
  const [name, setName] = useState(category.name);
  const [description, setDescription] = useState(category.description ?? "");
  const [error, setError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  if (isEditing) {
    return (
      <form
        className="flex flex-wrap items-end gap-3 rounded-2xl border border-border-subtle p-4"
        onSubmit={(e) => {
          e.preventDefault();
          setError(null);
          startTransition(async () => {
            try {
              await updateCategoryAction(category.id, name, description);
              setIsEditing(false);
            } catch {
              setError("Couldn't save — try again.");
            }
          });
        }}
      >
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
          className="rounded-full border border-border-subtle px-3 py-1.5 text-sm"
        />
        <input
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          className="flex-1 rounded-full border border-border-subtle px-3 py-1.5 text-sm"
        />
        <button
          type="submit"
          disabled={isPending}
          className="rounded-full bg-accent px-4 py-1.5 text-xs font-semibold text-accent-foreground disabled:opacity-50"
        >
          {isPending ? "Saving…" : "Save"}
        </button>
        <button
          type="button"
          onClick={() => setIsEditing(false)}
          className="rounded-full border border-border-subtle px-4 py-1.5 text-xs font-semibold"
        >
          Cancel
        </button>
        {error && <p className="w-full text-sm text-red-600">{error}</p>}
      </form>
    );
  }

  return (
    <div className="flex items-center justify-between rounded-2xl border border-border-subtle p-4">
      <div>
        <p className="font-medium">{category.name}</p>
        <p className="text-sm text-foreground/60">
          {category.description || "No description"} · {category.productCount} product
          {category.productCount === 1 ? "" : "s"}
        </p>
      </div>
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={() => setIsEditing(true)}
          className="text-xs font-medium text-foreground/60 hover:text-accent"
        >
          Edit
        </button>
        <button
          type="button"
          disabled={isPending}
          onClick={() => {
            if (category.productCount > 0) {
              alert("Can't delete a category that still has products in it.");
              return;
            }
            if (!confirm(`Delete "${category.name}"?`)) return;
            startTransition(async () => {
              await deleteCategoryAction(category.id);
            });
          }}
          className="text-xs font-medium text-foreground/50 hover:text-red-600 disabled:opacity-50"
        >
          Delete
        </button>
      </div>
    </div>
  );
}
