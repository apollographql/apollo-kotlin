@file:Suppress("DEPRECATION")

package com.apollographql.apollo.tooling

import com.apollographql.apollo.testing.internal.runTest
import com.apollographql.apollo.testing.pathToUtf8
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

private val preIntrospectionResponseJune2018 = pathToUtf8("apollo-tooling/src/test/fixtures/pre-introspection-response-june2018.json")
private val introspectionRequestJune2018 = pathToUtf8("apollo-tooling/src/test/fixtures/introspection-request-june2018.json")

private val preIntrospectionResponseOctober2021 = pathToUtf8("apollo-tooling/src/test/fixtures/pre-introspection-response-october2021.json")
private val introspectionRequestOctober2021 = pathToUtf8("apollo-tooling/src/test/fixtures/introspection-request-october2021.json")

private val preIntrospectionResponseSeptember2025 =
  pathToUtf8("apollo-tooling/src/test/fixtures/pre-introspection-response-september2025.json")
private val introspectionRequestSeptember2025 = pathToUtf8("apollo-tooling/src/test/fixtures/introspection-request-september2025.json")

private val preIntrospectionResponseSeptember2025OneOf =
  pathToUtf8("apollo-tooling/src/test/fixtures/pre-introspection-response-september2025-oneOf.json")
private val introspectionRequestSeptember2025OneOf =
  pathToUtf8("apollo-tooling/src/test/fixtures/introspection-request-september2025-oneOf.json")

private val introspectionRequestFailSafe = pathToUtf8("apollo-tooling/src/test/fixtures/introspection-request-failSafe.json")

private val introspectionResponseSuccess = pathToUtf8("apollo-tooling/src/test/fixtures/introspection-response-success.json")
private val introspectionResponseSuccessFullJson = pathToUtf8("apollo-tooling/src/test/fixtures/introspection-response-success-full.json")
private val introspectionResponseSuccessFullSdl =
  pathToUtf8("apollo-tooling/src/test/fixtures/introspection-response-success-full.graphqls")
private val introspectionResponseFail = pathToUtf8("apollo-tooling/src/test/fixtures/introspection-response-fail.json")

class SchemaDownloaderTests {
  private lateinit var mockServer: MockServer
  private lateinit var tempFileJson: File
  private lateinit var tempFileSdl: File

  private fun setUp() {
    mockServer = MockServer()
    tempFileJson = File.createTempFile("schema", ".json")
    tempFileSdl = File.createTempFile("schema", ".graphqls")
  }

  private fun tearDown() {
    mockServer.close()
    tempFileJson.delete()
  }

  @Test
  fun `schema is downloaded correctly when server supports June 2018 spec`() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(preIntrospectionResponseJune2018)
    mockServer.enqueueString(introspectionResponseSuccess)

    SchemaDownloader.download(
        endpoint = mockServer.url(),
        graph = null,
        key = null,
        graphVariant = "",
        schema = tempFileJson,
    )

    mockServer.takeRequest()
    val introspectionRequest = mockServer.takeRequest().body.utf8()
    assertEquals(introspectionRequestJune2018, introspectionRequest)
    assertEquals(introspectionResponseSuccess, tempFileJson.readText())
  }

  @Test
  fun `schema is downloaded correctly when server supports October 2021 spec`() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(preIntrospectionResponseOctober2021)
    mockServer.enqueueString(introspectionResponseSuccess)

    SchemaDownloader.download(
        endpoint = mockServer.url(),
        graph = null,
        key = null,
        graphVariant = "",
        schema = tempFileJson,
    )

    mockServer.takeRequest()
    val introspectionRequest = mockServer.takeRequest().body.utf8()
    assertEquals(introspectionRequestOctober2021, introspectionRequest)
    assertEquals(introspectionResponseSuccess, tempFileJson.readText())
  }

  @Test
  fun `schema is downloaded correctly when server supports September 2025 spec`() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(preIntrospectionResponseSeptember2025)
    mockServer.enqueueString(introspectionResponseSuccess)

    SchemaDownloader.download(
        endpoint = mockServer.url(),
        graph = null,
        key = null,
        graphVariant = "",
        schema = tempFileJson,
    )

    mockServer.takeRequest()
    val introspectionRequest = mockServer.takeRequest().body.utf8()
    assertEquals(introspectionRequestSeptember2025, introspectionRequest)
    assertEquals(introspectionResponseSuccess, tempFileJson.readText())
  }

  @Test
  fun `full schema is downloaded and converted correctly to SDL when server supports September 2025 spec`() =
    runTest(before = { setUp() }, after = { tearDown() }) {
      mockServer.enqueueString(preIntrospectionResponseSeptember2025)
      mockServer.enqueueString(introspectionResponseSuccessFullJson)

      SchemaDownloader.download(
          endpoint = mockServer.url(),
          graph = null,
          key = null,
          graphVariant = "",
          schema = tempFileSdl,
      )

      mockServer.takeRequest()
      val introspectionRequest = mockServer.takeRequest().body.utf8()
      assertEquals(introspectionRequestSeptember2025, introspectionRequest)
      assertEquals(introspectionResponseSuccessFullSdl, tempFileSdl.readText())
    }

  @Test
  fun `schema is downloaded correctly when server supports September 2025 spec including oneOf`() =
    runTest(before = { setUp() }, after = { tearDown() }) {
      mockServer.enqueueString(preIntrospectionResponseSeptember2025OneOf)
      mockServer.enqueueString(introspectionResponseSuccess)

      SchemaDownloader.download(
          endpoint = mockServer.url(),
          graph = null,
          key = null,
          graphVariant = "",
          schema = tempFileJson,
      )

      mockServer.takeRequest()
      val introspectionRequest = mockServer.takeRequest().body.utf8()
      assertEquals(introspectionRequestSeptember2025OneOf, introspectionRequest)
      assertEquals(introspectionResponseSuccess, tempFileJson.readText())
    }

  @Test
  fun `schema is downloaded correctly in fail-safe mode after 2-step fails`() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(preIntrospectionResponseSeptember2025)
    mockServer.enqueueString(introspectionResponseFail)
    mockServer.enqueueString(introspectionResponseSuccess)

    SchemaDownloader.download(
        endpoint = mockServer.url(),
        graph = null,
        key = null,
        graphVariant = "",
        schema = tempFileJson,
    )

    mockServer.takeRequest()
    mockServer.takeRequest()
    val introspectionRequest = mockServer.takeRequest().body.utf8()
    assertEquals(introspectionRequestFailSafe, introspectionRequest)
    assertEquals(introspectionResponseSuccess, tempFileJson.readText())
  }

}
