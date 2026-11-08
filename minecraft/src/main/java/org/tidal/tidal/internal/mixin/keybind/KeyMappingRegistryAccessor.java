package cc.tidal.tidal.internal.mixin.keybind;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(KeyMapping.class)
public interface KeyMappingRegistryAccessor {
    @SuppressWarnings("rawtypes")
    @Accessor("ALL")
    static Map tidal$all() {
        throw new AssertionError();
    }

    @SuppressWarnings("rawtypes")
    @Accessor("MAP")
    static Map tidal$map() {
        throw new AssertionError();
    }
}
