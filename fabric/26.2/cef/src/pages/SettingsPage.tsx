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

import type { ReactNode } from "react";
import type { CEFStatus, TidalSettings } from "../types";
import { StatusPill } from "../components/StatusPill";
import { Toggle } from "../components/Toggle";

interface SettingsPageProps {
  settings: TidalSettings;
  cef: CEFStatus;
  onSetting: (setting: string, value: boolean | number) => void;
  category: string;
  onOpenControls: () => void;
}

function SettingRow({ title, description, children }: {
  title: string;
  description: string;
  children: ReactNode;
}) {
  return (
    <div className="setting-row">
      <div>
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
      {children}
    </div>
  );
}

export function SettingsPage({ settings, cef, onSetting, category, onOpenControls }: SettingsPageProps) {
  return (
    <div className="settings-stack">
      {category === "General" && <section className="settings-section">
        <p className="eyebrow">GENERAL</p>
        <SettingRow title="Use CEF UI" description="Start the embedded interface during client startup and use it when ready.">
          <Toggle
            checked={settings.useCefUi}
            label="Use CEF UI"
            onChange={(value) => onSetting("useCefUi", value)}
          />
        </SettingRow>
        <SettingRow title="Current renderer" description={cef.detail}>
          <StatusPill tone={cef.renderer === "CEF" ? "success" : cef.restartRequired ? "warning" : "neutral"}>
            {cef.renderer}
          </StatusPill>
        </SettingRow>
        <SettingRow title="Smooth transitions" description="Animate menu and control state changes.">
          <Toggle
            checked={settings.uiAnimations}
            label="Smooth transitions"
            onChange={(value) => onSetting("uiAnimations", value)}
          />
        </SettingRow>
      </section>}
      {category === "Appearance" && <section className="settings-section">
        <p className="eyebrow">APPEARANCE</p>
        <SettingRow title="Light theme" description="Use Tidal's light interface palette.">
          <Toggle
            checked={settings.lightTheme}
            label="Light theme"
            onChange={(value) => onSetting("lightTheme", value)}
          />
        </SettingRow>
      </section>}
      {category === "Advanced" && <section className="settings-section">
        <p className="eyebrow">ADVANCED</p>
        <SettingRow title="Developer mode" description="Reveal technical diagnostics and asset tools.">
          <Toggle
            checked={settings.advancedDeveloperMode}
            label="Advanced developer mode"
            onChange={(value) => onSetting("advancedDeveloperMode", value)}
          />
        </SettingRow>
        <SettingRow title="Edge smoothing" description="Supersampling level used by the native renderer.">
          <button
            className="quiet-button"
            onClick={() => onSetting(
              "uiAntialiasing",
              settings.uiAntialiasing === 1 ? 2 : settings.uiAntialiasing === 2 ? 4 : 1,
            )}
          >
            {settings.uiAntialiasing === 1 ? "Native" : `${settings.uiAntialiasing}×`}
          </button>
        </SettingRow>
      </section>}
      {category === "Graphics" && <section className="settings-section"><p className="eyebrow">GRAPHICS</p><SettingRow title="Edge smoothing" description="Supersampling quality for the native renderer."><button className="quiet-button" onClick={() => onSetting("uiAntialiasing", settings.uiAntialiasing === 1 ? 2 : settings.uiAntialiasing === 2 ? 4 : 1)}>{settings.uiAntialiasing === 1 ? "Native" : `${settings.uiAntialiasing}×`}</button></SettingRow><SettingRow title="Renderer status" description={cef.detail}><StatusPill tone={cef.renderer === "CEF" ? "success" : "warning"}>{cef.renderer}</StatusPill></SettingRow></section>}
      {category === "Text & Fonts" && <section className="settings-section"><p className="eyebrow">TEXT & FONTS</p><h3>Interface typography</h3><p className="settings-note">Tidal uses the system UI font with readable sizing and consistent line spacing.</p></section>}
      {category === "Input" && <section className="settings-section"><p className="eyebrow">INPUT</p><h3>Minecraft keybinds</h3><p className="settings-note">View or change Tidal shortcuts alongside your Minecraft controls.</p><button className="quiet-button settings-action" onClick={onOpenControls}>Open Controls</button></section>}
      {cef.progress > 0 && cef.progress < 1 && (
        <div className="progress-card" role="status">
          <span>Preparing the embedded browser…</span>
          <progress value={cef.progress} max={1} />
        </div>
      )}
    </div>
  );
}
