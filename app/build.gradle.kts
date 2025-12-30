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

fun getLocales(): Collection<String> {
    return rootProject.projectDir.resolve(
        "resources/src/commonMain/composeResources"
    ).list()!!
        .filter { it.startsWith("values") }
        .map {
            if (it == "values") "en"
            else it.substringAfter("values-")
        }
}

kotlin {
    jvmToolchain(properties["awery.java.desktop"].toString().toInt())
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
                implementation(projects.extension.loaders)

                implementation(libs.kotlinx.serialization.json)
                implementation(libs.filekit.core)
                implementation(libs.filekit.dialogs)
                implementation(libs.filekit.coil)
                implementation(composeLibs.coil.compose)

                implementation(projects.data)
                implementation(projects.resources)
                implementation(projects.extension.sdk)
                implementation(projects.extension.loaders.androidCompat)
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

                val osName = System.getProperty("os.name")
                val osArch = System.getProperty("os.arch")

                val targetOs = when {
                    osName == "Mac OS X" -> "macos"
                    osName.startsWith("Win") -> "windows"
                    osName.startsWith("Linux") -> "linux"
                    else -> error("Unsupported OS $osName")
                }

                val targetArch = when (osArch) {
                    "x86_64", "amd64" -> "x64"
                    "aarch64" -> "arm64"
                    else -> error("Unsupported arch $osArch")
                }

                runtimeOnly(
                    "org.jetbrains.skiko:skiko-awt-runtime-$targetOs-$targetArch:0.9.27"
                )
            }
        }
    }
}

android {
    namespace = "com.mrboomdev.awery"
    compileSdk = properties["awery.sdk.target"].toString().toInt()

    defaultConfig {
        versionName = properties["awery.app.versionName"].toString()
        versionCode = properties["awery.app.versionCode"].toString().toInt()
        targetSdk = properties["awery.sdk.target"].toString().toInt()
        minSdk = properties["awery.sdk.min"].toString().toInt()
    }

    /**
     * REQUIRED FOR CI SIGNING
     * android.injected.signing.* only overrides an EXISTING signingConfig
     */
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
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["APP_NAME"] = "Awery Debug"
        }

        release {
            signingConfig = signingConfigs.getByName("release")
            versionNameSuffix = "-release"
            manifestPlaceholders["APP_NAME"] = "Awery"
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        aidl = false
        dataBinding = false
        mlModelBinding = false
        prefab = false
        renderScript = false
        shaders = false
        viewBinding = false
        compose = true
        buildConfig = true
    }

    @Suppress("UnstableApiUsage")
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
    val os: String,
    val arch: String,
    val targetFormat: TargetFormat
) {
    WINDOWS_X64("windows", "x64", TargetFormat.Exe),
    LINUX_X64("linux", "x64", TargetFormat.Deb)
}

DesktopTarget.values().map { target ->
    target to tasks.register(
        listOf("download", target.os, target.arch, "jre")
            .joinToCamelCase()
    ) {
        val version = properties["awery.jre.version"].toString()
        val variant = properties["awery.jre.variant"].toString()
        inputs.property("jreVersion", version)
        inputs.property("jreVariant", variant)
        doLast { }
    }
}

compose.desktop {
    application {
        mainClass = "com.mrboomdev.awery.app.MainKt"

        nativeDistributions {
            targetFormats =
                DesktopTarget.values().map { it.targetFormat }.toSet()
            packageName = "Awery"
            packageVersion =
                properties["awery.app.versionName"].toString()
            includeAllModules = true

            javaHome = System.getenv("JBR_HOME")
                ?: error("JBR_HOME not set")

            windows {
                iconFile = rootProject.file("app_icon.ico")
                shortcut = true
                menu = true
                menuGroup = "Awery"
                perUserInstall = true
            }
        }
    }
}

/* ========================= LOCALES ========================= */

tasks.register("generateAndroidLocaleConfig") {
    val outputDir = layout.projectDirectory.dir("src/androidMain/res/xml")
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
