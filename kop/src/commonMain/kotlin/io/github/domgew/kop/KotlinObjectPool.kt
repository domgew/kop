package io.github.domgew.kop

import io.github.domgew.kop.internal.KotlinObjectPoolImpl
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope

public interface KotlinObjectPool<T> : AutoCloseable {

    /**
     * Take the next object from the object pool adhering to [KotlinObjectPoolConfig.strategy].
     * If necessary, a new object is created.
     *
     * If the object pool is at its maximum, it waits for the next available object.
     *
     * Be aware you need to return the object yourself.
     * You can also use [KotlinObjectPool.withObject] for automatic return.
     *
     * @see KotlinObjectPool.withObject
     */
    public suspend fun take(): T

    /**
     * Try to take the next object from the object pool adhering to [KotlinObjectPoolConfig.strategy].
     * If necessary, a new object is created.
     *
     * If the object pool is at its maximum, it returns [Optional.None].
     *
     * Be aware you need to return the object yourself.
     * You can also use [KotlinObjectPool.tryWithObject] for automatic return.
     *
     * @see KotlinObjectPool.tryWithObject
     */
    public suspend fun tryTake(): Optional<T>

    /**
     * Returns the object to the pool, for the next use.
     */
    public suspend fun giveBack(
        item: T,
    )

    public companion object {

        /**
         * @param onBeforeClose Callback to be called before an object is closed - blocking
         * @param onAfterClose Callback to be called after an object was closed - blocking
         * @param coroutineScope The coroutine scope in which the cleanup jobs are to run in - mostly useful in testing
         * @param timeSource The time source to use to compute the time to live from - mostly useful in testing
         * @param createInstance Used to create a new object instance, when needed
         */
        public operator fun <T> invoke(
            config: KotlinObjectPoolConfig,
            onBeforeClose: ((T) -> Unit)? = null,
            onAfterClose: ((T) -> Unit)? = null,
            @OptIn(DelicateCoroutinesApi::class)
            coroutineScope: CoroutineScope = GlobalScope,
            timeSource: TimeSource = TimeSource.Monotonic,
            createInstance: suspend () -> T,
        ): KotlinObjectPool<T> =
            KotlinObjectPoolImpl(
                config = config,
                onBeforeClose = onBeforeClose,
                onAfterClose = onAfterClose,
                coroutineScope = coroutineScope,
                timeSource = timeSource,
                instanceCreator = createInstance,
            )

        @Throws(
            KotlinObjectPoolBuildScope.MissingConfig::class,
        )
        public fun <T> build(
            block: KotlinObjectPoolBuildScope<T>.() -> Unit,
        ): KotlinObjectPool<T> =
            KotlinObjectPoolBuildScope<T>()
                .apply(block)
                .build()

        /**
         * @param baseConfig The base configuration from which to build from
         */
        @Throws(
            KotlinObjectPoolBuildScope.MissingConfig::class,
        )
        public fun <T> build(
            baseConfig: KotlinObjectPoolConfig,
            block: KotlinObjectPoolBuildScope<T>.() -> Unit,
        ): KotlinObjectPool<T> =
            KotlinObjectPoolBuildScope<T>(
                baseConfig = baseConfig,
            )
                .apply(block)
                .build()
    }
}
