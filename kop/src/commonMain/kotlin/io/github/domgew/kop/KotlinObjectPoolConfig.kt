package io.github.domgew.kop

import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope

@OptIn(DelicateCoroutinesApi::class)
public data class KotlinObjectPoolConfig(
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
    val keepAliveFor: Duration = Duration.INFINITE,
    /**
     * The strategy of how the next object to use is determined.
     *
     * @see KotlinObjectPoolStrategy
     */
    val strategy: KotlinObjectPoolStrategy = KotlinObjectPoolStrategy.LIFO,
) {

    @Deprecated(
        message = "[coroutineScope] moved to actual constructor",
        replaceWith = ReplaceWith(
            expression = """
                KotlinObjectPoolConfig(
                    maxSize = maxSize,
                    keepAliveFor = keepAliveFor
                        ?: Duration.INFINITE,
                    strategy = strategy,
                )
            """,
        ),
    )
    public constructor(
        /**
         * This is the largest number of objects the pool provides at the same time.
         *
         * Be aware that this creates space for [maxSize] references/pointers.
         */
        maxSize: Int,
        /**
         * The amount of time the object can spend inside the pool without being used.
         *
         * During cleanup, it is closed, if it implements [AutoCloseable].
         *
         * Be aware that the cleanup blocks the object pool.
         */
        keepAliveFor: Duration? = null,
        /**
         * The strategy of how the next object to use is determined.
         *
         * @see KotlinObjectPoolStrategy
         */
        strategy: KotlinObjectPoolStrategy = KotlinObjectPoolStrategy.LIFO,
        /**
         * The coroutine scope in which the cleanup jobs are to run in.
         */
        coroutineScope: CoroutineScope = GlobalScope,
    ) : this(
        maxSize = maxSize,
        keepAliveFor = keepAliveFor
            ?: Duration.INFINITE,
        strategy = strategy,
    )
}
