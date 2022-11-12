package top.mrxiaom.mirai.dailysign.command

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.Contact.Companion.uploadImage
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
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
        val user = main.getUser(sender.id)
        // global
        if (config.saveName == "default") {
            if (user.global.hasSign()){
                // TODO 已签到消息
                return
            }
            if (user.global.sign()) {
                config.giveRewardsTo(group, sender)
            }
        }
        else {
            val info = user.groups[group.id]?: SignUser.SignInfo()
            if (info.hasSign()) {
                // TODO 已签到消息
                return
            }
            if (user.global.sign()) {
                config.giveRewardsTo(group, sender)
            }
        }
    }

    suspend fun sendSuccessMessage(
        config: DailySignConfig,
        info: SignUser.SignInfo,
        rewardInfo: List<DailySignConfig.RewardInfo>,
        event: GroupMessageEvent
    ) {
        val rewards = rewardInfo.map {
            (if (it.isGlobal) config.rewardTemplateGlobal else config.rewardTemplateGroup)
                .replace("\$currency", it.currency.name)
                .replace("\$money", it.money.toString())
        }
        val msg = MiraiDailySign.runReplaceScript(config.successMessage.joinToString("\n"), event)
            .replace("\$lasting", info.lastingSignDays.toString())
            .replace("\$rewards", rewards.joinToString())
        event.group.sendMessage(buildMessageChain {
            if (msg.contains("\$quote")) add(QuoteReply(event.source))
            addAll(Regex("\\\$at|\\\$avatar").split(msg.replace("\$quote", "")) { s, isMatched ->
                if (!isMatched) PlainText(s)
                else when (s) {
                    "\$at" -> At(event.sender)
                    "\$avatar" -> runBlocking(Dispatchers.IO) {
                        try {
                            event.group.uploadImage(URL(event.sender.avatarUrl).openStream())
                        } catch (_: Throwable) {
                            PlainText(event.sender.id.toString())
                        }
                    }

                    else -> PlainText(s)
                }
            })
        })
    }
}
