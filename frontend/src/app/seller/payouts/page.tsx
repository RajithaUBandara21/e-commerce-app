import { auth } from "@/auth";
import { getMyPayouts, getMySellerProfile } from "@/lib/api";
import type { SellerPayout } from "@/lib/api";
import { SellerStatusGate } from "@/components/SellerStatusGate";

export const dynamic = "force-dynamic";

const STATUS_STYLES: Record<SellerPayout["status"], string> = {
  PENDING: "bg-surface-muted text-foreground/60",
  PAID: "bg-green-100 text-green-700",
  FAILED: "bg-red-100 text-red-700",
};

export default async function SellerPayoutsPage() {
  const session = await auth();
  if (!session?.userId || !session.accessToken) {
    return null;
  }

  const seller = await getMySellerProfile(session.accessToken);

  return (
    <SellerStatusGate seller={seller}>
      <PayoutsContent accessToken={session.accessToken} />
    </SellerStatusGate>
  );
}

async function PayoutsContent({ accessToken }: { accessToken: string }) {
  let payouts: SellerPayout[];
  let loadFailed = false;
  try {
    payouts = await getMyPayouts(accessToken);
  } catch (error) {
    console.error("Failed to load payouts:", error);
    loadFailed = true;
    payouts = [];
  }

  const totalPending = payouts.filter((p) => p.status === "PENDING").reduce((sum, p) => sum + p.netAmount, 0);
  const totalPaid = payouts.filter((p) => p.status === "PAID").reduce((sum, p) => sum + p.netAmount, 0);

  return (
    <>
      <h1 className="mb-8 text-2xl font-semibold tracking-tight">Your payouts</h1>

      {loadFailed ? (
        <p className="text-foreground/60">Couldn&apos;t load your payouts right now — try refreshing.</p>
      ) : (
        <>
          <div className="mb-8 grid grid-cols-2 gap-4">
            <div className="rounded-2xl border border-border-subtle p-4">
              <p className="text-xs uppercase tracking-wide text-foreground/50">Pending</p>
              <p className="text-2xl font-semibold">${totalPending.toFixed(2)}</p>
            </div>
            <div className="rounded-2xl border border-border-subtle p-4">
              <p className="text-xs uppercase tracking-wide text-foreground/50">Paid out</p>
              <p className="text-2xl font-semibold">${totalPaid.toFixed(2)}</p>
            </div>
          </div>

          {payouts.length === 0 ? (
            <p className="text-foreground/60">No payouts yet — they&apos;re created automatically once an order is paid.</p>
          ) : (
            <div className="flex flex-col gap-3">
              {payouts.map((payout) => (
                <div
                  key={payout.id}
                  className="flex items-center justify-between rounded-2xl border border-border-subtle p-4"
                >
                  <div>
                    <p className="font-medium">{payout.orderReference}</p>
                    <p className="text-sm text-foreground/60">
                      ${payout.netAmount.toFixed(2)} net (${payout.grossAmount.toFixed(2)} gross − $
                      {payout.commissionAmount.toFixed(2)} commission)
                    </p>
                    {payout.failureReason && <p className="text-sm text-red-600">{payout.failureReason}</p>}
                  </div>
                  <span className={`rounded-full px-3 py-1 text-xs font-semibold ${STATUS_STYLES[payout.status]}`}>
                    {payout.status}
                  </span>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </>
  );
}
