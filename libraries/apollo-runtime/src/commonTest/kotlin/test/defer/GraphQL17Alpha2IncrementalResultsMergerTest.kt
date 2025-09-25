@file:OptIn(ApolloInternal::class)

package test.defer

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.api.DeferredFragmentIdentifier
import com.apollographql.apollo.api.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.json.readAny
import com.apollographql.apollo.internal.incremental.GraphQL17Alpha2IncrementalResultsMerger
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun String.buffer() = Buffer().writeUtf8(this)

@Suppress("UNCHECKED_CAST")
private fun jsonToMap(json: String): Map<String, Any?> = BufferedSourceJsonReader(json.buffer()).readAny() as Map<String, Any?>

class GraphQL17Alpha2IncrementalResultsMergerTest {
  @Test
  fun mergeJsonSingleIncrementalItem() {
    val incrementalResultsMerger = GraphQL17Alpha2IncrementalResultsMerger()

    //language=JSON
    val payload1 = """
    {
      "data": {
        "computers": [
          {
            "id": "Computer1",
            "screen": {
              "isTouch": true
            }
          },
          {
            "id": "Computer2",
            "screen": {
              "isTouch": false
            }
          }
        ]
      },
      "hasNext": true
    }
    """.trimIndent()
    incrementalResultsMerger.merge(payload1.buffer())
    assertEquals(jsonToMap(payload1), incrementalResultsMerger.merged)
    assertEquals(
        setOf(),
        incrementalResultsMerger.incrementalResultIdentifiers
    )

    //language=JSON
    val payload2 = """
    {
      "incremental": [
        {
          "data": {
            "cpu": "386",
            "year": 1993,
            "screen": {
              "resolution": "640x480"
            }
          },
          "path": [
            "computers",
            0
          ],
          "label": "query:Query1:0",
          "extensions": {
            "duration": {
              "amount": 100,
              "unit": "ms"
            }
          }
        }
      ],
      "hasNext": true
    }
    """.trimIndent()
    //language=JSON
    val mergedPayloads_1_2 = """
    {
      "data": {
        "computers": [
          {
            "id": "Computer1",
            "cpu": "386",
            "year": 1993,
            "screen": {
              "isTouch": true,
              "resolution": "640x480"
            }
          },
          {
            "id": "Computer2",
            "screen": {
              "isTouch": false
            }
          }
        ]
      },
      "hasNext": true,
      "extensions": {
        "incremental": [
          {
            "duration": {
              "amount": 100,
              "unit": "ms"
            }
          }
        ]
      }
    }      
    """.trimIndent()
    incrementalResultsMerger.merge(payload2.buffer())
    assertEquals(jsonToMap(mergedPayloads_1_2), incrementalResultsMerger.merged)
    assertEquals(
        setOf(
            DeferredFragmentIdentifier(path = listOf("computers", 0), label = "query:Query1:0"),
        ),
        incrementalResultsMerger.incrementalResultIdentifiers
    )

    //language=JSON
    val payload3 = """
    {
      "incremental": [
        {
          "data": {
            "cpu": "486",
            "year": 1996,
            "screen": {
              "resolution": "640x480"
            }
          },
          "path": [
            "computers",
            1
          ],
          "label": "query:Query1:0",
          "extensions": {
            "duration": {
              "amount": 25,
              "unit": "ms"
            }
          }
        }
      ],
      "hasNext": true
    }
    """.trimIndent()

    //language=JSON
    val mergedPayloads_1_2_3 = """
    {
      "data": {
        "computers": [
          {
            "id": "Computer1",
            "cpu": "386",
            "year": 1993,
            "screen": {
              "isTouch": true,
              "resolution": "640x480"
            }
          },
          {
            "id": "Computer2",
            "cpu": "486",
            "year": 1996,
            "screen": {
              "isTouch": false,
              "resolution": "640x480"
            }
          }
        ]
      },
      "hasNext": true,
      "extensions": {
        "incremental": [
          {
            "duration": {
              "amount": 25,
              "unit": "ms"
            }
          }
        ]
      }
    }
    """.trimIndent()
    incrementalResultsMerger.merge(payload3.buffer())
    assertEquals(jsonToMap(mergedPayloads_1_2_3), incrementalResultsMerger.merged)
    assertEquals(
        setOf(
            DeferredFragmentIdentifier(path = listOf("computers", 0), label = "query:Query1:0"),
            DeferredFragmentIdentifier(path = listOf("computers", 1), label = "query:Query1:0"),
        ),
        incrementalResultsMerger.incrementalResultIdentifiers
    )

    //language=JSON
    val payload4 = """
    {
      "incremental": [
        {
          "data": null,
          "path": [
            "computers",
            0,
            "screen"
          ],
          "errors": [
            {
              "message": "Cannot resolve isColor",
              "locations": [
                {
                  "line": 12,
                  "column": 11
                }
              ],
              "path": [
                "computers",
                0,
                "screen",
                "isColor"
              ]
            }
          ],
          "label": "fragment:ComputerFields:0"
        }
      ],
      "hasNext": true
    }
    """.trimIndent()
    //language=JSON
    val mergedPayloads_1_2_3_4 = """
    {
      "data": {
        "computers": [
          {
            "id": "Computer1",
            "cpu": "386",
            "year": 1993,
            "screen": {
              "isTouch": true,
              "resolution": "640x480"
            }
          },
          {
            "id": "Computer2",
            "cpu": "486",
            "year": 1996,
            "screen": {
              "isTouch": false,
              "resolution": "640x480"
            }
          }
        ]
      },
      "hasNext": true,
      "errors": [
        {
          "message": "Cannot resolve isColor",
          "locations": [
            {
              "line": 12,
              "column": 11
            }
          ],
          "path": [
            "computers",
            0,
            "screen",
            "isColor"
          ]
        }
      ]
    }
    """.trimIndent()
    incrementalResultsMerger.merge(payload4.buffer())
    assertEquals(jsonToMap(mergedPayloads_1_2_3_4), incrementalResultsMerger.merged)
    assertEquals(
        setOf(
            DeferredFragmentIdentifier(path = listOf("computers", 0), label = "query:Query1:0"),
            DeferredFragmentIdentifier(path = listOf("computers", 1), label = "query:Query1:0"),
        ),
        incrementalResultsMerger.incrementalResultIdentifiers
    )

    //language=JSON
    val payload5 = """
    {
      "incremental": [
        {
          "data": {
            "isColor": false
          },
          "path": [
            "computers",
            1,
            "screen"
          ],
          "errors": [
            {
              "message": "Another error",
              "locations": [
                {
                  "line": 1,
                  "column": 1
                }
              ]
            }
          ],
          "label": "fragment:ComputerFields:0",
          "extensions": {
            "value": 42,
            "duration": {
              "amount": 130,
              "unit": "ms"
            }
          }
        }
      ],
      "hasNext": false
    }
    """.trimIndent()
    //language=JSON
    val mergedPayloads_1_2_3_4_5 = """
    {
      "data": {
        "computers": [
          {
            "id": "Computer1",
            "cpu": "386",
            "year": 1993,
            "screen": {
              "isTouch": true,
              "resolution": "640x480"
            }
          },
          {
            "id": "Computer2",
            "cpu": "486",
            "year": 1996,
            "screen": {
              "isTouch": false,
              "resolution": "640x480",
              "isColor": false
            }
          }
        ]
      },
      "hasNext": true,
      "extensions": {
        "incremental": [
          {
            "value": 42,
            "duration": {
              "amount": 130,
              "unit": "ms"
            }
          }
        ]
      },
      "errors": [
        {
          "message": "Another error",
          "locations": [
            {
              "line": 1,
              "column": 1
            }
          ]
        }
      ]
    }
    """.trimIndent()
    incrementalResultsMerger.merge(payload5.buffer())
    assertEquals(jsonToMap(mergedPayloads_1_2_3_4_5), incrementalResultsMerger.merged)
    assertEquals(
        setOf(
            DeferredFragmentIdentifier(path = listOf("computers", 0), label = "query:Query1:0"),
            DeferredFragmentIdentifier(path = listOf("computers", 1), label = "query:Query1:0"),
            DeferredFragmentIdentifier(path = listOf("computers", 1, "screen"), label = "fragment:ComputerFields:0"),
        ),
        incrementalResultsMerger.incrementalResultIdentifiers
    )
  }

  @Test
  fun mergeJsonMultipleIncrementalItems() {
    val incrementalResultsMerger = GraphQL17Alpha2IncrementalResultsMerger()

    //language=JSON
    val payload1 = """
    {
      "data": {
        "computers": [
          {
            "id": "Computer1",
            "screen": {
              "isTouch": true
            }
          },
          {
            "id": "Computer2",
            "screen": {
              "isTouch": false
            }
          }
        ]
      },
      "hasNext": true
    }
    """.trimIndent()
    incrementalResultsMerger.merge(payload1.buffer())
    assertEquals(jsonToMap(payload1), incrementalResultsMerger.merged)
    assertEquals(
        setOf(),
        incrementalResultsMerger.incrementalResultIdentifiers
    )

    //language=JSON
    val payload2_3 = """
    {
      "incremental": [
        {
          "data": {
            "cpu": "386",
            "year": 1993,
            "screen": {
              "resolution": "640x480"
            }
          },
          "path": [
            "computers",
            0
          ],
          "label": "query:Query1:0",
          "extensions": {
            "duration": {
              "amount": 100,
              "unit": "ms"
            }
          }
        },
        {
          "data": {
            "cpu": "486",
            "year": 1996,
            "screen": {
              "resolution": "640x480"
            }
          },
          "path": [
            "computers",
            1
          ],
          "label": "query:Query1:0",
          "extensions": {
            "duration": {
              "amount": 25,
              "unit": "ms"
            }
          }
        }
      ],
      "hasNext": true
    }
    """.trimIndent()
    //language=JSON
    val mergedPayloads_1_2_3 = """
    {
      "data": {
        "computers": [
          {
            "id": "Computer1",
            "cpu": "386",
            "year": 1993,
            "screen": {
              "isTouch": true,
              "resolution": "640x480"
            }
          },
          {
            "id": "Computer2",
            "cpu": "486",
            "year": 1996,
            "screen": {
              "isTouch": false,
              "resolution": "640x480"
            }
          }
        ]
      },
      "hasNext": true,
      "extensions": {
        "incremental": [
          {
            "duration": {
              "amount": 100,
              "unit": "ms"
            }
          },
          {
            "duration": {
              "amount": 25,
              "unit": "ms"
            }
          }
        ]
      }
    }
    """.trimIndent()
    incrementalResultsMerger.merge(payload2_3.buffer())
    assertEquals(jsonToMap(mergedPayloads_1_2_3), incrementalResultsMerger.merged)
    assertEquals(
        setOf(
            DeferredFragmentIdentifier(path = listOf("computers", 0), label = "query:Query1:0"),
            DeferredFragmentIdentifier(path = listOf("computers", 1), label = "query:Query1:0"),
        ),
        incrementalResultsMerger.incrementalResultIdentifiers
    )

    //language=JSON
    val payload4_5 = """
    {
      "incremental": [
        {
          "data": null,
          "path": [
            "computers",
            0,
            "screen"
          ],
          "errors": [
            {
              "message": "Cannot resolve isColor",
              "locations": [
                {
                  "line": 12,
                  "column": 11
                }
              ],
              "path": [
                "computers",
                0,
                "screen",
                "isColor"
              ]
            }
          ],
          "label": "fragment:ComputerFields:0"
        },
        {
          "data": {
            "isColor": false
          },
          "path": [
            "computers",
            1,
            "screen"
          ],
          "errors": [
            {
              "message": "Another error",
              "locations": [
                {
                  "line": 1,
                  "column": 1
                }
              ]
            }
          ],
          "label": "fragment:ComputerFields:0",
          "extensions": {
            "value": 42,
            "duration": {
              "amount": 130,
              "unit": "ms"
            }
          }
        }
      ],
      "hasNext": true
    }
    """.trimIndent()
    //language=JSON
    val mergedPayloads_1_2_3_4_5 = """
    {
      "data": {
        "computers": [
          {
            "id": "Computer1",
            "cpu": "386",
            "year": 1993,
            "screen": {
              "isTouch": true,
              "resolution": "640x480"
            }
          },
          {
            "id": "Computer2",
            "cpu": "486",
            "year": 1996,
            "screen": {
              "isTouch": false,
              "resolution": "640x480",
              "isColor": false
            }
          }
        ]
      },
      "hasNext": true,
      "extensions": {
        "incremental": [
          {
            "value": 42,
            "duration": {
              "amount": 130,
              "unit": "ms"
            }
          }
        ]
      },
      "errors": [
        {
          "message": "Cannot resolve isColor",
          "locations": [
            {
              "line": 12,
              "column": 11
            }
          ],
          "path": [
            "computers",
            0,
            "screen",
            "isColor"
          ]
        },
        {
          "message": "Another error",
          "locations": [
            {
              "line": 1,
              "column": 1
            }
          ]
        }
      ]
    }
    """.trimIndent()
    incrementalResultsMerger.merge(payload4_5.buffer())
    assertEquals(jsonToMap(mergedPayloads_1_2_3_4_5), incrementalResultsMerger.merged)
    assertEquals(
        setOf(
            DeferredFragmentIdentifier(path = listOf("computers", 0), label = "query:Query1:0"),
            DeferredFragmentIdentifier(path = listOf("computers", 1), label = "query:Query1:0"),
            DeferredFragmentIdentifier(path = listOf("computers", 1, "screen"), label = "fragment:ComputerFields:0"),
        ),
        incrementalResultsMerger.incrementalResultIdentifiers
    )
  }

  @Test
  fun emptyPayloads() {
    val incrementalResultsMerger = GraphQL17Alpha2IncrementalResultsMerger()

    //language=JSON
    val payload1 = """
    {
      "data": {
        "computers": [
          {
            "id": "Computer1",
            "screen": {
              "isTouch": true
            }
          },
          {
            "id": "Computer2",
            "screen": {
              "isTouch": false
            }
          }
        ]
      },
      "hasNext": true
    }
    """.trimIndent()
    incrementalResultsMerger.merge(payload1.buffer())
    assertFalse(incrementalResultsMerger.isEmptyResponse)

    //language=JSON
    val payload2 = """
      {
        "hasNext": true
      }
    """.trimIndent()
    incrementalResultsMerger.merge(payload2.buffer())
    assertTrue(incrementalResultsMerger.isEmptyResponse)

    //language=JSON
    val payload3 = """
    {
      "incremental": [
        {
          "data": {
            "cpu": "386",
            "year": 1993,
            "screen": {
              "resolution": "640x480"
            }
          },
          "path": [
            "computers",
            0
          ],
          "label": "query:Query1:0",
          "extensions": {
            "duration": {
              "amount": 100,
              "unit": "ms"
            }
          }
        }
      ],
      "hasNext": true
    }
    """.trimIndent()
    incrementalResultsMerger.merge(payload3.buffer())
    assertFalse(incrementalResultsMerger.isEmptyResponse)

    //language=JSON
    val payload4 = """
    {
      "hasNext": false
    }
    """.trimIndent()
    incrementalResultsMerger.merge(payload4.buffer())
    assertTrue(incrementalResultsMerger.isEmptyResponse)
  }
}
