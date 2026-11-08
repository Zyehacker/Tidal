# Resource Namespace Migration

## Finding

The reviewed production resources already live under the Tidal resource namespace, primarily `assets/tidal/`. No source `assets/oneconfig/` tree was found. Resource references in the UI use Tidal paths such as `/assets/tidal/ico/...`, `assets/tidal/brand/...`, and `assets/tidal/fonts/...`.

**Minimum safe migration for this audit: none.** Do not rename all resources, packages, service interfaces, mod IDs, or persisted configuration paths to make the project appear distinct.

## Classification

| Namespace or identifier | Class | Action |
|---|---|---|
| `assets/tidal/**` logos, theme previews, UI icons, fonts, UI color textures, language files, and sounds | Tidal-owned branding and UI resources, plus functional assets | Keep. New Tidal artwork should use the established namespace and a purpose-appropriate subdirectory. |
| `assets/tidal/ico/minecraft/**` and Minecraft font/theme resources | Intentional Minecraft presentation variant | Keep. It is a theme choice, not an inherited OneConfig brand namespace. |
| `assets/tidal/lang/**`, `assets/tidal/sounds.json`, and sound event files | User-facing language/audio registration and content | Keep IDs and paths; they are already Tidal-scoped and changing them can affect language/audio lookup. |
| `org.polyfrost.oneconfig.api.*` package names and `META-INF/services/org.polyfrost.oneconfig.*` filenames | Inherited API/service type names that the current code and Java service loader depend on | Do not rename in an identity pass. This is a binary/runtime contract, not an asset namespace. |
| Mixin accessors, external compatibility methods, and internal classes containing “OneConfig” | Integration and source identifiers | Keep unless an independent, compatibility-tested refactor is approved. Search rendered labels separately; class/method names alone are not user-visible branding. |
| `tidal.json`, `themes.json`, theme IDs/names, loader mod IDs, and config/resource lookup strings | Persisted/runtime identifiers | Keep. Renaming can orphan user preferences, break external API lookups, or affect module loading. |
| `minecraft/run/**`, generated resources, downloaded files, crash/replay logs | Runtime/generated/user data, not source-owned Tidal branding | Excluded from asset replacement. Never use generated OneConfig names here as a reason for broad production rewrites. |

## Safe Future Rule

1. Add new logos, UI iconography, backgrounds, theme previews, and style resources under `assets/tidal/` with descriptive paths.
2. If an actual OneConfig-branded source asset is discovered later, replace only that asset and its explicit references. Keep the old file available until the new path is verified on every supported loader/version.
3. Migrate a legacy resource path only when the resource is proven user-facing and a compatibility plan covers packaged lookups and persisted references. Do not infer resource paths from Java package names.
4. Keep OneConfig-derived code/API/service naming out of visual asset migration; any eventual API/package separation must be a distinct source and binary compatibility project.
