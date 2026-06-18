plugins {
    kotlin("jvm") version "2.0.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "net.azisaba"
version = "3.1.0"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(kotlin("stdlib"))
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    implementation("redis.clients:jedis:5.1.3")
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("commons-codec:commons-codec:1.17.1")
}

tasks {
    compileKotlin { kotlinOptions.jvmTarget = "21" }
    compileTestKotlin { kotlinOptions.jvmTarget = "21" }

    shadowJar {
        exclude("org.jetbrains.annotations")
        relocate("kotlin", "net.azisaba.yukitexture.libs.kotlin")
        relocate("com.github.kittinunf.fuel", "net.azisaba.yukitexture.libs.com.github.kittinunf.fuel")
        relocate("com.github.kittinunf.result", "net.azisaba.yukitexture.libs.com.github.kittinunf.result")
        relocate("org.apache.commons.codec", "net.azisaba.yukitexture.libs.org.apache.commons.codec")
        relocate("redis.clients", "net.azisaba.yukitexture.libs.redis.clients")
        minimize()
    }
}