# Tidal Identity Audit Summary

This is the compact, standalone summary of the audit, resource map, namespace guidance, and replacement specifications in `Assetreplacement/`. It is documentation only: no production file was changed and no asset was copied.

## Finding and Scope

Audited `modules/`, `minecraft/`, `bootstrap/`, and `moduleexamples/`, including source UI/HUD code, settings/localizations, loader metadata, APIs, and owned resources. The example modules surfaced no OneConfig-specific user-facing text. Generated build/test reports, crash/replay logs, downloaded content, and `minecraft/run/` user/runtime data are not production UI and are excluded.

No OneConfig-branded production logo, visible menu/settings copy, documentation link, source `assets/oneconfig/` tree, or OneConfig-specific setting/default was identified. Tidal copy, config identity, links, logos, and resources already use Tidal identity. Source-level remnants include `org.polyfrost.oneconfig` APIs/service types, internal names such as `OneConfigUIScreen`, `OneConfig.java`, `oneConfigScreenOpen`, and `oneconfigKeybind`; treat these as compatibility/runtime identifiers, not labels. `modules/poly-compose/build.gradle.kts` contains an upstream OneConfig/Polyfrost copyright and license notice; preserve it as legal provenance. Generated test output also contains old OneConfig strings but is not shipped UI. The credits entry “(OneClient)” is a separate historical attribution: verify it with maintainers and do not remove without confirmation.

## Priority and Required Work

| Priority | Finding and action |
|---|---|
| CRITICAL | None confirmed. |
| HIGH | Replace the Forge `mods.toml` sample/Lorem Ipsum description and example contact/URL text with approved Tidal metadata. Correct the bootstrap description `Tidal Client & Configuration Engine`, which can imply a bundled gameplay client. Both are stale/inaccurate metadata, not direct OneConfig branding. |
| MEDIUM | Reconcile loader-visible names: bootstrap `Tidal` versus main Fabric `Tidal (Platform Module)`. Decide whether users should see one product name or intentionally distinct entries. |
| HIGH | Approve a distinctive Tidal semantic palette and coherent theme treatment before production restyling; current glass themes use cyan/blue and local HUD/status styles, but no exact OneConfig palette match was established. |
| MEDIUM | Review component-local colors/shapes, spacing, font sizes, gradients, shadows, and screen-state styling for consistency. Preserve meaningful status and selection colors. |
| LOW | Review typography hierarchy/weights and the historical “OneClient” credit. No font or icon file is required to be replaced. |

The detailed inventory classifies items as BRANDING, VISUAL, ASSET, SETTING, TEXT, RESOURCE, METADATA, or ARCHITECTURE and records location, user visibility, replacement path, and priority.

Metadata constraints: for Forge, approve display name/description/contact/logo, verify `mod_name`/version expansion, Forge schema, supported loader/Minecraft ranges, and the packaged artifact. Keep license, mod IDs, dependency table semantics, and loader ranges unchanged unless separately validated. For bootstrap, verify whether Mod Menu exposes it and keep `id`, `provides`, dependencies, icon, license, and entrypoints. For the main Fabric module, check both Mod Menu entries and packaging variants; keep IDs, parent relations, mixins, entrypoints, and dependencies. Display-copy changes are low risk; changing loader/dependency identifiers can be high risk. The Compose bundle and legacy dependency shim metadata are third-party/infrastructure labels and should remain unless actual packaging creates confusing user-facing entries.

## Settings and Identity

- **Infrastructure, retain:** setting annotations/keys, config serialization and persistence, search mechanics, category/group machinery, defaults that control behavior, and API contracts.
- **User-facing, already Tidal:** menu/preferences copy, Tidal keybind, HUD options, categories, theme labels, notifications, and localized names/descriptions. No setting needs renaming to remove OneConfig identity; do not change keys, enum semantics, or behavior.
- **Brand/config identity, already Tidal:** `tidal.json`, `themes.json`, config titles `Tidal`/`Themes`, Tidal icon paths, `TidalGlass` themes, and descriptions naming the Tidal interface. The default theme is `TidalGlass Dark`; the configurable accent defaults to `0xFF00E5FF`.
- **Visual, review only:** accent and theme colors, previews, typography hierarchy, and local component states. Theme values are persisted; renaming theme/config identifiers can orphan user preferences.

## Asset and Resource Map

| Resource group | Current files/use | Audit disposition |
|---|---|---|
| Tidal branding | `bootstrap/.../tidal-icon.png`; `assets/tidal/brand/tidal.svg`, `tidal-light.svg`, `tidal-mc.svg`, `tidal-mc-light.svg`, `tidal-icon.svg`, `tidal.png`, `by-tidal.svg`, `atmofrost.svg` | Existing Tidal/credited art; retain in place. Respect credit attribution. |
| Theme and UI images | `polyglass-dark/light.svg`, `minecraft-dark/light.svg` are theme previews; `head.png` is a player-head fallback; `header.png` is in the changelog's URL-derived image pool (`images/$url.png`) | Retain; inspect dynamic changelog-selected image content during entry review. |
| Icons/color controls | General SVG controls under `modules/ui/.../assets/tidal/ico/`, Minecraft-styled SVG alternatives under `ico/minecraft/`, plus `color/alpha.png` and `hue.png` for the color picker | Functional assets, not OneConfig marks; retain theme mappings. |
| Fonts | Poppins TTF weights/italics for glass themes; Minecraft-Regular/Bold OTF for Minecraft themes; `unifont.otf` is present but not identified as the main theme family | Retain pending design/licensing/usage review. No WOFF/WOFF2 found in reviewed source resources. |
| Audio | Minecraft UI `click.ogg`, `close.ogg`, `open.ogg`, `slider.ogg`; root ambient `lullaby.ogg`, `moog_1.ogg`, `moog_2.ogg`, `trees.ogg`, and UI `pad_loop.ogg` | Tidal interaction/ambient audio, not OneConfig identity; retain IDs and behavior. |
| Text/registration/metadata | Tidal `en_default.lang` and `lang/en_us.json`; `assets/tidal/sounds.json`, `sounds/external_sounds.json`; Fabric metadata JSON, Forge `META-INF/mods.toml`, and Java `META-INF/services/*` | Localization is Tidal-specific. Sound/resource IDs and service filenames are runtime wiring. Only the specified loader display text needs review. |

No OneConfig-branded source asset was found; no new logo, font, icon pack, background, or preview is mandatory. No `.webp`, `.jpg`, or `.mcmeta` replacement asset was found in reviewed source-owned paths. If an approved future design requires new artwork, document it here as `MISSING — DESIGN REQUIRED`; do not create placeholders, empty asset folders, or duplicate existing Tidal assets.

## Namespace, Visual System, and Protection

Minimum safe namespace migration is **none**: source assets already use `assets/tidal/`. Keep new Tidal artwork there. Do not broadly rename namespaces, `org.polyfrost.oneconfig` APIs, service-provider filenames, mixin/accessor names, external compatibility methods, mod IDs, `tidal.json`, `themes.json`, or theme IDs. Replace a legacy resource only if it is confirmed user-facing, update only its explicit references, and validate all supported loaders/versions. Any API/package split is a separate binary-compatibility project.

Four themes are registered: `TidalGlass Dark/Light` and `Minecraft Dark/Light`. `Impls.kt` centralizes theme colors, shapes, logo paths, typography families, and Minecraft icon overrides; `UITheme`/`UIBranding`/`UITypography` define the theme contract. `ThemeConfig.java`, `Provider.kt`, and `ThemeRegistry.kt` handle accent/animation preferences and active-theme selection. Spacing, much font sizing, and some colors/shapes/gradients are local; border gradients and shell shadow are implemented separately. Review navigation, settings rows, buttons, toggles, sliders, dropdowns, search, tooltips, notifications, popups, theme cards, loading feedback where present, empty/error states, and HUD selection/anchor/lock/delete surfaces. Define tokens for page/sidebar/surface/popup/border/text/accent/focus and success/warning/error; specify contrast/opacity in light and dark modes, shape/radius, shadow, spacing, type hierarchy, and hover/focus/disabled/invalid states. Do not invent brand colors, fonts, logos, or motifs in this audit.

Preserve the public theme contract and registered-theme behavior. Renaming active theme values stored in `themes.json` requires migration/compatibility. A visual implementation must retain setting and HUD workflows and validate all four themes, custom accent, readable control states, notifications/popups, and HUD select/drag/resize/snap/locked behavior. Font changes additionally require licensing, glyph coverage, and supported-version checks.

## Final Boundary and Next Step

**Keep:** rendering/configuration/events/APIs, HUD editor, Minecraft integration, authentication, utilities, generic settings, existing Tidal assets, and current workflows. **Do not touch:** upstream legal notices, compatibility/service contracts, persisted IDs, loader wiring, or functionality for cosmetic reasons. Metadata prose is comparatively low risk; dependency/resource/identifier changes can break startup or user data. A broad theme pass can regress contrast and editor states.

Next: approve consistent Tidal loader wording and make display-only metadata corrections; then approve a design brief and implement/test tokens and component styling as a separate phase. Detailed references: [README](README.md), [AUDIT](AUDIT.md), [resource map](resources/resource-map.md), [namespace migration](resources/namespace-migration.md), [required metadata specification](replacements/required/metadata.md), and [recommended visual-system specification](replacements/recommended/visual-system.md).
