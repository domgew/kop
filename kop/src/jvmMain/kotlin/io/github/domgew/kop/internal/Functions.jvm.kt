package io.github.domgew.kop.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

internal actual fun getTimeMillis(): Long =
    System.currentTimeMillis()

internal actual fun runBlockingPlatform(
    coroutineScope: CoroutineScope,
    block: suspend () -> Unit,
) {
    runBlocking {
        block()
    }
}
