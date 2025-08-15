import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.HostManager
import java.util.regex.Pattern

plugins {
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kover)
    alias(libs.plugins.vanniktech.mavenPublish)
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
    jvmToolchain(17)

    jvm {
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
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
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

    androidNativeArm32 {
        block()
    }
    androidNativeArm64 {
        block()
    }
    androidNativeX64 {
        block()
    }
    androidNativeX86 {
        block()
    }
}

dokka {
    dokkaPublications.html {
    }

    dokkaSourceSets {
        getByName("commonMain") {
            samples.from(
                project.files(),
                project.files("src/commonTest/kotlin"),
            )
        }
    }
}

// https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-publish-libraries.html#set-up-the-publishing-plugin
// https://vanniktech.github.io/gradle-maven-publish-plugin/what/#kotlin-multiplatform-library
mavenPublishing {
    configure(
        platform = KotlinMultiplatform(
            javadocJar = JavadocJar.Dokka(
                taskName = "dokkaGeneratePublicationHtml",
            ),
            sourcesJar = true,
        ),
    )

    publishToMavenCentral(
        automaticRelease = false,
    )
    signAllPublications()
    coordinates(
        groupId = project.group
            .toString(),
        artifactId = "kop",
        version = project.version
            .toString(),
    )

    pom {
        name = "KOP"
        description = "Kotlin Multiplatform Object Pool"
        url = "https://github.com/domgew/kop"
        scm {
            url = "https://github.com/domgew/kop"
            connection = "scm:git:git://github.com/domgew/kop.git"
            developerConnection = "scm:git:ssh://github.com:domgew/kop.git"
        }
        licenses {
            license {
                name = "MIT"
                url = "https://opensource.org/licenses/MIT"
            }
        }
        issueManagement {
            system = "Github"
            url = "https://github.com/domgew/kop/issues"
        }
        developers {
            developer {
                id = "domgew"
                name = "domgew"
                email = "44265359+domgew@users.noreply.github.com"
                url = "https://github.com/domgew"
            }
        }
    }
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
                        || it.name.startsWith("android", true)
                        || it.name.startsWith("js", true)
                        || it.name.startsWith("wasmJs", true)
                        || it.name.startsWith("jvm", true)

                else ->
                    throw Exception("unknown host")
            }
        }
    val publishTasks = when {
        System.getenv("IS_CI") == "yes" ->
            project.tasks.withType<PublishToMavenRepository>()

        else ->
            project.tasks.withType<PublishToMavenLocal>()
    }
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
                        || it.name.startsWith("publishAndroid")
                        || it.name.startsWith("publishMingw")
                        || it.name.startsWith("publishJs")
                        || it.name.startsWith("publishWasmJs")
                        || it.name.startsWith("publishJvmPublication")
                        || it.name.startsWith("publishMetadata")
                        || it.name.startsWith("publishKotlinMultiplatform")

                else ->
                    throw Exception("unknown host")
            }
                .and(
                    if (System.getenv("IS_CI") == "yes") {
                        it.name.endsWith("MavenCentralRepository")
                    } else {
                        it.name.endsWith("MavenLocal")
                    },
                )
        }

    if (System.getenv("IS_CI") == "yes") {
        println("#####################################")
        println("test tasks:")
        val allTestTasks = project.tasks
            .withType<AbstractTestTask>()
            .sortedBy {
                it.name
            }
        for (task in allTestTasks) {
            println("\t${task.name}")
        }
        println()
        println("smartTest tasks:")
        for (task in testTasks.sortedBy { it.name }) {
            println("\t${task.name}")
        }
        println("#####################################")
        println("publish tasks:")
        val allPublishTasks = project.tasks
            .withType<PublishToMavenLocal>()
            .plus(
                project.tasks
                    .withType<PublishToMavenRepository>(),
            )
            .sortedBy {
                it.name
            }
        for (task in allPublishTasks) {
            println("\t${task.name}")
        }
        println()
        println("smartPublish tasks:")
        for (task in publishTasks.sortedBy { it.name }) {
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
