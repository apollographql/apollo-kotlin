package com.apollographql.apollo.livedata

import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException

/**
 * The ApolloLiveDataResponse class maps [Response] to [ApolloLiveDataResponse].
 * There are Success, Failure, UnsuccessfulResponse & Complete sealed type classes.
 */

@Suppress("unused", "HasPlatformType")
sealed class ApolloLiveDataResponse<out T> {

    /**
     * API Success response class.
     *
     * [data] is optional. (There are responses without data)
     */
    class Success<T>(val response: Response<T>) : ApolloLiveDataResponse<T>() {
        val data: T? = response.data()
        override fun toString() = "[ApiResponse.Success]: $data"
    }

    /**
     * API Failure response class.
     *
     * ## Throw Exception case. [Failure.Exception]
     * Gets called when an unexpected exception occurs while creating the request or processing the response.
     *
     * ## API Network format error case. [Failure.GraphQLError]
     * API communication conventions do not match or applications need to handle errors.
     */
    sealed class Failure<out T> {
        class Exception<out T>(val exception: ApolloException) : ApolloLiveDataResponse<T>() {
            val message: String? = exception.localizedMessage
            override fun toString(): String = "[ApiResponse.Failure]: $message"
        }

        class GraphQLError<out T>(val response: Response<out T>) : ApolloLiveDataResponse<T>() {
            val errors: List<Error> = response.errors()
            override fun toString(): String = "[ApiResponse.Failure]: $errors"
        }
    }

    /**
     * API Complete response class.
     *
     * Gets called when [com.apollographql.apollo.ApolloSubscriptionCall.Callback.onCompleted] occurs.
     */
    class Complete<out T> : ApolloLiveDataResponse<T>()

    companion object {
        /**
         * ApiResponse Factory
         *
         * [Failure] factory function. Only receives [ApolloException] arguments.
         */
        fun <T> error(ex: ApolloException) = Failure.Exception<T>(ex)

        /**
         * ApiResponse Factory
         *
         * [f] Create ApiResponse from [Response] returning from the block.
         * If [Response] has no errors, it will create [ApolloLiveDataResponse.Success]
         * If [Response] has errors, it will create [ApolloLiveDataResponse.Failure.GraphQLError]
         */
        fun <T> of(f: () -> Response<T>): ApolloLiveDataResponse<T> = try {
            val response = f()
            if (!response.hasErrors()) {
                Success(response)
            } else {
                Failure.GraphQLError(response)
            }
        } catch (ex: Exception) {
            Failure.Exception(ApolloException(ex.localizedMessage))
        }
    }
}