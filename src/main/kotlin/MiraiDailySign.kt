package top.mrxiaom.mirai.dailysign

import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.utils.info
import top.mrxiaom.mirai.dailysign.config.DailySignConfig
import top.mrxiaom.mirai.dailysign.data.SignUser
import java.io.File

object MiraiDailySign : KotlinPlugin(
    JvmPluginDescription(
        id = "top.mrxiaom.mirai.dailysign",
        name = "MiraiDailySign",
        version = "0.1.0",
    ) {
        author("MrXiaoM")

        dependsOn("xyz.cssxsh.mirai.plugin.mirai-economy-core")
    }
) {
    val loadedUsers = mutableMapOf<Long, SignUser>()
    val loadedConfigs = mutableMapOf<Long, DailySignConfig>()
    val defaultConfig by lazy { DailySignConfig() }
    override fun onEnable() {

        defaultConfig.reload()

        logger.info { "Plugin loaded" }
    }

    fun getUser(id: Long): SignUser {
        return loadedUsers[id] ?: SignUser(id).also { loadedUsers[id] = it }
    }

    fun reloadConfig() {
        defaultConfig.reload()
        val groups = File(dataFolder, "groups").listFiles()?.mapNotNull {
            it.nameWithoutExtension.toLongOrNull()
        } ?: listOf()
        loadedConfigs.keys.filter { !groups.contains(it) }.forEach(loadedConfigs::remove)
        for (group in groups) {
            val config = loadedConfigs[group] ?: DailySignConfig(group.toString()).also { loadedConfigs[group] = it }
            config.reload()
            config.loadRewards()
        }
    }
}

fun Group.getDailySign(): DailySignConfig = MiraiDailySign.loadedConfigs[id] ?: MiraiDailySign.defaultConfig
