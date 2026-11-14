# Third-party notices

Tidal 26.2 embeds the following Java/browser components. Their pinned versions
are in `fabric/26.2/build.gradle`; the frontend dependency graph is pinned by
`cef/package-lock.json`. Preserve the licenses and notices shipped by the
corresponding nested JARs and by the platform runtime downloaded to
`config/tidal/cef/runtime/`.

| Component | Version | License / notice |
| --- | --- | --- |
| jcefmaven | 152.0.6 | Apache License 2.0 — https://github.com/jcefmaven/jcefmaven/blob/master/LICENSE |
| JCEF API | `jcef-17e805a+cef-152.0.6+g708dc14+chromium-152.0.7977.83` | BSD; the nested API JAR contains `LICENSE.txt`. Chromium / CEF notices come from the platform runtime. |
| JOGL | 2.4.0 | Upstream Maven metadata declares BSD-2-Clause, BSD-3-Clause, SGI Free Software License B, Apache 2.0, Apache 1.1, and Ubuntu Font License. See https://jogamp.org/ and the JOGL distribution notices. |
| GlueGen | 2.4.0 | Upstream Maven metadata declares BSD-2-Clause, BSD-3-Clause, and BSD-4-Clause. See https://jogamp.org/ and the GlueGen distribution notices. |
| Apache Commons Compress | 1.28.0 | Apache License 2.0 — https://commons.apache.org/proper/commons-compress/ |
| Gson | 2.11.0 | Apache License 2.0 — https://github.com/google/gson |
| React / React DOM | 19.3.1 | MIT — https://github.com/facebook/react |
| Lucide icons | inline SVG paths | ISC — https://github.com/lucide-icons/lucide/blob/main/LICENSE |
| Vite | 8.3.8 | MIT — https://github.com/vitejs/vite |
| `@vitejs/plugin-react` | 6.3.0 | MIT — https://github.com/vitejs/vite-plugin-react |
| TypeScript | 5.9.3 | Apache License 2.0 — https://github.com/microsoft/TypeScript |

The JCEF Maven project documents these macOS JVM options for OSR:

```text
--add-opens=java.desktop/sun.awt=ALL-UNNAMED
--add-opens=java.desktop/sun.lwawt=ALL-UNNAMED
--add-opens=java.desktop/sun.lwawt.macosx=ALL-UNNAMED
```

JCEF Maven runtime and platform compatibility information:
https://github.com/jcefmaven/jcefmaven
