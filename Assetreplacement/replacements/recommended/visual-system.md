# Recommended Tidal Visual-System Specification

This brief describes a future visual differentiation pass, not a claim that current colors or controls were copied from OneConfig. No OneConfig-specific palette, font, icon, or component asset was confirmed. Do not implement values or create sample assets from this document alone.

## Central Theme System

- **Current:** Four built-in themes (`TidalGlass Dark/Light`, `Minecraft Dark/Light`) are registered centrally. Their colors, shapes, logos, typography, and Minecraft icon overrides are largely declared in `Impls.kt`; the theme contract is `UITheme`/`UIBranding`/`UITypography` in `Structs.kt`. The Tidal accent and animation preference live in `ThemeConfig.java` and theme state is handled by `Provider.kt`/`ThemeRegistry.kt`.
- **Location:** `modules/internal/src/main/kotlin/org/tidal/tidal/internal/ui/themes/Impls.kt`, `Structs.kt`, `Provider.kt`, `ThemeRegistry.kt`; `modules/internal/src/main/java/org/tidal/tidal/internal/ThemeConfig.java`.
- **Purpose:** Shared colors, component shapes, logo selection, typeface, theme registration, and user accent preference.
- **Why it creates OneConfig identity:** No exact OneConfig match was demonstrated. The default glass themes use cyan/blue glass treatments and common configuration-panel conventions; without a distinct, coherent Tidal visual language, those familiar choices can read as inherited/generic rather than ownable.
- **Replacement needed:** Commission a Tidal design brief first. Define semantic tokens for page/sidebar/surface/popup/border/text/accent/focus and success/warning/error states; specify light/dark contrast and opacity behavior; choose shape/radius, shadow, spacing, and typography rules; define active/hover/disabled/focus/error states. Do not select actual palette values, fonts, logos, or product motifs in this audit.
- **Suggested Tidal location:** Central theme definitions and token contract under `modules/internal/.../ui/themes/`; any approved previews or new artwork under existing `modules/internal/src/main/resources/assets/tidal/images/` and `assets/tidal/brand/` paths. Keep design definitions and future assets organized under `Assetreplacement/` until accepted.
- **Dependencies:** The theme data class is consumed by UI components and may be extended by registered themes; preserve its API/meaning. The active theme name is persisted in `themes.json`, and the accent in Tidal configuration, so renaming identifiers requires migration or compatibility handling.
- **Risk:** Medium to high across the whole UI. Low if adding tokens without changing behavior; higher if theme names, registered-theme API, persistence values, or contrast behavior change. Validate four built-in themes, custom accent behavior, readable controls, and notification overlays.

## Component and HUD Styling

- **Current:** Shared theme-aware components exist for navigation, config/settings rows, buttons, switches, sliders, dropdowns, search, tooltips, notifications, popups, and theme cards. Several exceptional colors and shapes are also declared locally, especially in HUD editor components and settings/status controls.
- **Location:** `modules/internal/src/main/kotlin/org/tidal/tidal/internal/ui/components/**`, `ui/screens/**`, `ui/hud/**`, and `ui/themes/**`. Verified examples include HUD alignment/dropdown/radio components, HUD design-studio selection/anchor/lock/delete states, notification success/error colors, info states, and keybind conflict indicators.
- **Purpose:** User-visible layout and interaction feedback, including the full HUD editor and config/settings workflow.
- **Why it creates OneConfig identity:** The audit has not established that these values reproduce OneConfig. The risk is inconsistency: central theme values coexist with one-off styling, which can weaken a coherent Tidal identity and make different screens feel unrelated.
- **Replacement needed:** During implementation, map each literal color/shape to a semantic token where it represents a shared concept; preserve intentional per-state colors when they communicate meaning. Review spacing/padding and font sizes, which are often component-local, as well as button/toggle/slider/dropdown states, search, navigation, tooltips, notifications, popups, empty/error states, and HUD editor overlays. Keep layout, editing gestures, selection/drag/resize, snapping, and setting behavior intact.
- **Suggested Tidal location:** Existing theme-aware components under `modules/internal/src/main/kotlin/org/tidal/tidal/internal/ui/`; semantic design rules documented under `Assetreplacement/replacements/recommended/`.
- **Dependencies:** Coordinate with the central token contract above and preserve Minecraft theme icon/font overrides. HUD editor components are functional tools and require interaction-state parity.
- **Risk:** Medium. A broad literal replacement can erase important status/selection contrast or regress the editor. Validate dark/light modes, focused/disabled/invalid states, notifications, popup stacking, all setting controls, and HUD select/drag/resize/locked states.

## Typography and Iconography

- **Current:** Poppins is used in glass themes; Minecraft font files and a Minecraft-specific icon set are used by Minecraft themes. A general SVG icon family is also present. This audit found no OneConfig font or logo asset in those resources.
- **Location:** `modules/ui/src/main/resources/assets/tidal/fonts/`, `assets/tidal/ico/`; theme selection in `Impls.kt`.
- **Purpose:** Text rendering and functional control/navigation symbols.
- **Why it creates OneConfig identity:** No direct identity link found. Typography sizing/weight and icon stroke consistency can still contribute to a generic look if left unspecified.
- **Replacement needed:** No font or icon file is currently required to be replaced. If the approved design brief asks for a different typographic voice or icon family, write an explicit usage/licensing and coverage specification first; keep Minecraft-specific variants where that mode is intentional. No random replacement asset should be generated.
- **Suggested Tidal location:** Existing font and icon resource paths only after approval; use `Assetreplacement/` for an accepted style/asset brief before production integration.
- **Dependencies:** Font licensing/redistribution, glyph coverage, text rendering in all supported versions, and icon contrast/size in both regular and Minecraft themes.
- **Risk:** Low for hierarchy/weight tuning; medium for replacing font files or icon identifiers because of glyph coverage and missing-resource risk.

## Asset Status

There is no mandatory new Tidal logo, font, icon pack, background, or theme-preview file identified by this audit. Existing Tidal assets are already available in production paths and should be referenced, not copied. If an approved future design requires new artwork, add a specific brief and mark that item `MISSING — DESIGN REQUIRED` until it is created; do not use placeholders.
