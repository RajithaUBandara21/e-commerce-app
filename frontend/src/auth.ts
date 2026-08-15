import NextAuth from "next-auth";
import Keycloak from "next-auth/providers/keycloak";

const { handlers, auth, signIn, signOut } = NextAuth({
  providers: [
    Keycloak({
      clientId: process.env.AUTH_KEYCLOAK_ID,
      issuer: process.env.AUTH_KEYCLOAK_ISSUER,
      // Public client (PKCE, authorization code) — no secret. See keycloak/setup-nextjs-client.sh.
      clientSecret: "unused",
      checks: ["pkce", "state"],
      client: { token_endpoint_auth_method: "none" },
    }),
  ],
  callbacks: {
    // Gates the routes matched by proxy.ts's `config.matcher` — without this,
    // `export { auth as proxy }` only attaches session info, it doesn't redirect.
    authorized({ auth }) {
      return !!auth;
    },
    // Keep the Keycloak access token (and the customer id it identifies) on
    // the server-held session — cart-service/order-service/api-gateway are
    // called with it, but it never needs to reach the browser as a JWT.
    async jwt({ token, account, profile }) {
      if (account) {
        token.accessToken = account.access_token;
        token.accessTokenExpires = account.expires_at ? account.expires_at * 1000 : undefined;
      }
      if (profile?.sub) {
        token.userId = profile.sub;
      }
      return token;
    },
    async session({ session, token }) {
      // Known simplification: auth() and useSession() share one session shape
      // in NextAuth v5, so putting accessToken here makes it available to
      // Server Actions (which is what it's for) but also to client JS via
      // useSession(). Hardening this to a server-only token (via next-auth/jwt's
      // getToken() instead of auth()) is Phase 6 work, not done here.
      session.accessToken = token.accessToken as string | undefined;
      session.userId = token.userId as string | undefined;
      return session;
    },
  },
});

export { handlers, auth, signIn, signOut };
