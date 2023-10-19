package com.apollographql.apollo3.tooling

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.exception.ApolloGraphQLException
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.tooling.platformapi.internal.TrackApolloKotlinUsageMutation
import com.apollographql.apollo3.tooling.platformapi.internal.type.ApolloKotlinUsageEventInput
import com.apollographql.apollo3.tooling.platformapi.internal.type.ApolloKotlinUsagePropertyInput
import java.time.Instant

@ApolloInternal
object Telemetry {
  /**
   * Track Apollo Kotlin usage.
   */
  suspend fun trackApolloKotlinUsage(
      serverUrl: String? = null,
      instanceId: String,
      properties: List<TelemetryProperty>,
      events: List<TelemetryEvent>,
  ): Result<Unit> {
    val apolloClient = newInternalPlatformApiApolloClient(serverUrl = serverUrl ?: INTERNAL_PLATFORM_API_URL)
    val response = apolloClient.mutation(
        TrackApolloKotlinUsageMutation(
            instanceId = instanceId,
            properties = properties.map {
              ApolloKotlinUsagePropertyInput(
                  type = it.type,
                  payload = Optional.presentIfNotNull(it.payload),
              )
            },
            events = events.map {
              ApolloKotlinUsageEventInput(
                  type = it.type,
                  date = it.date,
                  payload = Optional.presentIfNotNull(it.payload),
              )
            },
        )
    ).execute()
    return when (val e = response.exception) {
      null -> {
        Result.success(Unit)
      }

      is ApolloHttpException -> {
        val body = e.body?.use { it.readUtf8() } ?: ""
        Result.failure(Exception("Cannot track Apollo Kotlin usage: (code: ${e.statusCode})\n$body", e))
      }

      is ApolloGraphQLException -> {
        Result.failure(Exception("Cannot track Apollo Kotlin usage: ${e.errors.joinToString { it.message }}"))
      }

      else -> {
        Result.failure(Exception("Cannot track Apollo Kotlin usage: ${e.message}", e))
      }
    }
  }

  @ApolloInternal
  class TelemetryProperty(
      val type: String,
      val payload: Any?,
  )

  @ApolloInternal
  class TelemetryEvent(
      val type: String,
      val date: Instant,
      val payload: Any?,
  )
}
