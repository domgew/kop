package io.github.domgew.kop

public sealed interface Optional<out T> {

    public data class Some<out T>(
        val value: T,
    ) : Optional<T>

    public data object None : Optional<Nothing>
}
