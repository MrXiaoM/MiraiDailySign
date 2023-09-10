package top.mrxiaom.mirai.dailysign

import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.console.plugin.jvm.savePluginConfig
import net.mamoe.mirai.console.plugin.version
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.message.data.MessageChain.Companion.serializeToJsonString
import net.mamoe.mirai.utils.MiraiLogger
import net.mamoe.mirai.utils.info
import org.mozilla.javascript.*
import org.mozilla.javascript.Function
import top.mrxiaom.mirai.dailysign.command.ConsoleCommand
import top.mrxiaom.mirai.dailysign.command.MessageHost
import top.mrxiaom.mirai.dailysign.config.DailySignConfig
import top.mrxiaom.mirai.dailysign.data.SignUser
import java.io.File
import java.net.URL
import java.nio.charset.StandardCharsets

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
    val scriptLogger = MiraiLogger.Factory.create(this::class, "MiraiDailySignScript")
    val loadedUsers = mutableMapOf<Long, SignUser>()
    val loadedConfigs = mutableListOf<DailySignConfig>()
    val replaceScriptFile by lazy { File(configFolder, "script.js") }
    var replaceScript = ""
    override fun onEnable() {
        PermissionHolder["calendar", "每月签到日历触发权限"]
        reloadConfig()
        ConsoleCommand.register()
        globalEventChannel().registerListenerHost(MessageHost)

        logger.info { "Plugin loaded" }
    }

    fun getUser(id: Long): SignUser {
        return loadedUsers[id] ?: SignUser(id).also { loadedUsers[id] = it }
    }

    fun reloadScript() {
        if (!replaceScriptFile.exists()) {
            replaceScriptFile.writeText(
                getResource("script.js") ?: "// `script.js` not found.\n"
            )
        }
        replaceScript = replaceScriptFile.readText()
        logger.info("脚本 script.js 重载完成")
    }
    private fun ScriptableObject.put(name: String, obj: Any) {
        ScriptableObject.putProperty(this, name, Context.javaToJS(obj, this))
    }
    fun runReplaceScript(event: GroupMessageEvent, funcName: String, vararg args: Any): String? = Context.enter().use {
        try {
            val scope = it.initStandardObjects()
            scope.put("version", version.toString())
            scope.put("logger", scriptLogger)
            scope.put("sender", event.sender)
            scope.put("subject", event.subject)
            scope.put("time", event.time)
            scope.put("bot", event.bot)
            scope.put("message", event.message)
            scope.put("source", event.source)
            scope.put("javaContext", this)
            scope.put("javaLoader", this::class.java.classLoader)
            it.evaluateString(scope, replaceScript, "MiraiDailySign", 1, null)
            val function = scope.get(funcName, scope) as Function
            return@use function.call(it, scope, scope, args).toString()
        } catch (t: Throwable) {
            logger.warning(
                "执行  时，config/top.mrxiaom.mirai.dailysign/replace.js 发生一个异常",
                t.find<EvaluatorException>() ?: t.find<JavaScriptException>() ?: t.find<EcmaError>() ?: t
            )
            return null
        }
    }

    private inline fun <reified T : Throwable> Throwable.find(): T? {
        var throwable: Throwable? = this
        while (throwable != null) {
            throwable = throwable.cause
            if (throwable is T) return throwable
        }
        return null
    }

    fun reloadConfig() {
        reloadScript()
        val files = File(configFolder, "groups").listFiles { _, name -> name.endsWith(".yml") }?.mapNotNull {
            it.nameWithoutExtension
        } ?: listOf()
        if (files.isEmpty()) {
            DailySignConfig("default").addToList().also {
                it.perm
                savePluginConfig(it)
            }
            logger.info("首次加载，释放默认配置文件")
        } else {
            loadedConfigs.filter { !files.contains(it.fileName) }.forEach(loadedConfigs::remove)
            for (fileName in files) {
                val config = loadedConfigs.firstOrNull { it.fileName == fileName } ?: DailySignConfig(fileName).addToList()
                config.reload()
                config.loadRewards()
                config.perm
            }
            logger.info("已加载 ${loadedConfigs.size} 个配置")
        }
    }

    private fun DailySignConfig.addToList(): DailySignConfig {
        loadedConfigs.add(this)
        return this
    }
}
