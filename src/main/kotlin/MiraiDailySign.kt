package top.mrxiaom.mirai.dailysign

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
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
    }
) {
    val loadedUsers = mutableMapOf<Long, SignUser>()
    val loadedConfigs = mutableMapOf<Long, DailySignConfig>()
    val defaultConfig by lazy { DailySignConfig() }
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

    fun runReplaceScript(s: String, event: GroupMessageEvent): String = Context.enter().use {
        val scope = it.initStandardObjects()
        ScriptableObject.putProperty(scope, "javaContext", Context.javaToJS(this, scope))
        ScriptableObject.putProperty(scope, "javaLoader", Context.javaToJS(this::class.java.classLoader, scope))
        it.evaluateString(scope, replaceScript, "MiraiDailySign", 1, null)
        val function = scope.get("replace", scope) as Function
        return@use function.call(it, scope, scope, arrayOf(s, event)).toString()
    }

    fun reloadConfig() {
        reloadReplaceScript()
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

fun Group.dailySign(): DailySignConfig = MiraiDailySign.loadedConfigs[id] ?: MiraiDailySign.defaultConfig
fun GroupAwareMessageEvent.dailySign(): DailySignConfig = group.dailySign()
fun MessageChain.filterAt(predicate: (At) -> Boolean): List<At> = filterIsInstance<At>().filter { predicate(it) }
fun MessageEvent.filterAt(predicate: (At) -> Boolean): List<At> = message.filterAt(predicate)
fun MessageEvent.hasAtBot(): Boolean = filterAt { it.target == bot.id }.isNotEmpty()
fun MessageChain.textOnly(): String = filterIsInstance<PlainText>().joinToString()
fun MessageEvent.textOnly(): String = message.textOnly()

/**
 * ???????????????
 * @param input ????????????????????????
 * @param transform ?????????????????? null ???????????????????????????
 */
fun <T> Regex.split(
    input: CharSequence,
    transform: (s: String, isMatched: Boolean) -> T?
): List<T> {
    val list = mutableListOf<T>()
    var index = 0
    for (result in findAll(input)) {
        val first = result.range.first
        val last = result.range.last
        if (first > index) {
            val value = transform(input.substring(index, first), false)
            if (value != null) list.add(value)
        }
        val value = transform(input.substring(first, last + 1), true)
        if (value != null) list.add(value)
        index = last + 1
    }
    if (index < input.length) {
        val value = transform(input.substring(index), false)
        if (value != null) list.add(value)
    }
    return list
}
