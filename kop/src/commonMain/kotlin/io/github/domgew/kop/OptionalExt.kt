package io.github.domgew.kop

public fun <T> Optional<T>.getOrDefault(
    default: T,
): T =
    when (this) {
        is Optional.Some ->
            value

        else ->
            default
    }

public fun <T> Optional<T>.getOrNull(): T? =
    when (this) {
        is Optional.Some ->
            value

        else ->
            null
    }

public fun <T> Optional<T>.getOrThrow(
    exception: () -> Exception = {
        NullPointerException()
    },
): T =
    when (this) {
        is Optional.Some ->
            value

        Optional.None ->
            throw exception()
    }
