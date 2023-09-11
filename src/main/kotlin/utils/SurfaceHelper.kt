package top.mrxiaom.mirai.dailysign.utils

import net.mamoe.mirai.utils.ExternalResource
import org.jetbrains.skia.*
import top.mrxiaom.mirai.dailysign.MiraiDailySign
import xyz.cssxsh.mirai.skia.makeSnapshotResource
import xyz.cssxsh.skia.FontUtils
import java.io.File

class SurfaceHelper {
    lateinit var surface: Surface
    var format = EncodedImageFormat.PNG

    /**
     * 初始化画布
     */
    fun init(width: Int, height: Int) {
        surface = Surface.makeRasterN32Premul(width, height)
    }

    /**
     * 获取字体
     */
    fun font(fontFamily: String, fontStyle: String, fontSize: Float): Font {
        return font(arrayOf(fontFamily), fontStyle, fontSize)
    }

    /**
     * 获取字体
     */
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

    /**
     * 获取文字
     */
    fun text(s: String, font: Font): TextLine {
        return TextLine.make(s, font)
    }

    /**
     * 获取矩形，参数为矩形四边的坐标
     */
    fun rect(left: Float, top: Float, right: Float, bottom: Float): Rect {
        return Rect(left, top, right, bottom)
    }

    /**
     * 获取矩形，参数为左上角坐标以及矩形大小
     */
    fun rectSize(x: Float, y: Float, width: Float, height: Float): Rect {
        return rect(x, y, x + width,y + height)
    }

    /**
     * 从 ./config/top.mrxiaom.mirai.dailysign/$path 的相对路径中读取图片
     * 文件不存在时返回 null (undefined)
     */
    fun imageFromConfig(path: String): Image? {
        return image(File(MiraiDailySign.configFolder, path))
    }

    /**
     * 从 ./data/top.mrxiaom.mirai.dailysign/$path 的相对路径中读取图片
     * 文件不存在时返回 null (undefined)
     */
    fun imageFromData(path: String): Image? {
        return image(File(MiraiDailySign.dataFolder, path))
    }

    /**
     * 从 mirai 工作目录的相对路径，或者绝对路径中读取图片
     * 文件不存在时返回 null (undefined)
     */
    fun imageFromPath(path: String): Image? {
        return image(File(path))
    }
    fun image(file: File): Image? {
        return if (file.exists())
            Image.makeFromEncoded(file.readBytes())
        else null
    }

    /**
     * 绘制图片，如需更复杂的参数请使用 surface.canvas
     */
    fun drawImage(image: Image, x: Float, y: Float) {
        surface.canvas.drawImage(image, x, y)
    }

    /**
     * 绘制图片，如需更复杂的参数请使用 surface.canvas
     */
    fun drawImageRect(image: Image, dst: Rect) {
        surface.canvas.drawImageRect(image, dst)
    }

    /**
     * 绘制图片，如需更复杂的参数请使用 surface.canvas
     */
    fun drawImageRect(image: Image, src: Rect, dst: Rect) {
        surface.canvas.drawImageRect(image, src, dst)
    }

    /**
     * 清空画布
     */
    fun clear(paint: Paint) {
        surface.canvas.clear(paint.color)
    }

    /**
     * 绘制线条，需要为 color 参数设置 strokeWidth
     */
    fun drawLine(x0: Float, y0: Float, x1: Float, y1: Float, color: Paint) {
        surface.canvas.drawLine(x0, y0, x1, y1, color)
    }

    /**
     * 绘制文字
     */
    fun drawTextLine(text: String, font: Font, x: Float, y: Float, color: Paint) {
        drawTextLine(TextLine.make(text, font), x, y, color)
    }

    /**
     * 绘制文字
     */
    fun drawTextLine(text: TextLine, x: Float, y: Float, color: Paint) {
        surface.canvas.drawTextLine(text, x, y, color)
    }

    /**
     * 获取画笔
     */
    fun paint(s: String): Paint = paint(s, 0.0F)

    /**
     * 获取画笔
     */
    fun paint(s: String, strokeWidth: Float): Paint {
        return s.substring(1).chunked(2).toMutableList().run {
            val paint = Paint()
            if (size == 3) add(0, "FF")
            if (size != 4) {
                paint.setARGB(0xFF, 0x00, 0x00, 0x00)
            } else {
                paint.setARGB(this[0].toInt(16), this[1].toInt(16), this[2].toInt(16), this[3].toInt(16))
            }
            if (strokeWidth > 0) paint.strokeWidth = strokeWidth
            paint
        }
    }

    /**
     * 生成图片，用于发送消息
     */
    fun toExternalResource(): ExternalResource? {
        if (!this::surface.isInitialized) return null
        return surface.makeSnapshotResource(format)
    }
}