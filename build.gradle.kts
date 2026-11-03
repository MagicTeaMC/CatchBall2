plugins {
    java
    `java-library`
    `maven-publish`
    kotlin("jvm") version "2.2.0"
    id("com.gradleup.shadow") version "9.2.2"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "com.github.nutt1101"
version = "2.1.2"
description = "CatchBall"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenLocal()
    mavenCentral()
    exclusiveContent {
        forRepository { maven { url = uri("https://repo.papermc.io/repository/maven-public/") } }
        filter { includeGroup("io.papermc.paper") }
    }
    maven("https://libraries.minecraft.net") { content { includeGroup("com.mojang") } }
    maven("https://repo.momirealms.net/snapshots")
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
        url = uri("https://repo.papermc.io/repository/maven-public/")
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
    implementation("net.momirealms:sparrow-ui:beta.38")
    implementation("io.github.revxrsal:lamp.common:4.0.0-rc.18")
    implementation("io.github.revxrsal:lamp.bukkit:4.0.0-rc.18")
    implementation("dev.dejvokep:boosted-yaml:1.3.7")
    api("org.bstats:bstats-bukkit:3.1.0")
    api("de.tr7zw:item-nbt-api:2.16.0")
    implementation("com.github.Anon8281:UniversalScheduler:0.1.7")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
    testImplementation("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("io.lumine:Mythic-Dist:5.9.0")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.14")
    compileOnly("com.github.TechFortress:GriefPrevention:17.0.0")
    compileOnly("com.github.angeschossen:LandsAPI:7.15.4")
    compileOnly("com.github.Xyness:SimpleClaimSystem:1.12.3.2")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.palmergames.bukkit.towny:towny:0.101.2.6")
    compileOnly ("io.github.fabiozumbi12.RedProtect:RedProtect-Core:8.1.2"){ exclude(group = "*")} // Core is not needed but allow access to all region methods
    compileOnly ("io.github.fabiozumbi12.RedProtect:RedProtect-Spigot:8.1.2"){ exclude(group = "*")}
    compileOnly(files("./libs/Residence5.1.7.5.jar"))
}

val targetJavaVersion = 21

tasks.withType<JavaCompile>().configureEach {
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release.set(targetJavaVersion)
        options.compilerArgs.add("-parameters")
    }
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
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

        minimize {
            // Sparrow loads proxy classes and resources reflectively.
            exclude(dependency("net.momirealms:sparrow-ui:.*"))
            exclude(dependency("io.github.revxrsal:.*:.*"))
            exclude(dependency("com.github.Anon8281:UniversalScheduler:.*"))
        }

        relocate("com.github.Anon8281.universalScheduler", "tw.maoyue.catchball.libs.scheduler")
        relocate("net.momirealms.sparrow.ui", "tw.maoyue.catchball.libs.sparrow.ui")

        relocate("revxrsal.commands", "tw.maoyue.catchball.libs.lamp")
        relocate("dev.dejvokep.boostedyaml", "tw.maoyue.catchball.libs.boostedyaml")
        relocate("de.tr7zw.changeme.nbtapi", "tw.maoyue.catchball.libs.nbtapi")
        relocate("org.bstats", "tw.maoyue.catchball.libs.bstats")

    }
}

tasks.jar {
    archiveFileName.set("CatchBall-${version}-original.jar")
}

tasks.runServer {
    minecraftVersion("26.2")
}

runPaper.folia.registerTask()

tasks.test { useJUnitPlatform() }
