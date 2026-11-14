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

import type { TidalSnapshot } from "./types";

interface CefQueryOptions {
  request: string;
  onSuccess: (response: string) => void;
  onFailure: (code: number, message: string) => void;
}

declare global {
  interface Window {
    cefQuery?: (options: CefQueryOptions) => void;
    tidalUpdate?: (snapshot: TidalSnapshot) => void;
    tidalShown?: () => void;
  }
}

export function send<T = unknown>(message: Record<string, unknown>): Promise<T> {
  return new Promise((resolve, reject) => {
    if (!window.cefQuery) {
      reject(new Error("Tidal's Java bridge is unavailable."));
      return;
    }

    window.cefQuery({
      request: JSON.stringify(message),
      onSuccess: (response) => {
        try {
          resolve(JSON.parse(response) as T);
        } catch (error) {
          reject(error);
        }
      },
      onFailure: (_code, messageText) => reject(new Error(messageText)),
    });
  });
}
