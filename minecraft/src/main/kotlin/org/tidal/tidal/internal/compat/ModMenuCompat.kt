package cc.tidal.tidal.internal.compat

//? modmenu_compat {
import com.terraformersmc.modmenu.ModMenu
import com.terraformersmc.modmenu.util.mod.Mod
import net.minecraft.client.gui.screens.Screen
import cc.tidal.tidal.api.config.v1.CompatSnapshots
import cc.tidal.tidal.api.config.v1.ConfigManager
import cc.tidal.tidal.api.config.v1.Tree
import cc.tidal.tidal.api.config.v1.backend.Backend
import cc.tidal.tidal.api.platform.v1.ModInfo
import cc.tidal.tidal.api.platform.v1.Platform
import cc.tidal.tidal.internal.ui.api.ConfigRegistry
import cc.tidal.tidal.internal.ui.api.ConfigSource
import cc.tidal.tidal.internal.ui.compose.impls.TidalUIScreen
import cc.tidal.tidal.internal.ui.navigation.graph.ModConfigRoute
import cc.tidal.tidal.internal.ui.shell.LocalNavController

object ModMenuCompat {
    val mods: MutableList<Mod> = mutableListOf()

    private val ownModIds = setOf(
        ModMenuEntrypoint.ROOT_MOD_ID,
        ModMenuEntrypoint.LEGACY_BOOTSTRAP_MOD_ID,
        ModMenuEntrypoint.PLATFORM_MOD_ID,
    )

    fun preLoad() = CompatLoader.requireTranslations(-1000, true) {
        ModMenu.ROOT_MODS.forEach { (_, mod) ->
            if (mod.id in ownModIds) return@forEach
            // never call getConfigScreen here
            // it builds widgets that bake font glyphs and preLoad runs off-frame during
            // ResourceFinishedLoading where that corrupts the vanilla font atlas
            // hasConfigScreen only looks up the factory so the screen is built later in a render frame
            if (runCatching { ModMenu.hasConfigScreen(mod.id) }.getOrDefault(false)) {
                mods.add(mod)
            }
        }
    }

    @JvmStatic
    fun enable() {
        preLoad()
        postLoad()
    }

    fun postLoad() = CompatLoader.requireTranslations(1000, true) {
        val nativeCoveredMods = mods.mapNotNull { mod ->
            val nativeChild = mod.findNativeLoadedChild() ?: return@mapNotNull null
            mod.aliasNativeChildConfig(nativeChild)
            mod.id
        }.toSet()

        val foundMods = mods.filterNot {
            CompatLoader.nativeLoadedConfigs.contains(it.id) ||
                it.id in nativeCoveredMods ||
                hasNativeOcConfig(it.id)
        }

        foundMods
            .forEach { mod ->
                val modMenuTree = Tree.tree()

                modMenuTree.id = mod.id
                modMenuTree.title = mod.name
                modMenuTree.description = "(Mod Menu Compat)"
                ModInfo.loadedMods.firstOrNull { it.id == mod.id }?.extractIconFile()?.let { iconPath ->
                    modMenuTree.addMetadata("icon_path", iconPath)
                }
                modMenuTree.addMetadata("on_click") {
                    val foreignScreen = runCatching {
                        var screen: Screen? = null
                        CompatLoader.withForcedModId(mod.id) {
                            screen = ModMenu.getConfigScreen(mod.id, Platform.screen().current())
                        }
                        screen
                    }.getOrNull()

                    val nativeTree = runCatching { ConfigManager.active().get(mod.id) }.getOrNull()
                    val hasNative = nativeTree != null &&
                        nativeTree !== modMenuTree &&
                        nativeTree.getMetadata<Boolean>(Backend.UI_ONLY_METADATA) != true

                    val compatTree = nativeTree?.takeIf {
                        it !== modMenuTree &&
                            it.getMetadata<Boolean>(CompatSnapshots.SNAPSHOT_METADATA) == true
                    }
                    val showOc = hasNative || compatTree != null

                    val ocTree = if (hasNative) nativeTree else compatTree
                    if (showOc && ocTree != null) {
                        ConfigRegistry.registerTree(ocTree, ConfigSource.OC)
                    }

                    when {
                        showOc && LocalNavController.isReady ->
                            LocalNavController.wrapper.navigate(ModConfigRoute(mod.id))
                        showOc -> Platform.screen().display(TidalUIScreen(initialTreeId = mod.id))
                        foreignScreen != null -> Platform.screen().display(foreignScreen)
                    }
                }
                // listed and opened via Mod Menu only so never persisted by Tidal
                modMenuTree.addMetadata(Backend.UI_ONLY_METADATA, true)
                modMenuTree.addMetadata(Backend.UI_PLACEHOLDER_METADATA, true)

                ConfigManager.active().register(modMenuTree)
                CompatLoader.markFirstModAsSkip()
            }

        scheduleWarmup(foundMods)
    }

    private fun scheduleWarmup(mods: List<Mod>) {
        mods.forEach { mod ->
            CompatLoader.queueScreenWarmup {
                CompatLoader.withForcedModId(mod.id) {
                    // the screen is thrown away because building it is what makes the compat mixins fire
                    ModMenu.getConfigScreen(mod.id, Platform.screen().current())
                }
            }
        }
    }

    // a mod can ship both a native Tidal config and a Mod Menu entrypoint
    // the native one registers under its file id "legacyskyblock.json" while Mod Menu uses the bare
    // loader id "legacyskyblock" so the differing ids would show as two cards
    // nativeLoadedConfigs only tracks foreign-compat configs so detect the native tree from the registry
    private fun hasNativeOcConfig(modId: String): Boolean = runCatching {
        ConfigManager.active().trees().any { tree ->
            val id = tree.id ?: return@any false
            id.removeSuffix(".json") == modId &&
                tree.getMetadata<Boolean>(Backend.UI_ONLY_METADATA) != true
        }
    }.getOrDefault(false)

    private fun Mod.findNativeLoadedChild(): Mod? {
        return ModMenu.PARENT_MAP.get(this).firstNotNullOfOrNull { child ->
            if (CompatLoader.nativeLoadedConfigs.contains(child.id)) {
                child
            } else {
                child.findNativeLoadedChild()
            }
        }
    }

    private fun Mod.aliasNativeChildConfig(child: Mod) {
        val tree = runCatching { ConfigManager.active().get(child.id) }.getOrNull() ?: return
        tree.addMetadata("mod_card_title", name)
        ModInfo.loadedMods.firstOrNull { it.id == id }?.extractIconFile()?.let { iconPath ->
            tree.addMetadata("mod_card_icon_path", iconPath)
        }
    }

}
//? }
