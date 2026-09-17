import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.3.20"
    kotlin("plugin.serialization") version "2.3.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20"
    id("org.jetbrains.compose") version "1.10.3"
    id("com.google.devtools.ksp") version "2.3.6"
    id("androidx.room") version "2.7.1"
}

group = "com.multiroom"
version = (findProperty("appVersion") as String?) ?: "1.0.0"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")

    // Bundled SQLite driver: no system library on the target machine.
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.sqlite:sqlite-bundled:2.5.1")
    ksp("androidx.room:room-compiler:2.7.1")
}

room {
    schemaDirectory("$projectDir/schemas")
}

kotlin { jvmToolchain(21) }

compose.desktop {
    application {
        mainClass = "com.multiroom.MainKt"

        jvmArgs(
            "--enable-native-access=ALL-UNNAMED",
            "-Djava.awt.headless=false",
        )

        buildTypes {
            release {
                proguard {
                    // ProGuard 7.7 rejects class file version 69.
                    isEnabled = false
                }
            }
        }

        nativeDistributions {
            modules(
                "java.net.http",
                "java.sql",
                "java.naming",
                "java.management",
                "jdk.unsupported",
            )

            targetFormats(TargetFormat.Msi)
            packageName = "Snapcast Rooms"
            packageVersion = project.version.toString()
            description = "Unofficial Windows client for managing Snapcast players"
            vendor = "Obinna Asuzu"
            copyright = "Copyright 2026. MIT licensed."
            licenseFile.set(rootProject.file("LICENSE"))

            windows {
                menuGroup = "Snapcast Rooms"
                perUserInstall = true
                menu = true
                shortcut = true
                // Changing this installs alongside instead of upgrading.
                upgradeUuid = "8f14e45f-ea2b-4f2c-9a61-2b7f1c0d5a33"
            }
        }
    }
}
