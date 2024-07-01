package com.apollographql.apollo.internal

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

/**
 * This is copied from [Flow], which is marked [ExperimentalCoroutinesApi].
 * There is a risk this API will be removed or changed in the future, which could break consumers of this library - that is why we use our
 * own copy.
 *
 * [Original source](https://github.com/Kotlin/kotlinx.coroutines/blob/version-1.5.2/kotlinx-coroutines-core/common/src/flow/operators/Limit.kt#L116)
 *
 * TODO: remove when kotlinx.coroutines.flow.Flow.transformWhile is no longer marked ExperimentalCoroutinesApi.
 */
internal fun <T, R> Flow<T>.transformWhile(
    transform: suspend FlowCollector<R>.(value: T) -> Boolean,
): Flow<R> =
    flow {
      return@flow collectWhile { value ->
        transform(value)
      }
    }

private suspend inline fun <T> Flow<T>.collectWhile(crossinline predicate: suspend (value: T) -> Boolean) {
  val collector = object : FlowCollector<T> {
    override suspend fun emit(value: T) {
      if (!predicate(value)) {
        throw AbortFlowException(this)
      }
    }
  }
  try {
    collect {
      collector.emit(it)
    }
  } catch (e: AbortFlowException) {
    e.checkOwnership(collector)
  }
}

private class AbortFlowException(
    val owner: FlowCollector<*>,
) : CancellationException("Flow was aborted, no more elements needed") {
  fun checkOwnership(owner: FlowCollector<*>) {
    if (this.owner !== owner) throw this
  }
}
