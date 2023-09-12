# MiraiDailySign [WIP]

适配 [经济服务 Mirai Economy Core](https://github.com/cssxsh/mirai-economy-core) 的每日签到插件。

## 简介

本插件为用户的金钱提供了来源。每日签到可领取自定义货币自定义数量的金钱。

## 编写中的功能

* [x] 签到命令
* [x] 签到权限
* [x] 每日签到机制
* [x] 分经济上下文给予奖励
* [x] 随机金钱
* [x] 连续签到奖励
* [x] 月签到月历
* [x] 渲染月签到月历并发送
* [ ] Coming soon

## 脚本

本插件附带一个默认脚本 `config/top.mrxiaom.mirai.dailysign/script.js`。

你可以使用该脚本来为签到反馈提示增加自定义变量，以及自定义月签到月历的样式。

在脚本开头有注释提示，脚本中有访问网络获取一言和获取脚本内随机句子的示例，尽情发挥你的想象吧！

## 安装

首先下载并安装以下前置：
* `必装` [mirai-economy-core](https://github.com/cssxsh/mirai-economy-core/releases) (经济核心)
* `可选` [mirai-skia-plugin](https://github.com/cssxsh/mirai-skia-plugin/releases) (绘图前置，用于绘制签到月历)

再到 [Releases](https://github.com/MrXiaoM/MiraiDailySign/releases) 下载插件并放入 plugins 文件夹进行安装。

控制台使用命令 `/dailysign reload` 即可重载脚本 `script.js` 以及 `groups` 中的所有签到配置！  
重载后会将载入的签到配置写入文件，以保证更新版本之后配置文件兼容，请在重载之前确保你编辑的配置文件已保存！  
用户数据在 data 文件夹，不建议手动编辑。用户数据实时保存，若在运行时编辑用户数据，你的更改可能会被覆盖。

> 保证你的 mirai 版本大于或等于 2.11.0  
> 下载 MiraiDailySign-*.mirai2.jar  
> 安装完毕后，编辑配置文件作出你想要的修改。在控制台执行 `/dailysign reload` 重载配置即可~

## 配置教程

打开目录 `./config/top.mrxiaom.mirai.dailysign/groups/`，使用文本编辑器打开 `default.yml`，按照自己的需要修改配置项。

如果你需要不同配置，请把 `default.yml` 复制改名为 `任意名称.yml` 再进行编辑。

