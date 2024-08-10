package io.github.domgew.kop

import io.github.domgew.kop.internal.KotlinObjectPoolImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.testTimeSource
import kotlinx.coroutines.withTimeout

@OptIn(ExperimentalCoroutinesApi::class)
class KotlinObjectPoolTest {

    @Test
    fun lifoTest() = runTest {
        val testState = prepare(
            maxSize = 3,
            keepAliveFor = 2.minutes,
            strategy = KotlinObjectPoolStrategy.LIFO,
        )
        assertEquals(testState.lastCreatedInstanceId, 0)

        val instance1 = testState.objectPool.take()
        val instance2 = testState.objectPool.take()
        val instance3 = testState.objectPool.take()
        assertFailsWith<TimeoutCancellationException> {
            withTimeout(100) {
                testState.objectPool.take()
            }
        }

        assertEquals(emptyList(), testState.closed)
        assertEquals(1, instance1.identity)
        assertEquals(2, instance2.identity)
        assertEquals(3, instance3.identity)

        testState.objectPool.giveBack(instance1)
        testState.objectPool.giveBack(instance2)
        testState.objectPool.giveBack(instance3)
        val instance4 = testState.objectPool.take()

        assertEquals(emptyList(), testState.closed)
        assertEquals(3, instance4.identity)

        delay(2.1.minutes)
        testState.objectPool.giveBack(instance4)

        assertEquals(listOf(1, 2), testState.closed)

        testState.objectPool
            .close()

        advanceUntilIdle()

        assertEquals(listOf(1, 2, 3), testState.closed)
    }

    @Test
    fun fifoTest() = runTest {
        val testState = prepare(
            maxSize = 3,
            keepAliveFor = 2.minutes,
            strategy = KotlinObjectPoolStrategy.FIFO,
        )
        assertEquals(testState.lastCreatedInstanceId, 0)

        val instance1 = testState.objectPool.take()
        val instance2 = testState.objectPool.take()
        val instance3 = testState.objectPool.take()
        assertFailsWith<TimeoutCancellationException> {
            withTimeout(100) {
                testState.objectPool.take()
            }
        }

        assertEquals(emptyList(), testState.closed)
        assertEquals(1, instance1.identity)
        assertEquals(2, instance2.identity)
        assertEquals(3, instance3.identity)

        testState.objectPool.giveBack(instance1)
        testState.objectPool.giveBack(instance2)
        testState.objectPool.giveBack(instance3)
        val instance4 = testState.objectPool.take()

        assertEquals(emptyList(), testState.closed)
        assertEquals(1, instance4.identity)

        delay(2.1.minutes)
        testState.objectPool.giveBack(instance4)

        assertEquals(listOf(2, 3), testState.closed)

        testState.objectPool
            .close()

        advanceUntilIdle()

        assertEquals(listOf(2, 3, 1), testState.closed)
    }

    @Test
    fun instanceCreationCancelled() = runTest {
        val testState = prepare(
            maxSize = 1,
            keepAliveFor = 2.minutes,
            strategy = KotlinObjectPoolStrategy.LIFO,
            instanceCreationPrecondition = {
                delay(500)
            },
        )
        assertEquals(0, testState.lastCreatedInstanceId)

        val instance1Job = async {
            testState.objectPool.take()
        }
        delay(100)
        instance1Job.cancel()

        assertEquals(0, testState.lastCreatedInstanceId)

        val instance2Job = async {
            testState.objectPool.take()
        }
        delay(510)

        assertTrue(instance2Job.isCompleted)
        assertEquals(1, testState.lastCreatedInstanceId)
        assertEquals(emptyList(), testState.closed)

        testState.objectPool
            .close()

        advanceUntilIdle()

        assertEquals(emptyList(), testState.closed)
    }

    @Test
    fun instanceCreationFailed() = runTest {
        var alreadyFailed = false
        val testState = prepare(
            maxSize = 1,
            keepAliveFor = 2.minutes,
            strategy = KotlinObjectPoolStrategy.LIFO,
            instanceCreationPrecondition = {
                if (alreadyFailed) {
                    return@prepare
                }

                alreadyFailed = true
                throw CustomException()
            },
        )
        assertEquals(0, testState.lastCreatedInstanceId)

        assertFailsWith<CustomException> {
            testState.objectPool.take()
        }

        assertEquals(0, testState.lastCreatedInstanceId)

        val instance2 = testState.objectPool.take()

        assertEquals(1, testState.lastCreatedInstanceId)
        assertEquals(1, instance2.identity)

        assertEquals(emptyList(), testState.closed)

        testState.objectPool
            .close()

        advanceUntilIdle()

        assertEquals(emptyList(), testState.closed)
    }

    @Test
    fun withObject_lifoIntegration() = runTest {
        val testState = prepare(
            maxSize = 3,
            keepAliveFor = 2.minutes,
            strategy = KotlinObjectPoolStrategy.LIFO,
        )
        assertEquals(testState.lastCreatedInstanceId, 0)

        val result1 = testState.objectPool.withObject {
            assertEquals(1, testState.lastCreatedInstanceId)
            assertEquals(1, it.identity)

            delay(1)

            return@withObject it.identity
        }

        assertEquals(1, testState.lastCreatedInstanceId)
        assertEquals(1, result1)

        val result2 = testState.objectPool.withObject {
            assertEquals(1, testState.lastCreatedInstanceId)
            assertEquals(1, it.identity)

            return@withObject it.identity
        }

        assertEquals(1, result2)
        assertEquals(1, testState.lastCreatedInstanceId)

        val result3 = testState.objectPool.withObject {
            assertEquals(1, testState.lastCreatedInstanceId)
            assertEquals(1, it.identity)

            val inner = testState.objectPool.withObject {
                assertEquals(2, testState.lastCreatedInstanceId)
                assertEquals(2, it.identity)

                return@withObject it.identity
            }

            return@withObject Pair(it.identity, inner)
        }

        assertEquals(2, testState.lastCreatedInstanceId)
        assertEquals(Pair(1, 2), result3)

        assertFailsWith<CustomException> {
            testState.objectPool.withObject {
                assertEquals(1, it.identity)
                throw CustomException()
            }
        }
        assertEquals(2, testState.lastCreatedInstanceId)

        val result4 = testState.objectPool.withObject {
            assertEquals(1, it.identity)

            return@withObject it.identity
        }

        assertEquals(1, result4)

        testState.objectPool
            .close()

        advanceUntilIdle()

        assertEquals(listOf(2, 1), testState.closed)
    }

    @Test
    fun withObject_fifoIntegration() = runTest {
        val testState = prepare(
            maxSize = 3,
            keepAliveFor = 2.minutes,
            strategy = KotlinObjectPoolStrategy.FIFO,
        )
        assertEquals(testState.lastCreatedInstanceId, 0)

        val result1 = testState.objectPool.withObject {
            assertEquals(1, testState.lastCreatedInstanceId)
            assertEquals(1, it.identity)

            delay(1)

            return@withObject it.identity
        }

        assertEquals(1, testState.lastCreatedInstanceId)
        assertEquals(1, result1)

        val result2 = testState.objectPool.withObject {
            assertEquals(1, testState.lastCreatedInstanceId)
            assertEquals(1, it.identity)

            return@withObject it.identity
        }

        assertEquals(1, result2)
        assertEquals(1, testState.lastCreatedInstanceId)

        val result3 = testState.objectPool.withObject {
            assertEquals(1, testState.lastCreatedInstanceId)
            assertEquals(1, it.identity)

            val inner = testState.objectPool.withObject {
                assertEquals(2, testState.lastCreatedInstanceId)
                assertEquals(2, it.identity)

                return@withObject it.identity
            }

            return@withObject Pair(it.identity, inner)
        }

        assertEquals(2, testState.lastCreatedInstanceId)
        assertEquals(Pair(1, 2), result3)

        assertFailsWith<CustomException> {
            testState.objectPool.withObject {
                assertEquals(2, it.identity)
                throw CustomException()
            }
        }
        assertEquals(2, testState.lastCreatedInstanceId)

        val result4 = testState.objectPool.withObject {
            assertEquals(1, it.identity)

            return@withObject it.identity
        }

        assertEquals(1, result4)

        testState.objectPool
            .close()

        advanceUntilIdle()

        assertEquals(listOf(2, 1), testState.closed)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.prepare(
        maxSize: Int,
        keepAliveFor: Duration?,
        strategy: KotlinObjectPoolStrategy,
        instanceCreationPrecondition: suspend () -> Unit = {},
    ): TestState {
        val start = testTimeSource.markNow()
        var lastIdentity = 0
        val closed = mutableListOf<Int>()
        val objectPool = KotlinObjectPool(
            config = KotlinObjectPoolConfig(
                maxSize = maxSize,
                keepAliveFor = keepAliveFor,
                strategy = strategy,
                coroutineScope = this@prepare,
            ) {
                instanceCreationPrecondition()

                val currentIdentity = ++lastIdentity

                TestItem(
                    identity = currentIdentity,
                    closeHandler = {
                        closed.add(currentIdentity)
                    },
                )
            },
        )
        (objectPool as KotlinObjectPoolImpl).getTime = {
            start.elapsedNow()
                .inWholeMilliseconds
        }

        return TestState(
            objectPool = objectPool,
            closed = closed,
        ) {
            lastIdentity
        }
    }

    class TestState(
        val objectPool: KotlinObjectPool<TestItem>,
        val closed: List<Int>,
        private val getLastCreatedInstanceId: () -> Int,
    ) {

        val lastCreatedInstanceId: Int
            get() =
                getLastCreatedInstanceId()
    }

    @OptIn(ExperimentalStdlibApi::class)
    class TestItem(
        val identity: Int,
        private val closeHandler: () -> Unit,
    ) : AutoCloseable {

        override fun close() {
            closeHandler()
        }

        override fun equals(
            other: Any?,
        ): Boolean =
            other != null
                && other is TestItem
                && identity == other.identity

        override fun hashCode(): Int =
            identity.hashCode()
    }

    class CustomException : Exception()
}
