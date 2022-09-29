package com.apollographql.apollo3.internal

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel

/**
 * A wrapper for [Channel] that adds a method equivalent to [invokeOnClose], which is marked [ExperimentalCoroutinesApi].
 * There is a risk this API will be removed or changed in the future, which could break consumers of this library - that is why we use our
 * own method.
 *
 * [Original source](https://github.com/Kotlin/kotlinx.coroutines/blob/version-1.5.2/kotlinx-coroutines-core/common/src/channels/AbstractChannel.kt#L286)
 *
 * TODO: remove when Channel.invokeOnClose is no longer marked ExperimentalCoroutinesApi.
 */
internal class ChannelWrapper<E>(private val wrapped: Channel<E>) : Channel<E> by wrapped {
  private var handler: ((cause: Throwable?) -> Unit)? = null

  var isClosed: Boolean = false
    private set

  /**
   * See [invokeOnClose].
   */
  fun setInvokeOnClose(handler: (cause: Throwable?) -> Unit) {
    this.handler = handler
  }

  override fun close(cause: Throwable?): Boolean {
    isClosed = true
    val closeAdded = wrapped.close(cause)
    if (closeAdded) handler?.invoke(cause)
    handler = null
    return closeAdded
  }
}
