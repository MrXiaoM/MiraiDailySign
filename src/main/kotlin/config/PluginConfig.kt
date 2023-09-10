package top.mrxiaom.mirai.dailysign.config

import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.ValueName
import net.mamoe.mirai.console.data.value

object PluginConfig : ReadOnlyPluginConfig("config") {
    @ValueName("calendar-at")
    @ValueDescription("""
        是否需要@机器人才能获取签到日历
    """)
    val calendarAt by value(true)
    @ValueName("calendar-keywords")
    @ValueDescription("""
        签到日历触发关键词，关键词将忽略前后空格。
        将此项改为 [] 关闭签到日历功能。
    """)
    val calendarKeywords by value(listOf("签到日历"))

    @ValueName("calendar-permission")
    @ValueDescription("""
        是否需要有 top.mrxiaom.mirai.dailysign:calendar 权限才可以获取签到日历
    """)
    val calendarPermission by value(true)
}