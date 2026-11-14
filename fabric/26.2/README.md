# Tidal 26.2 client

This target uses the 26.2 Fabric toolchain and keeps client code in the client
source set. The native drawer has four destinations: HUD, Modules, Mods, and
Settings. Modules expose implemented Tidal features only, with category
filters, enable switches, keyboard toggles, module details, and a HUD editor.
Mods lists installed Fabric metadata and safely cached JAR icons; it offers a
runtime switch only to mods that explicitly provide Tidal's `tidal:toggle`
contract. Other mods are marked restart-required. Tidal does not unload or reload mod
classloaders or attempt to remove mixins.

View-bobbing, fullbright, zoom, and crosshair preferences are accessed from
their Tidal module inspector; the crosshair's shape, size, gap, thickness,
outline, dynamic gap, and color remain persisted. Settings contains only
application preferences: General, Appearance, Graphics, Text & Fonts, Input,
and Advanced. The Input action opens Minecraft's real keybinding screen.
Persisted Tidal preferences and module settings remain in
`config/tidal.json`. Graphics offers native, 2×, and 4× supersampled SVG
rasterization with bicubic downsampling. The SVG gallery is gated behind
Advanced / Developer Mode.

The HUD editor supports direct click-and-drag positioning and previews only the
selected element without enabling it. Shift-click selects multiple elements;
group drags preserve their relative offsets, live gap labels show nearby
spacing, and merged backgrounds render as a single persisted surface. These
operations are included in bounded undo/redo. Implemented HUD elements start at
separate positions; older saved elements sharing the default origin are
separated when the configuration is loaded.

Implemented HUD data includes FPS, server ping/address and player count, Java
heap use, coordinates, direction and compass heading, yaw/pitch, biome and
dimension, resource-pack names, absorption, hunger/saturation, horizontal
movement speed, armor points and durability, active-effect count and timers,
health, XP, held item and durability, inventory counts for selected items,
world day/time, local time, current-session elapsed time, rolling left/right
CPS, and live movement/action keys. Values are collected from the active client
and unavailable values are hidden where appropriate. Remaining catalogue
entries are unsupported and are not exposed as actionable toggles.

The Obsidian UI uses Minecraft's native text and GUI texture pipelines. Tidal
UI text uses the bundled Noto Sans TrueType font provider with linear filtering
on its 3× supersampled glyph atlas, followed by Minecraft's default and
Unifont providers for fallback; Minecraft's normal font and other glyph atlases
remain unchanged. The font and full SIL Open Font License are
included in the client resources. The Liquid Glass surfaces, controls, borders,
and icons are loaded from the 102-file SVG pack and its manifest under
`src/client/resources/assets/tidal/textures/gui/liquid-glass/`. The renderer
validates the manifest and SVG subset at startup, rasterizes to the active
framebuffer scale on a worker thread, supersamples at the selected quality,
downsamples with bicubic filtering, and uploads cached dynamic textures on
the render thread. Textures use premultiplied-alpha pixels, linear filtering,
clamp-to-edge addressing, manifest nine-slice insets, and a 128 MiB LRU cache.
The Advanced settings page opens a scrollable gallery that previews every manifest asset
at raster scales 1× through 4×. `:fabric:26.2:verifyLiquidGlassAssets` checks
all 102 assets, the 26 icons, four-times corner antialiasing, and nine-slice
output at multiple sizes.
The compact drawer uses a responsive header, category filters, a paged module
grid of descriptive module cards, and a contextual module inspector. Hover,
selection, toggle, and brief press feedback share a frame-time-based motion helper.
The drawer also has a 210 ms eased scale and vertical-offset transition. The
Obsidian palette uses ARGB colors, and HUD opacity multiplies source alpha.
The [UI/UX system specification](./DESIGN_SYSTEM.md) documents component
guidelines, semantic tokens, interaction timing, information architecture, and
performance-oriented code boundaries.
Header search accepts aliases across module categories and metadata search on
the Mods destination. HUD Studio includes bounded undo/redo for element
position, scale, and anchor changes.
The translucent Liquid Glass artwork is integrated, but Minecraft 26.2's
current UI renderer does not provide backdrop blur or refraction for these
screens. No blur-only mode is advertised as glass.
Text inputs keep their text area inset from the rounded border; navigation
labels are measured with the UI font, and other button labels are centered and
constrained to their available width. Enum values shown by the HUD editor and
inspector are formatted as readable labels. The module inspector is rendered
in a later GUI stratum than the module grid to prevent controls from bleeding
through its surface.

The centered drawer is capped at 760 × 460 GUI pixels, keeps 16 pixels of
viewport clearance where possible, and retains the same dimensions and
position across tabs. Its header, 740-pixel content area, three-column
240-pixel module cards, and footer use fixed logical-pixel measurements.

The HUD Studio opens from the Tidal menu or with the configurable `H` key; its
compact inspector is initially docked at the right edge to preserve the central
gameplay preview.

## Embedded CEF interface

The React/TypeScript interface in [`cef/`](./cef/) is selected by default with
the persisted `Use CEF UI` preference (`config/tidal.json`). The choice is saved
with the other Tidal application preferences. The native renderer remains
available as an immediate fallback and uses the same Tidal configuration,
module, HUD, and runtime-toggle APIs.

CEF starts warming in the background after the client finishes loading. On the
first run, JCEF downloads and extracts only the current platform runtime under
`config/tidal/cef/runtime/`, keeps browser cache data under
`config/tidal/cef/cache/`, and extracts the packaged UI under
`config/tidal/cef/frontend/`. No operating-system CEF binaries or remote UI
assets are bundled in the mod. Runtime download or initialization errors are
logged and shown in Settings while the native screen remains available. An
offline first open therefore falls back rather than showing a blank screen.
Disabling an initialized CEF runtime disposes its browser and client. Because
JCEF's process-wide `CefApp` lifecycle is terminal after disposal, re-enabling
it in that session reports that a Minecraft restart is required; disabling it
before its first initialization does not start or extract CEF.

Windowless rendering uses Tidal's `TidalOsrBrowser`, a `CefBrowser_N` subclass
that implements `CefRenderHandler` directly. CEF paint callbacks deliver dirty
rectangles and pixels to a CPU buffer; Minecraft uploads changed regions to its
GUI texture on the render thread. This path creates no JOGL drawable or hidden
utility window. The browser stays resident between opens. Mouse, scroll,
keyboard, text-entry, focus, resize, and Escape input are forwarded to the
browser. It is capped at 30 FPS and throttled while hidden. Runtime CPU,
memory, and multiple-GUI-scale behavior have not been profiled in-game.

JCEF `152.0.6` is pinned (the earlier `146.0.10` candidate was evaluated).
The current JCEF Maven project documents Java 8+ and platform downloads, but
does not certify every Java 25 / Minecraft 26.2 / Fabric combination. This
workspace has compiled the JCEF integration with Java 25 and verified a
Windows x64 browser frame received and uploaded to Minecraft. The trial ran
with `java.awt.headless=true` and without the former JOGL module/export
arguments or headless-property clearing. A missing `java.desktop` module is a
hard runtime limitation and requires a full Java 25 desktop runtime. JCEF OSR
is unsupported on Windows ARM64. Normal game launches require native access:

```text
--enable-native-access=ALL-UNNAMED
```

On macOS, also add the three `--add-opens` options documented in
[`cef/THIRD_PARTY_NOTICES.md`](./cef/THIRD_PARTY_NOTICES.md). The Gradle
`runClient` task supplies native access and the macOS options for development;
installed launchers must be configured separately. Missing macOS options are
detected before JCEF initialization and produce a native-renderer fallback
with an actionable status. Mouse/keyboard behavior, macOS and other platform
compatibility, and runtime CPU, memory, and multiple-GUI-scale behavior remain
unverified.

The frontend build is wired into `processClientResources`, `build`, and the
26.2 mod JAR. A clean frontend build requires Node.js `20.19+` or `22.12+` and
npm (`npm ci` followed by `npm run build`); build dependencies are pinned by
`cef/package-lock.json`. The bundled font/license are frontend assets; the upstream dependency notices
are included in the mod resources and documented in
[`cef/THIRD_PARTY_NOTICES.md`](./cef/THIRD_PARTY_NOTICES.md).
Enabled elements can be selected and dragged directly, resized using the
selection corner handle, snapped to an 8-pixel grid or nearby screen/element
edges, locked, assigned to one of nine screen anchors, shown or hidden, and
saved with normalized positions and scale. Per-element style, text/accent/
surface colors, and surface opacity can be adjusted. The editor previews
enabled elements and a labeled preview for a disabled selection without
enabling it. HUD panels use the same Obsidian palette as the menu.

HUD layouts include Default, Minimal, Performance, PvP, and Custom presets,
with controls to load, save/create, rename, and delete user layouts. These are
HUD-only layouts, separate from general Tidal configuration profiles. Several
catalogue utilities and requested visual modules remain unimplemented; see
[`FEATURE_INVENTORY.md`](./FEATURE_INVENTORY.md) for audited status. The
recommended additions for distinct per-module, HUD Studio, Visuals, Settings,
and Profiles SVGs are listed in
[`SVG_ASSET_GAP_REPORT.md`](./SVG_ASSET_GAP_REPORT.md).
