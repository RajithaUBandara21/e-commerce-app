import { auth } from "@/auth";
import { getMyProducts, getMySellerProfile } from "@/lib/api";
import { SellerProductRow } from "@/components/SellerProductRow";
import { SellerStatusGate } from "@/components/SellerStatusGate";

export const dynamic = "force-dynamic";

export default async function SellerProductsPage() {
  const session = await auth();
  if (!session?.userId || !session.accessToken) {
    return null;
  }

  const seller = await getMySellerProfile(session.accessToken);

  return (
    <SellerStatusGate seller={seller}>
      <ProductsContent sellerId={session.userId} />
    </SellerStatusGate>
  );
}

async function ProductsContent({ sellerId }: { sellerId: string }) {
  const products = await getMyProducts(sellerId).catch(() => []);

  return (
    <>
      <h1 className="mb-8 text-2xl font-semibold tracking-tight">Your products</h1>

      {products.length === 0 ? (
        <p className="text-foreground/60">You haven&apos;t listed any products yet.</p>
      ) : (
        <div className="flex flex-col gap-4">
          {products.map((product) => (
            <SellerProductRow key={product.id} product={product} />
          ))}
        </div>
      )}
    </>
  );
}
