import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Minimal, self-contained production build for the Docker image — traces
  // only the node_modules this app actually needs into .next/standalone.
  output: "standalone",
};

export default nextConfig;
