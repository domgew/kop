package io.github.domgew.kop

import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope

@KotlinObjectPoolDsl
public class KotlinObjectPoolBuildScope<T> internal constructor() {

    /**
     * Thrown when a configuration value is missing at the end of the build block invocation
     */
    public class MissingConfig internal constructor(
        /**
         * The missing configuration value
         */
        public val what: String,
    ) : Exception(
        "Missing configuration for $what",
    )

    /**
     * @see KotlinObjectPoolConfig.maxSize
     */
    public fun maxSize(
        value: Int,
    ) {
        _maxSize = value
    }

    /**
     * @see KotlinObjectPoolConfig.keepAliveFor
     */
    public fun keepAliveFor(
        value: Duration,
    ) {
        _keepAliveFor = value
    }

    /**
     * @see KotlinObjectPoolConfig.strategy
     */
    public fun strategy(
        value: KotlinObjectPoolStrategy,
    ) {
        _strategy = value
    }

    /**
     * Callback to be called before an object is closed.
     *
     * **Warning**: This is blocking.
     */
    public fun onBeforeClose(
        block: ((T) -> Unit)?,
    ) {
        _onBeforeClose = block
    }

    /**
     * Callback to be called after an object was closed.
     *
     * **Warning**: This is blocking.
     */
    public fun onAfterClose(
        block: ((T) -> Unit)?,
    ) {
        _onAfterClose = block
    }

    /**
     * The time source to use to compute the time to live from.
     *
     * Mostly useful in testing. Defaults to [TimeSource.Monotonic].
     */
    public fun timeSource(
        value: TimeSource,
    ) {
        _timeSource = value
    }

    /**
     * The coroutine scope in which the cleanup jobs are to run in.
     *
     * Mostly useful in testing. Defaults to [GlobalScope].
     */
    public fun coroutineScope(
        value: CoroutineScope,
    ) {
        _coroutineScope = value
    }

    /**
     * Used to create a new object instance, when needed.
     */
    public fun createInstance(
        block: suspend () -> T,
    ) {
        _createInstance = block
    }

    private var _maxSize: Int? = null
    private var _keepAliveFor: Duration = Duration.INFINITE
    private var _strategy: KotlinObjectPoolStrategy = KotlinObjectPoolStrategy.LIFO
    private var _onBeforeClose: ((T) -> Unit)? = null
    private var _onAfterClose: ((T) -> Unit)? = null
    private var _timeSource: TimeSource = TimeSource.Monotonic
    private var _createInstance: (suspend () -> T)? = null

    @OptIn(DelicateCoroutinesApi::class)
    private var _coroutineScope: CoroutineScope = GlobalScope

    internal constructor(
        baseConfig: KotlinObjectPoolConfig,
    ) : this() {
        _maxSize = baseConfig.maxSize
        _keepAliveFor = baseConfig.keepAliveFor
        _strategy = baseConfig.strategy
    }

    internal fun build(): KotlinObjectPool<T> =
        KotlinObjectPool(
            config = KotlinObjectPoolConfig(
                maxSize = _maxSize
                    ?: throw MissingConfig("maxSize"),
                keepAliveFor = _keepAliveFor,
                strategy = _strategy,
            ),
            onBeforeClose = _onBeforeClose,
            onAfterClose = _onAfterClose,
            coroutineScope = _coroutineScope,
            timeSource = _timeSource,
            createInstance = _createInstance
                ?: throw MissingConfig("createInstance"),
        )
}
