package top.mrxiaom.mirai.dailysign.utils

import net.mamoe.mirai.utils.ExternalResource
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Surface
import xyz.cssxsh.mirai.skia.makeSnapshotResource

class SurfaceHelper {
    lateinit var surface: Surface
    var format = EncodedImageFormat.PNG
    fun init(width: Int, height: Int) {
        surface = Surface.makeRasterN32Premul(width, height)
    }

    fun toExternalResource(): ExternalResource? {
        if (!this::surface.isInitialized) return null
        return surface.makeSnapshotResource(format)
    }
}