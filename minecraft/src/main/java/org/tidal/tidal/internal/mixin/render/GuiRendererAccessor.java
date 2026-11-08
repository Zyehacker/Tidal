package cc.tidal.tidal.internal.mixin.render;

//? if >= 1.21.8 {
import net.minecraft.client.gui.render.GuiRenderer;
//? if >= 26.1 {
import net.minecraft.client.renderer.state.gui.GuiRenderState;
//? } else {
/*import net.minecraft.client.gui.render.state.GuiRenderState;
*///? }
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiRenderer.class)
public interface GuiRendererAccessor {
    @Accessor("renderState")
    GuiRenderState tidal$getRenderState();

    @Mutable
    @Accessor("renderState")
    void tidal$setRenderState(GuiRenderState state);
}
//? }
