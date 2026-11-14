/*
 * tidal — proprietary source-viewing license
 * maintained by graphine studios
 * see LICENSE and CREDITS.md for licensing and attribution.
 *
 * provided here for inspection only.
 * this notice does not claim ownership of other contributors' material.
 * no general permission is granted by graphine studios to reuse, copy,
 * modify, redistribute or create derivative works from this file.
 */

import { defineConfig } from "vite";
import type { Plugin } from "vite";
import react from "@vitejs/plugin-react";

const fileProtocolAssets: Plugin = {
  name: "tidal-file-protocol-assets",
  transformIndexHtml: {
    order: "post",
    handler(html) {
      return html
        .replace(/<script\b([^>]*)>/gi, (tag, attributes: string) => {
          if (!/\bsrc\s*=/.test(attributes)) return tag;
          const cleaned = attributes
            .replace(/\s+type=(["'])module\1/i, "")
            .replace(/\s+crossorigin\b(?:=(?:"[^"]*"|'[^']*'|[^\s>]+))?/i, "")
            .replace(/\s+defer\b/i, "");
          return `<script${cleaned} defer>`;
        })
        .replace(/<link\b([^>]*)>/gi, (_tag, attributes: string) =>
          `<link${attributes.replace(
            /\s+crossorigin\b(?:=(?:"[^"]*"|'[^']*'|[^\s>]+))?/i,
            "",
          )}>`,
        );
    },
  },
};

export default defineConfig({
  base: "./",
  plugins: [react(), fileProtocolAssets],
  build: {
    outDir: "dist",
    emptyOutDir: true,
    sourcemap: false,
    manifest: true,
    modulePreload: false,
    rollupOptions: {
      output: {
        format: "iife",
        inlineDynamicImports: true,
      },
    },
  },
});
