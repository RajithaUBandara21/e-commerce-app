"use client";

import Link from "next/link";
import { Suspense } from "react";
import { signIn, signOut, useSession } from "next-auth/react";
import { Heart, ShoppingBag, User } from "lucide-react";
import { useWishlist } from "@/lib/wishlist";
import { useCartItemCount } from "@/lib/useCart";
import { SearchBox } from "@/components/SearchBox";

export function SiteHeader() {
  const { data: session, status } = useSession();
  const { ids: wishlistIds } = useWishlist();
  const cartCount = useCartItemCount();

  return (
    <header className="border-b border-border-subtle bg-background">
      <div className="mx-auto flex max-w-6xl items-center justify-between gap-6 px-6 py-4">
        <Link href="/" className="shrink-0 text-xl font-bold tracking-tight">
          Cloth<span className="text-accent">Shop</span>
        </Link>

        <nav className="hidden items-center gap-8 text-sm font-medium md:flex">
          <Link href="/" className="hover:text-accent">
            Home
          </Link>
          <Link href="/" className="hover:text-accent">
            Shop
          </Link>
          <Link href="/#categories" className="hover:text-accent">
            Categories
          </Link>
        </nav>

        <div className="flex items-center gap-1 sm:gap-2">
          <Suspense fallback={<div className="h-9 w-9" />}>
            <SearchBox />
          </Suspense>

          <Link href="/wishlist" aria-label="Wishlist" className="relative rounded-full p-2 hover:bg-surface-muted">
            <Heart className="h-5 w-5" />
            {wishlistIds.length > 0 && (
              <span className="absolute -right-0.5 -top-0.5 flex h-4 w-4 items-center justify-center rounded-full bg-accent text-[10px] font-semibold text-accent-foreground">
                {wishlistIds.length}
              </span>
            )}
          </Link>

          {status === "authenticated" ? (
            <button
              onClick={() => signOut()}
              aria-label={`Sign out${session.user?.name ? ` (${session.user.name})` : ""}`}
              className="rounded-full p-2 hover:bg-surface-muted"
              title={session.user?.name ? `Signed in as ${session.user.name}` : "Sign out"}
            >
              <User className="h-5 w-5" />
            </button>
          ) : (
            <button
              onClick={() => signIn("keycloak")}
              aria-label="Sign in"
              className="rounded-full p-2 hover:bg-surface-muted"
            >
              <User className="h-5 w-5" />
            </button>
          )}

          <Link href="/cart" aria-label="Cart" className="relative rounded-full p-2 hover:bg-surface-muted">
            <ShoppingBag className="h-5 w-5" />
            {cartCount > 0 && (
              <span className="absolute -right-0.5 -top-0.5 flex h-4 w-4 items-center justify-center rounded-full bg-accent text-[10px] font-semibold text-accent-foreground">
                {cartCount}
              </span>
            )}
          </Link>
        </div>
      </div>
    </header>
  );
}
