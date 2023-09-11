package top.mrxiaom.mirai.dailysign

import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.console.plugin.jvm.savePluginConfig
import net.mamoe.mirai.console.plugin.version
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.utils.MiraiLogger
import net.mamoe.mirai.utils.info
import org.mozilla.javascript.*
import org.mozilla.javascript.Function
import top.mrxiaom.mirai.dailysign.command.ConsoleCommand
import top.mrxiaom.mirai.dailysign.command.MessageHost
import top.mrxiaom.mirai.dailysign.config.DailySignConfig
import top.mrxiaom.mirai.dailysign.config.PluginConfig
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
    val scriptLogger = MiraiLogger.Factory.create(this::class, "MiraiDailySignScript")
    val loadedUsers = mutableMapOf<Long, SignUser>()
    val loadedConfigs = mutableListOf<DailySignConfig>()
    var script = ""
    override fun onEnable() {
        PermissionHolder["calendar", "每月签到日历触发权限"]
        reloadConfig()
        PluginConfig.save()
        ConsoleCommand.register()
        globalEventChannel().registerListenerHost(MessageHost)

        logger.info { "Plugin loaded" }
    }

    fun getUser(id: Long): SignUser { 
        return loadedUsers[id] ?: SignUser(id).also {
            it.reload()
            loadedUsers[id] = it
        }
    }

    fun reloadScript() {
        val scriptFile = File(configFolder, "script.js")
        if (!scriptFile.exists()) {
            scriptFile.writeText(
                getResource("script.js") ?: "// `script.js` not found.\n"
            )
        }
        script = scriptFile.readText()
        logger.info("脚本 script.js 重载完成")
    }
    private fun ScriptableObject.put(name: String, obj: Any) {
        ScriptableObject.putProperty(this, name, Context.javaToJS(obj, this))
    }
    fun runInJavaScript(event: GroupMessageEvent, funcName: String, vararg args: Any): String? = Context.enter().use { ctx ->
        try {
            val scope = ctx.initStandardObjects()
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
            ctx.evaluateString(scope, script, "MiraiDailySign", 1, null)
            val function = scope.get(funcName, scope) as Function
            return@use function.call(ctx, scope, scope, args.map { Context.javaToJS(it, scope) }.toTypedArray()).toString()
        } catch (t: Throwable) {
            logger.warning(
                "执行 $funcName 时，config/top.mrxiaom.mirai.dailysign/replace.js 发生一个异常",
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
        PluginConfig.reload()
        val files = File(configFolder, "groups").listFiles { _, name -> name.endsWith(".yml") }?.mapNotNull {
            it.nameWithoutExtension
        } ?: listOf()

        fun DailySignConfig.addToList(): DailySignConfig {
            loadedConfigs.add(this)
            return this
        }
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
}
