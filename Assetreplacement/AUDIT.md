# Tidal Identity Audit

## Result

No direct OneConfig branding was found in the inspected production UI copy, owned runtime resource namespace, current logos, or loader metadata values other than source/API identifiers. One upstream copyright/license notice remains in a build script and must be preserved as provenance, not treated as runtime branding. Existing UI copy, config identity, and art are largely Tidal-branded already. The clear release-facing problems are stale/template Forge metadata and inconsistent loader display names; the main visual risk is that the shared glass-theme palette and scattered HUD/status colors do not yet form a clearly independent design system. This is an audit only.

Audit scope: `modules/`, `minecraft/`, `bootstrap/`, and `moduleexamples/`, including source resources, localizations, metadata, public APIs, UI/HUD code, and the current asset roots. The scoped identity search in `moduleexamples/` found no OneConfig-specific user-facing text. Search results from generated run/crash data and build outputs were excluded from identity findings; those include historical OneConfig strings in generated test reports and are not shipped UI/source assets. `minecraft/run/` is runtime output, not a source asset tree.

## Inventory

| Current OneConfig item | Location | Type | User-visible | Replacement required | Replacement location | Priority |
|---|---|---|---|---|---|---|
| No OneConfig logo or wordmark identified in owned UI resources | `modules/internal/src/main/resources/assets/tidal/brand/`; `bootstrap/src/main/resources/tidal-icon.png` | ASSET · branding | Yes, as Tidal art | No OneConfig asset to replace. Retain the existing Tidal logo family; review it only if a future art direction calls for a redesign. | Existing production assets; no copy | KEEP |
| Product/resource namespace is Tidal, with no source `assets/oneconfig/` tree | `modules/**/src/main/resources/assets/tidal/`; `minecraft/src/main/resources/assets/tidal/` | RESOURCE · namespace | Indirectly | No broad namespace rename. Keep new Tidal-owned UI resources in `assets/tidal/`. | `resources/namespace-migration.md` | KEEP |
| `org.polyfrost.oneconfig` packages and service-provider types | `modules/ui/src/main/kotlin/org/polyfrost/oneconfig/`; `minecraft/src/main/resources/META-INF/services/` and source references | ARCHITECTURE · API/service contract | No, not by themselves | No. These are implementation/API and Java service lookup identifiers, not labels; do not rename in an identity pass. | None | KEEP |
| `OneConfigUIScreen`, `OneConfig.java`, `oneConfigScreenOpen`, and related legacy method/field names | `minecraft/src/main/**`; `modules/**` | ARCHITECTURE · internal symbols | No, not by themselves | No identity replacement. Rename only in a separately tested compatibility/API change if external contracts permit it. | None | KEEP |
| Explicit OneConfig/Polyfrost copyright and license provenance notice | `modules/poly-compose/build.gradle.kts` | ARCHITECTURE · legal/provenance | Visible in source checkout, not in runtime UI | No. Preserve the notice as written; do not rebrand, delete, or alter it as part of UI identity work. Refer any provenance/licensing question to maintainers. | None | KEEP |
| Third-party Compose bundle and legacy dependency shim display metadata | `modules/compose-bundle/src/main/resources/fabric.mod.json`; `modules/dependencies/legacy/src/main/resources/fabric.mod.json` | METADATA · dependency modules | May appear in loader metadata | No OneConfig identity found. Retain dependency attribution and internal identifiers; only adjust if packaging makes confusing product entries visible. | None | KEEP |
| Tidal menu, preferences, notification, and first-launch wording | `modules/internal/src/main/resources/assets/tidal/en_default.lang`; `minecraft/src/main/resources/assets/tidal/lang/en_us.json` | TEXT · localization | Yes | No OneConfig wording found. Retain current Tidal-specific copy; normal editorial work is outside this audit. | Existing language resources | KEEP |
| Config file identities `tidal.json` and `themes.json`, title `Tidal`/`Themes`, and Tidal config icon | `modules/internal/src/main/java/org/tidal/tidal/internal/tidalConfig.java`; `ThemeConfig.java` | SETTING · config identity | Yes | No. These already identify Tidal and should not be renamed for cosmetic reasons. | Existing config definitions | KEEP |
| Preferences, setting titles/descriptions, categories, option labels and search-facing localized strings | `TidalConfig.java`; `assets/tidal/lang/en_us.json`; `assets/tidal/en_default.lang` | SETTING · labels/copy | Yes | No OneConfig-specific setting text identified. Preserve setting keys/defaults and behavior. Reword only if a future copy review finds misleading or awkward text. | No current replacement asset | KEEP |
| Historical credits include a designer attribution ending in “(OneClient)” | `modules/internal/src/main/kotlin/org/tidal/tidal/internal/ui/screens/Credits.kt` | TEXT · credit attribution | Yes | Not a OneConfig reference. Verify the historical product attribution with maintainers; preserve accurate credit and do not silently remove it as rebranding. | No replacement until verified | LOW |
| Forge metadata contains sample description, lorem ipsum, commented example URLs, and template dependency labels | `minecraft/src/main/resources/META-INF/mods.toml` | METADATA · loader identity | Yes | Yes, replace user-facing placeholder text and align displayed name/description/contact with approved Tidal metadata. Preserve loader IDs and dependency semantics pending Forge validation. | `replacements/required/metadata.md` | HIGH |
| Bootstrap description calls Tidal a “Client & Configuration Engine” | `bootstrap/src/main/resources/fabric.mod.json` | METADATA · product description | Yes | Yes, clarify that this is the Tidal bootstrap/library distribution rather than implying a bundled client. No wording is invented in this audit. | `replacements/required/metadata.md` | HIGH |
| Main Fabric module is named `Tidal (Platform Module)` while bootstrap is named `Tidal` | `minecraft/src/main/resources/fabric.mod.json`; `bootstrap/src/main/resources/fabric.mod.json` | METADATA · display names | Yes in mod lists | Reconcile display names for the intended user-facing install; retain internal mod IDs, dependencies, and module split. | `replacements/required/metadata.md` | MEDIUM |
| Glass theme palette, theme names/previews, theme logos, global accent default | `modules/internal/src/main/kotlin/org/tidal/tidal/internal/ui/themes/Impls.kt`; `ThemeConfig.java`; `assets/tidal/images/` | VISUAL · colors/branding | Yes | Recommended: define and document a distinct Tidal palette/contrast system, then update central theme values and previews as one reviewed design. Current Tidal logos and theme choices are not OneConfig assets. | `replacements/recommended/visual-system.md`; existing Tidal assets remain in place | HIGH |
| Theme colors, shapes, typography family, logo, icon overrides, border gradients, and shadow are only partly centralized; most spacing/font sizes and some component colors/radii/gradients are local | `ui/themes/Structs.kt`, `Impls.kt`, `Provider.kt`; `ui/components/Borders.kt`; `ui/shell/Shell.kt`; `ui/hud/**`; `ui/components/**` | VISUAL · tokens/components | Yes | Recommended: map current values into semantic tokens and identify true exceptions. In particular review HUD editor surfaces, selection/anchor/locked states, alerts, keybind conflicts, dropdowns, layout spacing, and type scale; do not indiscriminately replace status colors. | `replacements/recommended/visual-system.md` | MEDIUM |
| Typography uses Poppins for glass themes, bundled Minecraft faces for Minecraft themes, and Unifont as a UI font resource | `modules/ui/src/main/resources/assets/tidal/fonts/`; `ui/themes/Impls.kt` | VISUAL · typography | Yes | No OneConfig font found. Keep current font assets pending design/licensing review; consider hierarchy, weights, and sizing as part of a Tidal style pass, not a blind font swap. | `replacements/recommended/visual-system.md` | LOW |
| UI icon family and Minecraft-styled icon overrides | `modules/ui/src/main/resources/assets/tidal/ico/`; `ui/themes/Impls.kt` | ASSET · UI icons | Yes | No OneConfig logo/icon family identified. Keep functional icon mappings; assess stroke/filled consistency only if the theme pass finds a concrete mismatch. | Existing icons; no copy | KEEP |
| Theme previews, player-head fallback, changelog image lookup, and existing brand/credit art | `modules/internal/src/main/resources/assets/tidal/images/`; `assets/tidal/brand/` | ASSET · images | Yes | Keep known Tidal/Minecraft previews and Tidal credit assets. `header.png` participates in the image pool used by changelog URL lookup; verify content when reviewing entries. No OneConfig image was identified. | Existing Tidal assets; see `resources/resource-map.md` | KEEP |
| Tidal UI click/slider/menu sounds and ambient audio | `minecraft/src/main/resources/assets/tidal/sounds/`; root `sounds/` | ASSET · audio | Yes | No OneConfig sound identity identified. Keep; any future change should be an authored sound-design decision, not a namespace migration. | Existing audio; see `resources/resource-map.md` | KEEP |
| Common menu, HUD-editor, settings, popup, search, notification, theme-picker, and search/empty/error-state presentation | `modules/internal/src/main/kotlin/org/tidal/tidal/internal/ui/**` | VISUAL · screen/component system | Yes | Recommended: use a component inventory during the theme pass to validate typography, spacing, radii, shadows, button/toggle/slider/dropdown states, search, tooltips, notifications, loading feedback where present, empty/error states, and popup layering. Existing status copy is generic/Tidal, not OneConfig-specific. Keep workflows and HUD editing behavior intact. | `replacements/recommended/visual-system.md` | MEDIUM |
| README and source metadata links use Tidal properties | `README.md`; Fabric metadata | BRANDING · URLs/credits | Yes | No OneConfig URL/documentation/developer credit found in the inspected current sources. Keep Tidal-owned links and verify them in a separate release check. | Existing docs/metadata | KEEP |
| Static source search hits for “OneConfig” are compatibility checks, accessors, or inherited identifiers | `minecraft/src/main/**`; `modules/ui/src/main/kotlin/org/polyfrost/oneconfig/api/ui/v1/ModCardType.kt` | TEXT · internal terminology | No user-facing occurrence confirmed | No replacement required for this phase. Re-scan rendered strings before release; do not use text replacement on identifiers. | None | KEEP |

## Priority Disposition

### MUST REPLACE

- No direct OneConfig-branded logo, visible title, description, documentation link, settings label, or `assets/oneconfig/` source resource was confirmed in the inspected production tree.
- Replace the sample/lorem ipsum Forge display metadata before representing that loader build as a release. This is required release cleanup, though it is stale template content rather than OneConfig branding.
- Correct the bootstrap description that inaccurately presents Tidal as a client. Coordinate the exact approved product wording across loader metadata.
- Preserve the visible historical “OneClient” attribution unless maintainers verify it is incorrect; it is not a OneConfig identity item.

### SHOULD REPLACE

- Establish a Tidal-owned semantic palette and restyle selectively where centralized cyan/blue glass values plus local HUD/status styling currently produce a generic or inherited look.
- Align the two Fabric-facing display names where both modules are visible to users.
- Review hierarchy and component detailing across settings, navigation, HUD editor, notifications, overlays, search, popups, and state feedback without changing their behavior.

### CAN REMAIN

- Tidal logo/icon files, current Tidal/Minecraft theme previews, useful Poppins/Minecraft/Unifont font resources, system icons, Minecraft icon variants, and UI/ambient sound assets.
- Generic settings names, categories, controls, config file identities, localization keys, APIs, utility code, and user workflows.

### DO NOT TOUCH

- OneConfig-derived package/service/API contracts, config serialization and persistence, event/rendering infrastructure, Minecraft/loader integration, compatibility mixins, authentication, module registration, or working HUD-editor behavior as part of a visual identity pass.
- The explicit upstream copyright/license notice in `modules/poly-compose/build.gradle.kts`; it is provenance/legal text, not product UI copy.
- Do not rename all resource namespaces or copy old assets into this planning directory.

## Settings Classification

- **Infrastructure / safe to retain:** setting annotations and keys, config serialization identifiers, search implementation, default values that encode behavior, and category/group mechanics.
- **User-facing / already Tidal:** menu and preferences copy, Tidal keybind, HUD editor options, config titles, theme labels, notification copy, and category names in the Tidal localization resources.
- **Brand-specific / already Tidal:** `Tidal`, `TidalGlass`, Tidal config filenames, logo paths, and descriptions mentioning the Tidal menu/interface.
- **Visual / review, not functional replacement:** accent default, theme colors, preview graphics, font hierarchy, component styling, and independently hardcoded visual states.
- **Rename/redesign status:** no setting was identified that must be renamed to remove OneConfig identity. Do not change keys/defaults or enum semantics for differentiation.

## Risk Assessment

Metadata display text is low-risk to correct, but loader IDs, declared dependencies, resource paths, persisted config files, and compatibility identifiers can break startup or existing user data if renamed. A theme pass can affect every screen and contrast state; validate all four built-in themes and HUD-editor states before shipping it. Keep current assets and behavior available until each replacement is implemented and checked.
