package io.github.domgew.kop.internal

import io.github.domgew.kop.KotlinObjectPool
import io.github.domgew.kop.KotlinObjectPoolConfig
import io.github.domgew.kop.KotlinObjectPoolStrategy
import io.github.domgew.kop.Optional
import kotlin.time.DurationUnit
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@OptIn(ExperimentalUuidApi::class)
internal class KotlinObjectPoolImpl<T>(
    private val config: KotlinObjectPoolConfig<T>,
    private val onBeforeClose: ((T) -> Unit)?,
    private val onAfterClose: ((T) -> Unit)?,
    private val instanceCreator: suspend () -> T,
) : KotlinObjectPool<T> {

    internal var getTime: () -> Long =
        { getTimeMillis() }

    private val availableItemsSemaphore = Semaphore(config.maxSize)
    private val itemsAccessMutex = Mutex()

    private val items =
        DoubleEndedRingBuffer<InstanceHolder<T>>(
            capacity = config.maxSize,
        )

    override suspend fun take(): T {
        availableItemsSemaphore.acquire()

        try {
            itemsAccessMutex.withLock {
                if (items.size > 0) {
                    return when (config.strategy) {
                        KotlinObjectPoolStrategy.LIFO ->
                            items.removeLast()

                        KotlinObjectPoolStrategy.FIFO ->
                            items.removeFirst()
                    }
                        .also {
                            it.destructor
                                .cancel()
                        }
                        .instance
                }
            }

            return instanceCreator()
        } catch (th: Throwable) {
            availableItemsSemaphore.release()
            throw th
        }
    }

    override suspend fun tryTake(): Optional<T> {
        if (
            !availableItemsSemaphore.tryAcquire()
        ) {
            return Optional.None
        }

        try {
            itemsAccessMutex.withLock {
                if (items.size > 0) {
                    return when (config.strategy) {
                        KotlinObjectPoolStrategy.LIFO ->
                            items.removeLast()

                        KotlinObjectPoolStrategy.FIFO ->
                            items.removeFirst()
                    }
                        .also {
                            it.destructor
                                .cancel()
                        }
                        .instance
                        .let {
                            Optional.Some(
                                value = it,
                            )
                        }
                }
            }

            return Optional.Some(
                value = instanceCreator(),
            )
        } catch (th: Throwable) {
            availableItemsSemaphore.release()
            throw th
        }
    }

    override suspend fun giveBack(
        item: T,
    ) {
        withContext(NonCancellable) {
            itemsAccessMutex.withLock {
                if (items.size >= config.maxSize) {
                    throw IllegalStateException(
                        "Pool is already full",
                    )
                }

                items.putLast(
                    item = createInstanceHolder(
                        instance = item,
                    ),
                )
                availableItemsSemaphore.release()
            }
        }
    }

    private fun createInstanceHolder(
        instance: T,
    ): InstanceHolder<T> {
        val uid = Uuid.random()
        val currentTimeMillis = getTime()
        val timeToLive = config.keepAliveFor
            ?.toLong(DurationUnit.MILLISECONDS)
        val destructor = config.coroutineScope.launch {
            if (timeToLive == null) {
                return@launch
            }

            delay(timeToLive)

            itemsAccessMutex.withLock {
                val cleanUpCutOff = getTime() - timeToLive

                while (items.size > 0) {
                    // first is oldest since we add at the last
                    if (
                        items.peekFirst()
                            .addedAtMillis > cleanUpCutOff
                    ) {
                        break
                    }

                    // this also removes the item from the buffer
                    val item = items.removeFirst()

                    // we don't want a dangling job, but we also don't want to cancel ourselves
                    if (item.uid != uid) {
                        item.destructor.cancel()
                    }

                    onBeforeClose?.invoke(item.instance)
                    if (item.instance is AutoCloseable) {
                        item.instance.close()
                    }
                    onAfterClose?.invoke(item.instance)
                }
            }
        }

        return InstanceHolder(
            instance = instance,
            uid = uid,
            destructor = destructor,
            addedAtMillis = currentTimeMillis,
        )
    }

    override fun close() {
        runBlockingPlatform(config.coroutineScope) {
            itemsAccessMutex.withLock {
                var firstCloseError: Throwable? = null
                var firstCallbackError: Throwable? = null

                // the iteration also removes it from the buffer
                for (item in items) {
                    item.destructor.cancel()

                    try {
                        onBeforeClose?.invoke(item.instance)
                    } catch (th: Throwable) {
                        firstCallbackError = firstCallbackError
                            ?: th
                    }
                    if (item.instance is AutoCloseable) {
                        try {
                            item.instance.close()
                        } catch (th: Throwable) {
                            firstCloseError = firstCloseError
                                ?: th
                        }
                    }
                    try {
                        onAfterClose?.invoke(item.instance)
                    } catch (th: Throwable) {
                        firstCallbackError = firstCallbackError
                            ?: th
                    }
                }

                if (firstCloseError != null) {
                    throw firstCloseError
                }
                if (firstCallbackError != null) {
                    throw firstCallbackError
                }
            }
        }
    }
}
