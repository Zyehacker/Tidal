# Contributing to Tidal

## Reports and suggestions

Bug reports and feature suggestions may be submitted through the repository's
appropriate channels, such as its issue tracker or discussions when
available. Include the target Minecraft version, relevant logs, and concise
steps to reproduce a bug. Do not include credentials or other private data.

## Code quality and verification

Keep changes focused, follow the existing patterns, and avoid unrelated
behavior changes. Test the affected target and report what was and was not
verified. For the 26.2 client, run:

```powershell
.\gradlew.bat :fabric:26.2:compileClientJava :fabric:26.2:verifyLiquidGlassAssets --offline --no-daemon --configure-on-demand
```

For other configured targets, use their corresponding
`:fabric:26.3:compileClientJava` or
`:fabric:1.21.11:compileClientJava` task. Preserve each target's Minecraft
compatibility identifiers, metadata, entrypoints, and version-specific
separation. Do not work around unavailable dependencies by changing unrelated
build configuration.

Changes are subject to maintainer review. Include focused tests or build
results when practical, and describe any in-game checks that remain necessary.

## Attribution and authorization

Keep existing author and copyright attribution accurate. Credits do not
transfer copyright, and submitting an issue or pull request does not
automatically authorize the project to use submitted code.

External code must not be merged or represented as covered by Tidal's
proprietary source-viewing terms until the project owner has explicitly
approved the contribution's ownership and licensing terms in writing. No
automatic copyright transfer or permission is assumed. The inclusion of
contributors in [CREDITS.md](./CREDITS.md) is attribution only; see
[LICENSE](./LICENSE) for the rights and authorization qualifications.

New first-party source files that support comments should include this notice,
using the correct syntax for their language and preserving required shebangs
or file headers:

```text
/*
 * tidal — proprietary source-viewing license
 * maintained by Graphine Studios
 * see LICENSE and CREDITS.md for licensing and attribution.
 *
 * provided here for inspection only.
 * this notice does not claim ownership of other contributors' material.
 * no general permission is granted by graphine studios to reuse, copy,
 * modify, redistribute or create derivative works from this file.
 */
```

Do not add comments to JSON or other commentless formats. Do not add notices
to generated files, build outputs, dependencies, third-party code, or binary
assets. Preserve existing third-party copyright and license notices.
