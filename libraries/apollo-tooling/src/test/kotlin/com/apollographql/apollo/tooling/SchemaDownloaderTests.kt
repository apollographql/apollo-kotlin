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

private val preIntrospectionResponseDraft = pathToUtf8("apollo-tooling/src/test/fixtures/pre-introspection-response-draft.json")
private val introspectionRequestDraft = pathToUtf8("apollo-tooling/src/test/fixtures/introspection-request-draft.json")

private val preIntrospectionResponseOneOf = pathToUtf8("apollo-tooling/src/test/fixtures/pre-introspection-response-oneOf.json")
private val introspectionRequestOneOf = pathToUtf8("apollo-tooling/src/test/fixtures/introspection-request-oneOf.json")

private val introspectionRequestFailSafe = pathToUtf8("apollo-tooling/src/test/fixtures/introspection-request-failSafe.json")

private val introspectionResponseSuccess = pathToUtf8("apollo-tooling/src/test/fixtures/introspection-response-success.json")
private val introspectionResponseFail = pathToUtf8("apollo-tooling/src/test/fixtures/introspection-response-fail.json")

class SchemaDownloaderTests {
  private lateinit var mockServer: MockServer
  private lateinit var tempFile: File

  private fun setUp() {
    mockServer = MockServer()
    tempFile = File.createTempFile("schema", ".json")
  }

  private fun tearDown() {
    mockServer.close()
    tempFile.delete()
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
        schema = tempFile,
    )

    mockServer.takeRequest()
    val introspectionRequest = mockServer.takeRequest().body.utf8()
    assertEquals(introspectionRequestJune2018, introspectionRequest)
    assertEquals(introspectionResponseSuccess, tempFile.readText())
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
        schema = tempFile,
    )

    mockServer.takeRequest()
    val introspectionRequest = mockServer.takeRequest().body.utf8()
    assertEquals(introspectionRequestOctober2021, introspectionRequest)
    assertEquals(introspectionResponseSuccess, tempFile.readText())
  }

  @Test
  fun `schema is downloaded correctly when server supports Draft spec as of 2023-11-15`() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(preIntrospectionResponseDraft)
    mockServer.enqueueString(introspectionResponseSuccess)

    SchemaDownloader.download(
        endpoint = mockServer.url(),
        graph = null,
        key = null,
        graphVariant = "",
        schema = tempFile,
    )

    mockServer.takeRequest()
    val introspectionRequest = mockServer.takeRequest().body.utf8()
    assertEquals(introspectionRequestDraft, introspectionRequest)
    assertEquals(introspectionResponseSuccess, tempFile.readText())
  }

  @Test
  fun `schema is downloaded correctly when server supports oneOf`() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(preIntrospectionResponseOneOf)
    mockServer.enqueueString(introspectionResponseSuccess)

    SchemaDownloader.download(
        endpoint = mockServer.url(),
        graph = null,
        key = null,
        graphVariant = "",
        schema = tempFile,
    )

    mockServer.takeRequest()
    val introspectionRequest = mockServer.takeRequest().body.utf8()
    assertEquals(introspectionRequestOneOf, introspectionRequest)
    assertEquals(introspectionResponseSuccess, tempFile.readText())
  }

  @Test
  fun `schema is downloaded correctly in fail-safe mode after 2-step fails`() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(preIntrospectionResponseDraft)
    mockServer.enqueueString(introspectionResponseFail)
    mockServer.enqueueString(introspectionResponseSuccess)

    SchemaDownloader.download(
        endpoint = mockServer.url(),
        graph = null,
        key = null,
        graphVariant = "",
        schema = tempFile,
    )

    mockServer.takeRequest()
    mockServer.takeRequest()
    val introspectionRequest = mockServer.takeRequest().body.utf8()
    assertEquals(introspectionRequestFailSafe, introspectionRequest)
    assertEquals(introspectionResponseSuccess, tempFile.readText())
  }

}
