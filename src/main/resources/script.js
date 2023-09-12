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

// 特殊方法
// 获取某人全局上下文的 Mirai币 数量 economy.getGlobalBalance(sender, "mirai-coin");
// 获取某人在当前群聊上下文的 Mirai币 数量 economy.getGroupBalance(subject, sender, "mirai-coin");

//////////////////[ MiraiDailySign ]/////////////////////

var monthArray = ["一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"];
var weekArray = ["星期日","星期一","星期二","星期三","星期四","星期五","星期六"];

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
    .replace("$week", weekArray[d.getDay()])
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
function signCalendar(p, data, isGlobal) {
    // 初始化画布
    p.init(400, 320);

    var d = new Date();
    var list = generateCalendar(data, d);

    var font = p.font("黑体", "NORMAL", 20);
    var fontTitle = p.font("黑体", "NORMAL", 32);
    var fontSmall = p.font("黑体", "NORMAL", 12);
    // 颜色是 ARGB 格式
    var bgColor = p.paint("#FFF3D8D1");
    var textColor = p.paint("#FF222222");
    var lineColor = p.paint("#FF545454", 1);
    p.clear(bgColor);
    var slotWidth = 44;
    var slotHeight = 38;

    var parentY = 64;
    var parentX = p.surface.width / 2 - 7 * slotWidth / 2;
    var dateYOffset = 24 + 16;

    // 渲染月份标题
    p.drawTextLine(monthArray[d.getMonth()], fontTitle, parentX, parentY - 13, textColor);
    var txtLastingSign = p.text("连续签到 " + data.lastingSignDays + " 天", font);
    // 连续签到信息
    p.drawTextLine(txtLastingSign, parentX + 7 * slotWidth - txtLastingSign.width, parentY - 12, textColor);

    // 渲染星期
    for (var i = 0; i < 7; i++) {
        var txtWeek = p.text(weekArray[i], fontSmall);
        p.drawTextLine(txtWeek, parentX + i * slotWidth + slotWidth / 2 - txtWeek.width / 2, parentY + 14, textColor);
    }
    // 渲染日期
    var line = 0;
    for (i in list) {
        var obj = list[i];
        if (obj.week == 0) line++;

        var day = String(obj.day);

        var x = parentX + obj.week * slotWidth;
        var y = parentY + dateYOffset + line * slotHeight;

        var txtDate = p.text(day, font);
        var txtStatus = p.text(obj.status, fontSmall);
        var dateColor = textColor;
        if (obj.status == "💡") {
            dateColor = p.paint("#FFFF866A");
        }
        // 注意: 渲染文字的坐标是文字的左下角基准线
        p.drawTextLine(txtDate, x + slotWidth / 2 - txtDate.width / 2, y, dateColor);
        p.drawTextLine(txtStatus, x + slotWidth / 2 - txtStatus.width / 2, y + txtStatus.height - 4, textColor);
    }
    var parentX2 = parentX + 7 * slotWidth;
    var parentY2 = parentY + dateYOffset + line * slotHeight + 18
    // 绘制竖线
    for (var i = 0; i <= 7; i++) {
        var x = parentX + i * slotWidth;
        p.drawLine(x, parentY, x, parentY2, lineColor);
    }
    // 绘制横线
    p.drawLine(parentX, parentY, parentX2, parentY, lineColor);
    for (var i = -1; i < line + 1; i++) {
        var y = parentY + dateYOffset + i * slotHeight + 18;
        p.drawLine(parentX, y, parentX2, y, lineColor);
    }
    // 绘制横线
    if (isGlobal) {
        p.drawTextLine("全局签到日历", fontSmall, 2, p.surface.height - 17, textColor);
    } else {
        p.drawTextLine("群 " + subject.id + " 签到日历", fontSmall, 2, p.surface.height - 17, textColor);
    }
    p.drawTextLine("Powered by MiraiDailySign v" + version, fontSmall, 2, p.surface.height - 4, textColor);
}

// 生成日历各日期位置数据
function generateCalendar(data, d) {
    // 插件记录的月份 和 js 获取的当前月份，都是从 0 开始的，0 代表一月
    var isThisMonth = d.getFullYear() == data.lastSignYear && d.getMonth() == data.lastSignMonth;
    var maxDate = new Date(d.getFullYear(), d.getMonth() + 1, 0).getDate();
    var cDate = d.getDate();
    // week == 0 是星期天
    var cWeek = d.getDay();
    var list = [];
    var addDate = function() {
        var status = "";
        // 检测已签到
        if (isThisMonth && data.hasDaySign(cDate)) status = "✔";
        // 检测未签到
        if (status == "" && cDate < d.getDate()) status = "❌";
        if (status == "" && cDate == d.getDate()) status = "💡";
        list.push({
            "day": cDate, "week": cWeek, "status": status
        });
    }

    while (cDate >= 1) {
        addDate();
        cWeek--;
        if (cWeek < 0) cWeek = 6;
        cDate--;
    }
    list.reverse();
    cDate = d.getDate() + 1;
    cWeek = d.getDay() + 1;
    if (cWeek > 6) cWeek = 0;
    while (cDate <= maxDate) {
        addDate();
        cWeek++;
        if (cWeek > 6) cWeek = 0;
        cDate++;
    }
    return list;
}