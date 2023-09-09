package top.mrxiaom.mirai.dailysign

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.console.plugin.jvm.savePluginConfig
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.events.GroupAwareMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.utils.info
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.ScriptableObject
import top.mrxiaom.mirai.dailysign.command.ConsoleCommand
import top.mrxiaom.mirai.dailysign.command.MessageHost
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
        dependsOn("xyz.cssxsh.mirai.plugin.mirai-skia-plugin", ">= 1.1.0", true)
    }
) {
    val loadedUsers = mutableMapOf<Long, SignUser>()
    val loadedConfigs = mutableListOf<DailySignConfig>()
    val replaceScriptFile by lazy { File(configFolder, "replace.js") }
    var replaceScript = ""
    override fun onEnable() {
        reloadConfig()
        ConsoleCommand.register()
        globalEventChannel().registerListenerHost(MessageHost)

        logger.info { "Plugin loaded" }
    }

    fun getUser(id: Long): SignUser {
        return loadedUsers[id] ?: SignUser(id).also { loadedUsers[id] = it }
    }

    fun reloadReplaceScript() {
        if (!replaceScriptFile.exists()) {
            replaceScriptFile.writeText(
                getResource("replace.js") ?: "// `replace.js` not found.\n"
            )
        }
        replaceScript = replaceScriptFile.readText()
    }

    fun runReplaceScript(s: String, event: GroupMessageEvent, config: DailySignConfig): String = Context.enter().use {
        val scope = it.initStandardObjects()
        ScriptableObject.putProperty(scope, "javaContext", Context.javaToJS(this, scope))
        ScriptableObject.putProperty(scope, "javaLoader", Context.javaToJS(this::class.java.classLoader, scope))
        it.evaluateString(scope, replaceScript, "MiraiDailySign", 1, null)
        val function = scope.get("replace", scope) as Function
        return@use function.call(it, scope, scope, arrayOf(s, event, config)).toString()
    }

    fun reloadConfig() {
        reloadReplaceScript()
        val files = File(dataFolder, "groups").listFiles()?.mapNotNull {
            it.nameWithoutExtension
        } ?: listOf()
        if (files.isEmpty()) {
            DailySignConfig("default").addToList().also { savePluginConfig(it) }
        } else {
            loadedConfigs.filter { !files.contains(it.fileName) }.forEach(loadedConfigs::remove)
            for (fileName in files) {
                val config = loadedConfigs.firstOrNull { it.fileName == fileName } ?: DailySignConfig(fileName).addToList()
                config.reload()
                config.loadRewards()
            }
        }
    }

    fun DailySignConfig.addToList(): DailySignConfig {
        loadedConfigs.add(this)
        return this
    }
}
