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

        public operator fun <T> invoke(
            config: KotlinObjectPoolConfig<T>,
        ): KotlinObjectPool<T> =
            KotlinObjectPoolImpl(
                config = config,
            )
    }
}
