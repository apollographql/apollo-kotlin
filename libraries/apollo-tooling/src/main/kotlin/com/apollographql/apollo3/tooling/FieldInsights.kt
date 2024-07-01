package com.apollographql.apollo.tooling

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.tooling.platformapi.internal.FieldLatenciesQuery
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
    if (data == null) {
      return FieldLatenciesResult.Error(cause = response.toException("Cannot fetch field latencies"))
    }

    val service = data.service
    if (service == null) {
      // As of Dec-2023, service doesn't return an error when it is null
      // Better safe than sorry though, and we still display the GraphQL errors.
      return FieldLatenciesResult.Error(cause = Exception("Cannot find service $serviceId: ${response.errors?.joinToString { it.message }}}"))
    }

    return FieldLatencies(fieldLatencies = service.statsWindow.fieldLatencies.mapNotNull {
      val parentType = it.groupBy.parentType ?: return@mapNotNull null
      val fieldName = it.groupBy.fieldName ?: return@mapNotNull null
      val durationMs = it.metrics.fieldHistogram.durationMs ?: return@mapNotNull null
      FieldLatencies.FieldLatency(
          parentType = parentType,
          fieldName = fieldName,
          durationMs = durationMs
      )
    })
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
