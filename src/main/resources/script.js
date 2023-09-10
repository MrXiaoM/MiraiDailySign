//////////////////[ MiraiDailySign ]/////////////////////

// 全局变量
// version: 插件版本号
// logger: 日志
// sender: 发送者
// subject: 群聊
// time: 发送者发送消息的时间戳 (秒)
// bot: 机器人
// message: 消息，建议 JSON.parse(message.serializeToJsonString()) 或 message.toString()
// source: 消息源

// 使用 Object.keys(变量) 可以查看 变量 的所有键，使用 logger.info(内容); 可以把日志内容打印到控制台。
// java/kotlin/javascript 的编写方法各不相同，调用前请先查看相关的键。

//////////////////[ MiraiDailySign ]/////////////////////

// 替换变量的主方法，方法名以及参数不可更改
function replace(s, config) {
    var d = new Date();
    var nameCardOrNick = sender.nameCard;
    if (nameCardOrNick == "") {
        nameCardOrNick = sender.nick;
    }
    return s
    .replace("$nameCardOrNick", nameCardOrNick)
    .replace("$id", sender.id)
    .replace("$date", d.getFullYear() + "年" + (d.getMonth() + 1) + "月" + d.getDate() + "日")
    .replace("$week", ["星期日","星期一","星期二","星期三",,"星期四","星期五","星期六"][d.getDay()])
    .replace("$time", d.getHours() + ":" + d.getMinutes() + ":" + d.getSeconds())
    .replace("$hitokoto", getRandomText())
    //.replace("$hitokoto", getFromInternet())

    ;
}

// 示例: 获取随机句子
// 本示例中的文本来自 https://hitokoto.cn/
function getRandomText() {
    var list = [
        "用代码表达语言的魅力，用代码书写山河的壮丽。",
        "黑云翻墨未遮山，白雨跳珠乱入船。",
        "曲终人散，黄粱一梦，该醒了！",
        "世界上没有一个人能代替另一个人。",
        "我们永远无法叫醒一个装睡的人。"
    ];
    return list[Math.floor(Math.random() * list.length)];
}

// 示例: 访问网络
function getFromInternet() {
    try {
        var url = new java.net.URL("https://v1.hitokoto.cn/");
        var conn = url.openConnection();
        var input = conn.getInputStream();
        var bytes = input.readAllBytes();
        input.close();
        // 以 UTF-8 编码读取 byte[] 为 java String 再转为 javascript String
        var jsonString = String(new java.lang.String(bytes, java.nio.charset.StandardCharsets.UTF_8));
        logger.info(jsonString);
        var json = JSON.parse(jsonString);
        return json.hitokoto;
    } catch (e) {
        return e;
    }
}

// 渲染签到日历图片的主方法，方法名以及参数不可更改
function signCalendar() {

}
