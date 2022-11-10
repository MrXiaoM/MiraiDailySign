package top.mrxiaom

import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.utils.info

object MiraiDailySign : KotlinPlugin(
    JvmPluginDescription(
        id = "top.mrxiaom.mirai.dailysign",
        name = "MiraiDailySign",
        version = "0.1.0",
    ) {
        author("MrXiaoM")
    }
) {
    override fun onEnable() {
        logger.info { "Plugin loaded" }
    }
}