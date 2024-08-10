package io.github.domgew.kop.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch

internal actual fun getTimeMillis(): Long {
    val jsDate = getDateNow()

    return jsDate.toLong()
}

private fun getDateNow(): Double =
    js("Date.now()")

internal actual fun runBlockingPlatform(
    coroutineScope: CoroutineScope,
    block: suspend () -> Unit,
) {
    coroutineScope.launch(NonCancellable) {
        block()
    }
}
