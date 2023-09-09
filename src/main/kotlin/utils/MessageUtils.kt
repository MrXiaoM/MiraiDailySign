package top.mrxiaom.mirai.dailysign.utils

import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.PlainText

fun MessageChain.filterAt(predicate: (At) -> Boolean): List<At> = filterIsInstance<At>().filter { predicate(it) }
fun MessageEvent.filterAt(predicate: (At) -> Boolean): List<At> = message.filterAt(predicate)
fun MessageEvent.hasAtBot(): Boolean = filterAt { it.target == bot.id }.isNotEmpty()
fun MessageChain.textOnly(): String = filterIsInstance<PlainText>().joinToString()
fun MessageEvent.textOnly(): String = message.textOnly()
/**
 * 分隔字符串
 * @param input 需要分隔的字符串
 * @param transform 转换器，返回 null 时不添加该项到结果
 */
fun <T> Regex.split(
    input: CharSequence,
    transform: (s: String, isMatched: Boolean) -> T?
): List<T> {
    val list = mutableListOf<T>()
    var index = 0
    for (result in findAll(input)) {
        val first = result.range.first
        val last = result.range.last
        if (first > index) {
            val value = transform(input.substring(index, first), false)
            if (value != null) list.add(value)
        }
        val value = transform(input.substring(first, last + 1), true)
        if (value != null) list.add(value)
        index = last + 1
    }
    if (index < input.length) {
        val value = transform(input.substring(index), false)
        if (value != null) list.add(value)
    }
    return list
}
