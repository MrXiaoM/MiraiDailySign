package top.mrxiaom.mirai.dailysign.utils

import org.jetbrains.skia.Surface

class SurfaceHelper {
    lateinit var surface: Surface

    fun init(width: Int, height: Int) {
        surface = Surface.makeRasterN32Premul(width, height)
    }

    fun toByteArray(): ByteArray? {
        if (!this::surface.isInitialized) return null
        TODO("渲染并导出图片")
    }
}