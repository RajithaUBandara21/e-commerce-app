
export { auth as proxy } from "@/auth";

export const config = {
  matcher: ["/cart/:path*", "/checkout/:path*", "/account/:path*", "/orders/:path*", "/seller/:path*", "/admin/:path*"],
};
