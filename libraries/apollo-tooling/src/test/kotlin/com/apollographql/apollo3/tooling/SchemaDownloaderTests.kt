package com.apollographql.apollo3.tooling

import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueueString
import com.apollographql.apollo3.testing.internal.runTest
import com.apollographql.apollo3.testing.pathToUtf8
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

private val preIntrospectionResponseJune2018 = pathToUtf8("apollo-tooling/src/test/fixtures/pre-introspection-response-june2018.json")

private val preIntrospectionResponseOctober2021 = pathToUtf8("apollo-tooling/src/test/fixtures/pre-introspection-response-october2021.json")

private val preIntrospectionResponseDraft = pathToUtf8("apollo-tooling/src/test/fixtures/pre-introspection-response-draft.json")

private val preIntrospectionResponseOneOf = pathToUtf8("apollo-tooling/src/test/fixtures/pre-introspection-response-oneOf.json")

private val introspectionResponse = pathToUtf8("apollo-tooling/src/test/fixtures/introspection-response.json")

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
    mockServer.enqueueString(introspectionResponse)

    SchemaDownloader.download(
        endpoint = mockServer.url(),
        graph = null,
        key = null,
        graphVariant = "",
        schema = tempFile,
    )

    mockServer.takeRequest()
    mockServer.takeRequest().body.utf8().let {
      assertFalse(it.contains(Regex("}\\s+description\\s+}\\s+}")))
      assertFalse(it.contains("specifiedByURL"))
      assertFalse(it.contains("isRepeatable"))
      assertFalse(it.contains("inputFields(includeDeprecated: true)"))
      assertFalse(it.contains(Regex("directives\\s+\\{\\s+name\\s+description\\s+locations\\s+args\\(includeDeprecated: true\\)")))
      assertFalse(it.contains(Regex("fields\\(includeDeprecated: true\\)\\s+\\{\\s+name\\s+description\\s+args\\(includeDeprecated: true\\)")))
      assertFalse(it.substringAfter("fragment InputValue on __InputValue {").contains("isDeprecated"))
      assertFalse(it.substringAfter("fragment InputValue on __InputValue {").contains("deprecationReason"))
      assertFalse(it.contains("isOneOf"))
    }
    assertEquals(introspectionResponse, tempFile.readText())
  }

  @Test
  fun `schema is downloaded correctly when server supports October 2021 spec`() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(preIntrospectionResponseOctober2021)
    mockServer.enqueueString(introspectionResponse)

    SchemaDownloader.download(
        endpoint = mockServer.url(),
        graph = null,
        key = null,
        graphVariant = "",
        schema = tempFile,
    )

    mockServer.takeRequest()
    mockServer.takeRequest().body.utf8().let {
      assertTrue(it.contains(Regex("}\\s+description\\s+}\\s+}")))
      assertTrue(it.contains("specifiedByURL"))
      assertTrue(it.contains("isRepeatable"))
      assertFalse(it.contains("inputFields(includeDeprecated: true)"))
      assertFalse(it.contains(Regex("directives\\s+\\{\\s+name\\s+description\\s+locations\\s+args\\(includeDeprecated: true\\)")))
      assertFalse(it.contains(Regex("fields\\(includeDeprecated: true\\)\\s+\\{\\s+name\\s+description\\s+args\\(includeDeprecated: true\\)")))
      assertFalse(it.substringAfter("fragment InputValue on __InputValue {").contains("isDeprecated"))
      assertFalse(it.substringAfter("fragment InputValue on __InputValue {").contains("deprecationReason"))
      assertFalse(it.contains("isOneOf"))
    }
    assertEquals(introspectionResponse, tempFile.readText())
  }

  @Test
  fun `schema is downloaded correctly when server supports Draft spec as of 2023-11-15`() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(preIntrospectionResponseDraft)
    mockServer.enqueueString(introspectionResponse)

    SchemaDownloader.download(
        endpoint = mockServer.url(),
        graph = null,
        key = null,
        graphVariant = "",
        schema = tempFile,
    )

    mockServer.takeRequest()
    mockServer.takeRequest().body.utf8().let {
      assertTrue(it.contains(Regex("}\\s+description\\s+}\\s+}")))
      assertTrue(it.contains("specifiedByURL"))
      assertTrue(it.contains("isRepeatable"))
      assertTrue(it.contains("inputFields(includeDeprecated: true)"))
      assertTrue(it.contains(Regex("directives\\s+\\{\\s+name\\s+description\\s+locations\\s+args\\(includeDeprecated: true\\)")))
      assertTrue(it.contains(Regex("fields\\(includeDeprecated: true\\)\\s+\\{\\s+name\\s+description\\s+args\\(includeDeprecated: true\\)")))
      assertTrue(it.substringAfter("fragment InputValue on __InputValue {").contains("isDeprecated"))
      assertTrue(it.substringAfter("fragment InputValue on __InputValue {").contains("deprecationReason"))
      assertFalse(it.contains("isOneOf"))
    }
    assertEquals(introspectionResponse, tempFile.readText())
  }

  @Test
  fun `schema is downloaded correctly when server supports oneOf`() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(preIntrospectionResponseOneOf)
    mockServer.enqueueString(introspectionResponse)

    SchemaDownloader.download(
        endpoint = mockServer.url(),
        graph = null,
        key = null,
        graphVariant = "",
        schema = tempFile,
    )

    mockServer.takeRequest()
    mockServer.takeRequest().body.utf8().let {
      assertTrue(it.contains(Regex("}\\s+description\\s+}\\s+}")))
      assertTrue(it.contains("specifiedByURL"))
      assertTrue(it.contains("isRepeatable"))
      assertTrue(it.contains("inputFields(includeDeprecated: true)"))
      assertTrue(it.contains(Regex("directives\\s+\\{\\s+name\\s+description\\s+locations\\s+args\\(includeDeprecated: true\\)")))
      assertTrue(it.contains(Regex("fields\\(includeDeprecated: true\\)\\s+\\{\\s+name\\s+description\\s+args\\(includeDeprecated: true\\)")))
      assertTrue(it.substringAfter("fragment InputValue on __InputValue {").contains("isDeprecated"))
      assertTrue(it.substringAfter("fragment InputValue on __InputValue {").contains("deprecationReason"))
      assertTrue(it.contains("isOneOf"))
    }
    assertEquals(introspectionResponse, tempFile.readText())
  }
}
