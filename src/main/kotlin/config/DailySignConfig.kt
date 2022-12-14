package top.mrxiaom.mirai.dailysign.config

import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.ValueName
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.permission.AbstractPermitteeId
import net.mamoe.mirai.console.permission.PermissionService.Companion.testPermission
import net.mamoe.mirai.console.permission.PermitteeId
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.User
import top.mrxiaom.mirai.dailysign.MiraiDailySign
import top.mrxiaom.mirai.dailysign.PermissionHolder
import xyz.cssxsh.mirai.economy.EconomyService
import xyz.cssxsh.mirai.economy.economy
import xyz.cssxsh.mirai.economy.globalEconomy
import xyz.cssxsh.mirai.economy.service.EconomyCurrency
import kotlin.random.Random

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
    fun hasPerm(permitteeId: PermitteeId) : Boolean = PermissionHolder[permission].testPermission(permitteeId)
    fun hasPerm(member: Member) : Boolean = hasPerm(AbstractPermitteeId.ExactMember(member.group.id, member.id))

    @ValueName("deny-message")
    @ValueDescription("没有权限时的提示(列表，换行)，留空[]为不提示\n" +
            "其中，\$quote 为回复消息，\$at 为 @，\$perm 为用户缺少的权限")
    val denyMessage by value(listOf<String>())
    @ValueName("success-message")
    @ValueDescription("签到成功时的提示(列表，换行)，留空[]为不提示\n" +
            "其中，\$quote 为回复消息，\$at 为 @，\$lasting 为连续签到天数\n" +
            "\$rewards 为签到奖励，\$avatar 为用户头像(加载失败时显示QQ号)\n" +
            "暂时懒得写表情，PRs welcome\n" +
            "因为想到一些签到插件还会有什么奇奇怪怪的无用提示，所以我都给加上变量了，请编辑以下脚本查看\n" +
            "config/top.mrxiaom.mirai.dailysign/replace.js\n" +
            "如有需要可自行添加变量(仅文字)，每当需要替换变量的时候都会执行一次该脚本")
    val successMessage by value(listOf(
        "\$quote",
        "\$avatar",
        "\$namecardOrNick (\$id) 签到成功!",
        "今天你已连续签到 \$lasting 天! 获得了以下奖励",
        "\$rewards",
        "",
        "\$今天是\$date \$week \$time",
        "祝你有美好的一天"
    ))
    @ValueName("already-sign-message")
    @ValueDescription("今日已签到的提示(列表，换行)，留空[]为不提示\n" +
            "其中，\$quote 为回复消息，\$at 为 @，\$avatar 为用户头像(加载失败时显示QQ号)\n" +
            "变量同上")
    val alreadySignMessage by value(listOf(
        "\$quote你今天已经签到过了"
    ))

    @ValueName("reward-template-global")
    @ValueDescription("签到奖励模板(全局上下文)，其中 \$currency 为货币种类，\$money 为货币数量")
    val rewardTemplateGlobal by value("★ \$currency * \$money\n")
    @ValueName("reward-template-group")
    @ValueDescription("签到奖励模板(群聊上下文)，其中 \$currency 为货币种类，\$money 为货币数量")
    val rewardTemplateGroup by value("☆ \$currency * \$money\n")

    fun getRewardTemple(info: RewardInfo): String =
        (if (info.isGlobal) rewardTemplateGlobal else rewardTemplateGroup)
        .replace("\$currency", info.currency.name)
        .replace("\$money", info.money.toString())

    @ValueName("rewards")
    @ValueDescription("签到奖励，格式如下\n" +
            "奖励金钱到群聊上下文 group:货币种类:数量\n" +
            "奖励金钱到全局上下文 global:货币种类:数量\n" +
            "数量可填固定数量，如 50，也可以用-符号连接两个整数来表示随机数，如 50-100\n" +
            "随机数的上界和下界均可取得")
    val rewards by value(listOf("group:mirai-coin:100"))
    val realRewards = mutableListOf<Reward>()
    class Reward(
        val isGlobal: Boolean,
        val currency: EconomyCurrency,
        val money: IMoney
    )
    class RewardInfo(
        val isGlobal: Boolean,
        val currency: EconomyCurrency,
        val money: Double
    )
    @OptIn(ConsoleExperimentalApi::class)
    fun loadRewards() {
        realRewards.clear()
        MiraiDailySign.logger.verbose("正在加载配置 $saveName 的签到奖励")
        for (line in rewards){
            val params = line.split(":")
            val global = when (params.getOrNull(0) ?: error("不存在参数: 经济上下文")) {
                "global" -> true
                "group" -> false
                else -> error("参数错误: 应为 global 或 group")
            }
            val currencyName = params.getOrNull(1) ?: error("不存在参数: 货币种类")
            val currency = EconomyService.basket[currencyName] ?: error("货币种类 $currencyName 不存在")
            val p2 = params.getOrNull(2) ?: error("不存在参数: 金钱数量")
            val money = p2.toFixedMoney() ?: p2.toRandomMoney() ?: error("参数错误: 输入的金钱 $p2 无效")
            realRewards.add(Reward(global, currency, money))
        }
    }
    fun giveRewardsTo(group: Group, user: User): List<RewardInfo> {
        val result = mutableListOf<RewardInfo>()
        realRewards.forEach {
            it.run {
                val finalMoney = money()
                if (isGlobal) globalEconomy {
                    service.account(user) += (currency to finalMoney)
                } else group.economy {
                    service.account(user) += (currency to finalMoney)
                }
                result.add(RewardInfo(isGlobal, currency, finalMoney))
            }
        }
        return result
    }
}
val DailySignConfig.isDefaultConfig: Boolean
    get() = saveName == "default"

interface IMoney{
    operator fun invoke(): Double
}
class FixedMoney(
    private val money: Double
): IMoney {
    override fun invoke(): Double = money
}
class RandomMoney(
    private val min: Int,
    private val max: Int
): IMoney {
    override fun invoke(): Double = Random.nextInt(min, max + 1).toDouble()
}
fun String.toFixedMoney(): FixedMoney? {
    return FixedMoney(this.toDoubleOrNull() ?: return null)
}
fun String.toRandomMoney(): RandomMoney? {
    return RandomMoney(
        substringBefore("-").toIntOrNull() ?: return null,
        substringAfter("-").toIntOrNull() ?: return null
    )
}
