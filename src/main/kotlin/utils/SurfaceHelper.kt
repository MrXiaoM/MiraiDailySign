package top.mrxiaom.mirai.dailysign.utils

import net.mamoe.mirai.utils.ExternalResource
import org.jetbrains.skia.*
import org.jetbrains.skia.shaper.ShapingOptions
import xyz.cssxsh.mirai.skia.makeSnapshotResource
import xyz.cssxsh.skia.FontUtils

class SurfaceHelper {
    lateinit var surface: Surface
    var format = EncodedImageFormat.PNG
    fun init(width: Int, height: Int) {
        surface = Surface.makeRasterN32Premul(width, height)
    }
    fun font(fontFamily: String, fontStyle: String, fontSize: Float): Font {
        return font(arrayOf(fontFamily), fontStyle, fontSize)
    }
    fun font(fontFamilies: Array<String?>, fontStyle: String, fontSize: Float): Font {
        val style = when(fontStyle.uppercase()) {
            "B", "BOLD" -> FontStyle.BOLD
            "I", "ITALIC" -> FontStyle.ITALIC
            "BI", "BOLD_ITALIC" -> FontStyle.BOLD_ITALIC
            else -> FontStyle.NORMAL
        }
        return Font(
            typeface = FontUtils.matchFamiliesStyle(fontFamilies, style),
            size = fontSize
        )
    }
    fun text(s: String, font: Font): TextLine {
        return TextLine.make(s, font)
    }
    fun clear(paint: Paint) {
        surface.canvas.clear(paint.color)
    }
    fun drawTextLine(text: String, font: Font, x: Float, y: Float, color: Paint) {
        drawTextLine(TextLine.make(text, font), x, y, color)
    }
    fun drawTextLine(text: TextLine, x: Float, y: Float, color: Paint) {
        surface.canvas.drawTextLine(text, x, y, color)
    }
    fun paint(s: String): Paint {
        return s.substring(1).chunked(2).toMutableList().run {
            val paint = Paint()
            if (size == 3) add(0, "FF")
            if (size != 4) {
                paint.setARGB(0xFF, 0x00, 0x00, 0x00)
            } else {
                paint.setARGB(this[0].toInt(16), this[1].toInt(16), this[2].toInt(16), this[3].toInt(16))
            }
            paint
        }
    }
    fun toExternalResource(): ExternalResource? {
        if (!this::surface.isInitialized) return null
        return surface.makeSnapshotResource(format)
    }
}