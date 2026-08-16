import { AdminNav } from "@/components/AdminNav";

// The gateway independently enforces ROLE_ADMIN on every write this dashboard
// makes (defense in depth); this gate is the read-side UI equivalent — same
// shape as SellerStatusGate.
export function AdminGate({ isAdmin, children }: { isAdmin: boolean; children: React.ReactNode }) {
  if (!isAdmin) {
    return (
      <div className="mx-auto w-full max-w-2xl flex-1 px-6 py-10">
        <h1 className="mb-4 text-2xl font-semibold tracking-tight">Admins only</h1>
        <p className="text-foreground/60">You don&apos;t have access to this page.</p>
      </div>
    );
  }

  return (
    <div className="mx-auto w-full max-w-4xl flex-1 px-6 py-10">
      <AdminNav />
      {children}
    </div>
  );
}
