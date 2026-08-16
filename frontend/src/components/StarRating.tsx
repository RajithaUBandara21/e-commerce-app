import { Star } from "lucide-react";

export function StarRating({ rating, size = 16 }: { rating: number; size?: number }) {
  const rounded = Math.round(rating);
  return (
    <div className="flex items-center gap-0.5" role="img" aria-label={`${rating.toFixed(1)} out of 5 stars`}>
      {Array.from({ length: 5 }, (_, i) => (
        <Star
          key={i}
          size={size}
          className={i < rounded ? "fill-accent text-accent" : "text-foreground/20"}
        />
      ))}
    </div>
  );
}
