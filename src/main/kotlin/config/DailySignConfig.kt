package top.mrxiaom.mirai.dailysign.config

import net.mamoe.mirai.console.data.*
import net.mamoe.mirai.console.permission.AbstractPermitteeId
import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.console.permission.PermissionService.Companion.testPermission
import net.mamoe.mirai.console.permission.PermitteeId
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.events.GroupMessageEvent
import top.mrxiaom.mirai.dailysign.MiraiDailySign
import top.mrxiaom.mirai.dailysign.PermissionHolder
import top.mrxiaom.mirai.dailysign.config.CustomMoney.Companion.toCustomMoney
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
        get() = PermissionHolder[permission.replace("\$file", fileName), "签到配置${fileName}触发权限"]
    fun hasPerm(permitteeId: PermitteeId) : Boolean = perm.testPermission(permitteeId)
    fun hasPerm(member: Member) : Boolean = hasPerm(member.permitteeId)

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
        "\$nameCardOrNick (\$id) 签到成功!",
        "今天你已连续签到 \$lasting 天! 获得了以下奖励",
        "\$rewards",
        "今天是 \$date \$week \$time",
        "「\$hitokoto」"
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
    @ValueName("reward-template-global-continuously")
    @ValueDescription("""
        连续签到奖励模板(全局上下文)，其中 ${"\$"}currency 为货币种类，${"\$"}money 为货币数量
    """)
    val rewardTemplateGlobalContinuously by value("★ \$currency * \$money (连续签到奖励)\n")
    @ValueName("reward-template-group-continuously")
    @ValueDescription("""
        连续签到奖励模板(群聊上下文)，其中 ${"\$"}currency 为货币种类，${"\$"}money 为货币数量
    """)
    val rewardTemplateGroupContinuously by value("☆ \$currency * \$money (连续签到奖励)\n")

    fun getRewardTemple(info: RewardInfo): String =
        if (info.isContinuous) {
            if (info.isGlobal) rewardTemplateGlobalContinuously else rewardTemplateGroupContinuously
        } else {
            if (info.isGlobal) rewardTemplateGlobal else rewardTemplateGroup
        }
            .replace("\$currency", info.currency.name)
            .replace("\$money", info.money.toString())

    @ValueName("rewards")
    @ValueDescription("""
        签到奖励，格式如下
        奖励金钱到群聊上下文 group:货币种类:数量
        奖励金钱到全局上下文 global:货币种类:数量
        
        数量可按以下规则填写 (以下示例均为完整示例):
        【固定数量】如 global:mirai-coin:50
        【随机数量 用-符号连接】如 global:mirai-coin:50-100
        【自定义脚本 js:函数名】如 global:mirai-coin:js:myMethod
        
        随机数的上界和下界均可取得
        如果用自定义脚本，需要在 script.js 中添加相应函数，返回货币数值，举个例子
        function myMethod() {
            return Math.random() * 100 + 50; // 50-150 随机数
        }
    """)
    val rewards by value(listOf(
        "global:mirai-coin:100"
    ))

    @ValueName("rewards-continuously")
    @ValueDescription("""
        连续签到额外奖励，格式为签到天数后面跟上奖励列表
        奖励列表的编写规则与上方签到奖励相同
    """)
    val rewardsContinuously by value(mapOf(
        3 to listOf(
            "global:mirai-coin:50",
            "group:mirai-coin:16"
        ),
        5 to listOf(
            "global:mirai-coin:50"
        ),
        7 to listOf(
            "global:mirai-coin:100"
        ),
    ))

    private val realRewards = mutableListOf<Reward>()
    private val realRewardsContinuously = mutableMapOf<Int, List<Reward>>()

    /**
     * 由 rewards 反序列化而来的签到奖励配置
     */
    class Reward(
        val isGlobal: Boolean,
        val currency: EconomyCurrency,
        val money: IMoney
    ) {
        companion object {
            operator fun get(line: String): Reward {
                val params = java.lang.String(line).split(":", 3)
                val global = when (params.getOrNull(0) ?: error("不存在参数: 经济上下文")) {
                    "global" -> true
                    "group" -> false
                    else -> error("参数错误: 经济上下文应为 global 或 group")
                }
                val currencyName = params.getOrNull(1) ?: error("不存在参数: 货币种类")
                val currency = EconomyService.basket[currencyName] ?: error("货币种类 $currencyName 不存在")
                val p2 = params.getOrNull(2) ?: error("不存在参数: 金钱数量")
                val money = p2.toFixedMoney() ?: p2.toRandomMoney() ?: p2.toCustomMoney() ?: error("参数错误: 输入的金钱 $p2 无效")
                return Reward(global, currency, money)
            }
        }
    }

    /**
     * 签到后返回的签到结果信息
     */
    class RewardInfo(
        val isGlobal: Boolean,
        val isContinuous: Boolean,
        val currency: EconomyCurrency,
        val money: Double
    )
    @OptIn(ConsoleExperimentalApi::class)
    fun loadRewards() {
        MiraiDailySign.logger.verbose("正在加载配置 $saveName 的签到奖励")
        // 普通奖励
        realRewards.clear()
        realRewards.addAll(rewards.map{ Reward[it] })
        // 连续签到奖励
        realRewardsContinuously.clear()
        for ((times, list) in rewardsContinuously.entries) {
            realRewardsContinuously[times] = list.map { Reward[it] }
        }
    }
    fun giveRewards(event: GroupMessageEvent): List<RewardInfo> {
        val result = mutableListOf<RewardInfo>()
        realRewards.forEach {
            it.run {
                val finalMoney = money(event)
                if (isGlobal) globalEconomy {
                    service.account(event.sender) += (currency to finalMoney)
                } else event.group.economy {
                    service.account(event.sender) += (currency to finalMoney)
                }
                result.add(RewardInfo(isGlobal, false, currency, finalMoney))
            }
        }
        return result
    }
    fun giveContinuousRewards(event: GroupMessageEvent, times: Int): List<RewardInfo> {
        val result = mutableListOf<RewardInfo>()
        realRewardsContinuously[times]?.forEach {
            it.run {
                val finalMoney = money(event)
                if (isGlobal) globalEconomy {
                    service.account(event.sender) += (currency to finalMoney)
                } else event.group.economy {
                    service.account(event.sender) += (currency to finalMoney)
                }
                result.add(RewardInfo(isGlobal, true, currency, finalMoney))
            }
        }
        return result
    }
}

interface IMoney{
    operator fun invoke(event: GroupMessageEvent): Double
}

/**
 * 固定金钱
 */
class FixedMoney(
    val money: Double
): IMoney {
    override fun invoke(event: GroupMessageEvent): Double = money
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
    override fun invoke(event: GroupMessageEvent): Double = Random.nextInt(min, max + 1).toDouble()
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

class CustomMoney(
    val method: String
): IMoney {
    override fun invoke(event: GroupMessageEvent): Double {
        return MiraiDailySign.runInJavaScript(event, method)?.toDoubleOrNull() ?: error("脚本返回值异常")
    }
    companion object {
        fun String.toCustomMoney(): CustomMoney? {
            return if (startsWith("js:"))
                CustomMoney(this.substring(3))
            else null
        }
    }
}
