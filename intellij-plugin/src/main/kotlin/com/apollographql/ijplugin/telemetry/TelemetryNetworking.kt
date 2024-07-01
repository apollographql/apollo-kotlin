package com.apollographql.ijplugin.telemetry

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.tooling.Telemetry
import org.jetbrains.kotlin.idea.util.application.isApplicationInternalMode

private const val STAGING_INTERNAL_PLATFORM_API_URL = "https://graphql-staging.api.apollographql.com/api/graphql"

@OptIn(ApolloInternal::class)
suspend fun executeTelemetryNetworkCall(telemetrySession: TelemetrySession) {
  Telemetry.trackApolloKotlinUsage(
      serverUrl = if (isApplicationInternalMode()) STAGING_INTERNAL_PLATFORM_API_URL else null,
      instanceId = telemetrySession.instanceId,
      properties = telemetrySession.properties.map { it.toToolingTelemetryProperty() },
      events = telemetrySession.events.map { it.toTelemetryEvent() },
  )
}

@OptIn(ApolloInternal::class)
private fun TelemetryProperty.toToolingTelemetryProperty() = Telemetry.TelemetryProperty(
    type = type,
    payload = parameters,
)

@OptIn(ApolloInternal::class)
private fun TelemetryEvent.toTelemetryEvent() = Telemetry.TelemetryEvent(
    type = type,
    date = date,
    payload = parameters,
)
