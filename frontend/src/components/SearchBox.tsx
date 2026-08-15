"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { Search, X } from "lucide-react";

export function SearchBox() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState(searchParams.get("q") ?? "");

  function submitSearch(e: React.FormEvent) {
    e.preventDefault();
    const trimmed = query.trim();
    router.push(trimmed ? `/?q=${encodeURIComponent(trimmed)}` : "/");
    setOpen(false);
  }

  if (!open) {
    return (
      <button
        type="button"
        onClick={() => setOpen(true)}
        aria-label="Search"
        className="rounded-full p-2 hover:bg-surface-muted"
      >
        <Search className="h-5 w-5" />
      </button>
    );
  }

  return (
    <form onSubmit={submitSearch} className="flex items-center gap-2">
      <input
        autoFocus
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder="Search products…"
        className="w-40 rounded-full border border-border-subtle px-3 py-1.5 text-sm outline-none focus:border-accent sm:w-56"
      />
      <button
        type="button"
        onClick={() => setOpen(false)}
        aria-label="Close search"
        className="rounded-full p-2 hover:bg-surface-muted"
      >
        <X className="h-5 w-5" />
      </button>
    </form>
  );
}
