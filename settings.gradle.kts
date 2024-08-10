pluginManagement {
    val dokkaVersion: String by settings
    val kotlinVersion: String by settings
    val koverVersion: String by settings

    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://jitpack.io/")
        mavenLocal()
    }

    plugins {
        kotlin("multiplatform") version kotlinVersion apply false
        id("org.jetbrains.dokka") version dokkaVersion apply false
        id("org.jetbrains.kotlinx.kover") version koverVersion apply false
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
        google()
        maven("https://jitpack.io/")

        // workaround for https://youtrack.jetbrains.com/issue/KT-51379
        exclusiveContent {
            forRepository {
                ivy("https://download.jetbrains.com/kotlin/native/builds") {
                    name = "Kotlin Native"
                    patternLayout {
                        listOf(
                            "macos-x86_64",
                            "macos-aarch64",
                            "osx-x86_64",
                            "osx-aarch64",
                            "ios-aarch64",
                            "ios-x86_64",
                            "tvos-aarch64",
                            "tvos-x86_64",
                            "watchos-aarch64",
                            "watchos-x86_64",
                            "linux-x86_64",
                            "linux-aarch64",
                            "windows-x86_64",
                        ).forEach { os ->
                            listOf("dev", "releases").forEach { stage ->
                                artifact("$stage/[revision]/$os/[artifact]-[revision].[ext]")
                            }
                        }
                    }
                    metadataSources { artifact() }
                }
            }
            filter { includeModuleByRegex(".*", ".*kotlin-native-prebuilt.*") }
        }
        exclusiveContent {
            forRepository {
                ivy {
                    name = "Node.js"
                    setUrl("https://nodejs.org/dist/")
                    patternLayout {
                        artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
                    }
                    metadataSources {
                        artifact()
                    }
                    content {
                        includeModule("org.nodejs", "node")
                    }
                }
            }
            filter { includeModuleByRegex("org.nodejs", "node") }
        }
        exclusiveContent {
            forRepository {
                ivy {
                    name = "Yarn"
                    setUrl("https://github.com/yarnpkg/yarn/releases/download/")
                    patternLayout {
                        artifact("v[revision]/[artifact](-v[revision]).[ext]")
                    }
                    metadataSources {
                        artifact()
                    }
                    content {
                        includeModule("com.yarnpkg", "yarn")
                    }
                }
            }
            filter { includeModuleByRegex("com.yarnpkg", "yarn") }
        }

        mavenLocal()
    }
}

rootProject.name = "kop"

include(":kop")

