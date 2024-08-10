package io.github.domgew.kop.internal

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import platform.posix.CLOCK_REALTIME
import platform.posix.clock_gettime
import platform.posix.timespec

@OptIn(ExperimentalForeignApi::class)
internal actual fun getTimeMillis(): Long =
    memScoped {
        val timeSpec = alloc<timespec>()
        val returnCode = clock_gettime(
            CLOCK_REALTIME.convert(),
            timeSpec.ptr,
        )

        if (returnCode != 0) {
            throw Exception("Could not get time")
        }

        @OptIn(UnsafeNumber::class)
        val epochSeconds: Long = timeSpec.tv_sec.convert()

        @OptIn(UnsafeNumber::class)
        val nano: Int = timeSpec.tv_nsec.convert()

        return@memScoped epochSeconds + (nano / 1000)
    }

internal actual fun runBlockingPlatform(
    coroutineScope: CoroutineScope,
    block: suspend () -> Unit,
) {
    runBlocking {
        block()
    }
}
