# Source Resource Map

This inventory covers source-owned resources in the audited modules, bootstrap, and Minecraft integration. Generated/downloaded files under `minecraft/run/`, build outputs, and runtime user data are not production replacement assets.

## Branding and theme imagery

| Current resource | Use found | Identity assessment | Disposition |
|---|---|---|---|
| `bootstrap/src/main/resources/tidal-icon.png` | Bootstrap mod-list icon, declared by bootstrap Fabric metadata | Tidal-branded | Keep; no OneConfig replacement needed. |
| `modules/internal/src/main/resources/assets/tidal/brand/tidal.svg`, `tidal-light.svg`, `tidal-mc.svg`, `tidal-mc-light.svg` | Theme-specific logo paths referenced by the built-in themes | Tidal marks, with variants for the glass and Minecraft themes | Keep in place. Redesign only if a later approved Tidal art direction requires it. |
| `assets/tidal/brand/tidal-icon.svg`, `tidal.png`, `tidal-mc.svg`, `by-tidal.svg`, `atmofrost.svg` | Tidal icon/default HUD icon and credit/brand presentation | Tidal or credited third-party identity, not OneConfig | Keep; do not duplicate into this folder. Respect the existing credit attribution. |
| `assets/tidal/images/polyglass-dark.svg`, `polyglass-light.svg`, `minecraft-dark.svg`, `minecraft-light.svg` | Theme-picker previews selected through `UITheme.previewImage` | Tidal's current selectable theme presentation | Keep as current previews. A future theme redesign should make and reference new previews; do not overwrite these during the audit. |
| `assets/tidal/images/head.png` | Fallback image in `PlayerHead.kt` | Generic player imagery | Keep as functional fallback. |
| `assets/tidal/images/header.png` | Part of the image pool used by changelog UI's URL-derived image lookup (`images/$url.png`) | Changelog/media, not a confirmed OneConfig mark | Keep pending content-by-entry review; treat runtime-selected images as dynamic. |

## UI Icons and Color Inputs

- `modules/ui/src/main/resources/assets/tidal/ico/*.svg` is the general functional icon collection used for controls/navigation/settings. It is not a OneConfig logo set.
- `modules/ui/src/main/resources/assets/tidal/ico/minecraft/*.svg` contains Minecraft-styled alternatives selected by the Minecraft themes through `MinecraftIconOverrides` in `Impls.kt`. Preserve these as theme-specific infrastructure.
- `modules/ui/src/main/resources/assets/tidal/color/alpha.png` and `hue.png` are color-picker input textures. They are functional controls, not identity art.
- No logo or icon located in these source-owned asset trees was identified as OneConfig-branded. There is no reason to copy them to `Assetreplacement/`.

## Fonts

- `modules/ui/src/main/resources/assets/tidal/fonts/Poppins/` supplies the Poppins weight/italic TTF files used by the two glass themes. It is user-visible typography but is not identified as OneConfig branding.
- `modules/ui/src/main/resources/assets/tidal/fonts/minecraft/` supplies Minecraft-Regular and Minecraft-Bold OTF faces to the Minecraft themes. This is intentional themed presentation.
- `modules/ui/src/main/resources/assets/tidal/fonts/unifont/unifont.otf` is a font resource. It is not named as the main theme typeface in `Impls.kt`; preserve pending a usage/licensing check rather than removing it based on identity alone.
- No WOFF/WOFF2 font was found in the reviewed source resource roots. Do not introduce a replacement font until the Tidal typography brief and font licensing are approved.

## Audio

- `minecraft/src/main/resources/assets/tidal/sounds/ui/` contains `click.ogg`, `close.ogg`, `open.ogg`, and `slider.ogg`, referenced through Tidal sound definitions/services.
- Root `sounds/ambient/` contains `lullaby.ogg`, `moog_1.ogg`, `moog_2.ogg`, and `trees.ogg`; root `sounds/ui/` contains `pad_loop.ogg`. These are audio source assets, not OneConfig marks.
- Keep sound event/resource IDs and behavior. Any future replacement should be original Tidal sound design, not a namespace or architecture change.

## JSON and Other Resource Formats

- `assets/tidal/lang/en_us.json` and `assets/tidal/en_default.lang` are localized user-facing settings/UI text. Current product references are Tidal-specific.
- Fabric metadata JSON files describe Tidal, its bootstrap, the Compose dependency bundle, and the legacy dependency shim. See `replacements/required/metadata.md` for display-facing cleanup; internal mod IDs/dependencies are not branding assets.
- `assets/tidal/sounds.json` and `sounds/external_sounds.json` are sound registration data. Preserve resource identifiers unless a tested migration is separately required.
- `META-INF/services/*` files are Java service-provider declarations, many using inherited `org.polyfrost.oneconfig` API names. They are runtime wiring and must not be renamed as branding cleanup.
- `minecraft/src/main/resources/META-INF/mods.toml` is loader metadata, not a visual asset; it currently contains template prose and is called out separately.
- No `.webp`, `.jpg`, or `.mcmeta` replacement asset was identified in the reviewed source-owned resource paths. This does not describe user/downloaded Minecraft resources or generated build/run content.

## Replacement Decision

- **Existing replacement assets:** Tidal logos/icons and Tidal theme resources already exist in production paths; reference them there.
- **OneConfig-branded source assets to replace:** none identified.
- **New design assets required by this audit:** none are mandatory. If a theme redesign later calls for a distinct mark or artwork, record it as `MISSING — DESIGN REQUIRED` with a brief before commissioning it; do not make a placeholder.
- **Copy policy:** no asset was copied into this directory because no OneConfig-specific production asset was found that needs replacement.
