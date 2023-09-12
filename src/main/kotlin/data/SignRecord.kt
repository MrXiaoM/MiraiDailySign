package top.mrxiaom.mirai.dailysign.data

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.savePluginData
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import top.mrxiaom.mirai.dailysign.MiraiDailySign
import java.util.*

object SignRecord : AutoSavePluginData(".record") {
    @ConsoleExperimentalApi
    override fun shouldPerformAutoSaveWheneverChanged(): Boolean = false

    var year by value(1970)
    var day by value(1)

    var globalSign by value(0)
    var groupSign by value(mutableMapOf<Long, Int>())

    fun increase(id: Long) {
        check()
        if (id <= 0) globalSign += 1
        else {
            val map = groupSign
            map[id] = map.getOrDefault(id, 0) + 1
            groupSign = map
        }
        MiraiDailySign.savePluginData(this)
    }

    fun check(): Boolean {
        val now = Calendar.getInstance()
        val clear = isOutOfDate(now)
        if (clear) {
            year = now[Calendar.YEAR]
            day = now[Calendar.DAY_OF_YEAR]
            globalSign = 0
            groupSign = mutableMapOf()
        }
        return clear
    }

    private fun isOutOfDate(time: Calendar): Boolean {
        if (time[Calendar.YEAR] != year) return true
        if (time[Calendar.DAY_OF_YEAR] != day) return true
        return false
    }

    val global: Int
        get() {
            if (check()) MiraiDailySign.savePluginData(this)
            return globalSign
        }
    fun group(id: Long): Int {
        if (check()) MiraiDailySign.savePluginData(this)
        return groupSign[id] ?: 0
    }
}