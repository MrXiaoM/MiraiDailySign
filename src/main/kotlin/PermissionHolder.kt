package top.mrxiaom.mirai.dailysign

import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.console.permission.PermissionId
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.plugin.id

object PermissionHolder {
    private val permissions = mutableMapOf<String, Permission>()
    operator fun get(name: String, description: String = ""): Permission {
        return permissions[name] ?: PermissionService.INSTANCE.register(PermissionId(MiraiDailySign.id, name), description)
    }
}