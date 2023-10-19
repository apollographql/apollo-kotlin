package com.apollographql.apollo3.tooling

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.exception.ApolloGraphQLException
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.tooling.platformapi.internal.FieldLatenciesQuery
import java.time.Instant

@ApolloExperimental
object FieldInsights {
  /**
   * Fetches the field latencies for a given service.
   *
   * @param serverUrl the URL of the Apollo Platform API. Used for testing only, defaults to the production URL.
   * @param apiKey the API key to use.
   * @param serviceId the service ID (aka Graph ID) to get the data from.
   * @param fromSeconds starting point of the data, as a number of seconds in the past from now. Defaults to 864000 (10 days).
   * @param percentile the percentile to use for the histogram, from 0 to 1. Defaults to 0.95.
   */
  suspend fun fetchFieldLatencies(
      serverUrl: String = INTERNAL_PLATFORM_API_URL,
      apiKey: String,
      serviceId: String,
      fromSeconds: Long = 864000,
      percentile: Double = 0.95,
  ): FieldLatenciesResult {
    val apolloClient = newInternalPlatformApiApolloClient(serverUrl = serverUrl, apiKey = apiKey)
    val response = apolloClient.query(
        FieldLatenciesQuery(
            serviceId = serviceId,
            fromTimestamp = Instant.ofEpochSecond(Instant.now().epochSecond - fromSeconds),
            percentile = percentile,
        )
    ).execute()
    val data = response.data
    return when {
      data == null -> {
        val cause = when (val e = response.exception!!) {
          is ApolloHttpException -> {
            val body = e.body?.use { it.readUtf8() } ?: ""
            Exception("Cannot fetch field latencies: (code: ${e.statusCode})\n$body", e)
          }

          is ApolloGraphQLException -> {
            Exception("Cannot fetch field latencies: ${e.errors.joinToString { it.message }}")
          }

          else -> {
            Exception("Cannot fetch field latencies: ${e.message}", e)
          }
        }
        FieldLatenciesResult.Error(cause = cause)
      }

      data.service == null && response.hasErrors() -> {
        FieldLatenciesResult.Error(cause = Exception("Cannot fetch field latencies: ${response.errors!!.joinToString { it.message }}"))
      }

      else -> {
        FieldLatencies(fieldLatencies = data.service?.statsWindow?.fieldLatencies?.mapNotNull {
          val parentType = it.groupBy.parentType ?: return@mapNotNull null
          val fieldName = it.groupBy.fieldName ?: return@mapNotNull null
          val durationMs = it.metrics.fieldHistogram.durationMs ?: return@mapNotNull null
          FieldLatencies.FieldLatency(
              parentType = parentType,
              fieldName = fieldName,
              durationMs = durationMs
          )
        } ?: emptyList())
      }
    }
  }

  @ApolloExperimental
  sealed interface FieldLatenciesResult {
    @ApolloExperimental
    class Error(val cause: Exception) : FieldLatenciesResult
  }

  @ApolloExperimental
  class FieldLatencies(
      val fieldLatencies: List<FieldLatency>,
  ) : FieldLatenciesResult {
    @ApolloExperimental
    class FieldLatency(
        val parentType: String,
        val fieldName: String,
        val durationMs: Double,
    ) {
      override fun toString(): String {
        return "FieldLatency(parentType='$parentType', fieldName='$fieldName', durationMs=$durationMs)"
      }
    }

    private val latenciesByField: Map<String, Double> by lazy {
      fieldLatencies.groupBy { "${it.parentType}/${it.fieldName}" }
          .mapValues { (_, fieldLatencies) ->
            fieldLatencies.first().durationMs
          }
    }

    fun getLatency(parentType: String, fieldName: String): Double? {
      return latenciesByField["$parentType/$fieldName"]
    }

    override fun toString(): String {
      return "FieldLatencies(fieldLatencies=$fieldLatencies)"
    }
  }
}
