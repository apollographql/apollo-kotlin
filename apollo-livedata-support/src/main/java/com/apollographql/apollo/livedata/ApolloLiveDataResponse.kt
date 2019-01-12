package com.apollographql.apollo.livedata

import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.Response

/**
 * The ApolloLiveDataResponse class maps [Response] to [ApolloLiveDataResponse].
 * There are Success, Failure, UnsuccessfulResponse, Complete sealed type classes.
 */

@Suppress("unused")
sealed class ApolloLiveDataResponse<out T> {

    /**
     * API Success response class.
     *
     * [data] is an Optional. (There are responses without data)
     */
    class Success<out T>(response: Response<T>) : ApolloLiveDataResponse<T>() {
        val operation = response.operation()
        val data: T? = response.data()
        val dependentKeys: Set<String> = response.dependentKeys()
        val fromCache: Boolean = response.fromCache()

        override fun toString() = "[ApiResponse.Success: $data]"
    }

    /**
     * API Failure response class.
     *
     * ## Throw Exception case.
     * Communication is not successful or an exception related to the model deserialization occurs.
     *
     * ## API Network format error case.
     * API communication conventions do not match or applications need to handle exceptions or errors.
     */
    class Failure<out T>(private val error: Exception, response: Response<out T>? = null) : ApolloLiveDataResponse<T>() {
        val errorMessage: String? = error.localizedMessage
        val errors: List<Error>? = response?.errors()

        override fun toString() = "[ApiResponse.Failure: ${error.localizedMessage}"
    }

    class UnsuccessfulResponse : Exception()

    class Complete<out T> : ApolloLiveDataResponse<T>()

    companion object {
        /**
         * ApiResponse Factory
         *
         * [Failure] factory function. Only receives [Exception] arguments.
         */
        fun <T> error(ex: Exception) = Failure<T>(ex)

        /**
         * ApiResponse Factory
         *
         * [f] Create ApiResponse from [Response] returning from the block.
         * If [Response] has not errors, It will create [ApolloLiveDataResponse.Success]
         * If [Response] has errors, It will create [ApolloLiveDataResponse.Failure] using [UnsuccessfulResponse]
         */
        fun <T> of(f: () -> Response<T>): ApolloLiveDataResponse<T> = try {
            val response = f()
            if (!response.hasErrors()) {
                Success(response)
            } else {
                Failure(UnsuccessfulResponse(), response)
            }
        } catch (ex: Exception) {
            Failure(ex)
        }
    }
}