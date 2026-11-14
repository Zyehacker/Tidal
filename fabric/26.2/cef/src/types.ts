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

export type Page = "HUD" | "Modules" | "Mods" | "Settings";

export interface TidalModule {
  id: string;
  name: string;
  description: string;
  group: string;
  enabled: boolean;
  supported: boolean;
  keybind: string;
}

export interface InstalledMod {
  id: string;
  name: string;
  version: string;
  description: string;
  status: string;
  statusDetail: string;
  supportsToggle: boolean;
  enabled: boolean;
  hasSettings: boolean;
}

export interface TidalSettings {
  useCefUi: boolean;
  hudEnabled: boolean;
  uiAnimations: boolean;
  lightTheme: boolean;
  advancedDeveloperMode: boolean;
  uiAntialiasing: number;
}

export interface CEFStatus {
  state: string;
  renderer: string;
  detail: string;
  progress: number;
  restartRequired: boolean;
}

export interface TidalSnapshot {
  page: Page;
  query: string;
  settings: TidalSettings;
  modules: TidalModule[];
  mods: InstalledMod[];
  cef: CEFStatus;
}
