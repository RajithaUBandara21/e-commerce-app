import { auth } from "@/auth";
import { getCategories } from "@/lib/api";
import { AdminGate } from "@/components/AdminGate";
import { CategoryForm } from "@/components/CategoryForm";
import { CategoryRow } from "@/components/CategoryRow";

export const dynamic = "force-dynamic";

export default async function AdminCategoriesPage() {
  const session = await auth();
  if (!session?.userId) {
    return null;
  }

  const isAdmin = session.roles?.includes("admin") ?? false;

  return (
    <AdminGate isAdmin={isAdmin}>
      <CategoriesContent />
    </AdminGate>
  );
}

async function CategoriesContent() {
  const categories = await getCategories().catch(() => []);

  return (
    <>
      <h1 className="mb-8 text-2xl font-semibold tracking-tight">Categories</h1>

      <div className="mb-6">
        <CategoryForm />
      </div>

      {categories.length === 0 ? (
        <p className="text-foreground/60">No categories yet.</p>
      ) : (
        <div className="flex flex-col gap-3">
          {categories.map((category) => (
            <CategoryRow key={category.id} category={category} />
          ))}
        </div>
      )}
    </>
  );
}
