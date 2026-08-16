"use client";

import { useState, useTransition } from "react";
import type { Product } from "@/lib/types";
import { ProductArt } from "@/components/ProductArt";
import { getImageUploadUrlAction, registerProductImageAction } from "@/lib/actions";

export function SellerProductRow({ product }: { product: Product }) {
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  async function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;

    setError(null);
    setIsUploading(true);
    try {
      const contentType = file.type || "image/jpeg";
      const { uploadUrl, objectKey } = await getImageUploadUrlAction(product.id, contentType);

      const putResponse = await fetch(uploadUrl, {
        method: "PUT",
        body: file,
        headers: { "Content-Type": contentType },
      });
      if (!putResponse.ok) {
        throw new Error(`Upload to storage failed (${putResponse.status})`);
      }

      startTransition(async () => {
        await registerProductImageAction(product.id, objectKey);
      });
    } catch {
      setError("Upload failed — try again.");
    } finally {
      setIsUploading(false);
    }
  }

  const busy = isUploading || isPending;

  return (
    <div className="flex items-center gap-4 rounded-2xl border border-border-subtle p-4">
      {product.imageUrls.length > 0 ? (
        // eslint-disable-next-line @next/next/no-img-element -- see ProductCard's note
        <img src={product.imageUrls[0]} alt={product.name} className="h-20 w-20 shrink-0 rounded-xl object-cover" />
      ) : (
        <ProductArt seed={`${product.id}-${product.name}`} className="h-20 w-20 shrink-0 rounded-xl" />
      )}

      <div className="min-w-0 flex-1">
        <p className="truncate font-medium">{product.name}</p>
        <p className="text-sm text-foreground/60">
          ${product.price.toFixed(2)} · {product.imageUrls.length} photo{product.imageUrls.length === 1 ? "" : "s"}
        </p>
        {error && <p className="text-sm text-red-600">{error}</p>}
      </div>

      <label
        className={`shrink-0 rounded-full border border-border-subtle px-4 py-1.5 text-xs font-semibold ${
          busy ? "pointer-events-none opacity-50" : "cursor-pointer hover:border-accent hover:text-accent"
        }`}
      >
        {busy ? "Uploading…" : "Upload photo"}
        <input
          type="file"
          accept="image/*"
          className="hidden"
          onChange={handleFileChange}
          disabled={busy}
        />
      </label>
    </div>
  );
}
