// No product photography exists yet (Product has no image field on the
// backend — see PLAN.md Phase 5 "known simplifications"). Rather than faking
// photos, this renders a deterministic, on-brand placeholder: a gradient tied
// to the product's own id/name so the same product always looks the same,
// with a simple garment glyph instead of empty grey boxes.

const GRADIENTS = [
  "from-orange-200 via-amber-100 to-orange-50",
  "from-rose-200 via-orange-100 to-amber-50",
  "from-stone-200 via-orange-50 to-stone-100",
  "from-amber-200 via-orange-100 to-rose-50",
];

function hashToIndex(seed: string, length: number) {
  let hash = 0;
  for (let i = 0; i < seed.length; i++) {
    hash = (hash << 5) - hash + seed.charCodeAt(i);
    hash |= 0;
  }
  return Math.abs(hash) % length;
}

export function ProductArt({ seed, className = "" }: { seed: string; className?: string }) {
  const gradient = GRADIENTS[hashToIndex(seed, GRADIENTS.length)];

  return (
    <div
      className={`flex items-center justify-center bg-gradient-to-br ${gradient} ${className}`}
      role="img"
      aria-label="Product photo placeholder"
    >
      <svg viewBox="0 0 64 64" className="h-1/3 w-1/3 text-black/20" fill="none" aria-hidden="true">
        <path
          d="M24 8 L20 14 L8 20 L14 30 L20 27 V56 H44 V27 L50 30 L56 20 L44 14 L40 8 C40 12 36 15 32 15 C28 15 24 12 24 8 Z"
          stroke="currentColor"
          strokeWidth="2.5"
          strokeLinejoin="round"
        />
      </svg>
    </div>
  );
}
