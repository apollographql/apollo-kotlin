@file:OptIn(ApolloInternal::class)

package com.apollographql.apollo3.internal

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.DeferredFragmentIdentifier
import com.apollographql.apollo3.api.json.BufferedSourceJsonReader
import com.apollographql.apollo3.api.json.readAny
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

class DeferJsonMergerTest {
  @Test
  @Suppress("UNCHECKED_CAST")
  fun mergeJson() {
    val deferredJsonMerger = DeferredJsonMerger()

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
    """
    deferredJsonMerger.merge(payload1.buffer())
    assertEquals(jsonToMap(payload1), deferredJsonMerger.merged)
    assertEquals(setOf(), deferredJsonMerger.mergedFragmentIds)


    val payload2 = """
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
        "hasNext": true,
        "extensions": {
          "duration": {
            "amount": 100,
            "unit": "ms"
          }
        }
      }
    """
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
          "duration": {
            "amount": 100,
            "unit": "ms"
          }
        }
      }      
    """
    deferredJsonMerger.merge(payload2.buffer())
    assertEquals(jsonToMap(mergedPayloads_1_2), deferredJsonMerger.merged)
    assertEquals(setOf(
        DeferredFragmentIdentifier(path = listOf("computers", 0), label = "query:Query1:0"),
    ), deferredJsonMerger.mergedFragmentIds)


    val payload3 = """
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
        "hasNext": true,
        "extensions": {
          "duration": {
            "amount": 25,
            "unit": "ms"
          }
        }
      }
    """
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
          "duration": {
            "amount": 25,
            "unit": "ms"
          }
        }
      }
    """
    deferredJsonMerger.merge(payload3.buffer())
    assertEquals(jsonToMap(mergedPayloads_1_2_3), deferredJsonMerger.merged)
    assertEquals(setOf(
        DeferredFragmentIdentifier(path = listOf("computers", 0), label = "query:Query1:0"),
        DeferredFragmentIdentifier(path = listOf("computers", 1), label = "query:Query1:0"),
    ), deferredJsonMerger.mergedFragmentIds)


    val payload4 = """
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
        "label": "fragment:ComputerFields:0",
        "hasNext": true
      }
    """
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
    """
    deferredJsonMerger.merge(payload4.buffer())
    assertEquals(jsonToMap(mergedPayloads_1_2_3_4), deferredJsonMerger.merged)
    assertEquals(setOf(
        DeferredFragmentIdentifier(path = listOf("computers", 0), label = "query:Query1:0"),
        DeferredFragmentIdentifier(path = listOf("computers", 1), label = "query:Query1:0"),
    ), deferredJsonMerger.mergedFragmentIds)

    val payload5 = """
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
        "hasNext": false,
        "extensions": {
          "value": 42,
          "duration": {
            "amount": 130,
            "unit": "ms"
          }
        }
      }
    """
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
          "value": 42,
          "duration": {
            "amount": 130,
            "unit": "ms"
          }
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
    """
    deferredJsonMerger.merge(payload5.buffer())
    assertEquals(jsonToMap(mergedPayloads_1_2_3_4_5), deferredJsonMerger.merged)
    assertEquals(setOf(
        DeferredFragmentIdentifier(path = listOf("computers", 0), label = "query:Query1:0"),
        DeferredFragmentIdentifier(path = listOf("computers", 1), label = "query:Query1:0"),
        DeferredFragmentIdentifier(path = listOf("computers", 1, "screen"), label = "fragment:ComputerFields:0"),
    ), deferredJsonMerger.mergedFragmentIds)
  }
}

private fun String.buffer() = Buffer().writeUtf8(this)

@Suppress("UNCHECKED_CAST")
private fun jsonToMap(json: String): Map<String, Any?> = BufferedSourceJsonReader(json.buffer()).readAny() as Map<String, Any?>

