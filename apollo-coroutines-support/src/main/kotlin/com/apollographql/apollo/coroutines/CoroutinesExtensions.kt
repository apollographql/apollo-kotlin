package com.apollographql.apollo.rx2

import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

fun <T> ApolloCall<T>.toDeferred(): Deferred<Response<T>> {
    val deferred = CompletableDeferred<Response<T>>()

    deferred.invokeOnCompletion {
        if (deferred.isCancelled) {
            cancel()
        }
    }

    this.enqueue(object : ApolloCall.Callback<T>() {
        override fun onResponse(response: Response<T>) {
            deferred.complete(response)
        }

        override fun onFailure(e: ApolloException) {
            deferred.completeExceptionally(e)
        }
    })

    return deferred
}
