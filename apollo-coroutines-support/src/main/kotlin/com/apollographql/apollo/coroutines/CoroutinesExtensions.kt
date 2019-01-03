package com.apollographql.apollo.coroutines

import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloPrefetch
import com.apollographql.apollo.ApolloQueryWatcher
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private class ChannelCallback<T>(val channel: Channel<Response<T>>): ApolloCall.Callback<T>() {

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

fun <T> ApolloCall<T>.toChannel(): Channel<Response<T>> {
    val channel = Channel<Response<T>>(Channel.UNLIMITED)

    channel.invokeOnClose {
        cancel()
    }
    enqueue(ChannelCallback(channel))

    return channel
}

fun <T> ApolloQueryWatcher<T>.toChannel(): Channel<Response<T>> {
    val channel = Channel<Response<T>>(Channel.UNLIMITED)

    channel.invokeOnClose {
        cancel()
    }
    enqueueAndWatch(ChannelCallback(channel))

    return channel
}

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


