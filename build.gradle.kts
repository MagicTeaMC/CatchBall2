plugins {
    java
    `java-library`
    `maven-publish`
    kotlin("jvm") version "2.2.0"
    id("com.gradleup.shadow") version "8.3.8"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "com.github.nutt1101"
version = "2.1.2"
description = "CatchBall"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.destroystokyo.com/repository/maven-public/")
    }

    maven {
        url = uri("https://maven.enginehub.org/repo/")
    }

    maven {
        url = uri("https://mvn.lumine.io/repository/maven-public/")
    }

    maven {
        url = uri("https://jitpack.io")
    }

    maven {
        url = uri("https://raw.githubusercontent.com/FabioZumbi12/RedProtect/mvn-repo/")
    }

    maven {
        url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }

    maven {
        url = uri("https://repo.jeff-media.com/public/")
    }

    maven {
        url = uri("https://repo.codemc.io/repository/maven-public/")
    }

    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
    maven {
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
    maven {
        url = uri("https://repo.glaremasters.me/repository/towny/")
    }
}

dependencies {
    api("org.bstats:bstats-bukkit:3.1.0")
    api("com.jeff_media:SpigotUpdateChecker:3.0.4")
    api("de.tr7zw:item-nbt-api:2.15.3-20251004.181015-4")
    api("cn.handyplus.lib.adapter:FoliaLib:1.2.1")
    api("com.tchristofferson:ConfigUpdater:2.2-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.20.5-R0.1-SNAPSHOT")
    compileOnly("io.lumine:Mythic-Dist:5.9.0")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.14")
    compileOnly("com.github.TechFortress:GriefPrevention:17.0.0")
    compileOnly("com.github.angeschossen:LandsAPI:7.15.4")
    compileOnly("com.github.Xyness:SimpleClaimSystem:1.12.3.2")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.palmergames.bukkit.towny:towny:0.101.1.14")
    compileOnly ("io.github.fabiozumbi12.RedProtect:RedProtect-Core:8.1.2"){ exclude(group = "*")} // Core is not needed but allow access to all region methods
    compileOnly ("io.github.fabiozumbi12.RedProtect:RedProtect-Spigot:8.1.2"){ exclude(group = "*")}
    compileOnly(files("./libs/Residence5.1.7.5.jar"))
}

val targetJavaVersion = 21

tasks.withType<JavaCompile>().configureEach {
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release.set(targetJavaVersion)
    }
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.build {
    dependsOn("shadowJar")
}

tasks {
    shadowJar {
        archiveBaseName.set("CatchBall")
        archiveFileName.set("CatchBall-${project.version}.jar")

        configurations = listOf(project.configurations.runtimeClasspath.get())

        minimize()

        relocate("com.jeff_media.updatechecker", "tw.maoyue.catchball.libs.updatechecker")
        relocate("de.tr7zw.changeme.nbtapi", "tw.maoyue.catchball.libs.nbtapi")
        relocate("org.bstats", "tw.maoyue.catchball.libs.bstats")

    }
}

tasks.jar {
    archiveFileName.set("CatchBall-${version}-original.jar")
}

tasks.runServer {
    minecraftVersion("1.21.8")
}

runPaper.folia.registerTask()
