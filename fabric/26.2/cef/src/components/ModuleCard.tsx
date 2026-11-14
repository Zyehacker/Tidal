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
import { Icon, type IconName } from "../icons";
import type { TidalModule } from "../types";
import { Toggle } from "./Toggle";

interface ModuleCardProps {
  module: TidalModule;
  onToggle: (module: TidalModule, enabled: boolean) => void;
  onInspect: (module: TidalModule) => void;
}

const categoryIcons: Record<string, IconName> = {
  hud: "layout",
  movement: "move",
  visual: "eye",
  inventory: "package",
  convenience: "sparkles",
};
const moduleIcons: Record<string, IconName> = {
  fps: "activity", frame_time: "clock", ping: "compass", memory_usage: "box",
  coordinates: "compass", direction: "anchor", yaw_pitch: "move", health: "heart",
  potion_effects: "sparkles", potion_timers: "clock", held_item: "package",
  toggle_sprint: "move", toggle_sneak: "move", zoom: "search", fullbright: "eye",
  custom_crosshair: "crosshair", view_bobbing: "activity", keystrokes: "keyboard",
};

function ModuleCardView({ module, onToggle, onInspect }: ModuleCardProps) {
  const icon = moduleIcons[module.id] ?? categoryIcons[module.group.toLowerCase()] ?? "layers";
  return (
    <article className={`module-card${module.enabled ? " module-card--active" : ""}`}>
      <button
        className="module-card__main"
        aria-label={`Configure ${module.name}`}
        disabled={!module.supported}
        onClick={() => onInspect(module)}
      >
        <Icon name={icon} size={19} className="module-card__icon" />
        <span className="module-card__copy">
          <strong>{module.name}</strong>
          <span>{module.description}</span>
        </span>
      </button>
      <div className="module-card__actions">
        <button
          className="icon-button module-card__gear"
          aria-label={`Inspect ${module.name}`}
          onClick={() => onInspect(module)}
        >
          <Icon name="settings" size={15} />
        </button>
        <Toggle
          checked={module.enabled}
          label={`Enable ${module.name}`}
          disabled={!module.supported}
          onChange={(enabled) => onToggle(module, enabled)}
        />
      </div>
    </article>
  );
}

export const ModuleCard = memo(ModuleCardView);
