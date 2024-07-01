package platformapi

import com.apollographql.apollo.testing.internal.runTest
import com.apollographql.apollo.tooling.FieldInsights
import com.apollographql.apollo.tooling.Telemetry
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertIs

/**
 * Tests using the Apollo Platform API.
 *
 * These do nothing by default because they rely on an external service.
 * They are enabled when running from the specific `platform-api-tests` CI workflow.
 */
class PlatformApiTest {
  @Test
  fun `can fetch field insights`() = runTest {
    val apiKey = System.getenv("PLATFORM_API_TESTS_KEY") ?: return@runTest
    val fieldLatenciesResult = FieldInsights.fetchFieldLatencies(
        apiKey = apiKey,
        serviceId = "Apollo-Kotlin-CI-tests",
    )
    assertIs<FieldInsights.FieldLatencies>(fieldLatenciesResult)
  }

  @Test
  fun `can post telemetry`() = runTest {
    if (System.getenv("PLATFORM_API_TESTS_KEY") == null) return@runTest
    Telemetry.trackApolloKotlinUsage(
        // Use the staging backend to not pollute the production data
        serverUrl = "https://graphql-staging.api.apollographql.com/api/graphql",
        instanceId = "apollo-kotlin-ci-tests",
        properties = listOf(
            Telemetry.TelemetryProperty(
                type = "testProperty",
                payload = mapOf(
                    "name" to "test",
                    "value" to "test",
                )
            )
        ),
        events = listOf(
            Telemetry.TelemetryEvent(
                type = "testEvent",
                date = Instant.now(),
                payload = mapOf(
                    "name" to "test",
                    "value" to "test",
                )
            )
        )
    )
  }
}