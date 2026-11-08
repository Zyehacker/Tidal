package cc.tidal.tidal.internal.ui.compose

import cc.tidal.compose.render.FontManager

object McFontService {
    fun init() {
        FontManager.loadFromResource("assets/tidal/fonts/Poppins/Poppins-Thin.ttf", "poppins-thin")
        FontManager.loadFromResource("assets/tidal/fonts/Poppins/Poppins-ThinItalic.ttf", "poppins-thin-italic")
        FontManager.loadFromResource("assets/tidal/fonts/Poppins/Poppins-Regular.ttf", "poppins")
        FontManager.loadFromResource("assets/tidal/fonts/Poppins/Poppins-Italic.ttf", "poppins-italic")
        FontManager.loadFromResource("assets/tidal/fonts/Poppins/Poppins-Medium.ttf", "poppins-medium")
        FontManager.loadFromResource("assets/tidal/fonts/Poppins/Poppins-MediumItalic.ttf", "poppins-medium-italic")
        FontManager.loadFromResource("assets/tidal/fonts/Poppins/Poppins-Bold.ttf", "poppins-bold")
        FontManager.loadFromResource("assets/tidal/fonts/Poppins/Poppins-BoldItalic.ttf", "poppins-bold-italic")
        FontManager.loadFromResource("assets/tidal/fonts/Poppins/Poppins-Black.ttf", "poppins-black")
        FontManager.loadFromResource("assets/tidal/fonts/Poppins/Poppins-BlackItalic.ttf", "poppins-black-italic")
        // used by toasts when a Minecraft theme is active
        FontManager.loadFromResource("assets/tidal/fonts/minecraft/Minecraft-Regular.otf", "minecraft")
        FontManager.loadFromResource("assets/tidal/fonts/minecraft/Minecraft-Bold.otf", "minecraft-bold")
        FontManager.loadFromResource("assets/tidal/fonts/unifont/unifont.otf", SkiaFontRenderer.UNIFONT_KEY)
        // PolyText uses the default font key so it needs a typeface registered there
        FontManager.setDefault("poppins")
    }
}
