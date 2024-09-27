package io.github.domgew.kop.internal

internal class DoubleEndedRingBuffer<T>(
    val capacity: Int,
) : Iterable<T> {

    private var _headIdxIncl: Int = 0
    private var _tailIdxExcl: Int = 0
    private var _size: Int = 0
    private val _buffer = MutableList<T?>(capacity) { null }

    val size: Int by ::_size

    fun getFirst(): T {
        require(size > 0)

        return _buffer[_headIdxIncl]!!
            .also {
                _buffer[_headIdxIncl] = null
                _headIdxIncl = (_headIdxIncl + 1).mod(capacity)
                _size = size - 1
            }
    }

    fun peekFirst(): T {
        require(size > 0)

        return _buffer[_headIdxIncl]!!
    }

    fun getLast(): T {
        require(size > 0)

        val tailIdxIncl = if (_tailIdxExcl == 0) {
            capacity - 1
        } else {
            _tailIdxExcl - 1
        }

        return _buffer[tailIdxIncl]!!
            .also {
                _buffer[tailIdxIncl] = null
                _tailIdxExcl = tailIdxIncl
                _size -= 1
            }
    }

    fun putLast(
        item: T,
    ) {
        require(size < capacity)

        _buffer[_tailIdxExcl] = item
        _tailIdxExcl = (_tailIdxExcl + 1).mod(capacity)
        _size += 1
    }

    override fun iterator(): Iterator<T> =
        iterator {
            while (size > 0) {
                yield(getFirst())
            }
        }
}
