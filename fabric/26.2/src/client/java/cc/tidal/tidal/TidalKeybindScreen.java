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

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class TidalKeybindScreen extends Screen {
    private static final int TOP = 76;
    private static final int ROW_HEIGHT = 30;
    private final Screen parent;
    private final List<TidalModule> modules = new ArrayList<>();
    private final List<TidalUi.ActionButton> rows = new ArrayList<>();
    private TidalUi.SearchField search;
    private TidalModule binding;
    private int scroll;

    TidalKeybindScreen(Screen parent) {
        super(Component.literal("Tidal keybinds"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        modules.clear();
        rows.clear();
        search = addRenderableWidget(new TidalUi.SearchField(font, 24, 28,
                Math.max(100, width - 48), 28, Component.literal("Search Tidal keybinds")));
        search.setHint(Component.literal("Search by module or group..."));
        search.setResponder(value -> {
            scroll = 0;
            layoutRows();
        });
        addRenderableWidget(new TidalUi.ActionButton(24, height - 38, 176, 26,
                "Restore module defaults", TidalUi.ActionButton.Style.CONTROL, () -> {
            TidalKeybinds.resetDefaults();
            layoutRows();
        }));
        addRenderableWidget(new TidalUi.ActionButton(width - 112, height - 38, 88, 26,
                "Done", TidalUi.ActionButton.Style.PRIMARY,
                () -> minecraft.gui.setScreen(parent)));
        TidalModules.all().stream().filter(module -> module.implemented)
                .sorted(Comparator.comparing((TidalModule module) -> module.group.name())
                        .thenComparing(module -> module.name, String.CASE_INSENSITIVE_ORDER))
                .forEach(module -> {
                    modules.add(module);
                    rows.add(addRenderableWidget(new TidalUi.ActionButton(24, TOP, width - 48,
                            ROW_HEIGHT - 2, bindingLabel(module),
                            TidalUi.ActionButton.Style.CONTROL, () -> {
                        binding = module;
                        layoutRows();
                    })));
                });
        layoutRows();
    }

    private void layoutRows() {
        if (search == null) return;
        String query = search.getValue().trim().toLowerCase(Locale.ROOT);
        int index = 0;
        int y = TOP - scroll;
        int bottom = height - 48;
        for (int i = 0; i < modules.size(); i++) {
            TidalModule module = modules.get(i);
            TidalUi.ActionButton row = rows.get(i);
            boolean matches = query.isEmpty()
                    || (module.name + " " + module.group.name() + " " + module.id)
                    .toLowerCase(Locale.ROOT).contains(query);
            if (!matches) {
                row.visible = false;
                continue;
            }
            row.setMessage(Component.literal(binding == module
                    ? TidalUi.fitText(font, module.group.name() + " · " + module.name
                            + "    Press a key (Esc to unbind)", width - 58)
                    : bindingLabel(module)));
            row.setPosition(24, y);
            row.visible = y >= TOP && y + ROW_HEIGHT <= bottom;
            y += ROW_HEIGHT;
            index++;
        }
        int available = Math.max(0, bottom - TOP);
        int contentHeight = index * ROW_HEIGHT;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentHeight - available)));
        if (scroll != 0) {
            y = TOP - scroll;
            for (int i = 0; i < modules.size(); i++) {
                TidalModule module = modules.get(i);
                TidalUi.ActionButton row = rows.get(i);
                boolean matches = query.isEmpty()
                        || (module.name + " " + module.group.name() + " " + module.id)
                        .toLowerCase(Locale.ROOT).contains(query);
                if (!matches) continue;
                row.setPosition(24, y);
                row.visible = y >= TOP && y + ROW_HEIGHT <= bottom;
                y += ROW_HEIGHT;
            }
        }
    }

    private String bindingLabel(TidalModule module) {
        String conflict = TidalKeybinds.conflict(module);
        return TidalUi.fitText(font,
                TidalUi.displayName(module.group) + " · " + module.name + "    "
                        + TidalKeybinds.displayName(module.keyCode)
                        + (conflict.isEmpty() ? "" : " · " + conflict),
                Math.max(1, width - 58));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                   float delta) {
        TidalUi.panel(graphics, 12, 12, width - 24, height - 24,
                TidalTheme.SURFACE, TidalTheme.Radius.WINDOW);
        TidalUi.strongLabel(graphics, font, "TIDAL KEYBINDS", 24, 12,
                TidalUi.PRIMARY);
        TidalUi.label(graphics, font,
                "Click a row and press a key. Press Esc to unbind.",
                24, 62, TidalUi.SECONDARY);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (binding != null) {
            TidalKeybinds.setKeyCode(binding,
                    event.key() == GLFW.GLFW_KEY_ESCAPE ? 0 : event.key());
            binding = null;
            layoutRows();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            minecraft.gui.setScreen(parent);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (binding != null) {
            TidalKeybinds.setKeyCode(binding, -(event.button() + 1));
            binding = null;
            layoutRows();
            return true;
        }
        if (event.button() == 1) {
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).visible && rows.get(i).isMouseOver(event.x(), event.y())) {
                    TidalKeybinds.setKeyCode(modules.get(i), 0);
                    TidalSounds.playClick("TidalKeybindScreen.clear");
                    layoutRows();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
                                 double verticalAmount) {
        if (mouseY >= TOP && mouseY < height - 48) {
            scroll = Math.max(0, scroll + (verticalAmount < 0 ? ROW_HEIGHT : -ROW_HEIGHT));
            layoutRows();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
