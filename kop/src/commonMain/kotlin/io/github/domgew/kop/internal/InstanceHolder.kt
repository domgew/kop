package io.github.domgew.kop.internal

import com.benasher44.uuid.Uuid
import kotlinx.coroutines.Job

internal class InstanceHolder<T>(
    val instance: T,
    val uid: Uuid,
    val destructor: Job,
    val addedAtMillis: Long,
)
