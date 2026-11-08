# Tidal Identity Replacement Workspace

This directory records the read-first identity audit and organizes future replacement work. It is not a production resource root. Nothing in this phase changes runtime code, metadata, assets, namespaces, or behavior.

## Contents

- `AUDIT.md` — evidence-based inventory, priorities, disposition, and settings review.
- `resources/resource-map.md` — source-owned asset inventory and usage classifications.
- `resources/namespace-migration.md` — minimum safe resource namespace policy.
- `replacements/required/metadata.md` — loader metadata that must be corrected before presenting a consistent release identity.
- `replacements/recommended/visual-system.md` — design brief for making the already Tidal-branded interface more unmistakably distinct.

## Replacement policy

Keep OneConfig-derived architecture, APIs, rendering, settings, HUD editing, and Minecraft integration unless a separately scoped compatibility change is approved. Do not overwrite or delete current resources during implementation. Existing Tidal assets should be referenced from their production locations; this audit found no OneConfig-branded artwork that needs to be copied here. A genuinely new design asset should be recorded as `MISSING — DESIGN REQUIRED` until designed, not replaced with a placeholder.

The `resources/` and `replacements/` folders contain documentation only at this stage. Production files remain unchanged.
