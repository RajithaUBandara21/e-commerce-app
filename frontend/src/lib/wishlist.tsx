"use client";

// A real (if minimal) feature, not a decorative heart icon: wishlisting is
// client-only, backed by localStorage. There's no backend wishlist endpoint —
// adding one is out of scope here — but a toggle that doesn't persist
// anything would be worse than not having it at all.
//
// Built on useSyncExternalStore rather than useState+useEffect: this is
// exactly the "subscribe to an external mutable store" case that hook is
// for, and it avoids the SSR/hydration mismatch a naive localStorage read
// during render would cause.

import { createContext, useCallback, useContext, useMemo, useSyncExternalStore } from "react";

const STORAGE_KEY = "cloth-shop:wishlist";

let ids: number[] = [];
const listeners = new Set<() => void>();

function readFromStorage(): number[] {
  try {
    const stored = window.localStorage.getItem(STORAGE_KEY);
    return stored ? JSON.parse(stored) : [];
  } catch {
    return [];
  }
}

function notify() {
  for (const listener of listeners) listener();
}

function subscribe(listener: () => void) {
  if (listeners.size === 0) ids = readFromStorage();
  listeners.add(listener);
  return () => listeners.delete(listener);
}

function getSnapshot() {
  return ids;
}

function getServerSnapshot() {
  return ids;
}

function setIds(next: number[]) {
  ids = next;
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(ids));
  } catch {
    // Storage unavailable (private browsing, quota) — state still updates in-memory.
  }
  notify();
}

type WishlistContextValue = {
  ids: number[];
  isWishlisted: (productId: number) => boolean;
  toggle: (productId: number) => void;
};

const WishlistContext = createContext<WishlistContextValue | null>(null);

export function WishlistProvider({ children }: { children: React.ReactNode }) {
  const snapshot = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);

  const toggle = useCallback((productId: number) => {
    setIds(ids.includes(productId) ? ids.filter((id) => id !== productId) : [...ids, productId]);
  }, []);

  const isWishlisted = useCallback((productId: number) => snapshot.includes(productId), [snapshot]);

  const value = useMemo(
    () => ({ ids: snapshot, isWishlisted, toggle }),
    [snapshot, isWishlisted, toggle],
  );

  return <WishlistContext.Provider value={value}>{children}</WishlistContext.Provider>;
}

export function useWishlist() {
  const context = useContext(WishlistContext);
  if (!context) throw new Error("useWishlist must be used within a WishlistProvider");
  return context;
}
