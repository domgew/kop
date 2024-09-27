# KOP - Kotlin Object Pool

[![Maven Central](https://img.shields.io/maven-central/v/io.github.domgew/kop)](https://central.sonatype.com/search?q=kop&namespace=io.github.domgew)
[![Latest Tag](https://img.shields.io/github/v/tag/domgew/kop?label=latest%20tag&cacheSeconds=180)](https://github.com/domgew/kop/tags)
[![Publish](https://img.shields.io/github/actions/workflow/status/domgew/kop/.github%2Fworkflows%2Fpublish.yml?label=publish&cacheSeconds=180)](https://github.com/domgew/kop/actions/workflows/publish.yml)
[![Test](https://img.shields.io/github/actions/workflow/status/domgew/kop/.github%2Fworkflows%2Ftest.yml?branch=main&label=test&cacheSeconds=180)](https://github.com/domgew/kop/actions/workflows/test.yml)
![Kotlin](https://img.shields.io/github/languages/top/domgew/kop?cacheSeconds=86400)
[![Licence: MIT](https://img.shields.io/github/license/domgew/kop?cacheSeconds=86400)](./LICENSE)

KOP is a Kotlin Multiplatform object pool.

* [Installation](#installation)
* [Documentation](#documentation)
* [Targets](#targets)
* [Examples](#examples)

## Installation

```kotlin
dependencies {
    // ...

    implementation("io.github.domgew:kop:<current_version>")

    // OR just for JVM:
    implementation("io.github.domgew:kop-jvm:<current_version>")

    // ...
}
```

```kotlin
repositories {
    mavenCentral()

    // ...
}
```

## Documentation

See Dokka-generated [docs](https://javadoc.io/doc/io.github.domgew/kop/latest/kop/io.github.domgew.kop/index.html).

## Targets

**Supported Targets**:

* JVM
* JS: Browser
* JS: NodeJS
* wasmJS: Browser
* wasmJS: NodeJS
* Native: Linux X64
* Native: Linux ARM64
* Native: macOS X64
* Native: macOS ARM64
* Native: mingw X64
* Native: iOS X64
* Native: iOS ARM64
* Native: watchOS X64
* Native: watchOS ARM64
* Native: tvOS X64
* Native: tvOS ARM64

**Potential Future Targets**:

* Native: Android X64
* Native: Android ARM64

## Examples

### [Kedis](https://github.com/domgew/kedis) - Kotlin Multiplatform Redis Cache

```kotlin
val objectPool = KotlinObjectPool(
    KotlinObjectPoolConfig(
        maxSize = 4,
        keepAliveFor = 1.minutes,
        strategy = KotlinObjectPoolStrategy.LIFO,
    ),
) {
    KedisClient(
        KedisConfiguration(
            endpoint = KedisConfiguration.Endpoint.HostPort(
                host = "127.0.0.1",
                port = 6379,
            ),
            authentication = KedisConfiguration.Authentication.NoAutoAuth,
            connectionTimeoutMillis = 250,
            keepAlive = true,
        ),
    )
}

suspend fun getValueWithCache() =
    objectPool.withObject { kedisClient: KedisClient ->
        if (!kedisClient.isAvailable()) {
            // logging might be nice
            return@withObject getExpensiveValue()
        }

        val value = kedisClient.get("testKey")

        if (value != null) {
            return@withObject value
        }

        val valueFromCostlySystem = getExpensiveValue()

        try {
            kedisClient.set(
                key = "testKey",
                value = valueFromCostlySystem,
                options = SetOptions(
                    expire = SetOptions.ExpireOption.ExpiresInSeconds(
                        seconds = 120,
                    ),
                ),
            )
        } catch (th: Throwable) {
            // ignore exception but ensure the coroutine scope is still active - probably logging would be nice
            ensureActive()
        }

        return@withObject valueFromCostlySystem
    }

suspend fun getExpensiveValue(): String =
    "Hello World!"

suspend fun KedisClient.isAvailable(): Boolean {
    if (isConnected) {
        return true
    }

    try {
        connect()
        return true
    } catch (th: Throwable) {
        ensureActive()
        return false
    }
}
```
