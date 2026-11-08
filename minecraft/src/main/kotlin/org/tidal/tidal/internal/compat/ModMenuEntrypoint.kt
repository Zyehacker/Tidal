package cc.tidal.tidal.internal.compat

//? modmenu_compat {
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.minecraft.client.gui.screens.Screen
import org.apache.logging.log4j.LogManager
import cc.tidal.tidal.api.config.v1.ConfigManager
import cc.tidal.tidal.internal.ui.compose.impls.TidalUIScreen

internal object ModMenuEntrypoint : ModMenuApi {

    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        if (CompatLoader.hasMod(ROOT_MOD_ID)) return ConfigScreenFactory<Screen> { null }
        return try {
            ConfigScreenFactory { TidalUIScreen.open() }
        } catch (e: LinkageError) {
            reportUnlinkable(e)
            ConfigScreenFactory<Screen> { null }
        }
    }

    override fun getProvidedConfigScreenFactories(): Map<String, ConfigScreenFactory<*>> {
        return try {
            val factories = ConfigManager.active().trees()
                .filter { it.id != null }
                .associateTo(mutableMapOf()) { it.id to ConfigScreenFactory { _ -> TidalUIScreen(initialTree = it) } }
            factories.putIfAbsent(ROOT_MOD_ID, ConfigScreenFactory { _ -> TidalUIScreen.open() })
            factories
        } catch (e: LinkageError) {
            reportUnlinkable(e)
            emptyMap()
        }
    }

    private fun reportUnlinkable(e: LinkageError) {
        LOGGER.error(
            "Tidal's screen class could not be linked, so its ModMenu entries are unavailable. " +
                "This is usually another mod's mixin failing to apply to TidalUIScreen - check the " +
                "log above for the Mixin error naming the responsible mod.",
            e,
        )
    }

    private val LOGGER = LogManager.getLogger("Tidal/ModMenu")

    internal const val ROOT_MOD_ID = "tidal"

    internal const val LEGACY_BOOTSTRAP_MOD_ID = "tidalbootstrap"

    internal const val PLATFORM_MOD_ID = "tidalv1"

}
//? }
