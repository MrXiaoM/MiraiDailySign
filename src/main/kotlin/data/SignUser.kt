package top.mrxiaom.mirai.dailysign.data

import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import java.util.Calendar.*

class SignUser(
    val id: Long
) : AutoSavePluginData(id.toString()) {

    @ConsoleExperimentalApi
    override fun shouldPerformAutoSaveWheneverChanged(): Boolean = false

    @Serializable
    class SignInfo(
        var lastSignYear: Int = 1970,
        var lastSignMonth: Int = 1,
        val signCalendarMonthly: MutableSet<Int> = mutableSetOf(),
        var lastingSignDays: Int = 0
    ){
        /**
         * 今日是否已签到
         */
        fun hasSign(): Boolean {
            val now = getInstance()
            if (now[YEAR] != lastSignYear) return false
            if (now[MONTH] != lastSignMonth) return false
            if (now[DAY_OF_MONTH] != (signCalendarMonthly.maxOrNull() ?: -1)) return false
            return true
        }

        /**
         * 是否连续签到
         */
        fun isLastingSign(): Boolean {
            val now = getInstance()
            if (now[YEAR] != lastSignYear) return false
            if (now[MONTH] != lastSignMonth) return false
            if (now[DAY_OF_MONTH] - 1 != (signCalendarMonthly.maxOrNull() ?: -1)) return false
            return true
        }

        fun sign(): Boolean {
            if (hasSign()) return false
            val now = getInstance()
            if (isLastingSign() || lastingSignDays == 0) lastingSignDays++
            if (lastSignYear != now[YEAR] || lastSignMonth != now[MONTH]) signCalendarMonthly.clear()
            lastSignYear = now[YEAR]
            lastSignMonth = now[MONTH]
            signCalendarMonthly.add(now[DAY_OF_MONTH])
            return true
        }
    }

    val global by value(SignInfo())
    val groups by value(mutableMapOf<Long, SignInfo>())
}