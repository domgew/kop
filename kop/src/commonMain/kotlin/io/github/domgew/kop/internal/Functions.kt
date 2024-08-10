package io.github.domgew.kop.internal

import kotlinx.coroutines.CoroutineScope

internal expect fun getTimeMillis(): Long

internal expect fun runBlockingPlatform(
    coroutineScope: CoroutineScope,
    block: suspend () -> Unit,
)
