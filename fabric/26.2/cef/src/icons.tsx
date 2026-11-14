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

import { memo } from "react";

export type IconName =
  | "activity" | "anchor" | "box" | "check" | "chevronDown" | "clock"
  | "compass" | "crosshair" | "eye" | "grid" | "heart" | "keyboard"
  | "layers" | "layout" | "move" | "package" | "redo" | "rotate"
  | "search" | "settings" | "shield" | "sliders" | "sparkles" | "target" | "undo" | "x";

const paths: Record<IconName, string[]> = {
  activity: ["M22 12h-4l-3 9L9 3l-3 9H2"],
  anchor: ["M12 22V8", "M5 12H2a10 10 0 0 0 20 0h-3", "M8 5a4 4 0 1 1 8 0"],
  box: ["M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z", "m3.3 7 8.7 5 8.7-5", "M12 22V12"],
  check: ["m5 12 4 4L19 6"],
  chevronDown: ["m6 9 6 6 6-6"],
  clock: ["M12 8v4l3 2", "M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0Z"],
  compass: ["m16.2 7.8-2.1 6.3-6.3 2.1 2.1-6.3 6.3-2.1Z", "M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0Z"],
  crosshair: ["M12 2v4", "M12 18v4", "M4 12H2", "M22 12h-2", "M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8Z"],
  eye: ["M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7Z", "M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6Z"],
  grid: ["M3 3h7v7H3z", "M14 3h7v7h-7z", "M14 14h7v7h-7z", "M3 14h7v7H3z"],
  heart: ["M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8l1.1 1.1L12 21l7.8-7.5 1.1-1.1a5.5 5.5 0 0 0-.1-7.8Z"],
  keyboard: ["M3 5h18v14H3z", "M7 9h.01M11 9h.01M15 9h.01M17 9h.01M7 12h.01M11 12h.01M15 12h.01M7 15h10"],
  layers: ["m12 2 9 5-9 5-9-5 9-5Z", "m3 12 9 5 9-5", "m3 17 9 5 9-5"],
  layout: ["M3 3h18v18H3z", "M9 3v18", "M9 9h12"],
  move: ["M5 9 2 12l3 3", "M9 5l3-3 3 3", "m15 19-3 3-3-3", "m19 9 3 3-3 3", "M2 12h20", "M12 2v20"],
  package: ["m12 3 8 4.5v9L12 21l-8-4.5v-9L12 3Z", "M12 12 4 7.5", "M12 12l8-4.5", "M12 12v9"],
  redo: ["M15 14l5-5-5-5", "M20 9H9a6 6 0 0 0 0 12h2"],
  rotate: ["M20 7v5h-5", "M4 17v-5h5", "M5.6 9a7 7 0 0 1 11.6-2L20 12", "M4 12l2.8 5a7 7 0 0 0 11.6-2"],
  search: ["m21 21-4.3-4.3", "M11 19a8 8 0 1 1 0-16 8 8 0 0 1 0 16Z"],
  settings: ["M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8Z", "M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1-1.8 3.1-.2-.1a1.7 1.7 0 0 0-1.9.3l-.2.2h-3.5l-.1-.2a1.7 1.7 0 0 0-1.9-.3l-.2.1-1.8-3.1.1-.1A1.7 1.7 0 0 0 8.6 15l-.2-.1v-3.7l.2-.1a1.7 1.7 0 0 0-.3-1.9l-.1-.1L10 6l.2.1a1.7 1.7 0 0 0 1.9-.3l.1-.2h3.6l.1.2a1.7 1.7 0 0 0 1.9.3L18 6l1.8 3.1-.1.1a1.7 1.7 0 0 0 .3 1.9l.2.1v3.7l-.2.1Z"],
  shield: ["M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11Z", "m9 12 2 2 4-4"],
  sliders: ["M4 21v-7", "M4 10V3", "M12 21v-9", "M12 8V3", "M20 21v-5", "M20 12V3", "M2 14h4", "M10 8h4", "M18 16h4"],
  sparkles: ["m12 3 1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3Z", "m19 14 1 2.5 2.5 1-2.5 1-1 2.5-1-2.5-2.5-1 2.5-1 1-2.5Z", "m5 2 .8 2.2L8 5l-2.2.8L5 8l-.8-2.2L2 5l2.2-.8L5 2Z"],
  target: ["M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20Z", "M12 18a6 6 0 1 0 0-12 6 6 0 0 0 0 12Z", "M12 14a2 2 0 1 0 0-4 2 2 0 0 0 0 4Z"],
  undo: ["M9 14 4 9l5-5", "M4 9h11a6 6 0 0 1 0 12h-2"],
  x: ["m18 6-12 12", "m6 6 12 12"],
};

function IconView({ name, size = 18, className }: {
  name: IconName;
  size?: number;
  className?: string;
}) {
  return (
    <svg
      aria-hidden="true"
      className={className}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      {paths[name].map((path, index) => <path key={index} d={path} />)}
    </svg>
  );
}

export const Icon = memo(IconView);
