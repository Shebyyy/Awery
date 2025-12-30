import android.databinding.tool.ext.joinToCamelCase
import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(composeLibs.plugins.multiplatform)
    alias(composeLibs.plugins.compiler)
    alias(composeLibs.plugins.hotReload)
    alias(androidLibs.plugins.app)
}

/* ========================= LOCALES ========================= */

fun getLocales(): Collection<String> {
    return rootProject.projectDir
        .resolve("resources/src/commonMain/composeResources")
        .list()!!
        .filter { it.startsWith("values") }
        .map {
            if (it == "values") "en"
            else it.substringAfter("values-")
        }
}

/* ========================= KOTLIN ========================= */

kotlin {
    jvmToolchain(
        properties["awery.java.desktop"].toString().toInt()
    )

    jvm("desktop")

    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget = JvmTarget.fromTarget(
                        properties["awery.java.android"].toString()
                    )
                }
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core)
                implementation(projects.ui)
                implementation(projects.data)
                implementation(projects.resources)
                implementation(projects.extension.sdk)
                implementation(projects.extension.loaders)
                implementation(projects.extension.loaders.androidCompat)

                implementation(libs.kotlinx.serialization.json)
                implementation(libs.filekit.core)
                implementation(libs.filekit.dialogs)
                implementation(libs.filekit.coil)
                implementation(composeLibs.coil.compose)
            }
        }

        androidMain.dependencies {
            implementation(androidLibs.core)
            implementation(androidLibs.appcompat)
            implementation(androidLibs.splashscreen)
            implementation(androidLibs.compose.activity)
            implementation(androidLibs.material)
            implementation("androidx.work:work-runtime-ktx:2.10.4")
        }

        val desktopMain by getting {
            dependencies {
                implementation(composeLibs.navigation.jetpack)
                implementation(composeLibs.jewel.standalone)
                implementation(composeLibs.jewel.window)
                implementation(libs.kotlinx.coroutines.desktop)

                implementation(composeLibs.desktop.get().let {
                    "${it.group}:${it.name}:${it.version}"
                }) {
                    exclude(group = "org.jetbrains.compose.material")
                }

                implementation("com.github.milchreis:uibooster:1.21.1")
                implementation("com.formdev:flatlaf-intellij-themes:3.6")
            }
        }
    }
}

/* ========================= ANDROID ========================= */

android {
    namespace = "com.mrboomdev.awery"
    compileSdk = properties["awery.sdk.target"].toString().toInt()

    defaultConfig {
        versionName =
            properties["awery.app.versionName"].toString()
        versionCode =
            properties["awery.app.versionCode"].toString().toInt()

        minSdk =
            properties["awery.sdk.min"].toString().toInt()
        targetSdk =
            properties["awery.sdk.target"].toString().toInt()
    }

    signingConfigs {
        create("release") {
            val storeFilePath =
                System.getProperty("android.injected.signing.store.file")

            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword =
                    System.getProperty("android.injected.signing.store.password")
                keyAlias =
                    System.getProperty("android.injected.signing.key.alias")
                keyPassword =
                    System.getProperty("android.injected.signing.key.password")
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["APP_NAME"] = "Awery Debug"
        }

        release {
            signingConfig =
                signingConfigs.getByName("release")
            manifestPlaceholders["APP_NAME"] = "Awery"
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        localeFilters += getLocales()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

/* ========================= DESKTOP ========================= */

enum class DesktopTarget(
    val format: TargetFormat
) {
    WINDOWS(TargetFormat.Exe),
    LINUX(TargetFormat.Deb)
}

compose.desktop {
    application {
        mainClass = "com.mrboomdev.awery.app.MainKt"

        nativeDistributions {
            includeAllModules = true

            /**
             * JBR MUST BE PROVIDED VIA ENV (CI + LOCAL)
             */
            javaHome = file(
                System.getenv("JBR_HOME")
                    ?: error("JBR_HOME is not set")
            )

            targetFormats =
                DesktopTarget.values().map { it.format }.toSet()

            packageName = "Awery"
            packageVersion =
                properties["awery.app.versionName"].toString()

            windows {
                iconFile =
                    rootProject.file("app_icon.ico")
                shortcut = true
                menu = true
                menuGroup = "Awery"
                perUserInstall = true
            }
        }
    }
}

/* ========================= ANDROID LOCALES ========================= */

tasks.register("generateAndroidLocaleConfig") {
    val outputDir =
        layout.projectDirectory.dir("src/androidMain/res/xml")
    val outputFile =
        outputDir.file("awery_generated_locales_config.xml")
    val locales = getLocales()

    inputs.property("locales", locales)
    outputs.file(outputFile)

    doLast {
        outputDir.asFile.mkdirs()
        outputFile.asFile.writeText(
            buildString {
                appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
                appendLine(
                    """<locale-config xmlns:android="http://schemas.android.com/apk/res/android">"""
                )
                locales.forEach {
                    appendLine("""    <locale android:name="$it" />""")
                }
                appendLine("</locale-config>")
            }
        )
    }
}.let { tasks.named("preBuild").dependsOn(it) }
