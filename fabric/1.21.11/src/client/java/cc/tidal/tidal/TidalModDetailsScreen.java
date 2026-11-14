/*
 * tidal — proprietary source-viewing license
 * maintained by graphine studios
 * see LICENSE and CREDITS.md for licensing and attribution.
 *
 * provided here for inspection only.
 * this notice does not claim ownership of other contributors' material.
 * no general permission is granted by graphine studios to reuse, copy,
 * modify, redistribute or create derivative works from this file.
 */

package cc.tidal.tidal;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class TidalModDetailsScreen extends Screen {
    private final Screen parent;
    private final TidalModManager.ModEntry entry;
    private final ModMetadata metadata;
    private final List<String> details;
    private final String homepage;
    private final String issues;

    TidalModDetailsScreen(Screen parent, TidalModManager.ModEntry entry) {
        super(Component.literal(entry.name));
        this.parent = parent;
        this.entry = entry;
        metadata = entry.container.getMetadata();
        homepage = webLink("homepage").orElseGet(() -> webLink("sources").orElse(null));
        issues = webLink("issues").orElse(null);
        details = buildDetails();
    }

    private java.util.Optional<String> webLink(String key) {
        return metadata.getContact().get(key).filter(value -> {
            try {
                URI uri = URI.create(value);
                return "https".equalsIgnoreCase(uri.getScheme())
                        || "http".equalsIgnoreCase(uri.getScheme());
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        });
    }

    private List<String> buildDetails() {
        List<String> result = new ArrayList<>();
        result.add("Mod ID: " + metadata.getId());
        result.add("Version: " + metadata.getVersion().getFriendlyString());
        result.add("Authors: " + metadata.getAuthors().stream()
                .map(person -> person.getName()).collect(Collectors.joining(", ")));
        result.add("License: " + String.join(", ", metadata.getLicense()));
        result.add("Environment: " + metadata.getEnvironment());
        result.add("Dependencies: " + metadata.getDepends().stream()
                .map(ModDependency::getModId).collect(Collectors.joining(", ")));
        String dependents = FabricLoader.getInstance().getAllMods().stream()
                .filter(container -> container.getMetadata().getDepends().stream()
                        .anyMatch(dependency -> dependency.getModId().equals(entry.id)))
                .map(container -> container.getMetadata().getName())
                .collect(Collectors.joining(", "));
        result.add("Required by: " + dependents);
        result.add("Runtime status: " + entry.status());
        return result;
    }

    @Override
    protected void init() {
        clearWidgets();
        int x = 24;
        int gap = 6;
        int buttonWidth = Math.max(70, (width - 48 - gap * 2) / 3);
        int y = height - 76;
        addRenderableWidget(new TidalUi.ActionButton(x, y, buttonWidth, 28,
                "Copy ID", TidalUi.ActionButton.Style.CONTROL,
                () -> copy(metadata.getId())));
        addRenderableWidget(new TidalUi.ActionButton(x + buttonWidth + gap, y,
                buttonWidth, 28, "Copy version", TidalUi.ActionButton.Style.CONTROL,
                () -> copy(metadata.getVersion().getFriendlyString())));
        addRenderableWidget(new TidalUi.ActionButton(x + (buttonWidth + gap) * 2, y,
                buttonWidth, 28, "Copy info", TidalUi.ActionButton.Style.CONTROL,
                () -> copy(exportInfo())));

        y += 34;
        int actionWidth = Math.max(70, (width - 48 - gap * 2) / 3);
        addRenderableWidget(new TidalUi.ActionButton(x, y, actionWidth, 28,
                entry.favorite ? "Remove favorite" : "Add favorite",
                TidalUi.ActionButton.Style.CONTROL, () -> {
            TidalModManager.setFavorite(entry, !entry.favorite);
            Minecraft.getInstance().setScreen(parent);
        }));
        if (entry.canOpenSettings()) {
            addRenderableWidget(new TidalUi.ActionButton(x + actionWidth + gap, y,
                    actionWidth, 28,
                    "Configure", TidalUi.ActionButton.Style.PRIMARY,
                    () -> TidalModManager.openSettings(entry, this)));
        }
        if (homepage != null) {
            addRenderableWidget(new TidalUi.ActionButton(x + (actionWidth + gap) * 2, y,
                    actionWidth, 28, "Project page", TidalUi.ActionButton.Style.CONTROL,
                    () -> Util.getPlatform().openUri(URI.create(homepage))));
        }
        addRenderableWidget(new TidalUi.ActionButton(24, 16, 160, 26,
                "Open mods folder", TidalUi.ActionButton.Style.CONTROL,
                () -> Util.getPlatform().openFile(
                        FabricLoader.getInstance().getGameDir().resolve("mods").toFile())));
        addRenderableWidget(new TidalUi.ActionButton(width - 112, 16, 88, 26,
                "Back", TidalUi.ActionButton.Style.QUIET,
                () -> Minecraft.getInstance().setScreen(parent)));
    }

    private void copy(String value) {
        Minecraft.getInstance().keyboardHandler.setClipboard(value);
    }

    private String exportInfo() {
        return details.stream().collect(Collectors.joining("\n", entry.name + "\n", ""));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
                                   float delta) {
        TidalUi.panel(graphics, 12, 12, width - 24, height - 24,
                TidalTheme.SURFACE, TidalTheme.Radius.WINDOW);
        TidalUi.strongLabel(graphics, font, entry.name, 24, 18, TidalUi.PRIMARY);
        TidalUi.label(graphics, font, entry.version + " · " + entry.status(),
                24, 36, TidalUi.SECONDARY);
        TidalUi.label(graphics, font, TidalUi.fitText(font, entry.description,
                width - 48), 24, 56, TidalUi.SECONDARY);
        int y = 88;
        for (String line : details) {
            if (y + font.lineHeight > height - 120) break;
            TidalUi.label(graphics, font, TidalUi.fitText(font, line, width - 48),
                    24, y, TidalUi.SECONDARY);
            y += font.lineHeight + 5;
        }
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
