/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,

  // Supabase packages use Node.js internals that aren't needed on the
  // client side.  Marking them as external prevents bundle warnings when
  // the env-vars are not set and the Supabase client is never initialised.
  webpack: (config, { isServer }) => {
    if (!isServer) {
      config.resolve.fallback = {
        ...config.resolve.fallback,
        fs: false,
        net: false,
        tls: false,
      };
    }
    return config;
  },
};

module.exports = nextConfig;
