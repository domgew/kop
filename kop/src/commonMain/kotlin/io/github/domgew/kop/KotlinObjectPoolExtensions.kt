package io.github.domgew.kop

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Takes the next object from the pool adhering to [KotlinObjectPoolConfig.strategy], calls [block], and returns it afterward. If necessary, a new object is created.
 *
 * If the object pool is at its maximum, it waits for the next available object.
 */
@OptIn(ExperimentalContracts::class)
public suspend inline fun <T, R> KotlinObjectPool<T>.withObject(
    block: (item: T) -> R,
): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val item = take()

    try {
        return block(item)
    } finally {
        withContext(NonCancellable) {
            giveBack(item)
        }
    }
}
