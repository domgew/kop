rootProject.name = "kop"

include(":kop")

if (System.getenv("IS_CI") != "yes") {
    include(":test-graalvm")
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
        google()

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
    }
}
