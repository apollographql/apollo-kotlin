package com.apollographql.apollo3.tooling

import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueueString
import com.apollographql.apollo3.testing.internal.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

private const val metaSchemaJune2018Response = """
    {
      "data": {
        "__schema": {
          "types": [
            {
              "name": "Int",
              "fields": null
            },
            {
              "name": "String",
              "fields": null
            },
            {
              "name": "Boolean",
              "fields": null
            },
            {
              "name": "ID",
              "fields": null
            },
            {
              "name": "__Schema",
              "fields": [
                {
                  "name": "types",
                  "args": []
                },
                {
                  "name": "queryType",
                  "args": []
                },
                {
                  "name": "mutationType",
                  "args": []
                },
                {
                  "name": "subscriptionType",
                  "args": []
                },
                {
                  "name": "directives",
                  "args": []
                }
              ]
            },
            {
              "name": "__Type",
              "fields": [
                {
                  "name": "kind",
                  "args": []
                },
                {
                  "name": "name",
                  "args": []
                },
                {
                  "name": "description",
                  "args": []
                },
                {
                  "name": "fields",
                  "args": [
                    {
                      "name": "includeDeprecated"
                    }
                  ]
                },
                {
                  "name": "interfaces",
                  "args": []
                },
                {
                  "name": "possibleTypes",
                  "args": []
                },
                {
                  "name": "enumValues",
                  "args": [
                    {
                      "name": "includeDeprecated"
                    }
                  ]
                },
                {
                  "name": "inputFields",
                  "args": []
                },
                {
                  "name": "ofType",
                  "args": []
                }
              ]
            },
            {
              "name": "__TypeKind",
              "fields": null
            },
            {
              "name": "__Field",
              "fields": [
                {
                  "name": "name",
                  "args": []
                },
                {
                  "name": "description",
                  "args": []
                },
                {
                  "name": "args",
                  "args": []
                },
                {
                  "name": "type",
                  "args": []
                },
                {
                  "name": "isDeprecated",
                  "args": []
                },
                {
                  "name": "deprecationReason",
                  "args": []
                }
              ]
            },
            {
              "name": "__InputValue",
              "fields": [
                {
                  "name": "name",
                  "args": []
                },
                {
                  "name": "description",
                  "args": []
                },
                {
                  "name": "type",
                  "args": []
                },
                {
                  "name": "defaultValue",
                  "args": []
                }
              ]
            },
            {
              "name": "__EnumValue",
              "fields": [
                {
                  "name": "name",
                  "args": []
                },
                {
                  "name": "description",
                  "args": []
                },
                {
                  "name": "isDeprecated",
                  "args": []
                },
                {
                  "name": "deprecationReason",
                  "args": []
                }
              ]
            },
            {
              "name": "__Directive",
              "fields": [
                {
                  "name": "name",
                  "args": []
                },
                {
                  "name": "description",
                  "args": []
                },
                {
                  "name": "locations",
                  "args": []
                },
                {
                  "name": "args",
                  "args": []
                }
              ]
            },
            {
              "name": "__DirectiveLocation",
              "fields": null
            }
          ]
        }
      }
    }
  """

private const val metaSchemaOctober2021Response = """
    {
      "data": {
        "__schema": {
          "types": [
            {
              "name": "Int",
              "fields": null
            },
            {
              "name": "String",
              "fields": null
            },
            {
              "name": "Boolean",
              "fields": null
            },
            {
              "name": "ID",
              "fields": null
            },
            {
              "name": "__Schema",
              "fields": [
                {
                  "name": "types",
                  "args": []
                },
                {
                  "name": "queryType",
                  "args": []
                },
                {
                  "name": "mutationType",
                  "args": []
                },
                {
                  "name": "subscriptionType",
                  "args": []
                },
                {
                  "name": "directives",
                  "args": []
                },
                {
                  "name": "description",
                  "args": []
                }
              ]
            },
            {
              "name": "__Type",
              "fields": [
                {
                  "name": "kind",
                  "args": []
                },
                {
                  "name": "name",
                  "args": []
                },
                {
                  "name": "description",
                  "args": []
                },
                {
                  "name": "fields",
                  "args": [
                    {
                      "name": "includeDeprecated"
                    }
                  ]
                },
                {
                  "name": "interfaces",
                  "args": []
                },
                {
                  "name": "possibleTypes",
                  "args": []
                },
                {
                  "name": "enumValues",
                  "args": [
                    {
                      "name": "includeDeprecated"
                    }
                  ]
                },
                {
                  "name": "inputFields",
                  "args": []
                },
                {
                  "name": "ofType",
                  "args": []
                },
                {
                  "name": "specifiedByURL",
                  "args": []
                }

              ]
            },
            {
              "name": "__TypeKind",
              "fields": null
            },
            {
              "name": "__Field",
              "fields": [
                {
                  "name": "name",
                  "args": []
                },
                {
                  "name": "description",
                  "args": []
                },
                {
                  "name": "args",
                  "args": []
                },
                {
                  "name": "type",
                  "args": []
                },
                {
                  "name": "isDeprecated",
                  "args": []
                },
                {
                  "name": "deprecationReason",
                  "args": []
                }
              ]
            },
            {
              "name": "__InputValue",
              "fields": [
                {
                  "name": "name",
                  "args": []
                },
                {
                  "name": "description",
                  "args": []
                },
                {
                  "name": "type",
                  "args": []
                },
                {
                  "name": "defaultValue",
                  "args": []
                }
              ]
            },
            {
              "name": "__EnumValue",
              "fields": [
                {
                  "name": "name",
                  "args": []
                },
                {
                  "name": "description",
                  "args": []
                },
                {
                  "name": "isDeprecated",
                  "args": []
                },
                {
                  "name": "deprecationReason",
                  "args": []
                }
              ]
            },
            {
              "name": "__Directive",
              "fields": [
                {
                  "name": "name",
                  "args": []
                },
                {
                  "name": "description",
                  "args": []
                },
                {
                  "name": "locations",
                  "args": []
                },
                {
                  "name": "args",
                  "args": []
                },
                {
                  "name": "isRepeatable",
                  "args": []
                }
              ]
            },
            {
              "name": "__DirectiveLocation",
              "fields": null
            }
          ]
        }
      }
    }
  """

private const val metaSchemaDraftResponse = """
    {
      "data": {
        "__schema": {
          "types": [
            {
              "name": "Int",
              "fields": null
            },
            {
              "name": "String",
              "fields": null
            },
            {
              "name": "Boolean",
              "fields": null
            },
            {
              "name": "ID",
              "fields": null
            },
            {
              "name": "__Schema",
              "fields": [
                {
                  "name": "types",
                  "args": []
                },
                {
                  "name": "queryType",
                  "args": []
                },
                {
                  "name": "mutationType",
                  "args": []
                },
                {
                  "name": "subscriptionType",
                  "args": []
                },
                {
                  "name": "directives",
                  "args": []
                },
                {
                  "name": "description",
                  "args": []
                }
              ]
            },
            {
              "name": "__Type",
              "fields": [
                {
                  "name": "kind",
                  "args": []
                },
                {
                  "name": "name",
                  "args": []
                },
                {
                  "name": "description",
                  "args": []
                },
                {
                  "name": "fields",
                  "args": [
                    {
                      "name": "includeDeprecated"
                    }
                  ]
                },
                {
                  "name": "interfaces",
                  "args": []
                },
                {
                  "name": "possibleTypes",
                  "args": []
                },
                {
                  "name": "enumValues",
                  "args": [
                    {
                      "name": "includeDeprecated"
                    }
                  ]
                },
                {
                  "name": "inputFields",
                  "args": [
                    {
                      "name": "includeDeprecated"
                    }
                  ]
                },
                {
                  "name": "ofType",
                  "args": []
                },
                {
                  "name": "specifiedByURL",
                  "args": []
                }

              ]
            },
            {
              "name": "__TypeKind",
              "fields": null
            },
            {
              "name": "__Field",
              "fields": [
                {
                  "name": "name",
                  "args": []
                },
                {
                  "name": "description",
                  "args": []
                },
                {
                  "name": "args",
                  "args": [
                    {
                      "name": "includeDeprecated"
                    }
                  ]
                },
                {
                  "name": "type",
                  "args": []
                },
                {
                  "name": "isDeprecated",
                  "args": []
                },
                {
                  "name": "deprecationReason",
                  "args": []
                }
              ]
            },
            {
              "name": "__InputValue",
              "fields": [
                {
                  "name": "name",
                  "args": []
                },
                {
                  "name": "description",
                  "args": []
                },
                {
                  "name": "type",
                  "args": []
                },
                {
                  "name": "isDeprecated",
                  "args": []
                },
                {
                  "name": "deprecationReason",
                  "args": []
                },
                {
                  "name": "defaultValue",
                  "args": []
                }
              ]
            },
            {
              "name": "__EnumValue",
              "fields": [
                {
                  "name": "name",
                  "args": []
                },
                {
                  "name": "description",
                  "args": []
                },
                {
                  "name": "isDeprecated",
                  "args": []
                },
                {
                  "name": "deprecationReason",
                  "args": []
                }
              ]
            },
            {
              "name": "__Directive",
              "fields": [
                {
                  "name": "name",
                  "args": []
                },
                {
                  "name": "description",
                  "args": []
                },
                {
                  "name": "locations",
                  "args": []
                },
                {
                  "name": "args",
                  "args": [
                    {
                      "name": "includeDeprecated"
                    }
                  ]
                },
                {
                  "name": "isRepeatable",
                  "args": []
                }
              ]
            },
            {
              "name": "__DirectiveLocation",
              "fields": null
            }
          ]
        }
      }
    }
  """

private const val metaSchemaOneOfResponse = """
    {
      "data": {
        "__schema": {
          "types": [
            {
              "name": "Int",
              "fields": null
            },
            {
              "name": "String",
              "fields": null
            },
            {
              "name": "Boolean",
              "fields": null
            },
            {
              "name": "ID",
              "fields": null
            },
            {
              "name": "__Schema",
              "fields": [
                {
                  "name": "types",
                  "args": []
                },
                {
                  "name": "queryType",
                  "args": []
                },
                {
                  "name": "mutationType",
                  "args": []
                },
                {
                  "name": "subscriptionType",
                  "args": []
                },
                {
                  "name": "directives",
                  "args": []
                },
                {
                  "name": "description",
                  "args": []
                }
              ]
            },
            {
              "name": "__Type",
              "fields": [
                {
                  "name": "kind",
                  "args": []
                },
                {
                  "name": "name",
                  "args": []
                },
                {
                  "name": "description",
                  "args": []
                },
                {
                  "name": "fields",
                  "args": [
                    {
                      "name": "includeDeprecated"
                    }
                  ]
                },
                {
                  "name": "interfaces",
                  "args": []
                },
                {
                  "name": "possibleTypes",
                  "args": []
                },
                {
                  "name": "enumValues",
                  "args": [
                    {
                      "name": "includeDeprecated"
                    }
                  ]
                },
                {
                  "name": "inputFields",
                  "args": [
                    {
                      "name": "includeDeprecated"
                    }
                  ]
                },
                {
                  "name": "ofType",
                  "args": []
                },
                {
                  "name": "specifiedByURL",
                  "args": []
                },
                {
                  "name": "isOneOf",
                  "args": []
                }
              ]
            },
            {
              "name": "__TypeKind",
              "fields": null
            },
            {
              "name": "__Field",
              "fields": [
                {
                  "name": "name",
                  "args": []
                },
                {
                  "name": "description",
                  "args": []
                },
                {
                  "name": "args",
                  "args": [
                    {
                      "name": "includeDeprecated"
                    }
                  ]
                },
                {
                  "name": "type",
                  "args": []
                },
                {
                  "name": "isDeprecated",
                  "args": []
                },
                {
                  "name": "deprecationReason",
                  "args": []
                }
              ]
            },
            {
              "name": "__InputValue",
              "fields": [
                {
                  "name": "name",
                  "args": []
                },
                {
                  "name": "description",
                  "args": []
                },
                {
                  "name": "type",
                  "args": []
                },
                {
                  "name": "isDeprecated",
                  "args": []
                },
                {
                  "name": "deprecationReason",
                  "args": []
                },
                {
                  "name": "defaultValue",
                  "args": []
                }
              ]
            },
            {
              "name": "__EnumValue",
              "fields": [
                {
                  "name": "name",
                  "args": []
                },
                {
                  "name": "description",
                  "args": []
                },
                {
                  "name": "isDeprecated",
                  "args": []
                },
                {
                  "name": "deprecationReason",
                  "args": []
                }
              ]
            },
            {
              "name": "__Directive",
              "fields": [
                {
                  "name": "name",
                  "args": []
                },
                {
                  "name": "description",
                  "args": []
                },
                {
                  "name": "locations",
                  "args": []
                },
                {
                  "name": "args",
                  "args": [
                    {
                      "name": "includeDeprecated"
                    }
                  ]
                },
                {
                  "name": "isRepeatable",
                  "args": []
                }
              ]
            },
            {
              "name": "__DirectiveLocation",
              "fields": null
            }
          ]
        }
      }
    }
  """

private const val schemaResponse = """
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
  """

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
    mockServer.enqueueString(metaSchemaJune2018Response)
    mockServer.enqueueString(schemaResponse)

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
    assertEquals(schemaResponse, tempFile.readText())
  }

  @Test
  fun `schema is downloaded correctly when server supports October 2021 spec`() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(metaSchemaOctober2021Response)
    mockServer.enqueueString(schemaResponse)

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
    assertEquals(schemaResponse, tempFile.readText())
  }

  @Test
  fun `schema is downloaded correctly when server supports Draft spec as of 2023-11-15`() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(metaSchemaDraftResponse)
    mockServer.enqueueString(schemaResponse)

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
    assertEquals(schemaResponse, tempFile.readText())
  }

  @Test
  fun `schema is downloaded correctly when server supports oneOf`() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(metaSchemaOneOfResponse)
    mockServer.enqueueString(schemaResponse)

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
    assertEquals(schemaResponse, tempFile.readText())
  }
}
