package top.mrxiaom.mirai.dailysign.command

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.ConsoleCommandSender
import top.mrxiaom.mirai.dailysign.MiraiDailySign

object ConsoleCommand : CompositeCommand(
    owner = MiraiDailySign,
    primaryName = "MiraiDailySign",
    secondaryNames = arrayOf("dailysign", "sign"),
    parentPermission = MiraiDailySign.parentPermission
) {
    @SubCommand("reload")
    suspend fun reload(sender: CommandSender) {
        MiraiDailySign.reloadConfig()
        if (sender !is ConsoleCommandSender) sender.sendMessage("配置文件已重载")
        MiraiDailySign.logger.info("配置文件已重载")
    }
}