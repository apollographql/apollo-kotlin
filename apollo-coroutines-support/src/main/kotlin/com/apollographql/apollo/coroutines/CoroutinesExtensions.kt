package com.apollographql.apollo.coroutines

import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloPrefetch
import com.apollographql.apollo.ApolloQueryWatcher
import com.apollographql.apollo.ApolloSubscriptionCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private class ChannelCallback<T>(val channel: Channel<Response<T>>) : ApolloCall.Callback<T>() {

    override fun onResponse(response: Response<T>) {
        channel.offer(response)
    }

    override fun onFailure(e: ApolloException) {
        channel.close(e)
    }

    override fun onStatusEvent(event: ApolloCall.StatusEvent) {
        if (event == ApolloCall.StatusEvent.COMPLETED) {
            channel.close()
        }
    }
}

private fun checkCapacity(capacity: Int) {
    when (capacity) {
        Channel.UNLIMITED,
        Channel.CONFLATED -> return
        else ->
            // Everything else than UNLIMITED or CONFLATED does not guarantee that channel.offer() succeeds all the time.
            // We don't support these use cases for now
            throw IllegalArgumentException("Bad channel capacity ($capacity). Only UNLIMITED and CONFLATED are supported")
    }
}

/**
 * Converts an {@link ApolloCall} to an {@link kotlinx.coroutines.channels.Channel}. The number of values produced
 * by the channel is based on the {@link com.apollographql.apollo.fetcher.ResponseFetcher} used with the call.
 *
 * @param <T>  the value type.
 * @param capacity the {@link Capacity} used for the underlying channel. Only {@link kotlinx.coroutines.channels.Channel.UNLIMITED}
 * and {@link kotlinx.coroutines.channels.Channel.CONFLATED} are supported at the moment
 * @throws IllegalArgumentException if capacity is not {@link kotlinx.coroutines.channels.Channel.UNLIMITED}
 * or {@link kotlinx.coroutines.channels.Channel.CONFLATED}
 * @return the converted channel
 */
fun <T> ApolloCall<T>.toChannel(capacity: Int = Channel.UNLIMITED): Channel<Response<T>> {
    checkCapacity(capacity)
    val channel = Channel<Response<T>>(capacity)

    channel.invokeOnClose {
        cancel()
    }
    enqueue(ChannelCallback(channel))

    return channel
}

/**
 * Converts an {@link ApolloCall} to an {@link kotlinx.coroutines.Deferred}. This is a convenience method
 * that will only return the first value emitted. If the more than one response is required, for an example
 * to retrieve cached and network response, use {@link toChannel} instead.
 *
 * @param <T>  the value type.
 * @return the deferred
 */
fun <T> ApolloCall<T>.toDeferred(): Deferred<Response<T>> {
    val deferred = CompletableDeferred<Response<T>>()

    deferred.invokeOnCompletion {
        if (deferred.isCancelled) {
            cancel()
        }
    }
    enqueue(object: ApolloCall.Callback<T>() {
        override fun onResponse(response: Response<T>) {
            deferred.complete(response)
        }

        override fun onFailure(e: ApolloException) {
            deferred.completeExceptionally(e)
        }
    })

    return deferred
}

/**
 * Converts an {@link ApolloQueryWatcher} to an {@link kotlinx.coroutines.channels.Channel}.
 *
 * @param <T>  the value type.
 * @param capacity the {@link Capacity} used for the underlying channel. Only {@link kotlinx.coroutines.channels.Channel.UNLIMITED}
 * and {@link kotlinx.coroutines.channels.Channel.CONFLATED} are supported at the moment
 * @throws IllegalArgumentException if capacity is not {@link kotlinx.coroutines.channels.Channel.UNLIMITED}
 * or {@link kotlinx.coroutines.channels.Channel.CONFLATED}
 * @return the converted channel
 */
fun <T> ApolloQueryWatcher<T>.toChannel(capacity: Int = Channel.UNLIMITED): Channel<Response<T>> {
    checkCapacity(capacity)
    val channel = Channel<Response<T>>(capacity)

    channel.invokeOnClose {
        cancel()
    }
    enqueueAndWatch(ChannelCallback(channel))

    return channel
}

/**
 * Converts an {@link ApolloSubscriptionCall} to an {@link kotlinx.coroutines.channels.Channel}.
 *
 * @param <T>  the value type.
 * @param capacity the {@link Capacity} used for the underlying channel. Only {@link kotlinx.coroutines.channels.Channel.UNLIMITED}
 * and {@link kotlinx.coroutines.channels.Channel.CONFLATED} are supported at the moment
 * @throws IllegalArgumentException if capacity is not {@link kotlinx.coroutines.channels.Channel.UNLIMITED}
 * or {@link kotlinx.coroutines.channels.Channel.CONFLATED}
 * @return the converted channel
 */
fun <T> ApolloSubscriptionCall<T>.toChannel(capacity: Int = Channel.UNLIMITED): Channel<Response<T>> {
    checkCapacity(capacity)
    val channel = Channel<Response<T>>(capacity)

    channel.invokeOnClose {
        cancel()
    }
    execute(object : ApolloSubscriptionCall.Callback<T> {
        override fun onResponse(response: Response<T>) {
            channel.offer(response)
        }

        override fun onFailure(e: ApolloException) {
            channel.close(e)
        }

        override fun onCompleted() {
            channel.close()
        }

        override fun onTerminated() {
            channel.close()
        }
    })

    return channel
}

/**
 * Converts an {@link ApolloPrefetch} to an {@link kotlinx.coroutines.Job}.
 *
 * @param <T>  the value type.
 * @return the converted job
 */
fun ApolloPrefetch.toJob(): Job {
    val deferred = CompletableDeferred<Unit>()

    deferred.invokeOnCompletion {
        if (deferred.isCancelled) {
            cancel()
        }
    }

    enqueue(object : ApolloPrefetch.Callback() {
        override fun onSuccess() {
            deferred.complete(Unit)
        }

        override fun onFailure(e: ApolloException) {
            deferred.completeExceptionally(e)
        }
    })

    return deferred
}


