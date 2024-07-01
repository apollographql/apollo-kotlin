package com.apollographql.apollo.tooling

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.tooling.platformapi.internal.TrackApolloKotlinUsageMutation
import com.apollographql.apollo.tooling.platformapi.internal.type.ApolloKotlinUsageEventInput
import com.apollographql.apollo.tooling.platformapi.internal.type.ApolloKotlinUsagePropertyInput
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
  ) {
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

    if (response.data == null) {
      throw response.toException("Cannot track Apollo Kotlin usage")
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
