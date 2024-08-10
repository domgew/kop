package io.github.domgew.kop.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse

class DoubleEndedRingBufferTest {

    @Test
    fun test() {
        val instance = DoubleEndedRingBuffer<String>(3)

        assertEquals(3, instance.capacity)
        assertEquals(0, instance.size)

        assertFails {
            instance.peekFirst()
        }
        assertFails {
            instance.getFirst()
        }
        assertFails {
            instance.getLast()
        }

        instance.putLast("1")

        assertEquals(1, instance.size)

        assertEquals("1", instance.peekFirst())
        assertEquals("1", instance.getFirst())

        instance.putLast("2")

        assertEquals("2", instance.peekFirst())
        assertEquals("2", instance.getLast())

        instance.putLast("3")
        instance.putLast("4")
        instance.putLast("5")

        assertFails {
            instance.putLast("6")
        }

        assertEquals(3, instance.size)
        assertEquals("3", instance.peekFirst())
        assertEquals("3", instance.getFirst())
        assertEquals("5", instance.getLast())
        assertEquals("4", instance.getLast())
        assertEquals(0, instance.size)

        for (item in instance) {
            assertFalse(true, "Shouldn't have been called")
        }

        instance.putLast("7")
        instance.putLast("8")
        instance.putLast("9")

        var iterated = 0

        for (item in instance.withIndex()) {
            iterated++
            assertEquals(
                (item.index + 7)
                    .toString(),
                item.value,
            )
        }

        assertEquals(3, iterated)
        assertEquals(0, instance.size)
    }
}
