package io.github.domgew.kop.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal actual fun runBlockingPlatform(
    coroutineScope: CoroutineScope,
    block: suspend () -> Unit,
) {
    coroutineScope.launch {
        withContext(NonCancellable) {
            block()
        }
    }
}
