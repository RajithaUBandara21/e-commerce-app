import { SellerOnboardingForm } from "@/components/SellerOnboardingForm";

export default function SellerOnboardingPage() {
  return (
    <div className="mx-auto w-full max-w-md flex-1 px-6 py-10">
      <h1 className="mb-2 text-2xl font-semibold tracking-tight">Register as a seller</h1>
      <p className="mb-8 text-foreground/60">
        Tell us about your business. An admin reviews every application before you can start listing products.
      </p>
      <SellerOnboardingForm />
    </div>
  );
}
