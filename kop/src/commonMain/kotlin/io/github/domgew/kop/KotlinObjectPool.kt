package io.github.domgew.kop

import io.github.domgew.kop.internal.KotlinObjectPoolImpl

@OptIn(ExperimentalStdlibApi::class)
public interface KotlinObjectPool<T> : AutoCloseable {

    /**
     * Take the next object from the object pool adhering to [KotlinObjectPoolConfig.strategy]. If necessary, a new object is created.
     *
     * If the object pool is at its maximum, it waits for the next available object.
     *
     * Be aware you need to return the object yourself. You can also use [KotlinObjectPool.withObject] for automatic return.
     *
     * @see KotlinObjectPool.withObject
     */
    public suspend fun take(): T

    /**
     * Returns the object to the pool, for the next use.
     */
    public suspend fun giveBack(
        item: T,
    )

    public companion object {

        /**
         * **Warning**: [createInstance] blocks the whole object pool.
         *
         * @param onBeforeClose Callback to be called before an object is closed
         * @param onAfterClose Callback to be called after an object was closed
         * @param createInstance Used to create a new object instance, when needed
         */
        public operator fun <T> invoke(
            config: KotlinObjectPoolConfig<T>,
            onBeforeClose: ((T) -> Unit)? = null,
            onAfterClose: ((T) -> Unit)? = null,
            createInstance: suspend () -> T,
        ): KotlinObjectPool<T> =
            KotlinObjectPoolImpl(
                config = config,
                onBeforeClose = onBeforeClose,
                onAfterClose = onAfterClose,
                instanceCreator = createInstance,
            )
    }
}
