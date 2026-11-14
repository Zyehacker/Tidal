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

package cc.tidal.tidal.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.gui.font.FontTexture;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(FontTexture.class)
public abstract class TidalFontTextureMixin {
    @Unique
    private static final String TIDAL_UI_FONT_PREFIX = "tidal:ui";

    @Inject(method = "<init>", at = @At("TAIL"))
    private void tidal$filterSupersampledUiGlyphs(Supplier<String> nameSupplier,
                                                   GlyphRenderTypes renderTypes,
                                                   boolean colored,
                                                   CallbackInfo callback) {
        if (nameSupplier.get().startsWith(TIDAL_UI_FONT_PREFIX)) {
            ((TidalTextureSamplerAccessor) (Object) this).tidal$setSampler(
                    RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR));
        }
    }
}
