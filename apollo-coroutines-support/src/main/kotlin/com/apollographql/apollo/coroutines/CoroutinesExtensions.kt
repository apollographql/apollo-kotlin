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
            throw IllegalArgumentException("Bad channel capacity ($capacity). Only UNLIMITED and CONFLATED are supported")
    }
}

/**
 * Converts an {@link ApolloCall} to an {@link kotlinx.coroutines.channels.Channel}. The number of values produced
 * by the channel is based on the {@link com.apollographql.apollo.fetcher.ResponseFetcher} used with the call.
 *
 * @param call the ApolloCall to convert
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
 * Converts an {@link ApolloQueryWatcher} to an {@link kotlinx.coroutines.channels.Channel}.
 *
 * @param call the ApolloQueryWatcher to convert
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
 * @param call the ApolloSubscriptionCall to convert
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
    })

    return channel
}

/**
 * Converts an {@link ApolloPrefetch} to an {@link kotlinx.coroutines.Job}.
 *
 * @param call the ApolloPrefetch to convert
 * @param <T>  the value type.
 * @return the converted job
 */
fun ApolloPrefetch.toJob(): Job {
    val deferred = CompletableDeferred<Unit>()

    deferred.invokeOnCompletion {
        cancel()
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


