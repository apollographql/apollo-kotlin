package com.apollographql.apollo.coroutines

import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import kotlinx.coroutines.channels.Channel

fun <T> ApolloCall<T>.toChannel(): Channel<Response<T>> {
    val channel = Channel<Response<T>>(Channel.UNLIMITED)
    enqueue(object : ApolloCall.Callback<T>() {
        override fun onResponse(response: Response<T>) {
            channel.offer(response)
        }

        override fun onFailure(e: ApolloException) {
            channel.close()
        }
    })

    return channel
}

