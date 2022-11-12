package top.mrxiaom.mirai.dailysign.command

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.ConsoleCommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import top.mrxiaom.mirai.dailysign.MiraiDailySign

object ConsoleCommand : SimpleCommand(
    owner = MiraiDailySign,
    primaryName = "MiraiDailySign",
    secondaryNames = arrayOf("dailysign", "sign"),
    parentPermission = MiraiDailySign.parentPermission
) {
    @Handler
    suspend fun CommandSender.handle(operation: String) {
        if (operation.equals("reload", true)) {
            MiraiDailySign.reloadConfig()
            if (this !is ConsoleCommandSender) sendMessage("配置文件已重载")
            MiraiDailySign.logger.info("配置文件已重载")
        }
    }
}