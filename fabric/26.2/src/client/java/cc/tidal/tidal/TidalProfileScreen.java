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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class TidalProfileScreen extends Screen {
    private final Screen returnScreen;

    TidalProfileScreen(Screen returnScreen) {
        super(Component.literal("Tidal Profile"));
        this.returnScreen = returnScreen;
    }

    @Override
    protected void init() {
        int buttonWidth = Math.max(1, Math.min(110, width - 32));
        addRenderableWidget(new TidalUi.ActionButton(
                (width - buttonWidth) / 2, height - 32, buttonWidth, 26,
                "Back", TidalUi.ActionButton.Style.CONTROL,
                () -> Minecraft.getInstance().gui.setScreen(returnScreen)));
        setInitialFocus(children().get(children().size() - 1));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                   float delta) {
        extractPanorama(graphics, delta);
        graphics.fill(0, 0, width, height, 0xB000070D);
        int panelWidth = Math.max(1, Math.min(440, width - 32));
        int panelHeight = Math.max(48, Math.min(190, height - 52));
        int panelX = (width - panelWidth) / 2;
        int panelY = Math.max(8, Math.min(20, (height - panelHeight) / 2 - 4));
        TidalUi.cleanCard(graphics, panelX, panelY, panelWidth, panelHeight);
        TidalUi.label(graphics, font, "TIDAL PROFILE", panelX + 12, panelY + 4,
                TidalUi.PRIMARY);
        TidalUi.label(graphics, font, Minecraft.getInstance().getUser().getName(),
                panelX + 12, panelY + 16, TidalUi.PRIMARY);
        TidalUi.label(graphics, font, "Aqua account",
                panelX + 12, panelY + 28,
                TidalUi.SECONDARY);
        TidalAquaIntegration.AccountStatus status = TidalClient.aquaAccountStatus();
        String aquaStatus = switch (status) {
            case CONNECTED -> {
                String username = TidalClient.authenticatedAquaUsername();
                yield username == null || username.isBlank()
                        ? "Connected" : "Connected: " + username;
            }
            case NOT_SIGNED_IN -> "Not signed in";
            case SESSION_EXPIRED -> "Session expired";
            case CONNECTION_UNAVAILABLE -> "Connection unavailable";
        };
        TidalUi.label(graphics, font, aquaStatus, panelX + 12, panelY + 38,
                TidalUi.PRIMARY);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(returnScreen);
    }
}
