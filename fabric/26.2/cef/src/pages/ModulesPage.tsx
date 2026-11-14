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

import { memo, useMemo, useState } from "react";
import { ModuleCard } from "../components/ModuleCard";
import type { TidalModule } from "../types";

interface ModulesPageProps {
  modules: TidalModule[];
  onModuleEnabled: (module: TidalModule, enabled: boolean) => void;
  onInspect: (module: TidalModule) => void;
}

function ModulesPageView({ modules, onModuleEnabled, onInspect }: ModulesPageProps) {
  const [category, setCategory] = useState("All");
  const categories = useMemo(
    () => ["All", ...new Set(modules.map((module) => module.group))],
    [modules],
  );
  const visible = category === "All"
    ? modules
    : modules.filter((module) => module.group === category);

  return (
    <>
      <div className="category-chips" aria-label="Filter modules by category">
        {categories.map((item) => (
          <button
            key={item}
            className={`category-chip${category === item ? " category-chip--active" : ""}`}
            aria-pressed={category === item}
            onClick={() => setCategory(item)}
          >
            {item}
          </button>
        ))}
      </div>
      <div className="module-grid">
        {visible.map((module) => (
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

export const ModulesPage = memo(ModulesPageView);
