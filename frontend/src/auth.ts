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
      session.accessToken = token.accessToken as string | undefined;
      session.userId = token.userId as string | undefined;
      return session;
    },
  },
});

export { handlers, auth, signIn, signOut };
