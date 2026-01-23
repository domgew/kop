plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.graalvm.native)
}

dependencies {
    implementation(project(":kop"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("test-graalvm")
            mainClass.set("MainKt")
            debug.set(false)
            verbose.set(false)
            fallback.set(false)
            sharedLibrary.set(false)
            richOutput.set(false)
            quickBuild.set(false)
            useFatJar.set(true)
            buildArgs.add("--link-at-build-time")
        }
    }
}
