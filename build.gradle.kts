plugins {
    val kotlinVersion = "1.7.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.13.0"
}

group = "top.mrxiaom"
version = "0.1.0"

repositories {
    maven("https://maven.aliyun.com/repository/central")
    mavenCentral()
}

dependencies {
    compileOnly("xyz.cssxsh.mirai:mirai-economy-core:1.0.1")
    implementation("org.mozilla:rhino:1.7.14")
}

mirai {
    jvmTarget = JavaVersion.VERSION_11
}
