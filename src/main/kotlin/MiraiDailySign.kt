package top.mrxiaom.mirai.dailysign

import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.extension.PluginComponentStorage
import net.mamoe.mirai.console.plugin.PluginManager
import net.mamoe.mirai.console.plugin.id
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.console.plugin.jvm.savePluginConfig
import net.mamoe.mirai.console.plugin.jvm.savePluginData
import net.mamoe.mirai.console.plugin.version
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
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
import top.mrxiaom.mirai.dailysign.data.SignRecord
import top.mrxiaom.mirai.dailysign.data.SignUser
import xyz.cssxsh.mirai.economy.EconomyService
import xyz.cssxsh.mirai.economy.economy
import xyz.cssxsh.mirai.economy.globalEconomy
import xyz.cssxsh.mirai.economy.service.EconomyCurrency
import xyz.cssxsh.mirai.economy.service.EconomyCurrencyManager
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
    private var skiaPluginLoaded: Boolean = false
    val isSkiaPluginLoaded: Boolean
        get() = skiaPluginLoaded
    val scriptLogger = MiraiLogger.Factory.create(this::class, "MiraiDailySignScript")
    val loadedUsers = mutableMapOf<Long, SignUser>()
    val loadedConfigs = mutableListOf<DailySignConfig>()
    var script = ""
    override fun PluginComponentStorage.onLoad() {
        runAfterStartup {
        }
    }
    override fun onEnable() {
        skiaPluginLoaded = PluginManager.plugins.any { it.id == "xyz.cssxsh.mirai.plugin.mirai-skia-plugin" }
        if (!skiaPluginLoaded) {
            logger.warning("未发现前置插件 mirai-skia-plugin，月签到日历查询功能将不可用。")
            logger.warning("你可以在以下链接下载该前置插件")
            logger.warning("@see https://github.com/cssxsh/mirai-skia-plugin/releases")
        }
        PermissionHolder["calendar", "每月签到日历触发权限"]
        reloadConfig()
        PluginConfig.save()

        SignRecord.reload()
        if (SignRecord.check()) SignRecord.save()

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
            scope.put("economy", EconomyHelper)
            scope.put("record", SignRecord)
            scope.put("javaContext", this)
            scope.put("javaLoader", this::class.java.classLoader)
            ctx.evaluateString(scope, script, "script.js ", 1, null)
            val function = scope.get(funcName, scope) as Function
            return@use function.call(ctx, scope, scope, args.map { Context.javaToJS(it, scope) }.toTypedArray()).toString()
        } catch (t: Throwable) {
            logger.warning(
                "执行 $funcName 时，config/top.mrxiaom.mirai.dailysign/script.js 发生一个异常",
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
        PluginConfig.save()

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
                it.save()
            }
            logger.info("首次加载，释放默认配置文件")
        } else {
            loadedConfigs.filter { !files.contains(it.fileName) }.forEach(loadedConfigs::remove)
            for (fileName in files) {
                val config = loadedConfigs.firstOrNull { it.fileName == fileName } ?: DailySignConfig(fileName).addToList()
                config.reload()
                config.loadRewards()
                config.perm
                config.save()
            }
            logger.info("已加载 ${loadedConfigs.size} 个配置")
        }
    }
    object EconomyHelper {
        fun getGlobalBalance(user: User, currencyName: String): Double {
            val currency = EconomyService.basket[currencyName] ?: error("货币种类 $currencyName 不存在")
            return globalEconomy {
                service.account(user).balance()[currency] ?: 0.0
            }
        }

        fun getGroupBalance(subject: Group, user: User, currencyName: String): Double {
            val currency = EconomyService.basket[currencyName] ?: error("货币种类 $currencyName 不存在")
            return subject.economy {
                service.account(user).balance()[currency] ?: 0.0
            }
        }
    }
}
