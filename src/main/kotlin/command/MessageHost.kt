package top.mrxiaom.mirai.dailysign.command

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.console.plugin.id
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Contact.Companion.uploadImage
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.*
import top.mrxiaom.mirai.dailysign.*
import top.mrxiaom.mirai.dailysign.config.DailySignConfig
import top.mrxiaom.mirai.dailysign.config.isDefaultConfig
import top.mrxiaom.mirai.dailysign.data.SignUser
import top.mrxiaom.mirai.dailysign.utils.filterAt
import top.mrxiaom.mirai.dailysign.utils.hasAtBot
import top.mrxiaom.mirai.dailysign.utils.textOnly
import top.mrxiaom.mirai.dailysign.utils.split
import java.net.URL

typealias main = MiraiDailySign

object MessageHost : SimpleListenerHost() {

    @EventHandler
    suspend fun GroupMessageEvent.listen() {
        // 有 @ 其他人时跳过
        if (message.filterAt { it.target != bot.id }.isNotEmpty()) return
        // 遍历配置
        for (config in main.loadedConfigs) {
            if (processConfig(config)) return
        }
    }
    suspend fun GroupMessageEvent.processConfig(config: DailySignConfig): Boolean {
        // at 检查
        if (config.at && !hasAtBot()) return false
        // 关键词检查
        if (!config.keywords.contains(textOnly().trim())) return false
        // 权限检查
        if (!config.hasPerm(sender)) {
            if (config.denyMessage.isNotEmpty()) {
                val msg = config.denyMessage.joinToString()
                    .replace("\$perm", main.id + ":" + config.permission)
                group.sendMessage(replaceRichVariable(msg, group, sender, QuoteReply(source)))
            }
            return true
        }
        // 获取用户
        val user = main.getUser(sender.id)
        // 全局配置
        if (config.global) {
            // 签到
            if (user.global.sign()) {
                val reward = config.giveRewardsTo(group, sender)
                sendSuccessMessage(config, user.global, reward, this)
            } else {
                sendAlreadySignMessage(config, this)
            }
        }
        // 特定配置
        else {
            val info = user.groups[group.id] ?: SignUser.SignInfo()
            // 签到
            if (info.sign()) {
                val reward = config.giveRewardsTo(group, sender)
                sendSuccessMessage(config, info, reward, this)
                user.groups[group.id] = info
            } else {
                // 发送已签到过的提示
                sendAlreadySignMessage(config, this)
            }
        }
        return true
    }

    suspend fun sendSuccessMessage(
        config: DailySignConfig,
        info: SignUser.SignInfo,
        rewardInfo: List<DailySignConfig.RewardInfo>,
        event: GroupMessageEvent
    ) {
        val rewards = rewardInfo.map { config.getRewardTemple(it) }
        // 通过js替换变量
        val msg = main.runReplaceScript(
            config.successMessage.joinToString("\n")
                .replace("\$lasting", info.lastingSignDays.toString())
                .replace("\$rewards", rewards.joinToString()),
            event, config
        )
        // 替换富文本变量 (头像、@、回复等)
        event.group.sendMessage(replaceRichVariable(msg, event.group, event.sender, QuoteReply(event.source)))
    }
    suspend fun sendAlreadySignMessage(
        config: DailySignConfig,
        event: GroupMessageEvent
    ) {
        // 通过js替换变量
        val msg = main.runReplaceScript(
            config.alreadySignMessage.joinToString("\n"),
            event, config
        )
        // 替换富文本变量 (头像、@、回复等)
        event.group.sendMessage(replaceRichVariable(msg, event.group, event.sender, QuoteReply(event.source)))
    }

    /**
     * 替换富文本变量
     *
     * $quote 回复消息
     *
     * $at @user(好友无效)
     *
     * $avatar user的头像
     *
     * @param msg 原文本
     * @param subject 联系人，如群聊或好友
     * @param user 用户，如群员或好友
     * @param quote 回复消息
     * @return 替换完成的消息链
     */
    fun replaceRichVariable(
        msg: String,
        subject: Contact,
        user: User,
        quote: QuoteReply? = null
    ): MessageChain = mutableListOf<SingleMessage>().also {
        if (quote != null && msg.contains("\$quote")) it.add(quote)
        it.addAll(Regex("\\\$at|\\\$avatar").split<SingleMessage>(msg.replace("\$quote", "")) { s, isMatched ->
            if (!isMatched) PlainText(s)
            else when (s) {
                "\$at" -> At(user.id)
                "\$avatar" -> runBlocking(Dispatchers.IO) {
                    try {
                        subject.uploadImage(URL(user.avatarUrl).openStream())
                    } catch (_: Throwable) {
                        PlainText(user.id.toString())
                    }
                }

                else -> PlainText(s)
            }
        })
    }.toMessageChain()
}
