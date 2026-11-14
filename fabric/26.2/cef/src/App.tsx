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

import { useCallback, useEffect, useMemo, useState } from "react";
import { send } from "./bridge";
import { Icon } from "./icons";
import { PageHeader } from "./components/PageHeader";
import { StatusPill } from "./components/StatusPill";
import { HudPage } from "./pages/HudPage";
import { ModulesPage } from "./pages/ModulesPage";
import { SettingsPage } from "./pages/SettingsPage";
import type { Page, TidalModule, TidalSnapshot } from "./types";

const pages: { label: string; page: Page }[] = [
  { label: "Overview", page: "HUD" },
  { label: "HUD Studio", page: "HUD" },
  { label: "Modules", page: "Modules" },
  { label: "Mods", page: "Mods" },
  { label: "Settings", page: "Settings" },
];

const categories: Record<string, string[]> = {
  Overview: ["Dashboard", "What's New"],
  "HUD Studio": ["Elements", "Layouts", "Appearance"],
  Modules: ["All Modules", "HUD & Information", "Visuals", "Player", "World", "Performance", "Interface"],
  Mods: ["Installed", "Enabled", "Compatibility"],
  Settings: ["General", "Appearance", "Graphics", "Text & Fonts", "Input", "Advanced"],
};

function App() {
  const [snapshot, setSnapshot] = useState<TidalSnapshot | null>(null);
  const [query, setQuery] = useState("");
  const [error, setError] = useState("");
  const [inspector, setInspector] = useState<TidalModule | null>(null);
  const [section, setSection] = useState("Overview");
  const [category, setCategory] = useState("Dashboard");

  const refresh = useCallback(() => {
    send<TidalSnapshot>({ type: "snapshot" })
      .then(setSnapshot)
      .catch((reason: Error) => setError(reason.message));
  }, []);

  useEffect(() => {
    window.tidalUpdate = setSnapshot;
    window.tidalShown = refresh;
    refresh();
    return () => {
      delete window.tidalUpdate;
      delete window.tidalShown;
    };
  }, [refresh]);

  const command = useCallback(async (message: Record<string, unknown>) => {
    setError("");
    try {
      setSnapshot(await send<TidalSnapshot>(message));
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : String(reason));
      refresh();
    }
  }, [refresh]);

  const selectSection = (label: string, target: Page) => {
    setSection(label);
    setCategory(categories[label][0]);
    void command({ type: "navigate", page: target });
  };
  const filteredModules = useMemo(() => {
    const source = snapshot?.modules.filter((module) =>
      section === "HUD Studio" ? module.group === "HUD" : section === "Modules" ? module.group !== "HUD" : true,
    ) ?? [];
    const needle = query.trim().toLocaleLowerCase();
    return source.filter((module) => {
      const group = module.group.toLocaleLowerCase();
      const matchesCategory = category === "All Modules" || category === "Elements"
        || category === "Dashboard" || category === "What's New" || category === "Layouts"
        || category === "Appearance" || category === "Installed" || category === "Compatibility"
        || category === "General" || category === "Graphics" || category === "Text & Fonts"
        || category === "Input" || category === "Advanced"
        || (category === "HUD & Information" && group === "hud")
        || (category === "Visuals" && group === "visual")
        || (category === "Player" && ["movement", "inventory"].includes(group))
        || (category === "World" && group === "world")
        || (category === "Performance" && /fps|frame_time|memory|ping/.test(module.id))
        || (category === "Interface" && ["convenience", "interface"].includes(group))
        || (category === "Enabled" && module.enabled);
      return matchesCategory && `${module.name} ${module.id} ${module.description} ${module.group}`
        .toLocaleLowerCase().includes(needle);
    });
  }, [category, section, query, snapshot]);

  const setModule = useCallback((module: TidalModule, enabled: boolean) => {
    void command({ type: "setModule", id: module.id, enabled });
  }, [command]);
  const setSetting = useCallback((setting: string, value: boolean | number) => {
    void command({ type: "setSetting", setting, value });
  }, [command]);

  if (!snapshot) {
    return (
      <main className="connection-state">
        <strong>TIDAL</strong>
        {error && <span className="error-message">{error}</span>}
      </main>
    );
  }

  return (
    <main className={`app${snapshot.settings.lightTheme ? " app--light" : ""}${snapshot.settings.uiAnimations ? "" : " app--no-motion"}`}>
      <header className="topbar">
        <div className="brand"><span className="brand__mark">T</span><strong>TIDAL</strong></div>
        <label className="search-field topbar__search"><Icon name="search" size={15}/><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search anything…" aria-label="Search anything"/></label>
        <StatusPill tone={snapshot.cef.state === "Ready" ? "success" : "warning"}>{snapshot.cef.renderer}</StatusPill>
        <button className="icon-button" aria-label="Minimize" onClick={() => void command({ type: "minimize" })}>−</button>
        <button className="icon-button close-button" aria-label="Close Tidal" onClick={() => void command({ type: "escape" })}><Icon name="x"/></button>
      </header>
      <nav className="primary-nav" aria-label="Tidal destinations">
          {pages.map(({label: item, page: target}) => (
            <button
              key={item}
              className={`primary-nav__item${section === item ? " primary-nav__item--active" : ""}`}
              onClick={() => selectSection(item, target)}
            >
              <span>{item}</span>
            </button>
          ))}
      </nav>
      <div className="app-body"><aside className="sidebar">
        <div className="sidebar__label">{section}</div>
        <nav className="context-nav" aria-label={`${section} categories`}>{categories[section].map((item) => <button key={item} className={`context-nav__item${category === item ? " context-nav__item--active" : ""}`} onClick={() => setCategory(item)}>{item}</button>)}</nav>
        <div className="sidebar__footer"><StatusPill tone={snapshot.cef.state === "Ready" ? "success" : "warning"}>{snapshot.cef.state}</StatusPill></div>
      </aside>
      <section className="workspace">
        <PageHeader
          title={section}
          search={query}
          onSearch={setQuery}
          onClose={() => void command({ type: "escape" })}
        />
        <div className="page-intro"><h2>{category}</h2><p>{section === "Overview" ? "Your Tidal setup at a glance." : section === "Modules" ? "Browse, enable, and configure your features." : section === "HUD Studio" ? "Manage the on-screen information shown in your world." : section === "Settings" ? "Set up Tidal to fit your preferences." : "Review installed modifications."}</p></div>
        <div className="page-content" key={`${section}-${category}`}>
          {section === "Overview" && category === "Dashboard" && <div className="overview-grid"><button className="overview-card" onClick={() => selectSection("Modules", "Modules")}><Icon name="grid"/><strong>{snapshot.modules.filter((module) => module.enabled).length} enabled modules</strong><span>Review active features</span></button><button className="overview-card" onClick={() => selectSection("HUD Studio", "HUD")}><Icon name="layout"/><strong>HUD Studio</strong><span>Manage on-screen elements</span></button><section className="overview-card overview-card--wide"><strong>Installed mods</strong><span>{snapshot.mods.length} mods detected · {snapshot.mods.filter((mod) => mod.enabled).length} currently enabled</span></section></div>}
          {section === "Overview" && category === "What's New" && <section className="settings-section"><p className="eyebrow">TIDAL 26.2</p><h3>Interface and HUD tools</h3><p className="settings-note">Browse module categories, configure HUD elements, inspect installed mods, and adjust renderer preferences from the primary navigation.</p><button className="quiet-button settings-action" onClick={() => selectSection("Modules", "Modules")}>Browse modules</button></section>}
          {section === "HUD Studio" && category === "Elements" && (
            <HudPage
              modules={filteredModules}
              hudEnabled={snapshot.settings.hudEnabled}
              onHudEnabled={(enabled) => setSetting("hudEnabled", enabled)}
              onModuleEnabled={setModule}
              onInspect={setInspector}
            />
          )}
          {section === "HUD Studio" && category !== "Elements" && <section className="settings-section"><p className="eyebrow">HUD STUDIO · {category.toUpperCase()}</p><h3>{category === "Layouts" ? "Edit HUD layout" : "HUD appearance"}</h3><p className="settings-note">Open the in-game HUD editor to reposition elements and adjust their presentation.</p><button className="quiet-button settings-action" onClick={() => void command({type:"openHudEditor"})}>Open HUD editor</button></section>}
          {section === "Modules" && (
            <ModulesPage
              modules={filteredModules}
              onModuleEnabled={setModule}
              onInspect={setInspector}
            />
          )}
          {section === "Settings" && (
            <SettingsPage
              settings={snapshot.settings}
              cef={snapshot.cef}
              onSetting={setSetting}
              category={category}
              onOpenControls={() => void command({ type: "openControls" })}
            />
          )}
          {section === "Mods" && <div className="settings-stack">{snapshot.mods.filter((mod) => (category !== "Enabled" || mod.enabled) && `${mod.name} ${mod.id} ${mod.description}`.toLowerCase().includes(query.trim().toLowerCase())).map((mod) => <section className="settings-section mod-row" key={mod.id}><div><h3>{mod.name}<span className="mod-version"> {mod.version}</span></h3><p>{mod.description}</p><p>{mod.statusDetail}</p></div><div className="mod-row__actions"><StatusPill tone={mod.enabled ? "success" : "neutral"}>{mod.status}</StatusPill>{mod.supportsToggle && <button className="quiet-button" onClick={() => void command({type:"setMod", id:mod.id, enabled:!mod.enabled})}>{mod.enabled ? "Disable" : "Enable"}</button>}{mod.hasSettings && <button className="quiet-button" onClick={() => void command({type:"openModSettings", id:mod.id})}>Settings</button>}</div></section>)}</div>}
          {section !== "Settings" && section !== "Overview" && section !== "Mods" && (section !== "HUD Studio" || category === "Elements") && filteredModules.length === 0 && (
            <div className="empty-state">No features match this search.</div>
          )}
        </div>
        {inspector && (
          <aside className="inspector" aria-label={`${inspector.name} inspector`}>
            <div className="inspector__header">
              <Icon name="sliders" />
              <strong>{inspector.name}</strong>
              <button className="icon-button" aria-label="Close inspector" onClick={() => setInspector(null)}>
                <Icon name="x" />
              </button>
            </div>
            <p>{inspector.description}</p>
            <span className="inspector__category">{inspector.group}</span>
            <button
              className="inspector__toggle"
              disabled={!inspector.supported}
              onClick={() => setModule(inspector, !inspector.enabled)}
            >
              {inspector.enabled ? "Turn off" : "Turn on"}
            </button>
          </aside>
        )}
        {error && <div className="toast toast--error" role="alert">{error}</div>}
      </section>
      </div>
    </main>
  );
}

export { App };
