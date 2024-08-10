import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.konan.target.HostManager
import java.util.regex.Pattern

val kotlinCoroutinesVersion: String by project
val uuidVersion: String by project

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.kover")
    `maven-publish`
    signing
}

group = "io.github.domgew"
version = "0.0.1-SNAPSHOT"

val commitTagPattern =
    Pattern.compile(
        "^(\\d+)\\.(\\d+)\\.(\\d+)(-([a-z]+)(\\d+))?$",
    )!!
val commitTag = System.getenv("CI_COMMIT_TAG")
    ?.trim()
    ?.ifEmpty { null }
    ?.takeIf {
        commitTagPattern.asMatchPredicate()
            .test(it)
    }

if (commitTag != null) {
    version = commitTag
}

kotlin {
    explicitApi()
    withSourcesJar(
        publish = true,
    )

    jvm {
        jvmToolchain(17)
    }
    js {
        browser()
        nodejs()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
        browser()
    }
    addNativeTargets {
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))

                implementation("com.benasher44:uuid:$uuidVersion")

                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinCoroutinesVersion")
            }
        }
    }
}

fun KotlinMultiplatformExtension.addNativeTargets(
    block: KotlinNativeTarget.() -> Unit,
) {
    linuxX64 {
        block()
    }
    linuxArm64 {
        block()
    }

    macosX64 {
        block()
    }
    macosArm64 {
        block()
    }

    mingwX64 {
        block()
    }

    iosArm64 {
        block()
    }
    iosX64 {
        block()
    }

    tvosArm64 {
        block()
    }
    tvosX64 {
        block()
    }

    watchosArm64 {
        block()
    }
    watchosX64 {
        block()
    }
}

val dokkaOutputDir = "${layout.buildDirectory.get()}/dokka"
tasks.dokkaHtml {
    outputDirectory.set(file(dokkaOutputDir))
}
val deleteDokkaOutputDir by tasks.register<Delete>("deleteDokkaOutputDirectory") {
    delete(dokkaOutputDir)
}
val javadocJar = tasks.register<Jar>("javadocJar") {
    dependsOn(deleteDokkaOutputDir, tasks.dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaOutputDir)
}

publishing {
    publications {
        withType<MavenPublication> {
            artifact(javadocJar)
            pom {
                name.set("KOP")
                description.set("Kotlin Multiplatform Object Pool")
                url.set("https://github.com/domgew/kop")
                scm {
                    url.set("https://github.com/domgew/kop")
                    connection.set("scm:git:git://github.com/domgew/kop.git")
                    developerConnection.set("scm:git:ssh://github.com:domgew/kop.git")
                }
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                issueManagement {
                    system.set("Github")
                    url.set("https://github.com/domgew/kop/issues")
                }
                developers {
                    developer {
                        name.set("domgew")
                        email.set("44265359+domgew@users.noreply.github.com")
                    }
                }
            }
        }
    }

    repositories {
        if (System.getenv("IS_CI") != "yes") {
            mavenLocal()
        } else {
            maven {
                name = "oss"

                val releasesRepoUrl = uri(
                    "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/",
                )
                val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                url =
                    if (
                        version.toString()
                            .endsWith("SNAPSHOT")
                    )
                        snapshotsRepoUrl
                    else
                        releasesRepoUrl

                credentials {
                    username = System.getenv("SONATYPE_USER")
                        ?.trim()
                        ?.ifEmpty { null }
                    password = System.getenv("SONATYPE_PASS")
                        ?.trim()
                        ?.ifEmpty { null }
                }
            }
        }
    }
}

signing {
    useInMemoryPgpKeys(
        System.getenv("GPG_PRIVATE_KEY"),
        System.getenv("GPG_PRIVATE_PASSWORD"),
    )
    sign(publishing.publications)
}

// https://github.com/gradle/gradle/issues/26091
val signingTasks = tasks.withType<Sign>()
tasks.withType<AbstractPublishToMaven>().configureEach {
    dependsOn(signingTasks)
}

afterEvaluate {
    val testTasks = project.tasks.withType<AbstractTestTask>()
        .matching {
            when {
                HostManager.hostIsMingw ->
                    it.name.startsWith("mingw", true)

                HostManager.hostIsMac ->
                    it.name.startsWith("macos", true)
                        || it.name.startsWith("ios", true)
                        || it.name.startsWith("watchos", true)
                        || it.name.startsWith("tvos", true)

                        || it.name.startsWith("js", true)
                        || it.name.startsWith("wasmJs", true)
                        || it.name.startsWith("jvm", true)

                HostManager.hostIsLinux ->
                    it.name.startsWith("linux", true)
                        || it.name.startsWith("js", true)
                        || it.name.startsWith("wasmJs", true)
                        || it.name.startsWith("jvm", true)

                else ->
                    throw Exception("unknown host")
            }
        }
    val publishTasks = project.tasks.withType<PublishToMavenRepository>()
        .matching {
            when {
                HostManager.hostIsMingw ->
                    it.name.startsWith("publishMingw")

                HostManager.hostIsMac ->
                    it.name.startsWith("publishMacos")
                        || it.name.startsWith("publishTvos")
                        || it.name.startsWith("publishWatchos")
                        || it.name.startsWith("publishIos")

                HostManager.hostIsLinux ->
                    it.name.startsWith("publishLinux")
                        || it.name.startsWith("publishJs")
                        || it.name.startsWith("publishWasmJs")
                        || it.name.startsWith("publishJvmPublication")
                        || it.name.startsWith("publishMetadata")
                        || it.name.startsWith("publishKotlinMultiplatform")

                else ->
                    throw Exception("unknown host")
            }
        }

    if (System.getenv("IS_CI") == "yes") {
        println("#####################################")
        println("test tasks:")
        for (task in project.tasks.withType<AbstractTestTask>()) {
            println("\t${task.name}")
        }
        println()
        println("smartTest tasks:")
        for (task in testTasks) {
            println("\t${task.name}")
        }
        println("#####################################")
        println("publish tasks:")
        for (task in project.tasks.withType<PublishToMavenRepository>()) {
            println("\t${task.name}")
        }
        println()
        println("smartPublish tasks:")
        for (task in publishTasks) {
            println("\t${task.name}")
        }
        println("#####################################")
    }

    project.tasks.register("smartTest") {
        dependsOn(testTasks)
    }
    project.tasks.register("smartPublish") {
        dependsOn(publishTasks)
    }
}
