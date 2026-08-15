"use client";

import { useQuery } from "@tanstack/react-query";
import { useSession } from "next-auth/react";
import { getCart } from "@/lib/api";

// First real client-side use of TanStack Query in this app (everything else
// so far is Server Components + Server Actions). The cart badge is the one
// piece of UI that lives in the header, outside any page's own data fetch,
// so it needs its own client-side query rather than server-fetched props.
export function useCartItemCount() {
  const { data: session, status } = useSession();

  const { data } = useQuery({
    queryKey: ["cart-count", session?.userId],
    queryFn: () => getCart(session!.userId!, session!.accessToken!),
    enabled: status === "authenticated" && !!session?.userId && !!session?.accessToken,
    staleTime: 15_000,
  });

  return data?.items.reduce((sum, item) => sum + item.quantity, 0) ?? 0;
}
