// The `export {}` keeps this file an ES module rather than an ambient script —
// without it, `declare module "next-auth"` below replaces next-auth's real
// exported types instead of augmenting them (a real bug hit while writing this:
// it silently made `NextAuthConfig`/`NextAuthResult` disappear and NextAuth()
// stopped being callable, with no error pointing back at this file).
export {};

declare module "next-auth" {
  interface Session {
    accessToken?: string;
    userId?: string;
  }
}

declare module "next-auth/jwt" {
  interface JWT {
    accessToken?: string;
    accessTokenExpires?: number;
    userId?: string;
  }
}

// @auth/core's own callback signatures (which next-auth re-exports) import
// JWT from here, not from "next-auth/jwt" — both need augmenting or the
// `token` param inside callbacks.session/callbacks.jwt won't see these fields.
declare module "@auth/core/jwt" {
  interface JWT {
    accessToken?: string;
    accessTokenExpires?: number;
    userId?: string;
  }
}
