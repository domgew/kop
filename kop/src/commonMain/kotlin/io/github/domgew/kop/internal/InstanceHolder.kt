package io.github.domgew.kop.internal

import kotlin.time.TimeMark
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job

@OptIn(ExperimentalUuidApi::class)
internal class InstanceHolder<T>(
    val instance: T,
    val uid: Uuid,
    val destructor: Job,
    val removeAt: TimeMark?,
)
