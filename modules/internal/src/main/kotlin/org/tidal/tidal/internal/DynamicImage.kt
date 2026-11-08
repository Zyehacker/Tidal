package cc.tidal.internal

import org.jetbrains.skia.Image
import java.io.InputStream

class DynamicImage(val path: String, private val stream: InputStream) {
    val image: Image by lazy { Image.makeFromEncoded(stream.readBytes()) }
}
