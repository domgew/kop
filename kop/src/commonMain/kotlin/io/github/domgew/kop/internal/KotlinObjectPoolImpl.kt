package io.github.domgew.kop.internal

import com.benasher44.uuid.uuid4
import io.github.domgew.kop.KotlinObjectPool
import io.github.domgew.kop.KotlinObjectPoolConfig
import io.github.domgew.kop.KotlinObjectPoolStrategy
import kotlin.time.DurationUnit
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class KotlinObjectPoolImpl<T>(
    private val config: KotlinObjectPoolConfig<T>,
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

        return try {
            itemsAccessMutex.withLock {
                if (items.size == 0) {
                    // Potential optimisation: instance creation without lock - only when necessary for complexity reasons
                    return@withLock config.instanceCreator()
                }

                return@withLock if (config.strategy == KotlinObjectPoolStrategy.LIFO) {
                    items.getLast()
                } else {
                    items.getFirst()
                }
                    .also {
                        // not in pool anymore
                        it.destructor
                            .cancel()
                    }
                    .instance
            }
        } catch (th: Throwable) {
            availableItemsSemaphore.release()
            throw th
        }
    }

    override suspend fun giveBack(
        item: T,
    ) {
        itemsAccessMutex.withLock {
            withContext(NonCancellable) {
                items.putLast(
                    item = createInstanceHolder(
                        instance = item,
                    ),
                )
                availableItemsSemaphore.release()
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun createInstanceHolder(
        instance: T,
    ): InstanceHolder<T> {
        val uid = uuid4()
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
                    val item = items.getFirst()

                    // we don't want a dangling job, but we also don't want to cancel ourselves
                    if (item.uid != uid) {
                        item.destructor.cancel()
                    }

                    // if there is no cleanup to be done, we can go to the next item
                    if (item.instance !is AutoCloseable) {
                        continue
                    }

                    item.instance.close()
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

    @OptIn(ExperimentalStdlibApi::class)
    override fun close() {
        runBlockingPlatform(config.coroutineScope) {
            itemsAccessMutex.withLock {
                // the iteration also removes it from the buffer
                for (item in items) {
                    item.destructor.cancel()

                    // is the object is not closeable we don't have anything to do
                    if (item.instance !is AutoCloseable) {
                        continue
                    }

                    item.instance.close()
                }
            }
        }
    }
}
