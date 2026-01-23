class TestObject(
    private val id: Int,
) : AutoCloseable {

    private var destroyed = false

    fun print() {
        println(this)
    }

    override fun close() {
        destroyed = true
        println("Destroying $this")
    }

    override fun toString(): String =
        "TestObject#$id(destroyed=$destroyed)"
}
