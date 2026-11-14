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

import type { TidalModule } from "../types";
import { ModuleCard } from "../components/ModuleCard";
import { Toggle } from "../components/Toggle";

interface HudPageProps {
  modules: TidalModule[];
  hudEnabled: boolean;
  onHudEnabled: (enabled: boolean) => void;
  onModuleEnabled: (module: TidalModule, enabled: boolean) => void;
  onInspect: (module: TidalModule) => void;
}

export function HudPage({
  modules, hudEnabled, onHudEnabled, onModuleEnabled, onInspect,
}: HudPageProps) {
  return (
    <>
      <section className="preference-card">
        <div>
          <h2>In-game HUD</h2>
          <p>Show Tidal elements while you play.</p>
        </div>
        <Toggle checked={hudEnabled} label="Enable Tidal HUD" onChange={onHudEnabled} />
      </section>
      <div className="card-list">
        {modules.map((module) => (
          <ModuleCard
            key={module.id}
            module={module}
            onToggle={onModuleEnabled}
            onInspect={onInspect}
          />
        ))}
      </div>
    </>
  );
}
