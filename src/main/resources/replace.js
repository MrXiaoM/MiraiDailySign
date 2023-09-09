// version: 插件版本号
// sender: 消息发送者
// subject: 群聊
// time: 消息发送时间(时间戳 秒)
// bot: 机器人
// message: 消息，建议先 message.serializeToJsonString() 转成 json 文本或 message.toString() 转成纯文本
// source: 消息源
// config: 相关配置文件

function replace(s) {
    var d = new Date();
    return s
    .replace("$namecardOrNick", sender.getNameCardOrNick())
    .replace("$id", sender.getId())
    .replace("$date", d.getFullYear() + "年" + (d.getMonth() + 1) + "月" + d.getDate() + "日")
    .replace("$week", ["星期日","星期一","星期二","星期三",,"星期四","星期五","星期六"][d.getDay()])
    .replace("$time", d.getHours() + ":" + d.getMinutes() + ":" + d.getSeconds());
}
