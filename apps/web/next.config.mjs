/** @type {import('next').NextConfig} */
const config = {
  // Verification builds must not overwrite a running development server's output.
  distDir: process.env.NEXT_OUTPUT_DIR || ".next",
};
export default config;
