import { auth } from "@/auth";
import { getProductReviews } from "@/lib/api";
import { StarRating } from "@/components/StarRating";
import { ReviewForm } from "@/components/ReviewForm";
import { DeleteReviewButton } from "@/components/DeleteReviewButton";

export async function ReviewSection({ productId }: { productId: number }) {
  const [session, reviews] = await Promise.all([auth(), getProductReviews(productId).catch(() => [])]);

  return (
    <div className="flex flex-col gap-6">
      <h2 className="text-lg font-semibold tracking-tight">Reviews</h2>

      {reviews.length === 0 ? (
        <p className="text-sm text-foreground/60">No reviews yet — be the first to share your thoughts.</p>
      ) : (
        <div className="flex flex-col gap-4">
          {reviews.map((review) => (
            <div key={review.id} className="flex flex-col gap-1 border-b border-border-subtle pb-4">
              <div className="flex items-center justify-between">
                <StarRating rating={review.rating} />
                <div className="flex items-center gap-3">
                  <span className="text-xs text-foreground/50">
                    {new Date(review.createdDate).toLocaleDateString()}
                  </span>
                  {session?.userId === review.customerId && (
                    <DeleteReviewButton productId={productId} reviewId={review.id} />
                  )}
                </div>
              </div>
              {review.comment && <p className="text-sm text-foreground/80">{review.comment}</p>}
            </div>
          ))}
        </div>
      )}

      <ReviewForm productId={productId} />
    </div>
  );
}
