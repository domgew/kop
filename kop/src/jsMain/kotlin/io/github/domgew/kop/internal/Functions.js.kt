package io.github.domgew.kop.internal

import kotlin.js.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch

internal actual fun getTimeMillis(): Long =
    Date.now()
        .toLong()

internal actual fun runBlockingPlatform(
    coroutineScope: CoroutineScope,
    block: suspend () -> Unit,
) {
    coroutineScope.launch(NonCancellable) {
        block()
    }
}
