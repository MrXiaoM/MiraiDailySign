package top.mrxiaom.mirai.dailysign.command

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.console.plugin.id
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Contact.Companion.uploadImage
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.*
import top.mrxiaom.mirai.dailysign.*
import top.mrxiaom.mirai.dailysign.config.DailySignConfig
import top.mrxiaom.mirai.dailysign.data.SignUser
import java.net.URL

typealias main = MiraiDailySign

object MessageHost : SimpleListenerHost() {

    @OptIn(ConsoleExperimentalApi::class)
    @EventHandler
    suspend fun GroupMessageEvent.listen() {
        val config = dailySign()
        if (config.at && !hasAtBot()) return
        if (!config.keywords.contains(textOnly().trim())) return
        if (!config.hasPerm(sender)) {
            if (config.denyMessage.isNotEmpty()) {
                val msg = config.denyMessage.joinToString()
                    .replace("\$perm", main.id + ":" + config.permission)
                group.sendMessage(replaceRichVariable(msg, group, sender, QuoteReply(source)))
            }
            return
        }
        val user = main.getUser(sender.id)
        // global
        if (config.saveName == "default") {
            if (user.global.hasSign()) {
                sendAlreadySignMessage(config, this)
                return
            }
            if (user.global.sign()) {
                val reward = config.giveRewardsTo(group, sender)
                sendSuccessMessage(config, user.global, reward, this)
            }
        }
        else {
            val info = user.groups[group.id]?: SignUser.SignInfo()
            if (info.hasSign()) {
                sendAlreadySignMessage(config, this)
                return
            }
            if (info.sign()) {
                val reward = config.giveRewardsTo(group, sender)
                sendSuccessMessage(config, info, reward, this)
                user.groups[group.id] = info
            }
        }
    }

    suspend fun sendSuccessMessage(
        config: DailySignConfig,
        info: SignUser.SignInfo,
        rewardInfo: List<DailySignConfig.RewardInfo>,
        event: GroupMessageEvent
    ) {
        val rewards = rewardInfo.map { config.getRewardTemple(it) }
        val msg = MiraiDailySign.runReplaceScript(
            config.successMessage.joinToString("\n")
                .replace("\$lasting", info.lastingSignDays.toString())
                .replace("\$rewards", rewards.joinToString()),
            event
        )
        event.group.sendMessage(replaceRichVariable(msg, event.group, event.sender, QuoteReply(event.source)))
    }
    suspend fun sendAlreadySignMessage(
        config: DailySignConfig,
        event: GroupMessageEvent
    ) {
        val msg = MiraiDailySign.runReplaceScript(
            config.alreadySignMessage.joinToString("\n"),
            event
        )
        event.group.sendMessage(replaceRichVariable(msg, event.group, event.sender, QuoteReply(event.source)))
    }
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
