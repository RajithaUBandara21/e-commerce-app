import { auth } from "@/auth";
import { getAllSellers } from "@/lib/api";
import type { Seller } from "@/lib/api";
import { AdminGate } from "@/components/AdminGate";
import { SellerModerationRow } from "@/components/SellerModerationRow";

export const dynamic = "force-dynamic";

export default async function AdminSellersPage() {
  const session = await auth();
  if (!session?.userId || !session.accessToken) {
    return null;
  }

  const isAdmin = session.roles?.includes("admin") ?? false;

  return (
    <AdminGate isAdmin={isAdmin}>
      <SellersContent accessToken={session.accessToken} />
    </AdminGate>
  );
}

async function SellersContent({ accessToken }: { accessToken: string }) {
  let sellers: Seller[];
  let loadFailed = false;
  try {
    sellers = await getAllSellers(accessToken);
  } catch (error) {
    console.error("Failed to load sellers:", error);
    loadFailed = true;
    sellers = [];
  }

  return (
    <>
      <h1 className="mb-8 text-2xl font-semibold tracking-tight">Sellers</h1>

      {loadFailed ? (
        <p className="text-foreground/60">Couldn&apos;t load sellers right now — try refreshing.</p>
      ) : sellers.length === 0 ? (
        <p className="text-foreground/60">No sellers have registered yet.</p>
      ) : (
        <div className="flex flex-col gap-3">
          {sellers.map((seller) => (
            <SellerModerationRow key={seller.id} seller={seller} />
          ))}
        </div>
      )}
    </>
  );
}
