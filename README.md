# MiraiDailySign

[![](https://shields.io/github/downloads/MrXiaoM/MiraiDailySign/total)](https://github.com/MrXiaoM/MiraiDailySign/releases) [![](https://img.shields.io/badge/mirai--console-2.15.0-blue)](https://github.com/mamoe/mirai) [![](https://img.shields.io/badge/MiraiForum-post-yellow)](https://mirai.mamoe.net/topic/2492)

拥有极高自由度的签到插件。适配 [经济服务 Mirai Economy Core](https://github.com/cssxsh/mirai-economy-core)。

## 简介

本插件为用户的经济服务金钱提供了来源。每日签到可领取自定义货币自定义数量的金钱。

## 功能

* [x] 高度自定义的回复格式与变量
* [x] 分经济上下文给予奖励
* [x] 给予随机金钱
* [x] 通过脚本给予自定义金钱
* [x] 日签到记录
* [x] 连续签到奖励
* [x] 月签到月历
* [ ] Coming soon

## 脚本

本插件附带一个默认脚本 `config/top.mrxiaom.mirai.dailysign/script.js`。

你可以使用该脚本
* 为签到反馈提示增加自定义变量
* 自定义月签到月历的样式
* 自定义签到奖励算法

在脚本开头有注释提示，脚本中有访问网络获取一言和获取脚本内随机句子的示例，尽情发挥你的想象吧！

关于自定义月签到月历的函数 `function signCalendar(p, data, isGlobal)` 中的参数 `p`，详细用法请见 [SurfaceHelper.kt](src/main/kotlin/utils/SurfaceHelper.kt) 的源码注释。

## 安装

首先下载并安装以下前置：
* `必装` [mirai-economy-core](https://github.com/cssxsh/mirai-economy-core/releases) (经济核心)
* `可选` [mirai-skia-plugin](https://github.com/cssxsh/mirai-skia-plugin/releases) (绘图前置，用于绘制签到月历)

再到 [Releases](https://github.com/MrXiaoM/MiraiDailySign/releases) 下载插件并放入 plugins 文件夹进行安装。

如果你觉得本插件好用，不妨给本帖`点赞`或给 Github 仓库点个 `Star`，感谢。

控制台使用命令 `/dailysign reload` 即可重载脚本 `script.js` 以及 `groups` 中的所有签到配置！  
重载后会将载入的签到配置写入文件，以保证更新版本之后配置文件兼容，请在重载之前确保你编辑的配置文件已保存！  
用户数据在 data 文件夹，不建议手动编辑。用户数据实时保存，若在运行时编辑用户数据，你的更改可能会被覆盖。

> 保证你的 mirai 版本大于或等于 2.11.0  
> 下载 MiraiDailySign-*.mirai2.jar  
> 安装完毕后，编辑配置文件作出你想要的修改。在控制台执行 `/dailysign reload` 重载配置即可~

## 其它插件

推荐与其它已适配经济系统的插件搭配使用。欢迎在 [Pull Requests](https://github.com/MrXiaoM/MiraiDailySign/pulls) 补充该列表，仅接受补充 [MiraiForum](https://mirai.mamoe.net/) 帖子链接，要求插件开源。

<!-- 补充链接时，请保持 https://mirai.mamoe.net/topic/帖子ID 的格式，请删除链接后面的帖子名称、回帖ID、页码等参数 -->

* [LoliYouWant](https://mirai.mamoe.net/topic/1515)
* [CommandYouWant](https://mirai.mamoe.net/topic/1703)

## 权限

| 权限                                                 | 说明         |
|----------------------------------------------------|------------|
| top.mrxiaom.mirai.dailysign:command.miraidailysign | 允许重载插件     |
| top.mrxiaom.mirai.dailysign:calendar               | 允许触发查看签到日历 |

签到命令也需要权限，默认权限是 `top.mrxiaom.mirai.dailysign:sign.default`，可以在配置文件中设置。

> 这里是内置权限系统的一些常用的给予权限命令，**不要乱加空格，不要乱删空格**
> 
> 给予某群所有人权限 `/perm permit m群号.* 权限`，如 `/perm permit m114514.* com.example:name`  
> 给予某群某人权限 `/perm permit m群号.QQ号 权限`，如 `/perm permit m114514.1919810 com.example:name`  
> 给予某人权限 `/perm permit QQ号 权限`，如 `/perm permit 1919810 com.example:name`

## 配置教程

打开目录 `./config/top.mrxiaom.mirai.dailysign/groups/`，使用文本编辑器打开 `default.yml`，按照自己的需要修改配置项。

如果你需要不同配置，请把 `default.yml` 复制改名为 `任意名称.yml` 再进行编辑。

## 用法

如果没有编辑过配置文件的话，用法如下
```
@机器人 签到
@机器人 签到日历
@机器人 群签到日历
```
默认需要at，可以设置不at，为了避免机器人之间冲突，`强烈建议`开启需要at。

## 捐助

前往 [爱发电](https://afdian.net/a/mrxiaom) 捐助我。
