import NextAuth from "next-auth";
import Keycloak from "next-auth/providers/keycloak";

// Keycloak realm roles live in the access token's own realm_access.roles claim —
// NextAuth's `profile`/`account` params don't surface it, so this decodes the JWT
// payload directly (no verification needed: the token was already validated by
// Keycloak issuing it to us over PKCE, and every downstream API call re-validates
// it anyway — this is purely for client-side "show the seller nav link" UI).
function decodeRealmRoles(accessToken: string): string[] {
  try {
    const payload = accessToken.split(".")[1];
    const decoded = JSON.parse(Buffer.from(payload, "base64url").toString("utf-8"));
    return Array.isArray(decoded?.realm_access?.roles) ? decoded.realm_access.roles : [];
  } catch {
    return [];
  }
}

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
        token.roles = account.access_token ? decodeRealmRoles(account.access_token) : [];
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
      session.accessToken = token.accessToken;
      // AdapterSession.userId (required string, from the database-session
      // strategy we don't use) intersects with our optional Session.userId
      // augmentation, so the merged param type requires a plain string here.
      session.userId = token.userId as string;
      session.roles = token.roles ?? [];
      return session;
    },
  },
});

export { handlers, auth, signIn, signOut };
