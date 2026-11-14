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

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TidalHudEditorScreen extends Screen {
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 520;
    private static final int CONTENT_WIDTH = 296;
    private static final int CONTROL_HEIGHT = 24;
    private record PanelControl(AbstractWidget widget, int x, int y) {}
    private record ElementState(int x, int y, float scale, TidalModule.Anchor anchor,
                                boolean normalizedPosition, float normalizedX, float normalizedY,
                                int surfaceGroupId, int backgroundColor, int backgroundOpacity,
                                int accentColor, TidalModule.HudStyle hudStyle) {}
    private record Edit(Map<TidalModule, ElementState> before,
                        Map<TidalModule, ElementState> after) {}
    private final Screen parent;
    private final List<PanelControl> panelControls = new ArrayList<>();
    private final Deque<Edit> undoHistory = new ArrayDeque<>();
    private final Deque<Edit> redoHistory = new ArrayDeque<>();
    private final Set<TidalModule> selection = new LinkedHashSet<>();
    private TidalModule selected;
    private TidalUi.ActionButton moduleButton;
    private TidalUi.ValueSlider scaleSlider;
    private TidalUi.CheckOption snapButton;
    private TidalUi.ActionButton visibilityButton;
    private TidalUi.ActionButton anchorButton;
    private TidalUi.ActionButton lockButton;
    private TidalUi.CheckOption backgroundToggle;
    private TidalUi.ActionButton layoutButton;
    private TidalUi.ActionButton saveLayoutButton;
    private TidalUi.ActionButton renameLayoutButton;
    private TidalUi.ActionButton deleteLayoutButton;
    private TidalUi.ActionButton doneButton;
    private TidalUi.ActionButton collapseButton;
    private TidalUi.ActionButton undoButton;
    private TidalUi.ActionButton redoButton;
    private TidalUi.ChoiceStrip styleControl;
    private TidalUi.ValueSlider opacitySlider;
    private TidalUi.SearchField layoutName;
    private boolean dragging;
    private boolean resizing;
    private boolean panelDragging;
    private boolean panelCollapsed;
    private boolean opacityDragging;
    private boolean scaleDragging;
    private ElementState scaleGestureStart;
    private boolean panelPositionInitialized;
    private boolean compactLayout;
    private int panelLayoutWidth = PANEL_WIDTH;
    private int contentWidth = CONTENT_WIDTH;
    private int contentX = (PANEL_WIDTH - CONTENT_WIDTH) / 2;
    private int panelX;
    private int panelY;
    private int panelScrollOffset;
    private double panelDragOffsetX;
    private double panelDragOffsetY;
    private double dragOffsetX;
    private double dragOffsetY;
    private double resizeStartX;
    private double resizeStartY;
    private float resizeStartScale;
    private int guideX = -1;
    private int guideY = -1;
    private int snapX;
    private int snapY;
    private int bestSnapX;
    private int bestSnapY;
    private ElementState gestureStart;
    private Map<TidalModule, ElementState> groupGestureStart;
    private String gapLabel = "";
    private int gapLabelX;
    private int gapLabelY;

    public TidalHudEditorScreen(Screen parent) {
        super(Component.literal("Tidal HUD Editor"));
        this.parent = parent;
        selected = TidalModules.all().stream()
                .filter(module -> module.implemented && module.group == TidalModule.Group.HUD)
                .filter(module -> module.enabled)
                .findFirst()
                .orElseGet(() -> TidalModules.byId("fps"));
        selection.add(selected);
    }

    @Override
    protected void init() {
        clearWidgets();
        panelControls.clear();
        if (!panelPositionInitialized) {
            panelX = width - panelWidth() - 18;
            panelY = 18;
            panelPositionInitialized = true;
        }
        clampPanelPosition();
        panelLayoutWidth = panelWidth();
        contentWidth = Math.max(1, panelLayoutWidth - 24);
        contentX = (panelLayoutWidth - contentWidth) / 2;
        compactLayout = contentWidth < 244;
        int layoutShift = compactLayout ? 30 : 0;
        int halfWidth = Math.max(1, (contentWidth - 8) / 2);
        collapseButton = addRenderableWidget(new TidalUi.ActionButton(
                panelX + panelLayoutWidth - 34,
                panelY + 8, 24, 24, panelCollapsed ? "Expand" : "Collapse",
                TidalUi.ActionButton.Style.ICON, () -> {
                    panelCollapsed = !panelCollapsed;
                    if (panelCollapsed) panelScrollOffset = 0;
                    layoutPanelControls();
                }));
        undoButton = addRenderableWidget(new TidalUi.ActionButton(
                panelX + panelLayoutWidth / 2 - 80,
                panelY + 6, 76, 26, "Undo", TidalUi.ActionButton.Style.QUIET,
                this::undo));
        redoButton = addRenderableWidget(new TidalUi.ActionButton(
                panelX + panelLayoutWidth / 2 + 4,
                panelY + 6, 76, 26, "Redo", TidalUi.ActionButton.Style.QUIET,
                this::redo));
        moduleButton = addControl(contentX, 58, contentWidth, "Selected",
                TidalUi.ActionButton.Style.CENTERED, this::selectNext);

        lockButton = addControl(contentX, 88, compactLayout ? contentWidth : halfWidth, "Lock",
                TidalUi.ActionButton.Style.CENTERED, () -> {
                    selected.locked = !selected.locked;
                    updateLabels();
                    TidalConfig.save();
                });
        visibilityButton = addControl(compactLayout ? contentX : contentX + halfWidth + 8,
                compactLayout ? 118 : 88, compactLayout ? contentWidth : halfWidth, "Visibility",
                TidalUi.ActionButton.Style.CENTERED, () -> {
                    selected.enabled = !selected.enabled;
                    if (selected.enabled) TidalHud.placeInAvailableSpace(selected);
                    updateLabels();
                    TidalConfig.save();
                });
        addControl(contentX, 118 + layoutShift, contentWidth, "Reset element",
                TidalUi.ActionButton.Style.CENTERED, this::resetSelected);
        anchorButton = addControl(contentX, 148 + layoutShift, contentWidth, "Anchor",
                TidalUi.ActionButton.Style.CENTERED, this::nextAnchor);
        scaleSlider = addRenderableWidget(new TidalUi.ValueSlider(
                panelX + contentX, panelY + 190 + layoutShift,
                contentWidth, CONTROL_HEIGHT, "Scale",
                () -> Math.round(selected.scale * 100), value -> {
                    selected.scale = value / 100.0f;
                    clampSelection();
                    updateLabels();
                }, 50, 200));
        panelControls.add(new PanelControl(scaleSlider, contentX, 190 + layoutShift));
        snapButton = addCheckControl(contentX, 220 + layoutShift, contentWidth,
                "Grid  On", "select", TidalConfig.snapToGrid, () -> {
                    TidalConfig.snapToGrid = !TidalConfig.snapToGrid;
                    updateLabels();
                    TidalConfig.save();
                });
        backgroundToggle = addCheckControl(contentX, 264 + layoutShift, contentWidth,
                "Background", "visibility", selected.backgroundOpacity > 0, () -> {
                    selected.backgroundOpacity = selected.backgroundOpacity > 0 ? 0 : 86;
                    TidalConfig.save();
                });
        styleControl = addRenderableWidget(new TidalUi.ChoiceStrip(panelX + contentX,
                panelY + 294 + layoutShift, contentWidth, 44, "Corner style",
                List.of("Square", "Rounded"),
                () -> selected.hudStyle == TidalModule.HudStyle.ROUNDED ? 1 : 0,
                index -> {
                    selected.hudStyle = index == 1
                            ? TidalModule.HudStyle.ROUNDED : TidalModule.HudStyle.SQUARE;
                    propagateSurfaceAppearance();
                    TidalConfig.save();
                }));
        panelControls.add(new PanelControl(styleControl, contentX, 294 + layoutShift));
        opacitySlider = addRenderableWidget(new TidalUi.ValueSlider(panelX + contentX,
                panelY + 342 + layoutShift, contentWidth, 32, "Background opacity",
                () -> selected.backgroundOpacity,
                value -> {
                    selected.backgroundOpacity = value;
                    propagateSurfaceAppearance();
                }, 0, 100));
        panelControls.add(new PanelControl(opacitySlider, contentX, 342 + layoutShift));
        layoutName = addRenderableWidget(new TidalUi.SearchField(font, panelX + contentX,
                panelY + 390 + layoutShift, contentWidth, CONTROL_HEIGHT,
                Component.literal("Layout name")));
        panelControls.add(new PanelControl(layoutName, contentX, 390 + layoutShift));
        layoutName.setHint(Component.literal("Name this HUD layout"));
        layoutName.setMaxLength(32);
        layoutName.setResponder(value -> layoutName.setError(false));
        int layoutControlWidth = Math.max(1, (contentWidth - 8) / 2);
        layoutButton = addControl(contentX, 418 + layoutShift, layoutControlWidth, "Load",
                TidalUi.ActionButton.Style.CENTERED, this::cycleLayout);
        saveLayoutButton = addControl(contentX + layoutControlWidth + 8, 418 + layoutShift,
                layoutControlWidth, "Save", TidalUi.ActionButton.Style.PRIMARY,
                this::saveLayout);
        renameLayoutButton = addControl(contentX, 450 + layoutShift,
                layoutControlWidth, "Rename", TidalUi.ActionButton.Style.CENTERED,
                this::renameLayout);
        deleteLayoutButton = addControl(contentX + layoutControlWidth + 8, 450 + layoutShift,
                layoutControlWidth, "Delete", TidalUi.ActionButton.Style.DESTRUCTIVE,
                this::deleteLayout);
        doneButton = addRenderableWidget(new TidalUi.ActionButton(panelX + contentX,
                panelY + PANEL_HEIGHT - CONTROL_HEIGHT - 8, contentWidth, CONTROL_HEIGHT,
                "Done", TidalUi.ActionButton.Style.CENTERED, this::onClose));
        layoutPanelControls();
        updateLabels();
    }

    private TidalUi.ActionButton addControl(int x, int y, int buttonWidth, String label,
                                            TidalUi.ActionButton.Style style, Runnable action) {
        TidalUi.ActionButton button = new TidalUi.ActionButton(panelX + x, panelY + y, buttonWidth,
                CONTROL_HEIGHT, label, style, action);
        panelControls.add(new PanelControl(button, x, y));
        return addRenderableWidget(button);
    }

    private TidalUi.CheckOption addCheckControl(int x, int y, int controlWidth, String label,
                                                String icon, boolean checked, Runnable action) {
        TidalUi.CheckOption control = new TidalUi.CheckOption(panelX + x, panelY + y,
                controlWidth, CONTROL_HEIGHT, label, icon, checked, action);
        panelControls.add(new PanelControl(control, x, y));
        return addRenderableWidget(control);
    }

    private void layoutPanelControls() {
        int panelHeight = Math.min(PANEL_HEIGHT, Math.max(1, height - 16));
        int bodyTop = 48;
        int bodyBottom = panelHeight - CONTROL_HEIGHT - 22;
        int maxScroll = Math.max(0, 474 + (compactLayout ? 30 : 0) - bodyBottom);
        panelScrollOffset = clamp(panelScrollOffset, 0, maxScroll);
        for (PanelControl control : panelControls) {
            int y = panelY + control.y - panelScrollOffset;
            control.widget.setPosition(panelX + control.x, y);
            control.widget.visible = !panelCollapsed
                    && y >= panelY + bodyTop
                    && y + control.widget.getHeight() <= panelY + bodyBottom;
        }
        panelLayoutWidth = panelWidth();
        collapseButton.setPosition(panelX + panelLayoutWidth - 34, panelY + 8);
        collapseButton.setMessage(Component.literal(panelCollapsed ? "Expand" : "Collapse"));
        collapseButton.highlighted = panelCollapsed;
        collapseButton.visible = true;
        undoButton.setPosition(panelX + panelLayoutWidth / 2 - 80, panelY + 6);
        redoButton.setPosition(panelX + panelLayoutWidth / 2 + 4, panelY + 6);
        undoButton.visible = !panelCollapsed;
        redoButton.visible = !panelCollapsed;
        doneButton.setPosition(panelX + contentX,
                panelY + panelHeight - CONTROL_HEIGHT - 8);
        doneButton.setWidth(contentWidth);
        doneButton.visible = !panelCollapsed;
        undoButton.active = !undoHistory.isEmpty();
        redoButton.active = !redoHistory.isEmpty();
    }

    private void clampPanelPosition() {
        int renderedWidth = panelWidth();
        int renderedHeight = Math.min(PANEL_HEIGHT, Math.max(1, height - 16));
        panelX = clamp(panelX, 8, Math.max(8, width - renderedWidth - 8));
        panelY = clamp(panelY, 8, Math.max(8, height - renderedHeight - 8));
    }

    private int panelWidth() {
        return Math.min(PANEL_WIDTH, Math.max(1, width - 16));
    }

    private int layoutShift() {
        return compactLayout ? 30 : 0;
    }

    private void resetSelected() {
        ElementState before = captureState(selected);
        selected.anchor = TidalModule.Anchor.TOP_LEFT;
        selected.x = 12;
        selected.y = 12;
        selected.normalizedPosition = false;
        selected.scale = 1.0f;
        selected.locked = false;
        clampSelection();
        recordEdit(selected, before);
        updateLabels();
        TidalConfig.save();
    }

    private void cycleLayout() {
        var names = TidalConfig.hudLayoutNames();
        int index = names.indexOf(TidalConfig.activeHudLayout);
        String next = names.get((index + 1 + names.size()) % names.size());
        TidalConfig.loadHudLayout(next);
        clampSelection();
        updateLabels();
    }

    private void saveLayout() {
        String name = layoutName.getValue().trim();
        if (name.isEmpty()) name = TidalConfig.activeHudLayout;
        if (TidalConfig.saveHudLayout(name)) {
            layoutName.setError(false);
            updateLabels();
        } else {
            layoutName.setError(true);
        }
    }

    private void renameLayout() {
        if (TidalConfig.renameHudLayout(TidalConfig.activeHudLayout, layoutName.getValue())) {
            layoutName.setError(false);
            updateLabels();
        } else {
            layoutName.setError(true);
        }
    }

    private void deleteLayout() {
        if (TidalConfig.deleteHudLayout(TidalConfig.activeHudLayout)) {
            TidalConfig.loadHudLayout(TidalConfig.activeHudLayout);
            clampSelection();
            updateLabels();
        }
    }

    private void selectNext() {
        var modules = TidalModules.all().stream()
                .filter(module -> module.implemented && module.group == TidalModule.Group.HUD)
                .toList();
        int index = modules.indexOf(selected);
        selected = modules.get((index + 1) % modules.size());
        selection.clear();
        selection.add(selected);
        clampSelection();
        updateLabels();
    }

    private void move(int dx, int dy) {
        Map<TidalModule, ElementState> before = captureStates(selection);
        int grid = TidalConfig.snapToGrid ? 8 : 1;
        for (Map.Entry<TidalModule, ElementState> entry : before.entrySet()) {
            TidalModule module = entry.getKey();
            if (module.locked) continue;
            ElementState state = entry.getValue();
            int x = state.x() + dx / 8 * grid;
            int y = state.y() + dy / 8 * grid;
            TidalHud.setPosition(module, x, y, width, height);
        }
        clampSelection();
        recordEdit(before);
        TidalConfig.save();
    }

    private void nextAnchor() {
        ElementState before = captureState(selected);
        TidalModule.Anchor[] anchors = TidalModule.Anchor.values();
        int next = (selected.anchor.ordinal() + 1) % anchors.length;
        TidalHud.setAnchor(selected, anchors[next], width, height);
        clampSelection();
        recordEdit(selected, before);
        updateLabels();
        TidalConfig.save();
    }

    private void updateLabels() {
        moduleButton.setMessage(Component.literal(selection.size() > 1
                ? "Selected: " + selection.size() + " elements"
                : "Selected: " + selected.name));
        snapButton.setMessage(Component.literal(
                "Grid  " + (TidalConfig.snapToGrid ? "On" : "Off")));
        snapButton.setChecked(TidalConfig.snapToGrid);
        visibilityButton.setMessage(Component.literal(
                selected.enabled ? "Visible  On" : "Visible  Off"));
        anchorButton.setMessage(Component.literal(
                "Anchor  " + TidalUi.displayName(selected.anchor)));
        lockButton.setMessage(Component.literal(selected.locked ? "Locked" : "Unlocked"));
        backgroundToggle.setChecked(selected.backgroundOpacity > 0);
        layoutButton.setMessage(Component.literal(TidalConfig.activeHudLayout));
        saveLayoutButton.setMessage(Component.literal("Save"));
        renameLayoutButton.setMessage(Component.literal("Rename"));
        deleteLayoutButton.setMessage(Component.literal("Delete"));
        if (undoButton != null) {
            undoButton.active = !undoHistory.isEmpty();
            redoButton.active = !redoHistory.isEmpty();
        }
        layoutPanelControls();
    }

    private ElementState captureState(TidalModule module) {
        return new ElementState(TidalHud.x(module, width), TidalHud.y(module, height),
                module.scale, module.anchor, module.normalizedPosition,
                module.normalizedX, module.normalizedY, module.surfaceGroupId,
                module.backgroundColor, module.backgroundOpacity, module.accentColor,
                module.hudStyle);
    }

    private void recordEdit(TidalModule module, ElementState before) {
        Map<TidalModule, ElementState> states = new LinkedHashMap<>();
        states.put(module, before);
        recordEdit(states);
    }

    private Map<TidalModule, ElementState> captureStates(Iterable<TidalModule> modules) {
        Map<TidalModule, ElementState> states = new LinkedHashMap<>();
        for (TidalModule module : modules) states.put(module, captureState(module));
        return states;
    }

    private void recordEdit(Map<TidalModule, ElementState> before) {
        Map<TidalModule, ElementState> after = captureStates(before.keySet());
        if (before.equals(after)) return;
        undoHistory.push(new Edit(new LinkedHashMap<>(before), after));
        while (undoHistory.size() > 64) undoHistory.removeLast();
        redoHistory.clear();
        updateLabels();
    }

    private void undo() {
        if (undoHistory.isEmpty()) return;
        Edit edit = undoHistory.pop();
        applyEdit(edit.before());
        redoHistory.push(edit);
        updateLabels();
    }

    private void redo() {
        if (redoHistory.isEmpty()) return;
        Edit edit = redoHistory.pop();
        applyEdit(edit.after());
        undoHistory.push(edit);
        updateLabels();
    }

    private void applyEdit(Map<TidalModule, ElementState> states) {
        for (Map.Entry<TidalModule, ElementState> entry : states.entrySet()) {
            TidalModule module = entry.getKey();
            ElementState state = entry.getValue();
            module.scale = state.scale();
            module.anchor = state.anchor();
            TidalHud.setPosition(module, state.x(), state.y(), width, height);
            module.normalizedPosition = state.normalizedPosition();
            module.normalizedX = state.normalizedX();
            module.normalizedY = state.normalizedY();
            module.surfaceGroupId = state.surfaceGroupId();
            module.backgroundColor = state.backgroundColor();
            module.backgroundOpacity = state.backgroundOpacity();
            module.accentColor = state.accentColor();
            module.hudStyle = state.hudStyle();
        }
        clampSelection();
        updateLabels();
        TidalConfig.save();
    }

    private void clampSelection() {
        for (TidalModule module : selection) {
            int x = clamp(TidalHud.x(module, width), 0,
                    Math.max(0, width - TidalHud.width(module)));
            int y = clamp(TidalHud.y(module, height), 0,
                    Math.max(0, height - TidalHud.height(module)));
            TidalHud.setPosition(module, x, y, width, height);
        }
    }

    private void mergeSelectedSurfaces() {
        if (selection.size() < 2) return;
        boolean oneMergedGroup = selection.stream()
                .map(module -> module.surfaceGroupId).distinct().count() == 1
                && selected.surfaceGroupId > 0;
        Set<Integer> mergedGroups = new LinkedHashSet<>();
        for (TidalModule module : selection) {
            if (module.surfaceGroupId > 0) mergedGroups.add(module.surfaceGroupId);
        }
        List<TidalModule> affected = new ArrayList<>();
        for (TidalModule module : TidalModules.all()) {
            if (selection.contains(module)
                    || (!oneMergedGroup && mergedGroups.contains(module.surfaceGroupId))) {
                affected.add(module);
            }
        }
        Map<TidalModule, ElementState> before = captureStates(affected);
        if (oneMergedGroup) {
            for (TidalModule module : selection) module.surfaceGroupId = 0;
        } else {
            int groupId = selected.surfaceGroupId;
            if (groupId == 0) {
                groupId = mergedGroups.stream().findFirst().orElseGet(this::nextSurfaceGroupId);
            }
            for (TidalModule module : TidalModules.all()) {
                if (!selection.contains(module) && !mergedGroups.contains(module.surfaceGroupId)) {
                    continue;
                }
                module.surfaceGroupId = groupId;
                module.backgroundColor = selected.backgroundColor;
                module.backgroundOpacity = selected.backgroundOpacity;
                module.accentColor = selected.accentColor;
                module.hudStyle = selected.hudStyle;
            }
        }
        recordEdit(before);
        TidalConfig.save();
    }

    private int clampGroupDelta(int delta, boolean horizontal) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (Map.Entry<TidalModule, ElementState> entry : groupGestureStart.entrySet()) {
            if (entry.getKey().locked) continue;
            ElementState state = entry.getValue();
            int start = horizontal ? state.x() : state.y();
            int extent = horizontal ? TidalHud.width(entry.getKey())
                    : TidalHud.height(entry.getKey());
            min = Math.min(min, start);
            max = Math.max(max, start + extent);
        }
        int viewport = horizontal ? width : height;
        if (min == Integer.MAX_VALUE) return 0;
        return clamp(delta, -min, Math.max(-min, viewport - max));
    }

    private void updateGapLabel(int x, int y) {
        int elementWidth = TidalHud.width(selected);
        int elementHeight = TidalHud.height(selected);
        int right = x + elementWidth;
        int bottom = y + elementHeight;
        int horizontalGap = Integer.MAX_VALUE;
        int verticalGap = Integer.MAX_VALUE;
        for (TidalModule other : TidalModules.all()) {
            if (selection.contains(other) || !other.enabled || !other.implemented
                    || other.group != TidalModule.Group.HUD) continue;
            int otherX = TidalHud.x(other, width);
            int otherY = TidalHud.y(other, height);
            int otherRight = otherX + TidalHud.width(other);
            int otherBottom = otherY + TidalHud.height(other);
            if (y < otherBottom && bottom > otherY) {
                if (otherX >= right) horizontalGap = Math.min(horizontalGap, otherX - right);
                else if (otherRight <= x) horizontalGap = Math.min(horizontalGap, x - otherRight);
            }
            if (x < otherRight && right > otherX) {
                if (otherY >= bottom) verticalGap = Math.min(verticalGap, otherY - bottom);
                else if (otherBottom <= y) verticalGap = Math.min(verticalGap, y - otherBottom);
            }
        }
        if (horizontalGap == Integer.MAX_VALUE && verticalGap == Integer.MAX_VALUE) {
            gapLabel = "";
            return;
        }
        StringBuilder label = new StringBuilder("Gap");
        if (horizontalGap != Integer.MAX_VALUE) label.append("  ").append(horizontalGap).append(" px");
        if (verticalGap != Integer.MAX_VALUE) {
            if (horizontalGap != Integer.MAX_VALUE) label.append("  ·");
            label.append("  ").append(verticalGap).append(" px");
        }
        gapLabel = label.toString();
        gapLabelX = clamp(x, 8, Math.max(8, width - font.width(gapLabel) - 24));
        gapLabelY = clamp(y - 22, 8, Math.max(8, height - 24));
    }

    private int nextSurfaceGroupId() {
        return TidalModules.all().stream().mapToInt(module -> module.surfaceGroupId)
                .max().orElse(0) + 1;
    }

    private void propagateSurfaceAppearance() {
        if (selected.surfaceGroupId == 0) return;
        for (TidalModule module : TidalModules.all()) {
            if (module.surfaceGroupId != selected.surfaceGroupId) continue;
            module.backgroundColor = selected.backgroundColor;
            module.backgroundOpacity = selected.backgroundOpacity;
            module.accentColor = selected.accentColor;
            module.hudStyle = selected.hudStyle;
        }
    }

    private static String colorName(int color) {
        if (color == TidalTheme.PRIMARY) return "Primary";
        if (color == TidalTheme.SECONDARY) return "Secondary";
        if (color == TidalTheme.ACCENT) return "Accent";
        if (color == TidalTheme.SUCCESS) return "Green";
        if (color == TidalTheme.WARNING) return "Amber";
        if (color == TidalTheme.ERROR) return "Red";
        if (color == TidalTheme.SURFACE) return "Surface";
        if (color == TidalTheme.BACKGROUND) return "Background";
        if (color == TidalTheme.INTERACTIVE) return "Interactive";
        if (color == TidalTheme.ELEVATED) return "Elevated";
        return "Custom";
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && scaleSlider.visible
                && scaleSlider.isMouseOver(event.x(), event.y())) {
            scaleDragging = true;
            scaleGestureStart = captureState(selected);
            scaleSlider.setDragging(true);
            scaleSlider.setValueFromX(event.x());
            return true;
        }
        if (event.button() == 0) {
            clampSelection();
            clampPanelPosition();
            int panelHeight = panelCollapsed ? 42 : Math.min(PANEL_HEIGHT, height - 16);
            int panelWidth = panelWidth();
            boolean inPanel = event.x() >= panelX && event.x() <= panelX + panelWidth
                    && event.y() >= panelY && event.y() <= panelY + panelHeight;
            boolean inHeader = inPanel && event.y() <= panelY + 40;
            if (inHeader && !collapseButton.isMouseOver(event.x(), event.y())
                    && !undoButton.isMouseOver(event.x(), event.y())
                    && !redoButton.isMouseOver(event.x(), event.y())) {
                panelDragging = true;
                panelDragOffsetX = event.x() - panelX;
                panelDragOffsetY = event.y() - panelY;
                return true;
            }
            if (!panelCollapsed && opacitySlider.visible
                    && opacitySlider.isMouseOver(event.x(), event.y())) {
                opacityDragging = true;
                opacitySlider.setDragging(true);
            }
            if (super.mouseClicked(event, doubleClick)) return true;

            if (inPanel) return true;
            if (!selected.locked && handleContains(event.x(), event.y())) {
                selection.clear();
                selection.add(selected);
                resizing = true;
                gestureStart = captureState(selected);
                groupGestureStart = null;
                gapLabel = "";
                resizeStartX = event.x();
                resizeStartY = event.y();
                resizeStartScale = selected.scale;
                return true;
            }
            TidalModule hit = findEnabledElement(event.x(), event.y());
            if (hit == null && contains(selected, event.x(), event.y())) hit = selected;
            if (hit != null) {
                if (InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                        || InputConstants.isKeyDown(minecraft.getWindow(),
                        GLFW.GLFW_KEY_RIGHT_SHIFT)) selection.add(hit);
                else if (!selection.contains(hit)) {
                    selection.clear();
                    selection.add(hit);
                }
                selected = hit;
                updateLabels();
                dragging = !selected.locked;
                gestureStart = captureState(selected);
                groupGestureStart = captureStates(selection);
                gapLabel = "";
                dragOffsetX = event.x() - TidalHud.x(selected, width);
                dragOffsetY = event.y() - TidalHud.y(selected, height);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (panelDragging && event.button() == 0) {
            panelX = (int) Math.round(event.x() - panelDragOffsetX);
            panelY = (int) Math.round(event.y() - panelDragOffsetY);
            clampPanelPosition();
            layoutPanelControls();
            return true;
        }
        if (opacityDragging && event.button() == 0) {
            opacitySlider.setValueFromX(event.x());
            return true;
        }
        if (scaleDragging && event.button() == 0) {
            scaleSlider.setValueFromX(event.x());
            return true;
        }
        if (resizing && event.button() == 0) {
            double change = ((event.x() - resizeStartX) + (event.y() - resizeStartY)) / 2.0;
            selected.scale = clamp(resizeStartScale + (float) (change / 100.0), 0.5f, 2.0f);
            clampSelection();
            updateLabels();
            return true;
        }
        if (dragging && event.button() == 0) {
            int grid = TidalConfig.snapToGrid ? 8 : 1;
            int x = snapAndClamp(event.x() - dragOffsetX, grid,
                    Math.max(0, width - TidalHud.width(selected)));
            int y = snapAndClamp(event.y() - dragOffsetY, grid,
                    Math.max(0, height - TidalHud.height(selected)));
            snapToEdges(selected, x, y);
            ElementState selectedOrigin = groupGestureStart.get(selected);
            int moveX = clampGroupDelta(snapX - selectedOrigin.x(), true);
            int moveY = clampGroupDelta(snapY - selectedOrigin.y(), false);
            for (Map.Entry<TidalModule, ElementState> entry : groupGestureStart.entrySet()) {
                TidalModule module = entry.getKey();
                if (module.locked) continue;
                ElementState origin = entry.getValue();
                TidalHud.setPosition(module, origin.x() + moveX, origin.y() + moveY,
                        width, height);
            }
            updateGapLabel(TidalHud.x(selected, width), TidalHud.y(selected, height));
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (panelDragging && event.button() == 0) {
            panelDragging = false;
            return true;
        }
        if (opacityDragging && event.button() == 0) {
            opacityDragging = false;
            opacitySlider.setDragging(false);
            TidalConfig.save();
            return true;
        }
        if (scaleDragging && event.button() == 0) {
            scaleDragging = false;
            scaleSlider.setDragging(false);
            if (scaleGestureStart != null) recordEdit(selected, scaleGestureStart);
            scaleGestureStart = null;
            TidalConfig.save();
            return true;
        }
        if ((dragging || resizing) && event.button() == 0) {
            dragging = false;
            resizing = false;
            guideX = guideY = -1;
            if (groupGestureStart != null) recordEdit(groupGestureStart);
            else if (gestureStart != null) recordEdit(selected, gestureStart);
            gestureStart = null;
            groupGestureStart = null;
            gapLabel = "";
            TidalConfig.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
                                 double verticalAmount) {
        int panelHeight = panelCollapsed ? 42 : Math.min(PANEL_HEIGHT, height - 16);
        int panelWidth = panelWidth();
        if (!panelCollapsed && mouseX >= panelX && mouseX <= panelX + panelWidth
                && mouseY >= panelY && mouseY <= panelY + panelHeight) {
            panelScrollOffset = Math.max(0,
                    panelScrollOffset + (verticalAmount < 0 ? 24 : -24));
            layoutPanelControls();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private static int snapAndClamp(double position, int grid, int max) {
        int rounded = (int) Math.round(position);
        if (grid > 1) rounded = Math.round((float) rounded / grid) * grid;
        return clamp(rounded, 0, max);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private TidalModule findEnabledElement(double mouseX, double mouseY) {
        var modules = TidalModules.all();
        for (int i = modules.size() - 1; i >= 0; i--) {
            TidalModule module = modules.get(i);
            if (!module.enabled || !module.implemented
                    || module.group != TidalModule.Group.HUD) {
                continue;
            }
            if (contains(module, mouseX, mouseY)) return module;
        }
        return null;
    }

    private boolean contains(TidalModule module, double mouseX, double mouseY) {
        int x = TidalHud.x(module, width);
        int y = TidalHud.y(module, height);
        return mouseX >= x && mouseX <= x + TidalHud.width(module)
                && mouseY >= y && mouseY <= y + TidalHud.height(module);
    }

    private boolean handleContains(double mouseX, double mouseY) {
        int right = TidalHud.x(selected, width) + TidalHud.width(selected);
        int bottom = TidalHud.y(selected, height) + TidalHud.height(selected);
        return mouseX >= right - 7 && mouseX <= right + 3
                && mouseY >= bottom - 7 && mouseY <= bottom + 3;
    }

    private void snapToEdges(TidalModule moving, int proposedX, int proposedY) {
        snapX = proposedX;
        snapY = proposedY;
        guideX = guideY = -1;
        if (!TidalConfig.snapToEdges) return;
        bestSnapX = bestSnapY = 7;
        considerXSnap(proposedX, 0, 0);
        considerXSnap(proposedX, Math.max(0, width - TidalHud.width(moving)), width);
        considerYSnap(proposedY, 0, 0);
        considerYSnap(proposedY, Math.max(0, height - TidalHud.height(moving)), height);
        for (TidalModule other : TidalModules.all()) {
            if (selection.contains(other) || !other.enabled || !other.implemented
                    || other.group != TidalModule.Group.HUD) continue;
            int x = TidalHud.x(other, width);
            int y = TidalHud.y(other, height);
            considerXSnap(proposedX, x, x);
            considerXSnap(proposedX, x + TidalHud.width(other),
                    x + TidalHud.width(other));
            considerXSnap(proposedX, x - TidalHud.width(moving), x);
            considerXSnap(proposedX,
                    x + TidalHud.width(other) - TidalHud.width(moving),
                    x + TidalHud.width(other));
            considerYSnap(proposedY, y, y);
            considerYSnap(proposedY, y + TidalHud.height(other),
                    y + TidalHud.height(other));
            considerYSnap(proposedY, y - TidalHud.height(moving), y);
            considerYSnap(proposedY,
                    y + TidalHud.height(other) - TidalHud.height(moving),
                    y + TidalHud.height(other));
        }
    }

    private void considerXSnap(int proposed, int target, int guide) {
        int distance = Math.abs(proposed - target);
        if (distance < bestSnapX) {
            bestSnapX = distance;
            snapX = target;
            guideX = guide;
        }
    }

    private void considerYSnap(int proposed, int target, int guide) {
        int distance = Math.abs(proposed - target);
        if (distance < bestSnapY) {
            bestSnapY = distance;
            snapY = target;
            guideY = guide;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                   float delta) {
        TidalMotion.beginFrame();
        graphics.fill(12, 12, Math.max(12, width - 12), Math.max(12, height - 12),
                TidalUi.withAlpha(TidalTheme.BACKGROUND, 0x7A));
        clampSelection();
        clampPanelPosition();
        TidalHud.renderEditorPreview(graphics, selected);

        int panelWidth = panelWidth();
        int panelHeight = panelCollapsed ? 42 : Math.min(PANEL_HEIGHT, height - 16);
        if (panelCollapsed) {
            TidalUi.panel(graphics, panelX, panelY, panelWidth, panelHeight,
                    TidalTheme.SURFACE, TidalTheme.Radius.PANEL);
        } else {
            TidalUi.drawAsset(graphics, "tidal/ui/polyglass_hud_studio_drawer",
                    panelX, panelY, panelWidth, panelHeight);
        }
        TidalUi.strongLabel(graphics, font, "HUD EDIT MODE", panelX + 12, panelY + 10,
                TidalUi.PRIMARY);
        if (!panelCollapsed) {
            TidalUi.label(graphics, font, panelDragging ? "Drag panel to reposition"
                            : dragging ? "Release to place"
                            : resizing ? "Release to finish resizing"
                            : "Shift-click multi-select · drag to move · corner resizes",
                    panelX + 12, panelY + 35, TidalUi.SECONDARY);
            TidalUi.label(graphics, font, "ELEMENT", panelX + 12, panelY + 48,
                    TidalUi.MUTED);
            drawInspectorSection(graphics, "POSITION & SCALE", 174 + layoutShift(), panelHeight);
            drawInspectorSection(graphics, "STYLE", 248 + layoutShift(), panelHeight);
            drawInspectorSection(graphics, "LAYOUT", 378 + layoutShift(), panelHeight);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
        for (TidalModule module : selection) {
            int selectionX = TidalHud.x(module, width);
            int selectionY = TidalHud.y(module, height);
            TidalUi.drawAsset(graphics, "borders/focus-ring-r7",
                    selectionX - 2, selectionY - 2, TidalHud.width(module) + 4,
                    TidalHud.height(module) + 4);
        }

        int x = TidalHud.x(selected, width);
        int y = TidalHud.y(selected, height);
        int right = x + TidalHud.width(selected);
        int bottom = y + TidalHud.height(selected);
        if (guideX >= 0) graphics.fill(guideX, 0, guideX + 1, height,
                TidalUi.withAlpha(TidalTheme.ACCENT, 100));
        if (guideY >= 0) graphics.fill(0, guideY, width, guideY + 1,
                TidalUi.withAlpha(TidalTheme.ACCENT, 100));
        String resizeKnob = resizing ? "controls/knob-pressed"
                : handleContains(mouseX, mouseY) ? "controls/knob-hover" : "controls/knob-normal";
        TidalUi.drawAsset(graphics, resizeKnob, right - 5, bottom - 5, 9, 9);
        if (!gapLabel.isEmpty()) {
            int labelWidth = font.width(gapLabel) + 16;
            TidalUi.panel(graphics, gapLabelX, gapLabelY, labelWidth, 20,
                    TidalUi.withAlpha(TidalTheme.ELEVATED, 0xF0), 6);
            TidalUi.label(graphics, font, gapLabel, gapLabelX + 8, gapLabelY + 6,
                    TidalTheme.PRIMARY);
        }
    }

    private void drawInspectorSection(GuiGraphicsExtractor graphics, String title, int y,
                                      int panelHeight) {
        int visibleY = panelY + y - panelScrollOffset;
        if (visibleY < panelY + 48
                || visibleY + font.lineHeight
                > panelY + panelHeight - CONTROL_HEIGHT - 22) return;
        TidalUi.label(graphics, font, title, panelX + 12, visibleY, TidalUi.MUTED);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        dragging = false;
        resizing = false;
        TidalConfig.save();
        minecraft.gui.setScreen(parent);
    }
}
