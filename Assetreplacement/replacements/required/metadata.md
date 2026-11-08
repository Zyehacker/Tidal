# Required Metadata Cleanup Specification

This is a replacement specification only. Do not edit loader metadata during the audit phase. Final wording and loader-specific fields must be reviewed by the project maintainers before implementation.

## Forge display metadata

- **Current:** Example `mods.toml` content, including a long Lorem Ipsum description, sample comments/URLs, and dependency examples; `displayName` is supplied from a build variable.
- **Location:** `minecraft/src/main/resources/META-INF/mods.toml`.
- **Purpose:** Forge mod-list identity and description.
- **Why it creates OneConfig identity:** It does not directly mention OneConfig, but visibly exposes upstream/template filler instead of a coherent Tidal identity and makes the product look unfinished.
- **Replacement needed:** Replace user-facing placeholder description and set approved Tidal name/description/contact/logo values for the actual Forge distribution. Reuse approved project wording from maintained Tidal metadata where semantically correct; do not invent product claims. Keep the existing license and verify the selected Forge schema.
- **Suggested Tidal location:** This production metadata file remains the eventual target; keep this specification under `Assetreplacement/replacements/required/` until a separate implementation task.
- **Dependencies:** Confirm the Forge build's metadata expansion (`mod_name`, version), supported loader/Minecraft ranges, issue URL, and whether a logo is actually packaged. Preserve mod IDs and dependency semantics until validated against the generated artifact.
- **Risk:** Medium to high if mod IDs, dependency table names, loader ranges, or generated values are changed along with copy. Metadata display-only changes are low risk; runtime dependency edits are out of scope. Verify the built Forge artifact in its mod list and startup logs.

## Bootstrap identity wording

- **Current:** `name` is `Tidal`; `description` is `Tidal Client & Configuration Engine`.
- **Location:** `bootstrap/src/main/resources/fabric.mod.json`.
- **Purpose:** Loader-visible bootstrap identity.
- **Why it creates OneConfig identity:** It does not say OneConfig, but “Client” risks presenting Tidal as a bundled gameplay client rather than the standalone mod/configuration library described by the repository. The naming is inconsistent with the main module's display name.
- **Replacement needed:** Use approved, accurate Tidal bootstrap wording and coordinate it with the main module metadata. Keep `id`, `provides`, dependencies, icon path, license, and entrypoints unchanged.
- **Suggested Tidal location:** `bootstrap/src/main/resources/fabric.mod.json` in a later metadata-only implementation.
- **Dependencies:** Confirm the intended bootstrap role and whether Mod Menu exposes this entry to end users. Source text must agree with README/project documentation.
- **Risk:** Low if only the description changes. Renaming IDs or `provides` can affect dependency resolution and is not part of this specification.

## Main Fabric module display name

- **Current:** `name` is `Tidal (Platform Module)` and the description says users can access mod settings in one place.
- **Location:** `minecraft/src/main/resources/fabric.mod.json`.
- **Purpose:** Loader/Mod Menu-visible name and description for the core platform module.
- **Why it creates OneConfig identity:** It does not expose OneConfig, but the implementation detail “Platform Module” and bootstrap name mismatch are not a clear, unified product presentation.
- **Replacement needed:** Decide whether users should see one product name or intentionally distinguish the bootstrap and runtime library. Apply consistent approved Tidal display text while leaving internal `id`, Mod Menu parent, entrypoints, mixins, and dependencies unchanged.
- **Suggested Tidal location:** `minecraft/src/main/resources/fabric.mod.json` in the future implementation phase.
- **Dependencies:** Check both entries in Mod Menu and identify which module users install/see for every supported packaging variant. Reconcile with bootstrap metadata and any Forge/legacy metadata.
- **Risk:** Low for display labels; potentially high if IDs, parent relationships, `provides`, or dependency versions are altered.

## Implementation boundary

This specification covers user-facing metadata strings only. The `mods.toml` dependency identifiers and all Fabric mod IDs/relationships may contain legacy naming or look like template code; treat them as loader contracts, not branding, and investigate separately before changing them. No production metadata has been edited or copied into this directory.
