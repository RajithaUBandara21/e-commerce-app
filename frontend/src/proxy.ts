// Next.js 16 renamed `middleware.ts` to `proxy.ts` (same mechanism, same file
// conventions) — see node_modules/next/dist/docs/01-app/03-api-reference/03-file-conventions/proxy.md.
export { auth as proxy } from "@/auth";

export const config = {
  matcher: ["/cart/:path*", "/checkout/:path*", "/account/:path*"],
};
