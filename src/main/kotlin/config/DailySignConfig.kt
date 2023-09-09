package top.mrxiaom.mirai.dailysign.config

import net.mamoe.mirai.console.data.*
import net.mamoe.mirai.console.permission.AbstractPermitteeId
import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.console.permission.PermissionService.Companion.testPermission
import net.mamoe.mirai.console.permission.PermitteeId
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.User
import top.mrxiaom.mirai.dailysign.MiraiDailySign
import top.mrxiaom.mirai.dailysign.PermissionHolder
import top.mrxiaom.mirai.dailysign.config.FixedMoney.Companion.toFixedMoney
import top.mrxiaom.mirai.dailysign.config.RandomMoney.Companion.toRandomMoney
import xyz.cssxsh.mirai.economy.EconomyService
import xyz.cssxsh.mirai.economy.economy
import xyz.cssxsh.mirai.economy.globalEconomy
import xyz.cssxsh.mirai.economy.service.EconomyCurrency
import kotlin.random.Random

class DailySignConfig(
    val fileName: String = "default"
) : ReadOnlyPluginConfig("groups/$fileName") {
    @ValueName("global")
    @ValueDescription("""
        该配置是否为全局配置，全局签到的统计数据在所有群通用，关闭此项后签到将视为在某群签到，签到天数等统计数据独立
    """)
    val global by value(true)
    @ValueName("at")
    @ValueDescription("""
        需要 @ 机器人才能签到
    """)
    val at by value(true)
    @ValueName("keywords")
    @ValueDescription("""
        签到命令关键词，关键词会忽略消息前后空格
    """)
    val keywords by value(listOf("签到"))
    @ValueName("permission")
    @ValueDescription("""
        签到所需权限，留空为不需要权限
        若设置权限，则需要 top.mrxiaom.mirai.dailysign:你输入的值
        这个权限才能签到，其中 ${"\$"}file 会被替换成当前配置文件名(不含后缀)
        以默认配置文件为例，需要权限 top.mrxiaom.mirai.dailysign:sign.default
    """)
    val permission by value("sign.\$file")
    val perm: Permission
        get() = PermissionHolder[permission.replace("\$file", fileName)]
    fun hasPerm(permitteeId: PermitteeId) : Boolean = perm.testPermission(permitteeId)
    fun hasPerm(member: Member) : Boolean = hasPerm(AbstractPermitteeId.ExactMember(member.group.id, member.id))

    @ValueName("deny-message")
    @ValueDescription("""
        没有权限时的提示(列表，换行)，留空[]为不提示
        其中，${"\$"}quote 为回复消息，${"\$"}at 为 @，${"\$"}perm 为用户缺少的权限
    """)
    val denyMessage by value(listOf<String>())
    @ValueName("success-message")
    @ValueDescription("""
        签到成功时的提示(列表，换行)，留空[]为不提示
        其中，${"\$"}quote 为回复消息，${"\$"}at 为 @，${"\$"}lasting 为连续签到天数
        ${"\$"}rewards 为签到奖励，${"\$"}avatar 为用户头像(加载失败时显示QQ号)
        暂时懒得写表情，PRs welcome
        因为想到一些签到插件还会有什么奇奇怪怪的无用提示，所以我都给加上变量了，请编辑以下脚本查看
        config/top.mrxiaom.mirai.dailysign/replace.js
        如有需要可自行添加变量(仅文字)，每当需要替换变量的时候都会执行一次该脚本
    """)
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
    @ValueDescription("""
        今日已签到的提示(列表，换行)，留空[]为不提示
        其中，${"\$"}quote 为回复消息，${"\$"}at 为 @，${"\$"}avatar 为用户头像(加载失败时显示QQ号)
        变量同上
    """)
    val alreadySignMessage by value(listOf(
        "\$quote你今天已经签到过了"
    ))

    @ValueName("reward-template-global")
    @ValueDescription("""
        签到奖励模板(全局上下文)，其中 ${"\$"}currency 为货币种类，${"\$"}money 为货币数量
    """)
    val rewardTemplateGlobal by value("★ \$currency * \$money\n")
    @ValueName("reward-template-group")
    @ValueDescription("""
        签到奖励模板(群聊上下文)，其中 ${"\$"}currency 为货币种类，${"\$"}money 为货币数量
    """)
    val rewardTemplateGroup by value("☆ \$currency * \$money\n")

    fun getRewardTemple(info: RewardInfo): String =
        (if (info.isGlobal) rewardTemplateGlobal else rewardTemplateGroup)
        .replace("\$currency", info.currency.name)
        .replace("\$money", info.money.toString())

    @ValueName("rewards")
    @ValueDescription("""
        签到奖励，格式如下
        奖励金钱到群聊上下文 group:货币种类:数量
        奖励金钱到全局上下文 global:货币种类:数量
        数量可填固定数量，如 50，也可以用-符号连接两个整数来表示随机数，如 50-100
        随机数的上界和下界均可取得
    """)
    val rewards by value(listOf("group:mirai-coin:100"))
    private val realRewards = mutableListOf<Reward>()

    /**
     * 由 rewards 反序列化而来的签到奖励配置
     */
    class Reward(
        val isGlobal: Boolean,
        val currency: EconomyCurrency,
        val money: IMoney
    )

    /**
     * 签到后返回的签到结果信息
     */
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

interface IMoney{
    operator fun invoke(): Double
}

/**
 * 固定金钱
 */
class FixedMoney(
    val money: Double
): IMoney {
    override fun invoke(): Double = money
    companion object {
        fun String.toFixedMoney(): FixedMoney? {
            return FixedMoney(this.toDoubleOrNull() ?: return null)
        }
    }
}

/**
 * 有范围的随机金钱
 */
class RandomMoney(
    val min: Int,
    val max: Int
): IMoney {
    override fun invoke(): Double = Random.nextInt(min, max + 1).toDouble()
    companion object {
        fun String.toRandomMoney(): RandomMoney? {
            return if (!contains("-")) null
            else RandomMoney(
                substringBefore("-").toIntOrNull() ?: return null,
                substringAfter("-").toIntOrNull() ?: return null
            )
        }
    }
}
