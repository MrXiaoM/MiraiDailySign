plugins {
    val kotlinVersion = "1.7.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.15.0"
}

group = "top.mrxiaom"
version = "0.1.0"

repositories {
    maven("https://repo.huaweicloud.com/repository/maven/")
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    compileOnly("xyz.cssxsh.mirai:mirai-economy-core:1.0.6")
    compileOnly("xyz.cssxsh.mirai:mirai-skia-plugin:1.3.2")
    implementation("org.mozilla:rhino:1.7.14")
}

mirai {
    jvmTarget = JavaVersion.VERSION_11
}
