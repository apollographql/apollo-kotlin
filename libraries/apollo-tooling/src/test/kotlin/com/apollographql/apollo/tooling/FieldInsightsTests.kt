package com.apollographql.apollo.tooling

import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import com.apollographql.apollo.testing.internal.runTest
import com.google.common.truth.Truth
import org.junit.Test

class FieldInsightsTests {
  private lateinit var mockServer: MockServer

  private fun setUp() {
    mockServer = MockServer()
  }

  private fun tearDown() {
    mockServer.close()
  }

  private val fieldLatenciesResponse = """
    {
      "data": {
        "service": {
          "statsWindow": {
            "fieldLatencies": [
              {
                "groupBy": {
                  "parentType": "Address",
                  "fieldName": "addressLines"
                },
                "metrics": {
                  "fieldHistogram": {
                    "durationMs": 0.16952823783332988
                  }
                }
              },
              {
                "groupBy": {
                  "parentType": "EducationEntry",
                  "fieldName": "degree"
                },
                "metrics": {
                  "fieldHistogram": {
                    "durationMs": 0.18132490810292387
                  }
                }
              },
              {
                "groupBy": {
                  "parentType": "EducationEntry",
                  "fieldName": "institution"
                },
                "metrics": {
                  "fieldHistogram": {
                    "durationMs": 0.18132490810292387
                  }
                }
              },
              {
                "groupBy": {
                  "parentType": "Year",
                  "fieldName": "year"
                },
                "metrics": {
                  "fieldHistogram": {
                    "durationMs": 0.20512916777832918
                  }
                }
              }
            ]
          }
        }
      }
    }
  """.trimIndent()

  @Test
  fun fetchFieldLatenciesSuccess() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(fieldLatenciesResponse)
    val results = FieldInsights.fetchFieldLatencies(serverUrl = mockServer.url(), apiKey = "apiKey", serviceId = "serviceId")
    Truth.assertThat(results).isInstanceOf(FieldInsights.FieldLatencies::class.java)
    Truth.assertThat((results as FieldInsights.FieldLatencies).fieldLatencies).hasSize(4)

    val firstFieldLatency = results.fieldLatencies.first()
    Truth.assertThat(firstFieldLatency.fieldName).isEqualTo("addressLines")
    Truth.assertThat(firstFieldLatency.parentType).isEqualTo("Address")
    Truth.assertThat(firstFieldLatency.durationMs).isEqualTo(0.16952823783332988)

    val latency = results.getLatency("Address", "addressLines")
    Truth.assertThat(latency).isEqualTo(0.16952823783332988)
  }

  @Test
  fun fetchFieldLatenciesFail() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString("There was an issue", statusCode = 400)
    val results = FieldInsights.fetchFieldLatencies(serverUrl = mockServer.url(), apiKey = "apiKey", serviceId = "serviceId")
    Truth.assertThat(results).isInstanceOf(FieldInsights.FieldLatenciesResult.Error::class.java)
    val cause = (results as FieldInsights.FieldLatenciesResult.Error).cause
    Truth.assertThat(cause.message).isEqualTo("Cannot fetch field latencies: (code: 400)\nThere was an issue")
  }
}
