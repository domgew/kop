package io.github.domgew.kop

import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope

@OptIn(DelicateCoroutinesApi::class)
public data class KotlinObjectPoolConfig<T>(
    /**
     * This is the largest number of objects the pool provides at the same time.
     *
     * Be aware that this creates space for [maxSize] references/pointers.
     */
    val maxSize: Int,
    /**
     * The amount of time the object can spend inside the pool without being used.
     *
     * During cleanup, it is closed, if it implements [AutoCloseable].
     *
     * Be aware that the cleanup blocks the object pool.
     */
    val keepAliveFor: Duration? = null,
    val strategy: KotlinObjectPoolStrategy = KotlinObjectPoolStrategy.LIFO,
    /**
     * The coroutine scope in which the cleanup jobs are to run in.
     */
    val coroutineScope: CoroutineScope = GlobalScope,
    /**
     * This is used to create a new object instance, when needed.
     *
     * Be aware that the creation blocks the object pool.
     */
    val instanceCreator: suspend () -> T,
)
