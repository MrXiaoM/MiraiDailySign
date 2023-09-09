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
* [ ] 连续签到奖励
* [x] 月签到日历
* [ ] 渲染月签到日历并发送

未经测试

## 安装

到 [Releases](https://github.com/MrXiaoM/MiraiDailySign/releases) 下载插件并放入 plugins 文件夹进行安装

> 保证你的 mirai 版本大于或等于 2.11.0  
> 下载 MiraiDailySign-*.mirai2.jar  
> 安装完毕后，编辑配置文件作出你想要的修改。在控制台执行 `/dailysign reload` 重载配置即可~

## 配置教程

打开目录 `./config/top.mrxiaom.mirai.dailysign/groups/`，使用文本编辑器打开 `default.yml`，按照自己的需要修改配置项。

如果你需要不同配置，请把 `default.yml` 复制改名为 `任意名称.yml` 再进行编辑。

