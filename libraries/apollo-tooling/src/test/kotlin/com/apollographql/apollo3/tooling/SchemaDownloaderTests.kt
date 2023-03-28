package com.apollographql.apollo3.tooling

import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.internal.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SchemaDownloaderTests {
  private lateinit var mockServer: MockServer
  private lateinit var tempFile: File

  private fun setUp() {
    mockServer = MockServer()
    tempFile = File.createTempFile("schema", ".json")
  }

  private suspend fun tearDown() {
    mockServer.stop()
    tempFile.delete()
  }

  private val schemaString1 = """
  {
    "__schema": {
      "queryType": {
        "name": "foo"
      },
      "types": [
        {
          "kind": "OBJECT",
          "name": "UserInfo",
          "description": null,
          "fields": [
            {
              "name": "id",
              "description": null,
              "args": [],
              "type": {
                "kind": "NON_NULL",
                "name": null,
                "ofType": {
                  "kind": "SCALAR",
                  "name": "ID",
                  "ofType": null
                }
              },
              "isDeprecated": false,
              "deprecationReason": null
            }
          ],
          "inputFields": null,
          "interfaces": [
            {
              "kind": "INTERFACE",
              "name": "MyInterface",
              "ofType": null
            }
          ],
          "enumValues": null,
          "possibleTypes": null
        },
        {
          "kind": "INTERFACE",
          "name": "MyInterface",
          "description": null,
          "fields": [
            {
              "name": "id",
              "description": null,
              "args": [],
              "type": {
                "kind": "NON_NULL",
                "name": null,
                "ofType": {
                  "kind": "SCALAR",
                  "name": "ID",
                  "ofType": null
                }
              },
              "isDeprecated": false,
              "deprecationReason": null
            }
          ],
          "inputFields": null,
          "interfaces": [],
          "enumValues": null,
          "possibleTypes": [
            {
              "kind": "OBJECT",
              "name": "UserInfo",
              "ofType": null
            }
          ]
        },
        {
          "kind": "INPUT_OBJECT",
          "name": "DeprecatedInput",
          "description": null,
          "fields": null,
          "inputFields": [
            {
              "name": "deprecatedField",
              "description": "deprecatedField",
              "type": {
                "kind": "SCALAR",
                "name": "String",
                "ofType": null
              },
              "defaultValue": null,
              "isDeprecated": true,
              "deprecationReason": "DeprecatedForTesting"
            }
          ],
          "interfaces": null,
          "enumValues": null,
          "possibleTypes": null
        }
      ]
    }
  }
  """.trimIndent()


  @Test
  fun `schema is downloaded correctly when server doesn't support deprecated input fields and arguments`() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueue(statusCode = 400)
    mockServer.enqueue(schemaString1)

    SchemaDownloader.download(
        endpoint = mockServer.url(),
        graph = null,
        key = null,
        graphVariant = "",
        schema = tempFile,
    )

    mockServer.takeRequest().body.utf8().let {
      assertTrue(it.contains("inputFields(includeDeprecated: true)"))
      assertTrue(it.contains("args(includeDeprecated: true)"))
    }

    mockServer.takeRequest().body.utf8().let {
      assertFalse(it.contains("inputFields(includeDeprecated: true)"))
      assertFalse(it.contains("args(includeDeprecated: true)"))
    }
    assertEquals(schemaString1, tempFile.readText())
  }

  @Test
  fun `schema is downloaded correctly when server doesn't support deprecated input fields and arguments nor isRepeatable on directives`() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueue(statusCode = 400)
    mockServer.enqueue(statusCode = 400)
    mockServer.enqueue(schemaString1)

    SchemaDownloader.download(
        endpoint = mockServer.url(),
        graph = null,
        key = null,
        graphVariant = "",
        schema = tempFile,
    )

    mockServer.takeRequest().body.utf8().let {
      assertTrue(it.contains("inputFields(includeDeprecated: true)"))
      assertTrue(it.contains("args(includeDeprecated: true)"))
      assertTrue(it.contains("isRepeatable"))
    }
    mockServer.takeRequest().body.utf8().let {
      assertFalse(it.contains("inputFields(includeDeprecated: true)"))
      assertFalse(it.contains("args(includeDeprecated: true)"))
      assertTrue(it.contains("isRepeatable"))
    }
    mockServer.takeRequest().body.utf8().let {
      assertFalse(it.contains("inputFields(includeDeprecated: true)"))
      assertFalse(it.contains("args(includeDeprecated: true)"))
      assertFalse(it.contains("isRepeatable"))
    }
    assertEquals(schemaString1, tempFile.readText())
  }
}
