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
    @Description("重载配置文件")
    suspend fun reload(sender: CommandSender) {
        MiraiDailySign.reloadConfig()
        sender.log("配置文件已重载")
    }

    private suspend fun CommandSender.log(msg: String) {
        if (this !is ConsoleCommandSender) sendMessage(msg)
        MiraiDailySign.logger.info(msg)
    }
}