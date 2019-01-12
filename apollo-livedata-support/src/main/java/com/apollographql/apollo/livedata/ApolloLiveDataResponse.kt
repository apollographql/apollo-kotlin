package com.apollographql.apollo.livedata

import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException

/**
 * The ApolloLiveDataResponse class maps [Response] to [ApolloLiveDataResponse].
 * There are Success, Failure, UnsuccessfulResponse, Complete sealed type classes.
 */

@Suppress("unused", "HasPlatformType")
sealed class ApolloLiveDataResponse<out T> {

    /**
     * API Success response class.
     *
     * [data] is an Optional. (There are responses without data)
     */
    class Success<T>(var response: Response<T>) : ApolloLiveDataResponse<T>() {
        var operation = response.operation()
        var data: T? = response.data()
        var dependentKeys: Set<String> = response.dependentKeys()
        var fromCache: Boolean = response.fromCache()

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
    class Failure<out T>(val exception: ApolloException, response: Response<out T>? = null) : ApolloLiveDataResponse<T>() {
        val errorMessage: String? = exception.localizedMessage
        val errors: List<Error>? = response?.errors()

        override fun toString(): String {
            if (exception.localizedMessage == "UnsuccessfulResponse") {
                return "[ApiResponse.Failure: ${errors.toString()}"
            }
            return "[ApiResponse.Failure: ${exception.localizedMessage}"
        }
    }

    class UnsuccessfulResponse : ApolloException("UnsuccessfulResponse")

    class Complete<out T> : ApolloLiveDataResponse<T>()

    companion object {
        /**
         * ApiResponse Factory
         *
         * [Failure] factory function. Only receives [ApolloException] arguments.
         */
        fun <T> error(ex: ApolloException) = Failure<T>(ex)

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
            Failure(ApolloException(ex.localizedMessage))
        }
    }
}