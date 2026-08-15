"use client";

import Link from "next/link";
import { signIn, signOut, useSession } from "next-auth/react";

export function SiteHeader() {
  const { data: session, status } = useSession();

  return (
    <header className="border-b border-zinc-200 dark:border-zinc-800">
      <div className="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
        <Link href="/" className="text-lg font-semibold tracking-tight">
          Cloth Shop
        </Link>
        <nav className="flex items-center gap-6 text-sm">
          <Link href="/cart">Cart</Link>
          {status === "authenticated" ? (
            <button
              onClick={() => signOut()}
              className="rounded-full border border-zinc-300 px-4 py-1.5 dark:border-zinc-700"
            >
              Sign out{session.user?.name ? ` (${session.user.name})` : ""}
            </button>
          ) : (
            <button
              onClick={() => signIn("keycloak")}
              className="rounded-full bg-foreground px-4 py-1.5 text-background"
            >
              Sign in
            </button>
          )}
        </nav>
      </div>
    </header>
  );
}
