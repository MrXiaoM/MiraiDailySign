package top.mrxiaom.mirai.dailysign.config

import net.mamoe.mirai.console.data.*
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import xyz.cssxsh.mirai.economy.EconomyService
import xyz.cssxsh.mirai.economy.economy
import xyz.cssxsh.mirai.economy.globalEconomy
import xyz.cssxsh.mirai.economy.service.EconomyCurrency

class DailySignConfig(
    val groupId: String = "default"
) : ReadOnlyPluginConfig("groups/$groupId") {
    @ValueName("at")
    @ValueDescription("需要 @ 机器人才能签到")
    val at by value(true)
    @ValueName("keywords")
    @ValueDescription("签到命令关键词")
    val keywords by value(listOf("签到"))
    @ValueName("permission")
    @ValueDescription("签到所需权限，留空为不需要权限\n" +
            "若设置权限，则需要 top.mrxiaom.mirai.dailysign:你输入的值\n" +
            "这个权限才能签到，其中 \$file 会被替换成当前配置文件名(不含后缀)\n" +
            "以默认配置文件为例，需要权限 top.mrxiaom.mirai.dailysign:sign.default")
    val permission by value("sign.\$file")
    @ValueName("deny-message")
    @ValueDescription("没有权限时的提示，留空为不提示\n" +
            "其中，\$quote 为回复消息，\$at 为 @，\$perm 为用户缺少的权限")
    val denyMessage by value("")
    @ValueName("rewards")
    @ValueDescription("签到奖励，格式如下\n" +
            "奖励金钱到群聊上下文 group:货币种类:数量\n" +
            "奖励金钱到全局上下文 global:货币种类:数量")
    val rewards by value(listOf("group:mirai-coin:100"))
    val realRewards = mutableListOf<Reward>()
    class Reward(
        val isGlobal: Boolean,
        val currency: EconomyCurrency,
        val money: Double
    )
    fun loadRewards() {
        realRewards.clear()
        for (line in rewards){
            val params = line.split(":")
            val global = when (params.getOrNull(0) ?: error("不存在参数: 经济上下文")) {
                "global" -> true
                "group" -> false
                else -> error("参数错误: 应为 global 或 group")
            }
            val currencyName = params.getOrNull(1) ?: error("不存在参数: 货币种类")
            val currency = EconomyService.basket[currencyName] ?: error("货币种类 $currencyName 不存在")
            val money = (params.getOrNull(2) ?: error("不存在参数: 金钱数量")).toDoubleOrNull() ?: error("错误: 填入的金钱数量应为数字")
            realRewards.add(Reward(global, currency, money))
        }
    }
    fun giveRewardsTo(group: Group, user: User) {
        realRewards.forEach {
            it.run {
                if (isGlobal) globalEconomy {
                    service.account(user) += (currency to money)
                } else group.economy {
                    service.account(user) += (currency to money)
                }
            }
        }
    }
}
