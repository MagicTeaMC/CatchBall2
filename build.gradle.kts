plugins {
    java
    `java-library`
    `maven-publish`
    kotlin("jvm") version "1.8.0"
    id("com.gradleup.shadow") version "8.3.5"
    id("xyz.jpenilla.run-paper") version "2.2.4"
}

group = "com.github.nutt1101"
version = "2.0.2"
description = "CatchBall"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.destroystokyo.com/repository/maven-public//")
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
}

dependencies {
    api("org.bstats:bstats-bukkit:3.1.0")
    api("com.github.Paulem79:Spigot-UpdateChecker:3cfb265fb8")
    api("de.tr7zw:item-nbt-api:2.14.1-SNAPSHOT")
    api("cn.handyplus.lib.adapter:FoliaLib:1.1.5")
    compileOnly("org.spigotmc:spigot-api:1.20.5-R0.1-SNAPSHOT")
    compileOnly("io.lumine:Mythic-Dist:5.7.1")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.12")
    compileOnly("com.github.TechFortress:GriefPrevention:17.0.0")
    compileOnly("com.github.angeschossen:LandsAPI:7.8.5")
    implementation("com.github.Xyness:SimpleClaimSystem:1.11") {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 22)
        }
    }
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly ("io.github.fabiozumbi12.RedProtect:RedProtect-Core:8.1.2"){ exclude(group = "*")} // Core is not needed but allow access to all region methods
    compileOnly ("io.github.fabiozumbi12.RedProtect:RedProtect-Spigot:8.1.2"){ exclude(group = "*")}
    compileOnly(files("./libs/Residence5.1.6.4.jar"))
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

        relocate("com.jeff_media.updatechecker", "org.milkteamc.autotreechop.libs.updatechecker")

    }
}

tasks.jar {
    archiveFileName.set("CatchBall-${version}-original.jar")
}

tasks.runServer {
    minecraftVersion("1.21.1")
}

runPaper.folia.registerTask()
