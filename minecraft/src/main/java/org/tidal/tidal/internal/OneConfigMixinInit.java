/*
 * This file is part of Tidal.
 * Tidal - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Tidal.
 *   <https://tidal.org> <https://github.com/Tidal/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   Tidal is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to Tidal, as published by Tidal,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to Tidal, as published by Tidal. If not, see
 * <https://tidal.org/legal/tidal/additional-terms>
 */

package cc.tidal.tidal.internal;

import kotlin.Unit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
//todo import cc.tidal.tidal.internal.generated.RelocatedMixins;
//? moul_compat {
import cc.tidal.tidal.internal.generated.RelocatedMixins;
//? }
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TidalMixinInit implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        List<String> mixins = new ArrayList<>();

        //? moul_compat {
        RelocatedMixins.INSTANCE.register(e -> {
            mixins.add(e);
            return Unit.INSTANCE;
        });
        //? }
        //? moul_compat {
        mixins.add("compat.moulconfig.Mixin_MCConfigEditorIntegration_Firmament");
        // unrelocated targets, e.g. Skysoft's SoftConfig
        mixins.add("compat.moulconfig.Mixin_ConfigProcessorDriver");
        mixins.add("compat.moulconfig.Mixin_MoulConfigProcessor");
        mixins.add("compat.moulconfig.Mixin_MoulConfigEditor");
        mixins.add("compat.moulconfig.Mixin_PropertyImpl");
        mixins.add("compat.moulconfig.Mixin_GuiOptionEditorSlider");
        mixins.add("compat.moulconfig.Mixin_GuiOptionEditorDropdown");
        //? }

        //? dandelion_compat
        //mixins.add("compat.DandelionScreenImplMixin");

        //? odin_compat
        //mixins.add("compat.odin.Mixin_OdinModuleManager");

        //? rconfig_compat
        //mixins.add("compat.rconfig.Mixin_Configurations");

        //? osl_config_compat
        //mixins.add("compat.osl.Mixin_OslConfigManager");

        mixins.add("Mixin_SimpleReloadInstance");
        mixins.add("Mixin_MainMenuFpsUncap");
        //? yacl_compat
        mixins.add("compat.yacl.Mixin_YetAnotherConfigLib_Builder");

        //? clothconfig_compat
        mixins.add("compat.cloth.Mixin_ConfigBuilderImpl");

        //? midnightlib_compat
        mixins.add("compat.midnightlib.Mixin_MidnightConfig");
        //? midnightlib_compat
        mixins.add("compat.midnightlib.SliderButtonAccessor");

        //? walksylib_compat
        mixins.add("compat.walksylib.Mixin_WalksyLib_ModEntryPointList");

        //? tr7zw_compat
        mixins.add("compat.tr7zw.Mixin_AbstractConfigScreen");

        //? ukulib_compat
        mixins.add("compat.ukulib.Mixin_BaseConfigScreen");

        //? axolotlclient_config_compat
        mixins.add("compat.axolotlclient.Mixin_AxolotlClientConfigImpl");

        //? skycubed_compat {
        /*mixins.add("compat.skycubed.Mixin_SkyCubed");
        mixins.add("compat.skycubed.Mixin_SkyCubedOverlays");
        *///? }

        //? skyblocker_compat {
        /*Boolean skyblockerSingleton = declaresStaticMethod("de.hysky.skyblocker.skyblock.fancybars.FancyStatusBars", "initStatic");
        if (skyblockerSingleton != null) {
            mixins.add(skyblockerSingleton
                    ? "compat.skyblocker.Mixin_SkyblockerFancyStatusBarsInstance"
                    : "compat.skyblocker.Mixin_SkyblockerFancyStatusBarsStatic");
        }
        mixins.add("compat.skyblocker.Mixin_SkyblockerWidgetManager");
        *///? }

        //? skyblocker_legacy_hud
        //mixins.add("compat.skyblocker.Mixin_SkyblockerScreenBuilder");

        //? skyblocker_hud_v2
        //mixins.add("compat.skyblocker.Mixin_SkyblockerLayerBuilder");

        //? stella_compat
        //mixins.add("compat.stella.Mixin_Stella");

        //? apec_compat
        //mixins.add("compat.apec.Mixin_ApecMenu");

        mixins.add("compat.skyhanni.Mixin_SkyHanniRenderData");

        mixins.add("compat.armorhud.Mixin_ArmorHudWidgetShown");

        mixins.add("compat.firmament.Mixin_FirmamentHudMeta");
        // Firmament has no stable release for 26.2 yet
        //? >= 1.21.8 && < 26.2
        //mixins.add("compat.firmament.Mixin_FirmamentContentCapture");

        //? modmenu_compat
        mixins.add("compat.Mixin_ModMenu");

        //? neoforge {
        //mixins.add("events.Mixin_ChatReceiveEvent_Forge");
        //mixins.add("events.Mixin_ScreenOpenEvent_Forge");
        //? } else {
        mixins.add("events.Mixin_ScreenOpenEvent_Fabric");
        //? }

        mixins.add("events.Mixin_ModernWindowFocusEvent");

        mixins.add("skia.Mixin_InitSkia");
        mixins.add("skia.Mixin_SkiaFrame");
        mixins.add("skia.Mixin_SkiaFrameVk");
        //? >= 26.1 {
        mixins.add("blaze3d.GpuDeviceAccessor");
        mixins.add("blaze3d.GlDeviceAccessor");
        mixins.add("skia.Mixin_SkiaFramePresent");
        //? }
        //? >= 1.21.8 {
        mixins.add("skia.Mixin_GuiRendererLegacyTarget");
        mixins.add("render.GameRendererAccessor");
        mixins.add("render.GuiRendererAccessor");
        //? }
        //? if < 1.21.8
        //mixins.add("skia.Mixin_MainTargetRedirect");
        mixins.add("skia.Mixin_DebugOverlayAboveUi");
        //? < 26.1 {
        /*mixins.add("skia.Mixin_ScreenshotComposite");
        *///? }
        mixins.add("skia.Mixin_InitSkiaFontRenderer");

        //? if >= 1.21.10 {
        mixins.add("keybind.Mixin_KeybindCategoryLabel");
        //?} else
        //mixins.add("keybind.KeyMappingCategoryAccessor");

        mixins.add("keybind.Mixin_TidalKeybindRebind");
        mixins.add("keybind.Mixin_KeyMappingResetDetect");
        mixins.add("keybind.Mixin_OptionsSaveDetect");
        mixins.add("keybind.Mixin_OptionsSkipMirrors");

        //? cinnabar
        //mixins.add("skia.Mixin_CinnabarSkiaFlush");

        if (isClassPresent("net.vulkanmod.vulkan.Renderer")) {
            mixins.add("skia.Mixin_VulkanModSkiaFlush");
            //? >= 1.21.10 {
            mixins.add("skia.Mixin_VulkanModBlurSnapshot");
            //? }
        }

        {
            Logger logger = LogManager.getLogger(TidalMixinInit.class);
            logger.info("Loaded {} non-common Mixins", mixins.size());

            for (int i = 0; i < mixins.size(); i += 5) {
                int end = Math.min(i + 5, mixins.size());
                List<String> batch = mixins.subList(i, end);
                logger.info("Loaded Mixins: {}", String.join(", ", batch));
            }
        }

        return mixins;
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, TidalMixinInit.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Boolean declaresStaticMethod(String className, String methodName) {
        try (InputStream in = TidalMixinInit.class.getClassLoader()
                .getResourceAsStream(className.replace('.', '/') + ".class")) {
            if (in == null) return null;
            ClassNode node = new ClassNode();
            new ClassReader(in).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            for (MethodNode method : node.methods) {
                if (method.name.equals(methodName) && (method.access & Opcodes.ACC_STATIC) != 0) return Boolean.TRUE;
            }
            return Boolean.FALSE;
        } catch (Throwable t) {
            LogManager.getLogger(TidalMixinInit.class)
                    .warn("could not read {} to pick a mixin shape, skipping the mixins that depend on it", className, t);
            return null;
        }
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

}
