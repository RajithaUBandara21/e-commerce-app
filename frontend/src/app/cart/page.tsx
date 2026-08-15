import { auth } from "@/auth";
import { getCart, getProducts } from "@/lib/api";
import { CartView, type CartLine } from "@/components/CartView";

export default async function CartPage() {
  const session = await auth();
  // proxy.ts already redirects unauthenticated visits to sign-in; this is
  // just the type-narrowing guard for the accessToken/userId below.
  if (!session?.userId || !session.accessToken) {
    return null;
  }

  const [cart, products] = await Promise.all([
    getCart(session.userId, session.accessToken),
    getProducts(),
  ]);

  const variantIndex = new Map(
    products.flatMap((product) =>
      product.variants.map((variant) => [
        variant.id,
        { productName: product.name, price: product.price, size: variant.size, color: variant.color },
      ] as const),
    ),
  );

  const lines: CartLine[] = cart.items.map((item) => {
    const details = variantIndex.get(item.variantId);
    return {
      variantId: item.variantId,
      quantity: item.quantity,
      productName: details?.productName ?? `Variant #${item.variantId}`,
      size: details?.size ?? "—",
      color: details?.color ?? "—",
      price: details?.price ?? 0,
    };
  });

  return (
    <div className="mx-auto w-full max-w-2xl flex-1 px-6 py-10">
      <h1 className="mb-8 text-2xl font-semibold tracking-tight">Your cart</h1>
      <CartView lines={lines} />
    </div>
  );
}
