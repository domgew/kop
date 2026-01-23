import io.github.domgew.kop.KotlinObjectPool
import io.github.domgew.kop.KotlinObjectPoolStrategy
import io.github.domgew.kop.getOrNull
import io.github.domgew.kop.withObject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun main(): Unit = runBlocking {
    var currentIdx = 0
    val objectPool = KotlinObjectPool.build {
        maxSize(2)
        keepAliveFor(1.seconds)
        strategy(KotlinObjectPoolStrategy.LIFO)
        onBeforeClose {
            println("Before closing $it")
        }
        onAfterClose {
            println("After closing $it")
        }
        createInstance {
            TestObject(
                id = ++currentIdx,
            )
        }
    }

    val first = objectPool.tryTake()
        .getOrNull()
    val second = objectPool.tryTake()
        .getOrNull()

    if (
        objectPool.tryTake()
            .getOrNull() != null
    ) {
        println("Third should have been null")
        return@runBlocking
    }

    objectPool.giveBack(first!!)
    objectPool.giveBack(second!!)

    objectPool.withObject {
        it.print()
    }

    delay(2.seconds)
}
