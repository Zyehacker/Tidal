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
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class TidalScreen extends Screen {
    private enum Tab { HUD, MODULES, MODS, COSMETICS, SETTINGS }

    private enum CosmeticsSection {
        SKIN("Equipped Skin"), CONTROLS("In-game Controls");

        private final String label;

        CosmeticsSection(String label) {
            this.label = label;
        }
    }

    private enum SettingsSection {
        GENERAL("General"), GRAPHICS("Graphics"), INPUT("Input");

        private final String label;

        SettingsSection(String label) {
            this.label = label;
        }

    }

    private enum HudSection {
        ELEMENTS("Elements"), LAYOUTS("Layouts"), APPEARANCE("Appearance");

        private final String label;

        HudSection(String label) {
            this.label = label;
        }
    }

    private enum ModuleFilter {
        ALL("All"), HUD_INFO("HUD & Information"), COMBAT("Combat"), PLAYER("Player"),
        WORLD("World"), VISUALS("Visuals"), MOVEMENT("Movement"),
        PERFORMANCE("Performance");

        private final String label;

        ModuleFilter(String label) {
            this.label = label;
        }
    }

    private enum ModFilter {
        ALL("All Mods"), FAVORITES("Favorites"), CONFIGURABLE("Configurable"),
        RUNTIME_TOGGLE("Runtime Toggle");

        private final String label;

        ModFilter(String label) {
            this.label = label;
        }
    }

    private static final SettingsSection[] SETTINGS_SECTIONS = {
            SettingsSection.GENERAL, SettingsSection.GRAPHICS, SettingsSection.INPUT
    };
    private static final int MAX_WIDTH = 980;
    private static final int MAX_HEIGHT = 590;
    private static final int MIN_WIDTH = 320;
    private static final int MIN_HEIGHT = 260;
    private static final int VIEWPORT_CLEARANCE = 16;
    private static final int HEADER_TOP = 12;
    private static final int HEADER_HEIGHT = 24;
    private static final int HEADER_DIVIDER_Y = 72;
    private static final int HEADER_RIGHT_INSET = 10;
    private static final int HEADER_GAP = 2;
    private static final int SEARCH_MIN_WIDTH = 72;
    private static final int GRID_INSET = 10;
    private static final int GRID_GAP = 10;
    private static final int CARD_HEIGHT = 58;
    private static final int ROW_PITCH = 66;
    private static final int FOOTER_HEIGHT = 30;
    private static final int FOOTER_CONTROL_HEIGHT = 24;
    private static final int FOOTER_CONTROL_GAP = 6;
    private static final int FOOTER_INSET = 10;
    private static final int PAGER_BUTTON_WIDTH = 24;
    private static final int INSPECTOR_INSET = 12;
    private static final long TRANSITION_MILLIS = TidalTheme.Motion.MENU_DURATION_MILLIS;
    private static final float TRANSITION_SCALE = 0.985f;
    private static final float TRANSITION_OFFSET_Y = 3.0f;
    private static final Deque<Tab> NAVIGATION_BACK_STACK = new ArrayDeque<>();
    private static final Identifier BRAND_LOGO = Identifier.fromNamespaceAndPath(
            "tidal", "textures/gui/liquid-glass/tidal/brand/logo.png");
    private static TidalModule selectedForRender;
    private static Tab lastTab = Tab.MODULES;
    private static HudSection lastHudSection = HudSection.ELEMENTS;
    private static CosmeticsSection lastCosmeticsSection = CosmeticsSection.SKIN;
    private static SettingsSection lastSettingsSection = SettingsSection.GENERAL;
    private static ModuleFilter lastModuleFilter = ModuleFilter.ALL;
    private static ModFilter lastModFilter = ModFilter.ALL;

    private Tab tab = lastTab;
    private final List<TidalModule> implementedModules = new ArrayList<>();
    private final List<TidalModManager.ModEntry> installedMods = new ArrayList<>();
    private final List<GridEntry> entries = new ArrayList<>();
    private final List<TidalUi.ModuleRow> moduleCards = new ArrayList<>();
    private final List<TidalUi.InstalledModRow> modCards = new ArrayList<>();
    private final List<TidalUi.ActionButton> sidebarButtons = new ArrayList<>();
    private final List<TidalUi.ActionButton> contextButtons = new ArrayList<>();
    private final List<AbstractWidget> settingsControls = new ArrayList<>();
    private final List<AbstractWidget> modulePreferenceControls = new ArrayList<>();
    private final List<AbstractWidget> hudControls = new ArrayList<>();
    private final List<TidalUi.ActionButton> mainNavigationButtons = new ArrayList<>();
    private final List<AbstractWidget> cosmeticsControls = new ArrayList<>();
    private TidalUi.SearchField search;
    private TidalUi.ActionButton closeButton;
    private TidalUi.CheckOption crosshairOutlineControl;
    private TidalUi.CheckOption dynamicGapControl;
    private TidalUi.ActionButton galleryButton;
    private TidalUi.ActionButton hudToggleButton;
    private TidalUi.ActionButton editHudButton;
    private TidalUi.ActionButton resetButton;
    private TidalUi.ActionButton modsExportButton;
    private TidalUi.CheckOption fpsBoostControl;
    private TidalUi.ActionButton zoomOutButton;
    private TidalUi.ActionButton zoomInButton;
    private TidalUi.CheckOption enableAquaCosmeticsControl;
    private TidalUi.CheckOption equippedSkinIntegrationControl;
    private TidalUi.ActionButton previousButton;
    private TidalUi.ActionButton nextButton;
    private Inspector inspector;
    private TidalModule selected;
    private SettingsSection settingsSection = lastSettingsSection;
    private HudSection hudSection = lastHudSection;
    private CosmeticsSection cosmeticsSection = lastCosmeticsSection;
    private ModuleFilter moduleFilter = lastModuleFilter;
    private ModFilter modFilter = lastModFilter;
    private String searchValue = "";
    private String footerPageLabel = "";
    private boolean capturingKey;
    private boolean inspectorOpen;
    private Screen returnScreen;
    private int page;
    private int pageCount = 1;
    private TidalModule bindingTarget;
    private int menuX;
    private int menuY;
    private int menuWidth;
    private int menuHeight;
    private int sidebarWidth;
    private int sidebarScrollOffset;
    private int settingsScrollOffset;
    private int gridX;
    private int gridY;
    private int gridWidth;
    private int gridHeight;
    private int columns;
    private int rowsPerPage;
    private int modRowsPerPage;
    private int footerY;
    private int footerControlY;
    private int pagerLabelX;
    private int pagerLabelWidth;
    private long openedAt = TidalUi.nowMillis();
    private long closingAt;
    private UUID profileSkinOwner;
    private Identifier profileSkinTexture;
    private float previewRotation;
    private int previewZoom = 60;
    private boolean draggingSkinPreview;
    private int skinPreviewX;
    private int skinPreviewY;
    private int skinPreviewWidth;
    private int skinPreviewHeight;

    private record GridEntry(TidalModule module, int row, int column) {}

    public TidalScreen() {
        this((Screen) null);
    }

    TidalScreen(Screen returnScreen) {
        this(lastTab, "", null, true);
        this.returnScreen = returnScreen;
    }

    static TidalScreen settingsScreen() {
        return new TidalScreen(Tab.SETTINGS, "", null, true);
    }

    private TidalScreen(Tab initialTab, String initialSearch, TidalModule initialSelection) {
        this(initialTab, initialSearch, initialSelection, false);
    }

    private TidalScreen(Tab initialTab, String initialSearch, TidalModule initialSelection,
                        Screen returnScreen) {
        this(initialTab, initialSearch, initialSelection, false);
        this.returnScreen = returnScreen;
    }

    private TidalScreen(Tab initialTab, String initialSearch, TidalModule initialSelection,
                        boolean resetNavigation) {
        super(Component.literal("Tidal"));
        if (resetNavigation) NAVIGATION_BACK_STACK.clear();
        tab = initialTab;
        searchValue = initialSearch;
        selected = initialSelection;
    }

    static TidalModule selectedModule() {
        return selectedForRender;
    }

    @Override
    protected void init() {
        searchValue = search == null ? searchValue : search.getValue();
        TidalModManager.initialize();
        clearWidgets();
        moduleCards.clear();
        modCards.clear();
        modulePreferenceControls.clear();
        sidebarButtons.clear();
        contextButtons.clear();
        mainNavigationButtons.clear();
        settingsControls.clear();
        hudControls.clear();
        settingsControlSections.clear();
        calculateLayout();
        createHeaderWidgets();
        createContextNavigation();
        createSidebarWidgets();
        createGridWidgets();
        createFooterWidgets();
        inspector = addRenderableWidget(new Inspector());
        createModulePreferenceControls();
        refreshContent();
    }

    private void calculateLayout() {
        int availableWidth = Math.max(1, width - VIEWPORT_CLEARANCE * 2);
        int availableHeight = Math.max(1, height - VIEWPORT_CLEARANCE * 2);
        menuWidth = Math.min(availableWidth, Math.max(MIN_WIDTH, MAX_WIDTH));
        menuHeight = Math.min(availableHeight, Math.max(MIN_HEIGHT, MAX_HEIGHT));
        menuX = Math.max(0, (width - menuWidth) / 2);
        menuY = Math.max(0, (height - menuHeight) / 2);

        boolean fullWidth = false;
        sidebarWidth = menuWidth >= 700 ? Math.round(menuWidth * 0.2f)
                : Math.max(104, menuWidth / 4);
        gridX = menuX + (fullWidth ? GRID_INSET : sidebarWidth + GRID_INSET);
        gridY = menuY + 82;
        gridWidth = Math.max(1, menuX + menuWidth - GRID_INSET - gridX);
        footerY = menuY + menuHeight - FOOTER_HEIGHT - 7;
        footerControlY = footerY + (FOOTER_HEIGHT - FOOTER_CONTROL_HEIGHT) / 2;
        gridHeight = Math.max(1, footerY - gridY - 5);
        columns = gridWidth >= 700 ? 3 : gridWidth >= 400 ? 2 : 1;
        rowsPerPage = Math.max(1, (gridHeight + 8) / ROW_PITCH);
        modRowsPerPage = Math.max(1, (gridHeight - 58 + 8) / 84);
    }

    private void createHeaderWidgets() {
        int headerY = menuY + HEADER_TOP;
        int closeX = menuX + menuWidth - HEADER_RIGHT_INSET - HEADER_HEIGHT;
        int brandWidth = 126;
        int searchWidth = Math.min(248, closeX - menuX - brandWidth
                - HEADER_GAP * 2 - GRID_INSET);
        if (searchWidth >= SEARCH_MIN_WIDTH) {
            int searchX = closeX - searchWidth - HEADER_GAP;
            search = addRenderableWidget(new TidalUi.SearchField(font, searchX, headerY,
                    searchWidth, HEADER_HEIGHT, Component.literal("Search everything"),
                    searchWidth >= 112));
            search.setValue(searchValue);
            search.setHint(Component.literal("Search anything..."));
            search.setResponder(value -> {
                searchValue = value;
                if (tab == Tab.SETTINGS && !value.isBlank()) {
                    SettingsSection matchingSection = matchingSettingsSection(
                            value.trim().toLowerCase(Locale.ROOT));
                    if (matchingSection != null) settingsSection = matchingSection;
                }
                page = 0;
                closeInspector();
                refreshContent();
            });
        } else {
            search = null;
        }

        closeButton = addRenderableWidget(new TidalUi.ActionButton(closeX, headerY,
                HEADER_HEIGHT, HEADER_HEIGHT, "×", TidalUi.ActionButton.Style.CLOSE,
                this::startClosing));
    }

    private void createSidebarWidgets() {
        int y = menuY + 78;
        addMainNavigationButton("Edit HUD", () ->
                minecraft.setScreen(new TidalHudEditorScreen(this)), false, y);
        y += 26;
        addMainNavigationButton("Modules", () -> switchTab(Tab.MODULES),
                tab == Tab.MODULES, y);
        y += 26;
        addMainNavigationButton("Mods", () -> switchTab(Tab.MODS), tab == Tab.MODS, y);
        y += 26;
        addMainNavigationButton("Cosmetics", () -> switchTab(Tab.COSMETICS),
                tab == Tab.COSMETICS, y);
        y += 26;
        addMainNavigationButton("Settings", () -> switchTab(Tab.SETTINGS),
                tab == Tab.SETTINGS, y);
    }

    private void createContextNavigation() {
        int x = gridX + 6;
        int y = menuY + 42;
        if (tab == Tab.MODULES) {
            for (ModuleFilter filter : ModuleFilter.values()) {
                if (filter != ModuleFilter.ALL && TidalModules.all().stream()
                        .noneMatch(module -> module.implemented && !module.hidden
                                && matchesFilter(module, filter))) continue;
                x = addContextButton(filter.label, x, y, moduleFilter == filter, () -> {
                    moduleFilter = filter;
                    lastModuleFilter = filter;
                    page = 0;
                    closeInspector();
                    refreshContent();
                });
            }
        } else if (tab == Tab.MODS) {
            for (ModFilter filter : ModFilter.values()) {
                x = addContextButton(filter.label, x, y, modFilter == filter, () -> {
                    modFilter = filter;
                    lastModFilter = filter;
                    page = 0;
                    refreshContent();
                });
            }
        } else if (tab == Tab.COSMETICS) {
            for (CosmeticsSection section : CosmeticsSection.values()) {
                x = addContextButton(section.label, x, y,
                        cosmeticsSection == section, () -> {
                    cosmeticsSection = section;
                    lastCosmeticsSection = section;
                    page = 0;
                    refreshContent();
                });
            }
        } else if (tab == Tab.SETTINGS) {
            for (SettingsSection section : SETTINGS_SECTIONS) {
                x = addContextButton(section.label, x, y, settingsSection == section, () -> {
                    settingsSection = section;
                    lastSettingsSection = section;
                    settingsScrollOffset = 0;
                    page = 0;
                    refreshContent();
                });
            }
        }
    }

    private int addContextButton(String label, int x, int y, boolean selected,
                                 Runnable action) {
        int width = Math.max(44, TidalUi.textWidth(font, label) + 18);
        TidalUi.ActionButton button = new TidalUi.ActionButton(x, y, width, 26, label,
                TidalUi.ActionButton.Style.MENU_ITEM, action);
        button.highlighted = selected;
        contextButtons.add(addRenderableWidget(button));
        return x + width + 4;
    }

    private void layoutSettingsSidebar() {
        int startY = menuY + 216;
        int bottomY = menuY + menuHeight - 58;
        int pitch = 29;
        int contentHeight = sidebarButtons.size() * pitch - (pitch - 27);
        int viewportHeight = Math.max(0, bottomY - startY);
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        sidebarScrollOffset = Math.max(0, Math.min(sidebarScrollOffset, maxScroll));
        for (int i = 0; i < sidebarButtons.size(); i++) {
            TidalUi.ActionButton button = sidebarButtons.get(i);
            int y = startY + i * pitch - sidebarScrollOffset;
            button.setPosition(menuX + 10, y);
            button.visible = y >= startY && y + 27 <= bottomY;
        }
    }

    private void addMainNavigationButton(String label, Runnable action,
                                         boolean highlighted, int y) {
        TidalUi.ActionButton button = new TidalUi.ActionButton(menuX + 10, y,
                Math.max(1, sidebarWidth - 20), 27, label,
                TidalUi.ActionButton.Style.MENU_ITEM, action);
        button.highlighted = highlighted;
        mainNavigationButtons.add(addRenderableWidget(button));
    }

    private void addSidebarButton(String label, int y, Runnable action,
                                  java.util.function.BooleanSupplier selected) {
        TidalUi.ActionButton button = new TidalUi.ActionButton(menuX + 10, y,
                Math.max(1, sidebarWidth - 20), 27, label,
                TidalUi.ActionButton.Style.MENU_ITEM, action);
        button.highlighted = selected.getAsBoolean();
        sidebarButtons.add(addRenderableWidget(button));
    }

    private void switchTab(Tab target) {
        if (tab == target) return;
        NAVIGATION_BACK_STACK.push(tab);
        searchValue = search == null ? searchValue : search.getValue();
        tab = target;
        lastTab = target;
        page = 0;
        selected = null;
        inspectorOpen = false;
        init();
    }

    private void createGridWidgets() {
        int capacity = columns * rowsPerPage;
        for (int i = 0; i < capacity; i++) {
            TidalUi.ModuleRow card = new TidalUi.ModuleRow(gridColumnX(0), gridY,
                    gridColumnWidth(0),
                    CARD_HEIGHT, TidalModules.all().get(0), () -> {}, () -> {});
            moduleCards.add(addRenderableWidget(card));
        }
        TidalModManager.ModEntry firstMod = TidalModManager.all().isEmpty()
                ? null : TidalModManager.all().get(0);
        if (firstMod != null) {
            for (int i = 0; i < columns * modRowsPerPage; i++) {
                TidalUi.InstalledModRow card = new TidalUi.InstalledModRow(
                        gridColumnX(0), gridY,
                        gridColumnWidth(0), 76, firstMod, TidalModManager::requestToggle,
                        entry -> TidalModManager.openSettings(entry, this),
                        entry -> minecraft.setScreen(new TidalModDetailsScreen(this, entry)));
                modCards.add(addRenderableWidget(card));
            }

        }
        fpsBoostControl = addRenderableWidget(new TidalUi.CheckOption(
                gridX + 8, gridY + 30, Math.max(1, gridWidth - 16), 26,
                "FPS Boost", "fps_boost", TidalConfig.fpsBoostEnabled, () -> {
            TidalConfig.fpsBoostEnabled = !TidalConfig.fpsBoostEnabled;
            TidalConfig.save();
        }));
        galleryButton = addRenderableWidget(new TidalUi.ActionButton(
                gridX + TidalUi.CARD_CONTENT_INSET, gridY + 145, 208,
                FOOTER_CONTROL_HEIGHT, "Liquid Glass gallery",
                TidalUi.ActionButton.Style.CONTROL,
                () -> minecraft.setScreen(new LiquidGlassGalleryScreen(this))));
        createSettingsControls();
        createHudControls();
        createCosmeticsControls();
    }

    private int gridColumnX(int column) {
        int cellStart = gridX + column * gridWidth / columns;
        return cellStart + GRID_GAP / 2;
    }

    private int gridColumnWidth(int column) {
        int cellStart = column * gridWidth / columns;
        int cellEnd = (column + 1) * gridWidth / columns;
        return Math.max(1, cellEnd - cellStart - GRID_GAP);
    }

    private void createHudControls() {
        int x = gridX + 8;
        int width = Math.max(1, Math.min(400, gridWidth - 16));
        addHudControl(new TidalUi.ActionButton(x, gridY + 109, width, 30,
                "Open HUD editor", TidalUi.ActionButton.Style.PRIMARY,
                () -> minecraft.setScreen(new TidalHudEditorScreen(this))), HudSection.LAYOUTS);
        addHudControl(new TidalUi.CheckOption(x, gridY + 151, width, 28,
                "Snap to grid", "select", TidalConfig.snapToGrid, () -> {
            TidalConfig.snapToGrid = !TidalConfig.snapToGrid;
            TidalConfig.save();
        }), HudSection.LAYOUTS);
        addHudControl(new TidalUi.CheckOption(x, gridY + 185, width, 28,
                "Snap to screen edges", "expand", TidalConfig.snapToEdges, () -> {
            TidalConfig.snapToEdges = !TidalConfig.snapToEdges;
            TidalConfig.save();
        }), HudSection.LAYOUTS);
        addHudControl(new TidalUi.ActionButton(x, gridY + 227, width, 28,
                "Reset HUD positions", TidalUi.ActionButton.Style.CONTROL, () -> {
            TidalHud.initializePositions(this.width, this.height);
            TidalConfig.save();
            refreshContent();
        }), HudSection.LAYOUTS);
        addHudControl(new TidalUi.ChoiceStrip(x, gridY + 109, width, 54,
                "HUD surface style", List.of("Minimal", "Subtle", "Rounded", "Bordered",
                        "Transparent"),
                () -> TidalConfig.defaultHudStyle.ordinal(), index -> {
            TidalModule.HudStyle style = TidalModule.HudStyle.values()[index];
            TidalConfig.defaultHudStyle = style;
            for (TidalModule module : TidalModules.all()) {
                if (module.group == TidalModule.Group.HUD && module.implemented) {
                    module.hudStyle = style;
                }
            }
            TidalConfig.save();
        }), HudSection.APPEARANCE);
        addHudControl(new TidalUi.ColorPicker(x, gridY + 175, width, 44,
                () -> TidalConfig.defaultHudAccent, color -> {
            TidalConfig.defaultHudAccent = color;
            for (TidalModule module : TidalModules.all()) {
                if (module.group == TidalModule.Group.HUD && module.implemented) {
                    module.accentColor = color;
                }
            }
            TidalConfig.save();
        }), HudSection.APPEARANCE);
        addHudControl(new TidalUi.CheckOption(x, gridY + 231, width, 28,
                "Show Tidal HUD", "visibility", TidalConfig.hudEnabled, () -> {
            TidalConfig.hudEnabled = !TidalConfig.hudEnabled;
            TidalConfig.save();
            refreshContent();
        }), HudSection.APPEARANCE);
    }

    private void createCosmeticsControls() {
        zoomOutButton = addRenderableWidget(new TidalUi.ActionButton(0, 0, 30, 26, "−",
                TidalUi.ActionButton.Style.QUIET, () -> {
            previewZoom = Math.max(50, previewZoom - 8);
        }));
        zoomInButton = addRenderableWidget(new TidalUi.ActionButton(0, 0, 30, 26, "+",
                TidalUi.ActionButton.Style.QUIET, () -> {
            previewZoom = Math.min(110, previewZoom + 8);
        }));
        enableAquaCosmeticsControl = new TidalUi.CheckOption(0, 0, 280, 28,
                "Enable Aqua Cosmetics", "settings", TidalConfig.enableAquaCosmetics, () -> {
            TidalConfig.enableAquaCosmetics = !TidalConfig.enableAquaCosmetics;
            TidalConfig.save();
            refreshContent();
        });
        equippedSkinIntegrationControl = new TidalUi.CheckOption(0, 0, 280, 28,
                "Show equipped skin integration", "visibility",
                TidalConfig.showEquippedSkinIntegration, () -> {
            TidalConfig.showEquippedSkinIntegration =
                    !TidalConfig.showEquippedSkinIntegration;
            TidalConfig.save();
            refreshContent();
        });
        cosmeticsControls.add(addRenderableWidget(enableAquaCosmeticsControl));
        cosmeticsControls.add(addRenderableWidget(equippedSkinIntegrationControl));
        for (AbstractWidget control : cosmeticsControls) control.visible = false;
        zoomOutButton.visible = zoomInButton.visible = false;
    }

    private void addHudControl(AbstractWidget widget, HudSection section) {
        addRenderableWidget(widget);
        hudControls.add(widget);
        hudControlSections.put(widget, section);
        widget.visible = false;
    }

    private void createSettingsControls() {
        int x = gridX + 8;
        int width = Math.max(1, gridWidth - 16);
        addSettingsControl(new TidalUi.ActionButton(x, gridY + 109, width, 30,
                "Back up configuration", TidalUi.ActionButton.Style.CONTROL,
                () -> minecraft.keyboardHandler.setClipboard(
                        TidalConfig.backupConfig().toString())), SettingsSection.GENERAL);
        addSettingsControl(new TidalUi.ActionButton(x, gridY + 145, width, 30,
                "Copy configuration JSON", TidalUi.ActionButton.Style.CONTROL,
                () -> minecraft.keyboardHandler.setClipboard(TidalConfig.exportConfig())),
                SettingsSection.GENERAL);
        addSettingsControl(new TidalUi.ActionButton(x, gridY + 181, width, 30,
                "Restore default settings", TidalUi.ActionButton.Style.DESTRUCTIVE, () -> {
            TidalConfig.restoreDefaults();
            init();
        }), SettingsSection.GENERAL);
        addSettingsControl(new TidalUi.ChoiceStrip(x, gridY + 109, width, 54,
                "UI edge antialiasing", List.of("Native", "2×", "4×"),
                () -> TidalConfig.uiAntialiasing == 4 ? 2
                        : TidalConfig.uiAntialiasing == 2 ? 1 : 0,
                index -> {
                    TidalConfig.uiAntialiasing = switch (index) {
                        case 1 -> 2;
                        case 2 -> 4;
                        default -> 1;
                    };
                    TidalConfig.save();
                }), SettingsSection.GRAPHICS);
        addSettingsControl(new TidalUi.ActionButton(x, gridY + 169, width, 30,
                "Open Minecraft video and FOV settings", TidalUi.ActionButton.Style.CONTROL,
                () -> minecraft.setScreen(
                        new OptionsScreen(this, minecraft.options))),
                SettingsSection.GRAPHICS);
        addSettingsControl(new TidalUi.ActionButton(x, gridY + 109, width, 30,
                "Open Minecraft keybinds", TidalUi.ActionButton.Style.CONTROL,
                () -> minecraft.setScreen(
                        new net.minecraft.client.gui.screens.options.controls.ControlsScreen(
                                this, minecraft.options))), SettingsSection.INPUT);
        addSettingsControl(new TidalUi.ActionButton(x, gridY + 145, width, 30,
                "Search and rebind Tidal modules", TidalUi.ActionButton.Style.PRIMARY,
                () -> minecraft.setScreen(new TidalKeybindScreen(this))),
                SettingsSection.INPUT);
    }

    private void createModulePreferenceControls() {
        int width = Math.max(1, Math.min(224, menuWidth - GRID_INSET * 2) - 24);
        int halfWidth = Math.max(1, (width - 8) / 2);
        modulePreferenceControls.add(addRenderableWidget(new TidalUi.ChoiceStrip(
                0, 0, width, 52, "Crosshair shape", List.of("Cross", "Dot", "Square"),
                () -> TidalConfig.crosshairShape, index -> {
            TidalConfig.crosshairShape = index;
            TidalConfig.save();
        })));
        modulePreferenceControls.add(addRenderableWidget(new TidalUi.ValueSlider(
                0, 0, halfWidth, 38, "Size", () -> TidalConfig.crosshairSize, value -> {
            TidalConfig.crosshairSize = value;
            TidalConfig.save();
        }, 2, 16)));
        modulePreferenceControls.add(addRenderableWidget(new TidalUi.ValueSlider(
                0, 0, halfWidth, 38, "Gap", () -> TidalConfig.crosshairGap, value -> {
            TidalConfig.crosshairGap = value;
            TidalConfig.save();
        }, 0, 16)));
        modulePreferenceControls.add(addRenderableWidget(new TidalUi.ValueSlider(
                0, 0, width, 38, "Thickness", () -> TidalConfig.crosshairThickness, value -> {
            TidalConfig.crosshairThickness = value;
            TidalConfig.save();
        }, 1, 8)));
        crosshairOutlineControl = new TidalUi.CheckOption(
                0, 0, halfWidth, 28, "Outline", "select",
                TidalConfig.crosshairOutline, () -> {
            TidalConfig.crosshairOutline = !TidalConfig.crosshairOutline;
            TidalConfig.save();
        });
        modulePreferenceControls.add(addRenderableWidget(crosshairOutlineControl));
        dynamicGapControl = new TidalUi.CheckOption(
                0, 0, halfWidth, 28, "Dynamic gap", "settings",
                TidalConfig.dynamicCrosshair, () -> {
            TidalConfig.dynamicCrosshair = !TidalConfig.dynamicCrosshair;
            TidalConfig.save();
        });
        modulePreferenceControls.add(addRenderableWidget(dynamicGapControl));
        modulePreferenceControls.add(addRenderableWidget(new TidalUi.ColorPicker(
                0, 0, width, 42, () -> TidalConfig.crosshairColor, color -> {
            TidalConfig.crosshairColor = color;
            TidalConfig.save();
        })));
        modulePreferenceControls.add(addRenderableWidget(new TidalUi.ValueSlider(
                0, 0, width, 38, "Fullbright intensity",
                () -> moduleIntensity("fullbright"), value -> {
            TidalModule module = TidalModules.byId("fullbright");
            if (module != null) module.intensity = value;
            TidalConfig.save();
        }, 0, 100)));
        modulePreferenceControls.add(addRenderableWidget(new TidalUi.ValueSlider(
                0, 0, width, 38, "Zoom field of view",
                () -> moduleIntensity("zoom"), value -> {
            TidalModule module = TidalModules.byId("zoom");
            if (module != null) module.intensity = value;
            TidalConfig.save();
        }, 10, 100)));
        for (AbstractWidget control : modulePreferenceControls) control.visible = false;
    }


    private void addSettingsControl(AbstractWidget widget, SettingsSection section) {
        addRenderableWidget(widget);
        settingsControls.add(widget);
        settingsControlSections.put(widget, section);
        widget.visible = false;
    }


    private final java.util.Map<AbstractWidget, SettingsSection> settingsControlSections =
            new java.util.IdentityHashMap<>();
    private final java.util.Map<AbstractWidget, HudSection> hudControlSections =
            new java.util.IdentityHashMap<>();

    private static int moduleIntensity(String id) {
        TidalModule module = TidalModules.byId(id);
        return module == null ? 100 : module.intensity;
    }

    private void createFooterWidgets() {
        hudToggleButton = addRenderableWidget(new TidalUi.ActionButton(gridX,
                footerControlY, 90, FOOTER_CONTROL_HEIGHT, "HUD: On",
                TidalUi.ActionButton.Style.QUIET, () -> {
            TidalConfig.hudEnabled = !TidalConfig.hudEnabled;
            TidalConfig.save();
            refreshContent();
        }));
        editHudButton = addRenderableWidget(new TidalUi.ActionButton(gridX + 98,
                footerControlY, 82, FOOTER_CONTROL_HEIGHT, "Edit HUD",
                TidalUi.ActionButton.Style.PRIMARY,
                () -> minecraft.setScreen(new TidalHudEditorScreen(this))));
        resetButton = addRenderableWidget(new TidalUi.ActionButton(gridX,
                footerControlY, 120, FOOTER_CONTROL_HEIGHT, "Reset modules",
                TidalUi.ActionButton.Style.CENTERED,
                this::resetModules));
        modsExportButton = addRenderableWidget(new TidalUi.ActionButton(gridX,
                footerControlY, 120, FOOTER_CONTROL_HEIGHT, "Export mod list",
                TidalUi.ActionButton.Style.CONTROL, () -> minecraft.keyboardHandler
                        .setClipboard(TidalModManager.exportableMods().stream()
                                .map(entry -> entry.name + " (" + entry.id + ") "
                                        + entry.version)
                                .collect(java.util.stream.Collectors.joining("\n")))));
        previousButton = addRenderableWidget(new TidalUi.ActionButton(menuX, footerControlY,
                PAGER_BUTTON_WIDTH, FOOTER_CONTROL_HEIGHT, "‹",
                TidalUi.ActionButton.Style.PAGER, () -> {
            page = Math.max(0, page - 1);
            closeInspector();
            refreshContent();
        }));
        nextButton = addRenderableWidget(new TidalUi.ActionButton(menuX, footerControlY,
                PAGER_BUTTON_WIDTH, FOOTER_CONTROL_HEIGHT, "›",
                TidalUi.ActionButton.Style.PAGER, () -> {
            page = Math.min(pageCount - 1, page + 1);
            closeInspector();
            refreshContent();
        }));
    }

    private void resetModules() {
        TidalConfig.uiAnimations = true;
        TidalConfig.advancedDeveloperMode = false;
        TidalConfig.uiAntialiasing = 2;
        TidalConfig.defaultHudStyle = TidalModule.HudStyle.ROUNDED;
        TidalConfig.defaultHudAccent = TidalTheme.DEFAULT_ACCENT;
        TidalConfig.lightTheme = false;
        TidalConfig.themeAccent = TidalTheme.DEFAULT_ACCENT;
        TidalTheme.apply(false, TidalConfig.themeAccent);
        TidalConfig.hudEnabled = true;
        TidalConfig.crosshairSize = 5;
        TidalConfig.crosshairGap = 3;
        TidalConfig.crosshairThickness = 1;
        TidalConfig.crosshairColor = 0xFFFFFFFF;
        TidalConfig.crosshairShape = 0;
        TidalConfig.crosshairOutline = true;
        TidalConfig.dynamicCrosshair = false;
        for (TidalModule module : TidalModules.all()) {
            module.enabled = false;
            module.keyCode = 0;
            module.wasDown = false;
            module.x = 12;
            module.y = 12;
            module.scale = 1.0f;
            module.anchor = TidalModule.Anchor.TOP_LEFT;
            module.locked = false;
            module.hudStyle = TidalModule.HudStyle.ROUNDED;
            module.textColor = TidalTheme.PRIMARY;
            module.accentColor = TidalTheme.ACCENT;
            module.backgroundColor = TidalTheme.SURFACE;
            module.backgroundOpacity = 86;
            module.surfaceGroupId = 0;
            module.intensity = 100;
            if (module.group == TidalModule.Group.HUD) {
                module.hudStyle = TidalConfig.defaultHudStyle;
                module.accentColor = TidalConfig.defaultHudAccent;
            }
            module.normalizedPosition = false;
        }
        TidalConfig.snapToGrid = true;
        TidalConfig.snapToEdges = true;
        TidalHud.initializePositions(width, height);
        TidalConfig.save();
        refreshContent();
    }

    private void refreshContent() {
        syncControlState();
        implementedModules.clear();
        String query = search == null ? searchValue.toLowerCase(Locale.ROOT).trim()
                : search.getValue().toLowerCase(Locale.ROOT).trim();
        installedMods.clear();
        if (tab == Tab.MODS) {
            for (TidalModManager.ModEntry entry : TidalModManager.search(query)) {
                if (modFilter == ModFilter.FAVORITES && !entry.favorite) continue;
                if (modFilter == ModFilter.CONFIGURABLE && !entry.canOpenSettings()) continue;
                if (modFilter == ModFilter.RUNTIME_TOGGLE && !entry.canToggle()) continue;
                installedMods.add(entry);
            }
            entries.clear();
            int perPage = Math.max(1, columns * modRowsPerPage);
            pageCount = Math.max(1, (installedMods.size() + perPage - 1) / perPage);
            page = Math.max(0, Math.min(page, pageCount - 1));
            updateVisibleWidgets();
            updateInspectorLayout();
            return;
        }
        if (tab == Tab.COSMETICS) {
            entries.clear();
            selected = null;
            selectedForRender = null;
            page = 0;
            pageCount = 1;
            updateVisibleWidgets();
            updateInspectorLayout();
            return;
        }
        boolean moduleSearch = !query.isEmpty() && tab != Tab.SETTINGS;
        boolean gridTab = moduleSearch || tab == Tab.MODULES
                || (tab == Tab.HUD && hudSection == HudSection.ELEMENTS);
        if (gridTab) {
            for (TidalModule module : TidalModules.all()) {
                if (!module.implemented || module.hidden) continue;
                if (!moduleSearch) {
                    if (tab == Tab.HUD) {
                        if (module.group != TidalModule.Group.HUD) continue;
                    } else if (!matchesFilter(module, moduleFilter)) {
                        continue;
                    }
                }
                if (moduleSearch && !matchesModuleSearch(module, query)) continue;
                implementedModules.add(module);
            }
        }
        Comparator<TidalModule> byName =
                Comparator.comparing((TidalModule module) -> module.name);
        implementedModules.sort(Comparator.comparing((TidalModule module) -> !module.favorite)
                .thenComparing(byName));

        if (selected == null || !implementedModules.contains(selected)) {
            selected = implementedModules.isEmpty() ? null : implementedModules.get(0);
            selectedForRender = selected;
        }
        rebuildEntries();
        pageCount = Math.max(1, (entryRowCount() + rowsPerPage - 1) / rowsPerPage);
        page = Math.max(0, Math.min(page, pageCount - 1));
        updateVisibleWidgets();
        updateInspectorLayout();
    }

    private void syncControlState() {
        if (crosshairOutlineControl != null) {
            crosshairOutlineControl.setChecked(TidalConfig.crosshairOutline);
        }
        if (dynamicGapControl != null) {
            dynamicGapControl.setChecked(TidalConfig.dynamicCrosshair);
        }
    }

    private static boolean matchesFilter(TidalModule module, ModuleFilter filter) {
        return switch (filter) {
            case ALL -> true;
            case HUD_INFO -> module.group == TidalModule.Group.HUD;
            case COMBAT -> module.group == TidalModule.Group.COMBAT;
            case PLAYER -> module.group == TidalModule.Group.PLAYER;
            case WORLD -> module.group == TidalModule.Group.WORLD;
            case VISUALS -> module.group == TidalModule.Group.VISUAL;
            case MOVEMENT -> module.group == TidalModule.Group.MOVEMENT;
            case PERFORMANCE -> switch (module.id) {
                case "fps", "frame_time", "ping", "memory_usage", "session_timer" -> true;
                default -> false;
            };
        };
    }

    private static boolean matchesModuleSearch(TidalModule module, String query) {
        String aliases = switch (module.id) {
            case "fps" -> "frame rate frames per second performance";
            case "frame_time" -> "frame time frametime render duration milliseconds";
            case "ping" -> "latency ms server connection";
            case "memory_usage" -> "memory ram heap java";
            case "left_cps", "right_cps", "combined_cps" ->
                    "clicks click speed cps mouse";
            case "coordinates" -> "coords position location xyz";
            case "direction", "compass" -> "heading orientation cardinal";
            case "yaw_pitch" -> "rotation camera angles";
            case "health", "absorption" -> "hearts hp life";
            case "hunger", "saturation", "combined_hunger" -> "food nutrition";
            case "potion_effects", "potion_timers" -> "effects buffs status";
            case "held_item", "item_durability" -> "item equipment tool";
            case "ender_pearl_counter" -> "pearls teleport";
            case "toggle_sprint" -> "run sprint movement";
            case "toggle_sneak" -> "crouch sneak movement";
            case "zoom" -> "magnify fov camera";
            case "fullbright" -> "brightness gamma lighting";
            case "view_bobbing" -> "camera shake movement";
            default -> "";
        };
        String searchable = (module.name + " " + module.description + " "
                + module.id.replace('_', ' ') + " " + TidalUi.displayName(module.group)
                + " " + aliases).toLowerCase(Locale.ROOT);
        for (String token : query.split("\\s+")) {
            if (!searchable.contains(token)) return false;
        }
        return true;
    }

    private boolean isGlobalModuleSearch(String query) {
        return !query.isEmpty() && tab != Tab.MODS
                && tab != Tab.COSMETICS && tab != Tab.SETTINGS;
    }

    private SettingsSection matchingSettingsSection(String query) {
        for (var entry : settingsControlSections.entrySet()) {
            if (matchesSearchText(entry.getKey().getMessage().getString(), query)) {
                return entry.getValue();
            }
        }
        for (SettingsSection section : SETTINGS_SECTIONS) {
            if (matchesSearchText(section.label, query)) return section;
        }
        return null;
    }

    private static boolean matchesSearchText(String text, String query) {
        String normalized = text.toLowerCase(Locale.ROOT);
        for (String token : query.split("\\s+")) {
            if (!normalized.contains(token)) return false;
        }
        return true;
    }

    private void rebuildEntries() {
        entries.clear();
        int row = 0;
        int column = 0;
        for (TidalModule module : implementedModules) {
            entries.add(new GridEntry(module, row, column));
            if (++column == columns) {
                column = 0;
                row++;
            }
        }
        if (column != 0) row++;
        if (entries.isEmpty()) row = 0;
        totalEntryRows = row;
    }

    private int totalEntryRows;

    private int entryRowCount() {
        return totalEntryRows;
    }

    private void layoutSettingsControls() {
        int x = gridX + 8;
        int y = gridY + 62 - settingsScrollOffset;
        int bottom = footerY - 10;
        int contentHeight = 0;
        for (AbstractWidget control : settingsControls) {
            if (settingsControlSections.get(control) != settingsSection) continue;
            control.setPosition(x, y);
            contentHeight += control.getHeight() + 12;
            y += control.getHeight() + 12;
        }
        int viewportHeight = Math.max(0, bottom - (gridY + 58));
        settingsScrollOffset = Math.max(0,
                Math.min(settingsScrollOffset, Math.max(0,
                        contentHeight - 12 - viewportHeight)));
        if (settingsScrollOffset > 0) {
            y = gridY + 62 - settingsScrollOffset;
            for (AbstractWidget control : settingsControls) {
                if (settingsControlSections.get(control) != settingsSection) continue;
                control.setPosition(x, y);
                y += control.getHeight() + 12;
            }
        }
    }

    private void updateVisibleWidgets() {
        String query = search == null ? searchValue.trim().toLowerCase(Locale.ROOT)
                : search.getValue().trim().toLowerCase(Locale.ROOT);
        boolean moduleSearch = isGlobalModuleSearch(query);
        if (tab == Tab.SETTINGS) layoutSettingsControls();
        for (TidalUi.ModuleRow card : moduleCards) card.visible = false;
        for (TidalUi.InstalledModRow card : modCards) card.visible = false;

        int sidebarIndex = 0;
        if (tab == Tab.HUD) {
            for (HudSection section : HudSection.values()) {
                if (sidebarIndex >= sidebarButtons.size()) break;
                sidebarButtons.get(sidebarIndex++).highlighted = hudSection == section;
            }
        } else if (tab == Tab.MODULES) {
            for (ModuleFilter filter : ModuleFilter.values()) {
                if (sidebarIndex >= sidebarButtons.size()) break;
                sidebarButtons.get(sidebarIndex++).highlighted = moduleFilter == filter;
            }
        } else if (tab == Tab.MODS) {
            for (ModFilter filter : ModFilter.values()) {
                if (sidebarIndex >= sidebarButtons.size()) break;
                sidebarButtons.get(sidebarIndex++).highlighted = modFilter == filter;
            }
        } else if (tab == Tab.COSMETICS) {
            sidebarIndex = sidebarButtons.size();
        } else if (tab == Tab.SETTINGS) {
            for (SettingsSection section : SETTINGS_SECTIONS) {
                if (sidebarIndex >= sidebarButtons.size()) break;
                sidebarButtons.get(sidebarIndex++).highlighted = settingsSection == section;
            }
        }
        for (TidalUi.ActionButton button : sidebarButtons) button.visible = !moduleSearch;
        if (tab == Tab.SETTINGS && !moduleSearch) layoutSettingsSidebar();
        for (TidalUi.ActionButton button : mainNavigationButtons) button.visible = true;
        for (TidalUi.ActionButton button : mainNavigationButtons) {
            button.highlighted = button.getMessage().getString().equals(switch (tab) {
                case MODULES -> "Modules";
                case MODS -> "Mods";
                case COSMETICS -> "Cosmetics";
                case SETTINGS -> "Settings";
                default -> "";
            });
        }
        fpsBoostControl.setPosition(gridX + 8, gridY + 30);
        fpsBoostControl.setWidth(Math.max(1, gridWidth - 16));
        fpsBoostControl.setChecked(TidalConfig.fpsBoostEnabled);
        fpsBoostControl.visible = tab == Tab.MODS;
        for (TidalUi.ActionButton button : contextButtons) {
            String label = button.getMessage().getString();
            button.highlighted = switch (tab) {
                case MODULES -> java.util.Arrays.stream(ModuleFilter.values())
                        .anyMatch(filter -> filter.label.equals(label)
                                && filter == moduleFilter);
                case MODS -> java.util.Arrays.stream(ModFilter.values())
                        .anyMatch(filter -> filter.label.equals(label) && filter == modFilter);
                case COSMETICS -> java.util.Arrays.stream(CosmeticsSection.values())
                        .anyMatch(section -> section.label.equals(label)
                                && section == cosmeticsSection);
                case SETTINGS -> java.util.Arrays.stream(SETTINGS_SECTIONS)
                        .anyMatch(section -> section.label.equals(label)
                                && section == settingsSection);
                default -> false;
            };
        }

        galleryButton.visible = false;
        galleryButton.setPosition(gridX + TidalUi.CARD_CONTENT_INSET, gridY + 245);
        for (AbstractWidget control : settingsControls) {
            boolean inCategory = !moduleSearch && tab == Tab.SETTINGS
                    && settingsControlSections.get(control) == settingsSection;
            control.visible = inCategory && (query.isEmpty()
                    || matchesSearchText(control.getMessage().getString(), query)
                    || matchesSearchText(settingsSection.label, query))
                    && control.getY() >= gridY + 58
                    && control.getBottom() <= footerY - 10;
        }
        layoutCosmeticsControls();
        for (AbstractWidget control : hudControls) {
            control.visible = !moduleSearch && tab == Tab.HUD
                    && hudControlSections.get(control) == hudSection;
        }

        int firstRow = page * rowsPerPage;
        int cardIndex = 0;
        for (GridEntry entry : entries) {
            int localRow = entry.row - firstRow;
            if (localRow < 0 || localRow >= rowsPerPage) continue;
            if (cardIndex < moduleCards.size()) {
                TidalModule module = entry.module;
                TidalUi.ModuleRow card = moduleCards.get(cardIndex++);
                int x = gridColumnX(entry.column);
                int y = gridY + localRow * ROW_PITCH;
                card.setRectangle(gridColumnWidth(entry.column), CARD_HEIGHT, x, y);
                card.setModule(module, () -> selectModule(module), () -> toggleModule(module));
                card.visible = true;
            }
        }
        int firstMod = page * columns * modRowsPerPage;
        for (int i = 0; i < columns * modRowsPerPage; i++) {
            int entryIndex = firstMod + i;
            if (entryIndex >= installedMods.size() || i >= modCards.size()) break;
            TidalModManager.ModEntry entry = installedMods.get(entryIndex);
            TidalUi.InstalledModRow card = modCards.get(i);
            int row = i / columns;
            int column = i % columns;
            card.setRectangle(gridColumnWidth(column), 76,
                    gridColumnX(column),
                    gridY + 58 + row * 84);
            card.setEntry(entry);
            card.visible = tab == Tab.MODS;
        }

        boolean gridTab = moduleSearch || tab == Tab.MODULES
                || (tab == Tab.HUD && hudSection == HudSection.ELEMENTS);
        hudToggleButton.visible = tab == Tab.HUD && hudSection == HudSection.ELEMENTS;
        editHudButton.visible = !moduleSearch && tab == Tab.HUD
                && hudSection == HudSection.ELEMENTS;
        resetButton.visible = false;
        modsExportButton.visible = tab == Tab.MODS;
        previousButton.visible = gridTab && pageCount > 1;
        nextButton.visible = gridTab && pageCount > 1;
        previousButton.active = page > 0;
        nextButton.active = page + 1 < pageCount;
        footerPageLabel = "Page " + (page + 1) + " of " + pageCount;
        pagerLabelWidth = TidalUi.textWidth(font, footerPageLabel);
        int nextX = menuX + menuWidth - FOOTER_INSET - PAGER_BUTTON_WIDTH;
        int previousX = nextX - FOOTER_CONTROL_GAP - pagerLabelWidth
                - FOOTER_CONTROL_GAP - PAGER_BUTTON_WIDTH;
        previousButton.setPosition(previousX, footerControlY);
        nextButton.setPosition(nextX, footerControlY);
        pagerLabelX = previousX + PAGER_BUTTON_WIDTH + FOOTER_CONTROL_GAP;
        hudToggleButton.setMessage(Component.literal(
                TidalConfig.hudEnabled ? "HUD: On" : "HUD: Off"));
    }

    private void layoutCosmeticsControls() {
        boolean cosmeticsVisible = tab == Tab.COSMETICS && !isGlobalModuleSearch(
                search == null ? searchValue.trim() : search.getValue().trim());
        int contentX = gridX + 8;
        int contentWidth = Math.max(1, gridWidth - 16);
        skinPreviewX = gridX + 10;
        skinPreviewY = gridY + 50;
        skinPreviewWidth = Math.max(1, gridWidth - 20);
        skinPreviewHeight = Math.max(1, gridHeight - 56);

        boolean skinPage = cosmeticsVisible && cosmeticsSection == CosmeticsSection.SKIN;
        zoomOutButton.setPosition(skinPreviewX + skinPreviewWidth - 72,
                skinPreviewY + skinPreviewHeight - 32);
        zoomInButton.setPosition(skinPreviewX + skinPreviewWidth - 36,
                skinPreviewY + skinPreviewHeight - 32);
        zoomOutButton.visible = skinPage && TidalConfig.enableAquaCosmetics
                && TidalConfig.showEquippedSkinIntegration && minecraft.player != null;
        zoomInButton.visible = zoomOutButton.visible;
        boolean controlsPage = cosmeticsVisible
                && cosmeticsSection == CosmeticsSection.CONTROLS;
        enableAquaCosmeticsControl.setPosition(contentX, gridY + 72);
        enableAquaCosmeticsControl.setWidth(contentWidth);
        enableAquaCosmeticsControl.setChecked(TidalConfig.enableAquaCosmetics);
        equippedSkinIntegrationControl.setPosition(contentX, gridY + 124);
        equippedSkinIntegrationControl.setWidth(contentWidth);
        equippedSkinIntegrationControl.setChecked(TidalConfig.showEquippedSkinIntegration);
        for (AbstractWidget control : cosmeticsControls) control.visible = controlsPage;
    }

    private void selectModule(TidalModule module) {
        selected = module;
        selectedForRender = module;
        inspectorOpen = true;
        capturingKey = false;
        updateInspectorLayout();
    }

    private void toggleModule(TidalModule module) {
        module.enabled = !module.enabled;
        if (module.enabled) TidalHud.placeInAvailableSpace(module);
        TidalConfig.save();
        updateVisibleWidgets();
    }

    private void updateInspectorLayout() {
        if (inspector == null) return;
        TidalUi.ModuleRow anchor = null;
        for (TidalUi.ModuleRow card : moduleCards) {
            if (card.visible && card.getMessage().getString().equals(
                    selected == null ? "" : selected.name)) {
                anchor = card;
                break;
            }
        }
        int inspectorWidth = Math.min(224, Math.max(180,
                menuWidth - GRID_INSET * 2));
        int descriptionLines = selected == null ? 1 : TidalUi.wrapText(font,
                selected.description, inspectorWidth - INSPECTOR_INSET * 2).size();
        int desiredHeight = selected != null && selected.implemented
                ? selected.id.equals("custom_crosshair") ? 300
                : selected.id.equals("fullbright") || selected.id.equals("zoom") ? 204
                : Math.max(144, 108 + descriptionLines * font.lineHeight)
                : Math.max(106, 80 + descriptionLines * font.lineHeight);
        int inspectorHeight = Math.min(desiredHeight,
                Math.max(1, footerY - gridY - 8));
        int x = menuX + menuWidth - inspectorWidth - 10;
        int y = gridY + 8;
        if (anchor != null) {
            int rightX = anchor.getRight() + GRID_GAP;
            int leftX = anchor.getX() - inspectorWidth - GRID_GAP;
            if (rightX + inspectorWidth <= menuX + menuWidth - INSPECTOR_INSET) x = rightX;
            else if (leftX >= menuX + INSPECTOR_INSET) x = leftX;
            else x = Math.max(menuX + INSPECTOR_INSET,
                    menuX + menuWidth - inspectorWidth - INSPECTOR_INSET);
            y = anchor.getY() - 4;
        }
        x = Math.max(menuX + INSPECTOR_INSET,
                Math.min(x, menuX + menuWidth - inspectorWidth - INSPECTOR_INSET));
        y = Math.max(gridY, Math.min(y, footerY - inspectorHeight - 4));
        inspector.setContext(selected, inspectorOpen, x, y, inspectorWidth, inspectorHeight);
        layoutModulePreferenceControls(selected, inspectorOpen, x, y, inspectorWidth);
    }

    private void layoutModulePreferenceControls(TidalModule module, boolean open,
                                                int x, int y, int width) {
        int contentX = x + INSPECTOR_INSET;
        int contentWidth = Math.max(1, width - INSPECTOR_INSET * 2);
        int halfWidth = Math.max(1, (contentWidth - 8) / 2);
        int rowY = y + 80;
        int[] rowOffsets = {0, 56, 56, 98, 140, 140, 172};
        int[] xOffsets = {0, 0, halfWidth + 8, 0, 0, halfWidth + 8, 0};
        for (int i = 0; i < modulePreferenceControls.size(); i++) {
            AbstractWidget control = modulePreferenceControls.get(i);
            if (i < 7) {
                control.setPosition(contentX + xOffsets[i], rowY + rowOffsets[i]);
                control.visible = open && module != null
                        && module.id.equals("custom_crosshair");
            } else {
                control.setPosition(contentX, rowY);
                control.visible = open && module != null
                        && (i == 7 && module.id.equals("fullbright")
                        || i == 8 && module.id.equals("zoom"));
            }
        }
    }

    private void closeInspector() {
        inspectorOpen = false;
        capturingKey = false;
        if (inspector != null) {
            inspector.setContext(selected, false, inspector.getX(), inspector.getY(),
                    inspector.getWidth(), inspector.getHeight());
            layoutModulePreferenceControls(selected, false, inspector.getX(), inspector.getY(),
                    inspector.getWidth());
        }
    }

    private static String keyName(int keyCode) {
        if (keyCode == 0) return "Unbound";
        if (keyCode < 0) return "Mouse " + (-keyCode);
        return InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName().getString();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
                                   float delta) {
        TidalMotion.beginFrame();
        float progress = transitionProgress();
        TidalUi.setFrameAlpha(progress);
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(width / 2.0f, height / 2.0f
                + TRANSITION_OFFSET_Y * (1.0f - progress));
        float scale = TRANSITION_SCALE + (1.0f - TRANSITION_SCALE) * progress;
        pose.scale(scale, scale);
        pose.translate(-width / 2.0f, -height / 2.0f);
        TidalUi.drawAsset(graphics, "tidal/ui/polyglass_window_frame",
                menuX, menuY, menuWidth, menuHeight,
                TidalUi.withAlpha(0xFFFFFFFF, TidalTheme.Glass.SURFACE_OPACITY));
        if (sidebarWidth > 0) {
            int sidebarOverlayAlpha = Math.round((TidalTheme.Glass.SIDEBAR_OPACITY
                    - TidalTheme.Glass.SURFACE_OPACITY) * 255.0f
                    / (255 - TidalTheme.Glass.SURFACE_OPACITY));
            int surface = TidalUi.withAlpha(TidalTheme.SURFACE, sidebarOverlayAlpha);
            graphics.fill(menuX + TidalTheme.Radius.WINDOW, menuY + TidalTheme.Radius.WINDOW,
                    menuX + sidebarWidth, menuY + menuHeight - TidalTheme.Radius.WINDOW,
                    surface);
        }
        int logoSize = 30;
        int logoX = menuX + 14;
        int logoY = menuY + 14;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BRAND_LOGO, logoX, logoY, 0, 0,
                logoSize, logoSize, 1920, 1920, 1920, 1920, 0xFFFFFFFF);
        int brandTextX = logoX + logoSize + 7;
        int brandTextWidth = Math.max(1, menuX + sidebarWidth - brandTextX - 10);
        TidalUi.strongLabel(graphics, font, TidalUi.fitText(font, "TIDAL", brandTextWidth),
                brandTextX, menuY + 14,
                TidalUi.PRIMARY);
        TidalUi.label(graphics, font,
                TidalUi.fitText(font, "by Graphine Studios", brandTextWidth),
                brandTextX, menuY + 30,
                TidalUi.SECONDARY);
        graphics.fill(menuX + 10, menuY + HEADER_DIVIDER_Y,
                menuX + menuWidth - 10, menuY + HEADER_DIVIDER_Y + 1,
                TidalTheme.BORDER);
        graphics.fill(menuX + sidebarWidth, menuY + HEADER_DIVIDER_Y + 1,
                menuX + sidebarWidth + 1, footerY - 5, TidalTheme.BORDER_ELEVATED);
        graphics.fill(gridX, footerY - 5, gridX + gridWidth, footerY - 4,
                TidalTheme.BORDER_ELEVATED);
        UUID accountId = minecraft.player == null
                ? minecraft.getUser().getProfileId() : minecraft.player.getUUID();
        String accountName = minecraft.player == null
                ? minecraft.getUser().getName() : minecraft.player.getName().getString();
        TidalAquaIntegration.AccountStatus aquaStatus = TidalClient.aquaAccountStatus();
        if (aquaStatus == TidalAquaIntegration.AccountStatus.CONNECTED) {
            String aquaUsername = TidalClient.authenticatedAquaUsername();
            if (aquaUsername != null && !aquaUsername.isBlank()) accountName = aquaUsername;
        }
        String accountStatusLabel = switch (aquaStatus) {
            case CONNECTED -> "AQUA · CONNECTED";
            case NOT_SIGNED_IN -> "AQUA · NOT SIGNED IN";
            case SESSION_EXPIRED -> "AQUA · SESSION EXPIRED";
            case CONNECTION_UNAVAILABLE -> "AQUA · CONNECTION UNAVAILABLE";
        };
        int accountY = footerY - 13;
        int accountX = menuX + 8;
        int accountWidth = Math.max(1, sidebarWidth - 16);
        TidalUi.panel(graphics, accountX, accountY, accountWidth, 34,
                TidalTheme.ELEVATED, 5);
        var playerSkin = minecraft.player == null ? null : minecraft.player.getSkin();
        Identifier skinTexture = playerSkin == null || playerSkin.body() == null
                ? null : playerSkin.body().texturePath();
        if (skinTexture == null) {
            skinTexture = DefaultPlayerSkin.getDefaultSkin().body().texturePath();
        }
        if (!accountId.equals(profileSkinOwner) || !skinTexture.equals(profileSkinTexture)) {
            profileSkinOwner = accountId;
            profileSkinTexture = skinTexture;
        }
        int headX = accountX + 8;
        int headY = accountY + 5;
        drawRoundedSkinHead(graphics, profileSkinTexture, headX, headY, 24);
        int profileTextX = headX + 32;
        int profileTextWidth = Math.max(1, accountX + accountWidth - profileTextX - 7);
        TidalUi.label(graphics, font,
                TidalUi.fitText(font, accountStatusLabel, profileTextWidth), profileTextX,
                accountY + 4, TidalTheme.MUTED);
        TidalUi.strongLabel(graphics, font,
                TidalUi.fitText(font, accountName, profileTextWidth),
                profileTextX, accountY + 18, TidalUi.PRIMARY);

        String query = search == null ? searchValue.trim().toLowerCase(Locale.ROOT)
                : search.getValue().trim().toLowerCase(Locale.ROOT);
        boolean globalModuleSearch = isGlobalModuleSearch(query);
        if (!globalModuleSearch && tab == Tab.COSMETICS) {
            drawCosmeticsPage(graphics, mouseX, mouseY, delta);
        } else if (!globalModuleSearch && tab == Tab.SETTINGS) {
            drawStaticCategory(graphics);
        } else if (!globalModuleSearch && tab == Tab.MODS) {
            String modsTitle = "Features";
            String modsDetail = "FPS Boost is a master toggle; optimization hooks are not implemented yet.";
            TidalUi.strongLabel(graphics, font, modsTitle, gridX + 4,
                    gridY + 4, TidalUi.PRIMARY);
            TidalUi.label(graphics, font, TidalUi.fitText(font, modsDetail,
                            Math.max(0, gridWidth - 8)),
                    gridX + 4, gridY + 18, TidalUi.SECONDARY);
            if (installedMods.isEmpty()) {
                TidalUi.strongLabel(graphics, font,
                        searchValue.isBlank() ? "No mods in this view."
                                : "No mods match this search.",
                        gridX + 4, gridY + 64, TidalUi.SECONDARY);
            }

        } else if (!globalModuleSearch && tab == Tab.HUD
                && hudSection == HudSection.LAYOUTS) {
            drawSectionIntro(graphics, "HUD layouts",
                    "Position, snap, and reset the elements shown in game.");
        } else if (!globalModuleSearch && tab == Tab.HUD
                && hudSection == HudSection.APPEARANCE) {
            drawSectionIntro(graphics, "HUD appearance",
                    "Surface style and accent color for every HUD element.");
        } else if (implementedModules.isEmpty() && tab != Tab.MODS
                && tab != Tab.COSMETICS) {
            boolean searching = search == null ? !searchValue.isBlank()
                    : !search.getValue().isBlank();
            String title = searching ? "No modules match this search."
                    : tab == Tab.MODULES ? "No available modules in this category."
                    : "No HUD modules are available.";
            String detail = searching ? "Try a different name or clear the search."
                    : "Change the category or search for another feature.";
            TidalUi.strongLabel(graphics, font, title, gridX + 4, gridY + 8,
                    TidalUi.SECONDARY);
            TidalUi.label(graphics, font, detail, gridX + 4,
                    gridY + 8 + font.lineHeight + 3, TidalUi.MUTED);
        }
        super.render(graphics, mouseX, mouseY, delta);
        if ((tab == Tab.HUD && hudSection == HudSection.ELEMENTS
                || tab == Tab.MODULES || tab == Tab.MODS) && pageCount > 1) {
            TidalUi.centeredLabel(graphics, font, footerPageLabel,
                    pagerLabelX + pagerLabelWidth / 2,
                    footerControlY + (FOOTER_CONTROL_HEIGHT - font.lineHeight) / 2,
                    TidalUi.SECONDARY);
        }

        pose.popMatrix();
        TidalUi.setFrameAlpha(1.0f);
    }

    private static void drawRoundedSkinHead(GuiGraphics graphics, Identifier texture,
                                            int x, int y, int size) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 8, 8,
                size, size, 8, 8, 64, 64, 0xFFFFFFFF);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 40, 8,
                size, size, 8, 8, 64, 64, 0xFFFFFFFF);
        int[] cornerInsets = {3, 2, 1, 0};
        for (int row = 0; row < cornerInsets.length; row++) {
            int inset = cornerInsets[row];
            int mask = TidalTheme.ELEVATED;
            graphics.fill(x, y + row, x + inset, y + row + 1, mask);
            graphics.fill(x + size - inset, y + row, x + size, y + row + 1, mask);
            graphics.fill(x, y + size - row - 1,
                    x + inset, y + size - row, mask);
            graphics.fill(x + size - inset, y + size - row - 1,
                    x + size, y + size - row, mask);
        }
    }

    private void drawCosmeticsPage(GuiGraphics graphics, int mouseX, int mouseY,
                                   float delta) {
        String title = cosmeticsSection.label;
        String subtitle = switch (cosmeticsSection) {
            case SKIN -> "Preview the Minecraft skin currently equipped by this player.";
            case CONTROLS -> "Control which equipped-skin previews Tidal displays.";
        };
        TidalUi.strongLabel(graphics, font, title, gridX + 8, gridY + 4, TidalUi.PRIMARY);
        TidalUi.label(graphics, font, TidalUi.fitText(font, subtitle,
                Math.max(1, gridWidth - 16)), gridX + 8, gridY + 20, TidalUi.SECONDARY);

        if (cosmeticsSection == CosmeticsSection.SKIN) {
            TidalUi.panel(graphics, skinPreviewX, skinPreviewY,
                    skinPreviewWidth, skinPreviewHeight, TidalTheme.ELEVATED, 8);
            if (!TidalConfig.enableAquaCosmetics) {
                TidalUi.centeredLabel(graphics, font, "Aqua Cosmetics are disabled",
                        skinPreviewX + skinPreviewWidth / 2,
                        skinPreviewY + skinPreviewHeight / 2 - font.lineHeight,
                        TidalUi.SECONDARY);
                return;
            }
            if (!TidalConfig.showEquippedSkinIntegration) {
                TidalUi.centeredLabel(graphics, font, "Equipped skin preview is hidden",
                        skinPreviewX + skinPreviewWidth / 2,
                        skinPreviewY + skinPreviewHeight / 2 - font.lineHeight,
                        TidalUi.SECONDARY);
                return;
            }
            if (minecraft.player == null) {
                Identifier fallback = DefaultPlayerSkin.getDefaultSkin().body().texturePath();
                drawRoundedSkinHead(graphics, fallback,
                        skinPreviewX + (skinPreviewWidth - 42) / 2,
                        skinPreviewY + (skinPreviewHeight - 42) / 2 - 12, 42);
                TidalUi.centeredLabel(graphics, font, "Join a world to preview your equipped skin",
                        skinPreviewX + skinPreviewWidth / 2,
                        skinPreviewY + skinPreviewHeight - 48, TidalUi.SECONDARY);
                return;
            }
            EntityRenderState renderState = minecraft.getEntityRenderDispatcher()
                    .extractEntity(minecraft.player, delta);
            renderState.shadowPieces.clear();
            renderState.outlineColor = EntityRenderState.NO_OUTLINE;
            if (renderState instanceof LivingEntityRenderState living) {
                living.bodyRot = 180.0f + previewRotation;
                living.yRot = previewRotation;
                living.xRot = 0.0f;
            }
            graphics.submitEntityRenderState(renderState, previewZoom,
                    new Vector3f(0.0f, 0.0f, 0.0f),
                    new Quaternionf().rotateZ((float) Math.PI), new Quaternionf(),
                    skinPreviewX + 8, skinPreviewY + 22,
                    skinPreviewX + skinPreviewWidth - 8,
                    skinPreviewY + skinPreviewHeight - 34);
            String model = minecraft.player.getSkin() == null ? "Default"
                    : TidalUi.displayWords(minecraft.player.getSkin().model().toString());
            TidalUi.label(graphics, font, "Equipped Minecraft skin · " + model,
                    skinPreviewX + 12, skinPreviewY + 8, TidalUi.SECONDARY);
        } else {
            TidalUi.label(graphics, font,
                    "Enable Aqua Cosmetics controls the preview; the skin option affects only this page.",
                    gridX + 8, gridY + 52, TidalUi.SECONDARY);
        }
    }

    private void drawSectionIntro(GuiGraphics graphics, String title, String detail) {
        TidalUi.strongLabel(graphics, font, title, gridX + 4, gridY + 8, TidalUi.PRIMARY);
        TidalUi.label(graphics, font, TidalUi.fitText(font, detail,
                Math.max(0, gridWidth - 8)), gridX + 4, gridY + 26, TidalUi.SECONDARY);
    }

    private float transitionProgress() {
        if (!TidalConfig.uiAnimations) return closingAt == 0 ? 1.0f : 0.0f;
        long start = closingAt == 0 ? openedAt : closingAt;
        float raw = Math.max(0.0f, Math.min(1.0f,
                (TidalUi.nowMillis() - start) / (float) TRANSITION_MILLIS));
        float eased = 1.0f - (float) Math.pow(1.0f - raw, 3.0);
        return closingAt == 0 ? eased : 1.0f - eased;
    }

    private void drawStaticCategory(GuiGraphics graphics) {
        int contentX = gridX + TidalUi.CARD_CONTENT_INSET;
        String title = settingsSection.label;
        String description = switch (settingsSection) {
            case GENERAL -> "Save, back up, export, or restore your client configuration.";
            case GRAPHICS -> "Tidal texture quality and Minecraft video settings.";
            case INPUT -> "Search and rebind Tidal modules; manage core keys in Minecraft Controls.";
        };
        TidalUi.strongLabel(graphics, font, title, contentX, gridY + 8, TidalUi.PRIMARY);
        TidalUi.label(graphics, font, description, contentX, gridY + 26,
                TidalUi.SECONDARY);
        String query = search == null ? searchValue.trim().toLowerCase(Locale.ROOT)
                : search.getValue().trim().toLowerCase(Locale.ROOT);
        if (!query.isEmpty()) {
            boolean anyVisible = false;
            for (AbstractWidget control : settingsControls) {
                if (control.visible) {
                    anyVisible = true;
                    break;
                }
            }
            if (!anyVisible) {
                TidalUi.strongLabel(graphics, font, "No settings match this search.",
                        contentX, gridY + 60, TidalUi.SECONDARY);
                TidalUi.label(graphics, font, "Clear the search to see every option.",
                        contentX, gridY + 78, TidalUi.MUTED);
            }
        }
    }

    private void startClosing() {
        NAVIGATION_BACK_STACK.clear();
        closingAt = TidalUi.nowMillis();
        closeInspector();
    }

    @Override
    public void tick() {
        super.tick();
        if (closingAt != 0 && TidalUi.nowMillis() - closingAt >= TRANSITION_MILLIS) {
            minecraft.setScreen(returnScreen);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (closingAt != 0) return true;
        if (event.key() == 256) {
            startClosing();
            return true;
        }
        if (capturingKey && (bindingTarget != null || selected != null)) {
            TidalModule target = bindingTarget != null ? bindingTarget : selected;
            target.keyCode = event.key();
            capturingKey = false;
            bindingTarget = null;
            TidalConfig.save();
            refreshContent();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
                                 double verticalAmount) {
        if (closingAt == 0 && tab == Tab.COSMETICS
                && cosmeticsSection == CosmeticsSection.SKIN
                && isInsideSkinPreview(mouseX, mouseY)) {
            previewZoom = Math.max(50, Math.min(110,
                    previewZoom + (verticalAmount > 0 ? 8 : -8)));
            return true;
        }
        if (closingAt == 0 && inspectorOpen && inspector.isMouseOver(mouseX, mouseY)) {
            return true;
        }
        if (closingAt == 0 && tab == Tab.SETTINGS
                && mouseX >= gridX && mouseX <= gridX + gridWidth
                && mouseY >= gridY + 58 && mouseY < footerY - 5) {
            settingsScrollOffset = Math.max(0,
                    settingsScrollOffset + (verticalAmount < 0 ? 32 : -32));
            updateVisibleWidgets();
            return true;
        }
        if (closingAt == 0 && mouseX >= gridX && mouseX <= gridX + gridWidth
                && mouseY >= gridY && mouseY <= footerY - 5
                && pageCount > 1) {
            page = Math.max(0, Math.min(pageCount - 1,
                    page + (verticalAmount < 0 ? 1 : -1)));
            closeInspector();
            refreshContent();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (closingAt != 0) return true;
        if (capturingKey) {
            if (bindingTarget != null) {
                bindingTarget.keyCode = -(event.button() + 1);
                TidalConfig.save();
                refreshContent();
            }
            capturingKey = false;
            bindingTarget = null;
            return true;
        }
        if (tab == Tab.COSMETICS && cosmeticsSection == CosmeticsSection.SKIN
                && event.button() == 0 && isInsideSkinModel(event.x(), event.y())
                && !zoomOutButton.isMouseOver(event.x(), event.y())
                && !zoomInButton.isMouseOver(event.x(), event.y())) {
            draggingSkinPreview = true;
            return true;
        }
        if (inspectorOpen) {
            for (int i = modulePreferenceControls.size() - 1; i >= 0; i--) {
                AbstractWidget control = modulePreferenceControls.get(i);
                if (control.visible && control.isMouseOver(event.x(), event.y())) {
                    if (control.mouseClicked(event, doubleClick)) {
                        setFocused(control);
                        if (event.button() == 0) setDragging(true);
                    }
                    return true;
                }
            }
            if (inspector.contains(event.x(), event.y())) {
                if (inspector.mouseClicked(event, doubleClick)) {
                    setFocused(inspector);
                    if (event.button() == 0) setDragging(true);
                }
                return true;
            }
            closeInspector();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (draggingSkinPreview && event.button() == 0) {
            previewRotation = (previewRotation
                    + (float) deltaX * 0.8f) % 360.0f;
            if (previewRotation < 0.0f) previewRotation += 360.0f;
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingSkinPreview && event.button() == 0) {
            draggingSkinPreview = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    private boolean isInsideSkinPreview(double mouseX, double mouseY) {
        return mouseX >= skinPreviewX && mouseX < skinPreviewX + skinPreviewWidth
                && mouseY >= skinPreviewY && mouseY < skinPreviewY + skinPreviewHeight;
    }

    private boolean isInsideSkinModel(double mouseX, double mouseY) {
        return isInsideSkinPreview(mouseX, mouseY)
                && mouseY >= skinPreviewY + 22
                && mouseY < skinPreviewY + skinPreviewHeight - 34;
    }

    @Override
    public void onClose() {
        TidalConfig.save();
        minecraft.setScreen(returnScreen);
    }

    private final class Inspector extends AbstractWidget {
        private TidalModule module;
        private boolean requestedOpen;
        private float progress;
        private final TidalMotion.Spring spring = new TidalMotion.Spring(0.0f);
        private Runnable closeAction;
        private long opened;

        private Inspector() {
            super(menuX, menuY, 1, 1, Component.literal("Module details"));
            visible = false;
            active = false;
            closeAction = TidalScreen.this::closeInspector;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            for (AbstractWidget control : modulePreferenceControls) {
                if (control.visible && control.isMouseOver(mouseX, mouseY)) return false;
            }
            return super.isMouseOver(mouseX, mouseY);
        }

        private void setContext(TidalModule module, boolean open, int x, int y,
                                int width, int height) {
            this.module = module;
            requestedOpen = open && module != null;
            setRectangle(width, height, x, y);
            active = requestedOpen;
            visible = requestedOpen || progress > 0.02f;
            setMessage(Component.literal(module == null ? "Module details" : module.name));
        }

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= getX() && mouseX < getRight()
                    && mouseY >= getY() && mouseY < getBottom();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                                                int mouseY, float delta) {
            if (progress > 0.02f || requestedOpen) {
                graphics.nextStratum();
                graphics.fill(0, 0, TidalScreen.this.width, TidalScreen.this.height,
                        TidalUi.withAlpha(0xFF000000,
                                Math.round(progress * 0xA8)));
            }
            graphics.nextStratum();
            float target = requestedOpen ? 1.0f : 0.0f;
            progress = spring.step(target, TidalTheme.Motion.PANEL_STIFFNESS,
                    TidalTheme.Motion.SPRING_DAMPING);
            if (!requestedOpen && progress < 0.02f) {
                progress = 0.0f;
                visible = false;
                return;
            }
            int slide = Math.round((1.0f - progress) * 8.0f);
            int renderX = getX() + slide;
            TidalUi.drawAsset(graphics, "tidal/ui/polyglass_module_card",
                    renderX, getY(), getWidth(), getHeight());
            if (isFocused()) {
                TidalUi.drawAsset(graphics, "borders/focus-ring-r12", renderX - 2,
                        getY() - 2, getWidth() + 4, getHeight() + 4);
            }
            if (module != null) {
                graphics.enableScissor(renderX + 2, getY() + 2,
                        renderX + getWidth() - 2, getBottom() - 2);
                drawDetails(graphics, renderX, mouseX, mouseY);
                graphics.disableScissor();
            }
        }

        private void drawDetails(GuiGraphics graphics, int renderX,
                                 int mouseX, int mouseY) {
            int x = renderX + INSPECTOR_INSET;
            int y = getY() + 9;
            String title = TidalUi.fitText(font, module.name,
                    getWidth() - INSPECTOR_INSET * 2 - 86);
            TidalUi.strongLabel(graphics, font, title, x, y, TidalUi.PRIMARY);
            int closeX = renderX + getWidth() - 30;
            int closeY = getY() + 4;
            int favoriteX = closeX - 26;
            int hiddenX = favoriteX - 26;
            drawInspectorAction(graphics, favoriteX, closeY,
                    module.favorite ? "success" : "select", module.favorite,
                    mouseX, mouseY);
            drawInspectorAction(graphics, hiddenX, closeY,
                    module.hidden ? "hidden" : "visibility", module.hidden,
                    mouseX, mouseY);
            boolean closeHovered = mouseX >= closeX && mouseX < closeX + 24
                    && mouseY >= closeY && mouseY < closeY + 24;
            TidalUi.drawAsset(graphics, closeHovered ? "buttons/button-secondary-hover"
                            : "buttons/button-secondary-normal",
                    closeX, closeY, 24, 24);
            TidalUi.drawIcon(graphics, "close", closeX + 4, closeY + 4,
                    closeHovered ? TidalUi.PRIMARY : TidalUi.SECONDARY);
            TidalUi.label(graphics, font, TidalUi.displayName(module.group), x, y + 17,
                    TidalUi.SECONDARY);
            int descriptionLines = TidalUi.wrappedLabel(graphics, font, module.description,
                    x, y + 33, getWidth() - INSPECTOR_INSET * 2,
                    module.implemented ? TidalUi.SECONDARY : TidalUi.MUTED);
            int statusY = y + 33 + descriptionLines * font.lineHeight + 4;
            String status = switch (module.status) {
                case IMPLEMENTED -> module.enabled ? "Enabled" : "Disabled";
                case PARTIAL -> module.enabled
                        ? "Partially implemented · Enabled" : "Partially implemented";
                case PLANNED -> "Planned · Unavailable";
            };
            int statusColor = module.status == TidalModule.Status.IMPLEMENTED
                    ? TidalUi.ACCENT : module.status == TidalModule.Status.PARTIAL
                    ? TidalUi.WARNING : TidalUi.MUTED;
            String statusIcon = module.status == TidalModule.Status.IMPLEMENTED
                    ? module.enabled ? "success" : null
                    : module.status == TidalModule.Status.PARTIAL ? "warning" : "info";
            if (statusIcon != null) {
                TidalUi.drawIcon(graphics, statusIcon, x, statusY - 2, statusColor);
            }
            if (statusIcon != null) {
                TidalUi.label(graphics, font,
                        TidalUi.fitText(font, status, getWidth() - INSPECTOR_INSET * 2 - 19),
                        x + 19, statusY, statusColor);
            } else {
                TidalUi.label(graphics, font,
                        TidalUi.fitText(font, status, getWidth() - INSPECTOR_INSET * 2),
                        x, statusY, statusColor);
            }
            if (!module.implemented || module.id.equals("custom_crosshair")) return;
            int controlY = getBottom() - 52;
            int controlWidth = getWidth() - INSPECTOR_INSET * 2;
            boolean keyControlHovered = mouseX >= x && mouseX < x + controlWidth
                    && mouseY >= controlY && mouseY < controlY + 27;
            drawControl(graphics, x, controlY, getWidth() - INSPECTOR_INSET * 2, 27,
                    capturingKey ? "Press key · Esc clears"
                            : "Key  ·  " + keyName(module.keyCode),
                    keyControlHovered, capturingKey);
            TidalUi.label(graphics, font, "HUD scale  ·  "
                    + Math.round(module.scale * 100) + "%", x, getBottom() - 20,
                    TidalUi.SECONDARY);
        }

        private void drawControl(GuiGraphics graphics, int x, int y, int width,
                                 int height, String label, boolean hovered, boolean focused) {
            TidalUi.drawAsset(graphics, focused ? "buttons/button-secondary-pressed"
                            : hovered ? "buttons/button-secondary-hover"
                            : "buttons/button-secondary-normal",
                    x, y, width, height);
            if (focused) {
                TidalUi.drawAsset(graphics, "borders/focus-ring-r7", x - 2, y - 2,
                        width + 4, height + 4);
            }
            TidalUi.drawAsset(graphics, "icons/keybind", x + 7,
                    y + (height - 16) / 2, 16, 16, TidalUi.SECONDARY);
            TidalUi.label(graphics, font, label, x + 27, y + (height - font.lineHeight) / 2,
                    TidalUi.PRIMARY);
        }

        private void drawInspectorAction(GuiGraphics graphics, int x, int y,
                                         String icon, boolean active,
                                         int mouseX, int mouseY) {
            boolean hovered = mouseX >= x && mouseX < x + 22
                    && mouseY >= y && mouseY < y + 22;
            TidalUi.drawAsset(graphics, hovered || active
                            ? "buttons/button-secondary-hover" : "buttons/button-secondary-normal",
                    x, y, 22, 22);
            TidalUi.drawIcon(graphics, icon, x + 3, y + 3,
                    active ? TidalUi.ACCENT : hovered ? TidalUi.PRIMARY : TidalUi.SECONDARY);
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            if (module == null) return;
            if (event.x() >= getRight() - 30 && event.y() >= getY() + 4
                    && event.y() < getY() + 28) {
                closeAction.run();
                return;
            }
            if (event.y() >= getY() + 4 && event.y() < getY() + 26
                    && event.x() >= getRight() - 82 && event.x() < getRight() - 60) {
                module.favorite = !module.favorite;
                TidalConfig.save();
                refreshContent();
                return;
            }
            if (event.y() >= getY() + 4 && event.y() < getY() + 26
                    && event.x() >= getRight() - 108 && event.x() < getRight() - 86) {
                module.hidden = !module.hidden;
                TidalConfig.save();
                closeInspector();
                refreshContent();
                return;
            }
            if (!module.implemented) return;
            int relativeY = (int) event.y() - getY();
            if (relativeY >= getBottom() - 53 && relativeY < getBottom() - 25) {
                bindingTarget = module;
                capturingKey = true;
            } else if (relativeY >= getBottom() - 22 && relativeY < getBottom() - 8) {
                module.scale = module.scale >= 1.5f ? 0.75f : module.scale + 0.25f;
                TidalConfig.save();
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
