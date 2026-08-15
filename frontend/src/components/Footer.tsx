import Link from "next/link";

export function Footer() {
  return (
    <footer className="border-t border-border-subtle bg-surface-muted">
      <div className="mx-auto grid max-w-6xl gap-10 px-6 py-12 sm:grid-cols-3">
        <div>
          <p className="text-lg font-bold tracking-tight">
            Cloth<span className="text-accent">Shop</span>
          </p>
          <p className="mt-2 max-w-xs text-sm text-foreground/60">
            Everyday clothing built on a production-grade microservices backend.
          </p>
        </div>

        <div>
          <p className="text-sm font-semibold">Shop</p>
          <ul className="mt-3 space-y-2 text-sm text-foreground/60">
            <li>
              <Link href="/" className="hover:text-accent">
                All products
              </Link>
            </li>
            <li>
              <Link href="/#categories" className="hover:text-accent">
                Categories
              </Link>
            </li>
            <li>
              <Link href="/wishlist" className="hover:text-accent">
                Wishlist
              </Link>
            </li>
          </ul>
        </div>

        <div>
          <p className="text-sm font-semibold">Account</p>
          <ul className="mt-3 space-y-2 text-sm text-foreground/60">
            <li>
              <Link href="/cart" className="hover:text-accent">
                Cart
              </Link>
            </li>
          </ul>
        </div>
      </div>
      <div className="border-t border-border-subtle px-6 py-4 text-center text-xs text-foreground/50">
        © {new Date().getFullYear()} Cloth Shop.
      </div>
    </footer>
  );
}
