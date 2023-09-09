function replace(s, event, config) {
    let d = new Date();
    let group = event.getGroup();
    let sender = event.getSender();
    return s
    .replaceAll("$namecardOrNick", sender.getNameCardOrNick())
    .replaceAll("$id", sender.getId())
    .replaceAll("$date", d.getFullYear() + "年" + (d.getMonth() + 1) + "月" + d.getDate() + "日")
    .replaceAll("$week", ["星期日","星期一","星期二","星期三",,"星期四","星期五","星期六"][d.getDay()])
    .replaceAll("$time", d.getHours() + ":" + d.getMinutes() + ":" + d.getSeconds());
}
