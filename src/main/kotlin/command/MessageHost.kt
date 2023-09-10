package top.mrxiaom.mirai.dailysign.command

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.console.permission.PermissionService.Companion.testPermission
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.console.plugin.id
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Contact.Companion.uploadImage
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.*
import top.mrxiaom.mirai.dailysign.MiraiDailySign
import top.mrxiaom.mirai.dailysign.MiraiDailySign.save
import top.mrxiaom.mirai.dailysign.PermissionHolder
import top.mrxiaom.mirai.dailysign.config.DailySignConfig
import top.mrxiaom.mirai.dailysign.config.PluginConfig
import top.mrxiaom.mirai.dailysign.data.SignUser
import top.mrxiaom.mirai.dailysign.utils.filterAt
import top.mrxiaom.mirai.dailysign.utils.hasAtBot
import top.mrxiaom.mirai.dailysign.utils.split
import top.mrxiaom.mirai.dailysign.utils.textOnly
import java.net.URL

typealias main = MiraiDailySign

object MessageHost : SimpleListenerHost() {

    @EventHandler
    suspend fun GroupMessageEvent.listen() {
        // 有 @ 其他人时跳过
        if (message.filterAt { it.target != bot.id }.isNotEmpty()) return
        // 月签到日历
        if (processCalendar()) return
        // 遍历配置
        for (config in main.loadedConfigs) {
            if (processConfig(config)) return
        }
    }
    private suspend fun GroupMessageEvent.processCalendar(): Boolean {
        // at 检查
        if (PluginConfig.calendarAt && !hasAtBot()) return false
        // 关键词检查
        val textOnly = textOnly().trim()
        val global = PluginConfig.calendarKeywordsGlobal.contains(textOnly)
        if (!global && !PluginConfig.calendarKeywords.contains(textOnly)) return false
        // 权限检查
        if (PluginConfig.calendarPermission && !PermissionHolder["calendar"].testPermission(sender.permitteeId)) return false
        // 获取用户
        val user = main.getUser(sender.id)
        var data = if (global) user.global else user.groups[group.id]
        if (data == null) {
            data = SignUser.SignInfo()
            // user.global 不可能为 null，若 data 为 null，必是 user.groups[group.id] 为 null
            user.groups[group.id] = data
        }
        // TODO 绘制日历图片并发送
        return true
    }
    private suspend fun GroupMessageEvent.processConfig(config: DailySignConfig): Boolean {
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
                user.save()
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
                user.save()
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
        var msg = config.successMessage.joinToString("\n")
            .replace("\$lasting", info.lastingSignDays.toString())
            .replace("\$rewards", rewards.joinToString())
        main.runReplaceScript(
            event, "replace", msg, config
        )?.also { msg = it } ?: kotlin.run {
            msg += "\n(替换变量时出现异常，请联系机器人管理员)"
        }
        // 替换富文本变量 (头像、@、回复等)
        event.group.sendMessage(replaceRichVariable(msg, event.group, event.sender, QuoteReply(event.source)))
    }
    suspend fun sendAlreadySignMessage(
        config: DailySignConfig,
        event: GroupMessageEvent
    ) {
        // 通过js替换变量
        var msg = config.alreadySignMessage.joinToString("\n")
        main.runReplaceScript(
            event, "replace", msg, config
        )?.also { msg = it } ?: kotlin.run {
            msg += "\n(替换变量时出现异常，请联系机器人管理员)"
        }
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
