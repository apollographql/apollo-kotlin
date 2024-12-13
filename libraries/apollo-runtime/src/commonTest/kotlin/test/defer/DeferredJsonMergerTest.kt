package test.defer

import com.apollographql.apollo.api.DeferredFragmentIdentifier
import com.apollographql.apollo.api.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.json.readAny
import com.apollographql.apollo.internal.DeferredJsonMerger
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun String.buffer() = Buffer().writeUtf8(this)

@Suppress("UNCHECKED_CAST")
private fun jsonToMap(json: String): Map<String, Any?> = BufferedSourceJsonReader(json.buffer()).readAny() as Map<String, Any?>

class DeferredJsonMergerTest {
  @Test
  fun mergeJsonSingleIncrementalItem() {
    val deferredJsonMerger = DeferredJsonMerger()

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
        "pending": [
          {
            "id": "0",
            "path": [
              "computers",
              0
            ],
            "label": "query:Query1:0"
          }
        ],
        "hasNext": true
      }
    """
    //language=JSON
    val mergedPayloads_1 = """
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
        }
      }
    """
    deferredJsonMerger.merge(payload1.buffer())
    assertEquals(jsonToMap(mergedPayloads_1), deferredJsonMerger.merged)
    assertEquals(setOf(), deferredJsonMerger.mergedFragmentIds)

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
            "id": "0"
          }
        ],
        "completed": [
          {
            "id": "0"
          }
        ],
        "pending": [
          {
            "id": "1",
            "path": [
              "computers",
              1
            ],
            "label": "query:Query1:0"
          }
        ],
        "extensions": {
          "duration": {
            "amount": 100,
            "unit": "ms"
          }
        },
        "hasNext": true
      }
    """
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
    assertEquals(
        setOf(
            DeferredFragmentIdentifier(path = listOf("computers", 0), label = "query:Query1:0")
        ),
        deferredJsonMerger.mergedFragmentIds
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
            "id": "1"
          }
        ],
        "completed": [
          {
            "id": "1"
          }
        ],
        "pending": [
          {
            "id": "2",
            "path": [
              "computers",
              0,
              "screen"
            ],
            "label": "fragment:ComputerFields:0"
          }
        ],
        "extensions": {
          "duration": {
            "amount": 25,
            "unit": "ms"
          }
        },
        "hasNext": true
      }
    """
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
    assertEquals(
        setOf(
            DeferredFragmentIdentifier(path = listOf("computers", 0), label = "query:Query1:0"),
            DeferredFragmentIdentifier(path = listOf("computers", 1), label = "query:Query1:0"),
        ),
        deferredJsonMerger.mergedFragmentIds
    )

    //language=JSON
    val payload4 = """
      {
        "completed": [
          {
            "id": "2",
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
        ],
        "pending": [
          {
            "id": "3",
            "path": [
              "computers",
              1,
              "screen"
            ],
            "label": "fragment:ComputerFields:0"
          }
        ],
        "hasNext": true
      }
    """
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
        "extensions": {
          "duration": {
            "amount": 25,
            "unit": "ms"
          }
        }
      }
    """
    deferredJsonMerger.merge(payload4.buffer())
    assertEquals(jsonToMap(mergedPayloads_1_2_3_4), deferredJsonMerger.merged)
    assertEquals(
        setOf(
            DeferredFragmentIdentifier(path = listOf("computers", 0), label = "query:Query1:0"),
            DeferredFragmentIdentifier(path = listOf("computers", 1), label = "query:Query1:0"),
        ),
        deferredJsonMerger.mergedFragmentIds
    )

    //language=JSON
    val payload5 = """
      {
        "incremental": [
          {
            "data": {
              "isColor": false
            },
            "id": "3",
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
        ],
        "completed": [
          {
            "id": "3"
          }
        ],
        "extensions": {
          "value": 42,
          "duration": {
            "amount": 130,
            "unit": "ms"
          }
        },
        "hasNext": false
      }
    """
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
        ],
        "extensions": {
          "value": 42,
          "duration": {
            "amount": 130,
            "unit": "ms"
          }
        }
      }
    """
    deferredJsonMerger.merge(payload5.buffer())
    assertEquals(jsonToMap(mergedPayloads_1_2_3_4_5), deferredJsonMerger.merged)
    assertEquals(
        setOf(
            DeferredFragmentIdentifier(path = listOf("computers", 0), label = "query:Query1:0"),
            DeferredFragmentIdentifier(path = listOf("computers", 1), label = "query:Query1:0"),
            DeferredFragmentIdentifier(path = listOf("computers", 1, "screen"), label = "fragment:ComputerFields:0"),
        ),
        deferredJsonMerger.mergedFragmentIds
    )
  }

  @Test
  fun mergeJsonMultipleIncrementalItems() {
    val deferredJsonMerger = DeferredJsonMerger()

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
        "pending": [
          {
            "id": "0",
            "path": [
              "computers",
              0
            ],
            "label": "query:Query1:0"
          },
          {
            "id": "1",
            "path": [
              "computers",
              1
            ],
            "label": "query:Query1:0"
          }
        ],
        "hasNext": true
      }
    """
    //language=JSON
    val mergedPayloads_1 = """
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
        }
      }
    """
    deferredJsonMerger.merge(payload1.buffer())
    assertEquals(jsonToMap(mergedPayloads_1), deferredJsonMerger.merged)
    assertEquals(setOf(), deferredJsonMerger.mergedFragmentIds)

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
            "id": "0"
          },
          {
            "data": {
              "cpu": "486",
              "year": 1996,
              "screen": {
                "resolution": "640x480"
              }
            },
            "id": "1"
          }
        ],
        "completed": [
          {
            "id": "0"
          },
          {
            "id": "1"
          }
        ],
        "pending": [
          {
            "id": "2",
            "path": [
              "computers",
              0,
              "screen"
            ],
            "label": "fragment:ComputerFields:0"
          },
          {
            "id": "3",
            "path": [
              "computers",
              1,
              "screen"
            ],
            "label": "fragment:ComputerFields:0"
          }
        ],
        "extensions": {
          "duration": {
            "amount": 100,
            "unit": "ms"
          }
        },
        "hasNext": true
      }
    """
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
        "extensions": {
          "duration": {
            "amount": 100,
            "unit": "ms"
          }
        }
      }
    """
    deferredJsonMerger.merge(payload2_3.buffer())
    assertEquals(jsonToMap(mergedPayloads_1_2_3), deferredJsonMerger.merged)
    assertEquals(
        setOf(
            DeferredFragmentIdentifier(path = listOf("computers", 0), label = "query:Query1:0"),
            DeferredFragmentIdentifier(path = listOf("computers", 1), label = "query:Query1:0"),
        ),
        deferredJsonMerger.mergedFragmentIds
    )

    //language=JSON
    val payload4_5 = """
      {
        "incremental": [
          {
            "data": {
              "isColor": false
            },
            "id": "3",
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
        ],
        "completed": [
          {
            "id": "2",
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
          },
          {
            "id": "3"
          }
        ],
        "extensions": {
          "value": 42,
          "duration": {
            "amount": 130,
            "unit": "ms"
          }
        },
        "hasNext": false
      }
    """
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
        "errors": [
          {
            "message": "Another error",
            "locations": [
              {
                "line": 1,
                "column": 1
              }
            ]
          },
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
        "extensions": {
          "value": 42,
          "duration": {
            "amount": 130,
            "unit": "ms"
          }
        }
      }
    """
    deferredJsonMerger.merge(payload4_5.buffer())
    assertEquals(jsonToMap(mergedPayloads_1_2_3_4_5), deferredJsonMerger.merged)
    assertEquals(
        setOf(
            DeferredFragmentIdentifier(path = listOf("computers", 0), label = "query:Query1:0"),
            DeferredFragmentIdentifier(path = listOf("computers", 1), label = "query:Query1:0"),
            DeferredFragmentIdentifier(path = listOf("computers", 1, "screen"), label = "fragment:ComputerFields:0"),
        ),
        deferredJsonMerger.mergedFragmentIds
    )
  }

  @Test
  fun emptyPayloads() {
    val deferredJsonMerger = DeferredJsonMerger()

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
        "pending": [
          {
            "id": "0",
            "path": [
              "computers",
              0
            ],
            "label": "query:Query1:0"
          },
          {
            "id": "1",
            "path": [
              "computers",
              1
            ],
            "label": "query:Query1:0"
          }
        ],
        "hasNext": true
      }
    """
    deferredJsonMerger.merge(payload1.buffer())
    assertFalse(deferredJsonMerger.isEmptyPayload)

    //language=JSON
    val payload2 = """
      {
        "hasNext": true
      }
    """
    deferredJsonMerger.merge(payload2.buffer())
    assertTrue(deferredJsonMerger.isEmptyPayload)
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
            "id": "0"
          }
        ],
        "hasNext": true
      }
    """
    deferredJsonMerger.merge(payload3.buffer())
    assertFalse(deferredJsonMerger.isEmptyPayload)

    //language=JSON
    val payload4 = """
      {
        "hasNext": false
      }
    """
    deferredJsonMerger.merge(payload4.buffer())
    assertTrue(deferredJsonMerger.isEmptyPayload)
  }

  /**
   * Example A from https://github.com/graphql/defer-stream-wg/discussions/69 (Nov 1 2024 version)
   */
  @Test
  fun june2023ExampleA() {
    val deferredJsonMerger = DeferredJsonMerger()
    //language=JSON
    val payload1 = """
      {
        "data": {
          "f2": {
            "a": "a",
            "b": "b",
            "c": {
              "d": "d",
              "e": "e",
              "f": { "h": "h", "i": "i" }
            }
          }
        },
        "pending": [{ "path": [], "id": "0" }],
        "hasNext": true
      }    
    """
    //language=JSON
    val mergedPayloads_1 = """
      {
        "data": {
          "f2": {
            "a": "a",
            "b": "b",
            "c": {
              "d": "d",
              "e": "e",
              "f": { "h": "h", "i": "i" }
            }
          }
        }
      }
    """
    deferredJsonMerger.merge(payload1.buffer())
    assertEquals(jsonToMap(mergedPayloads_1), deferredJsonMerger.merged)
    assertEquals(setOf(), deferredJsonMerger.mergedFragmentIds)

    //language=JSON
    val payload2 = """
      {
        "incremental": [
          { "id": "0", "data": { "MyFragment": "Query" } },
          { "id": "0", "subPath": ["f2", "c", "f"], "data": { "j": "j" } }
        ],
        "completed": [{ "id": "0" }],
        "hasNext": false
      }
    """
    //language=JSON
    val mergedPayloads_1_2 = """
      {
        "data": {
          "f2": {
            "a": "a",
            "b": "b",
            "c": {
              "d": "d",
              "e": "e",
              "f": { "h": "h", "i": "i", "j": "j" }
            }
          },
          "MyFragment": "Query"
        }
      }
    """
    deferredJsonMerger.merge(payload2.buffer())
    assertEquals(jsonToMap(mergedPayloads_1_2), deferredJsonMerger.merged)
    assertEquals(
        setOf(
            DeferredFragmentIdentifier(path = listOf(), label = null),
        ),
        deferredJsonMerger.mergedFragmentIds
    )
  }

  /**
   * Example A2 from https://github.com/graphql/defer-stream-wg/discussions/69 (Nov 1 2024 version)
   */
  @Test
  fun june2023ExampleA2() {
    val deferredJsonMerger = DeferredJsonMerger()
    //language=JSON
    val payload1 = """
      {
        "data": {"f2": {"a": "A", "b": "B", "c": {
          "d": "D", "e": "E", "f": {
            "h": "H", "i": "I"
          }
        }}},
        "pending": [{"id": "0", "path": [], "label": "D1"}],
        "hasNext": true
      }
    """
    //language=JSON
    val mergedPayloads_1 = """
      {
        "data": {
          "f2": {
            "a": "A",
            "b": "B",
            "c": {
              "d": "D",
              "e": "E",
              "f": {
                "h": "H",
                "i": "I"
              }
            }
          }
        }
      }
    """
    deferredJsonMerger.merge(payload1.buffer())
    assertEquals(jsonToMap(mergedPayloads_1), deferredJsonMerger.merged)
    assertEquals(setOf(), deferredJsonMerger.mergedFragmentIds)

    //language=JSON
    val payload2 = """
      {
        "incremental": [
          {"id": "0", "subPath": ["f2", "c", "f"], "data": {"j": "J", "k": "K"}}
        ],
        "pending": [{"id": "1", "path": ["f2", "c", "f"], "label": "D2"}],
        "completed": [
          {"id": "0"}
        ],
        "hasNext": true
      }
    """
    //language=JSON
    val mergedPayloads_1_2 = """
      {
        "data": {
          "f2": {
            "a": "A",
            "b": "B",
            "c": {
              "d": "D",
              "e": "E",
              "f": {
                "h": "H",
                "i": "I",
                "j": "J",
                "k": "K"
              }
            }
          }
        }
      }
    """
    deferredJsonMerger.merge(payload2.buffer())
    assertEquals(jsonToMap(mergedPayloads_1_2), deferredJsonMerger.merged)
    assertEquals(
        setOf(
            DeferredFragmentIdentifier(path = listOf(), label = "D1"),
        ),
        deferredJsonMerger.mergedFragmentIds
    )

    //language=JSON
    val payload3 = """
      {
        "incremental": [
          {"id": "1", "data": {"l": "L", "m": "M"}}
        ],
        "completed": [
          {"id": "1"}
        ],
        "hasNext": false
      }
    """

    //language=JSON
    val mergedPayloads_1_2_3 = """
      {
        "data": {
          "f2": {
            "a": "A",
            "b": "B",
            "c": {
              "d": "D",
              "e": "E",
              "f": {
                "h": "H",
                "i": "I",
                "j": "J",
                "k": "K",
                "l": "L",
                "m": "M"
              }
            }
          }
        }
      }
    """
    deferredJsonMerger.merge(payload3.buffer())
    assertEquals(jsonToMap(mergedPayloads_1_2_3), deferredJsonMerger.merged)
    assertEquals(
        setOf(
            DeferredFragmentIdentifier(path = listOf(), label = "D1"),
            DeferredFragmentIdentifier(path = listOf("f2", "c", "f"), label = "D2"),
        ),
        deferredJsonMerger.mergedFragmentIds
    )
  }

  /**
   * Example B1 from https://github.com/graphql/defer-stream-wg/discussions/69 (Nov 1 2024 version)
   */
  @Test
  fun june2023ExampleB1() {
    val deferredJsonMerger = DeferredJsonMerger()
    //language=JSON
    val payload1 = """
      {
        "data": {
          "a": { "b": { "c": { "d": "d" } } }
        },
        "pending": [
          { "path": [], "id": "0", "label": "Blue" },
          { "path": ["a", "b"], "id": "1", "label": "Red" }
        ],
        "hasNext": true
      }
    """

    //language=JSON
    val mergedPayloads_1 = """
      {
        "data": {
          "a": {
            "b": {
              "c": {
                "d": "d"
              }
            }
          }
        }
      }
    """
    deferredJsonMerger.merge(payload1.buffer())
    assertEquals(jsonToMap(mergedPayloads_1), deferredJsonMerger.merged)
    assertEquals(setOf(), deferredJsonMerger.mergedFragmentIds)

    //language=JSON
    val payload2 = """
      {
        "incremental": [
          { "id": "1", "data": { "potentiallySlowFieldA": "potentiallySlowFieldA" } },
          { "id": "1", "data": { "e": { "f": "f" } } }
        ],
        "completed": [{ "id": "1" }],
        "hasNext": true
      }
    """
    //language=JSON
    val mergedPayloads_1_2 = """
      {
        "data": {
          "a": {
            "b": {
              "c": {
                "d": "d"
              },
              "e": {
                "f": "f"
              },
              "potentiallySlowFieldA": "potentiallySlowFieldA"
            }
          }
        }
      }
    """
    deferredJsonMerger.merge(payload2.buffer())
    assertEquals(jsonToMap(mergedPayloads_1_2), deferredJsonMerger.merged)
    assertEquals(
        setOf(
            DeferredFragmentIdentifier(path = listOf("a", "b"), label = "Red"),
        ),
        deferredJsonMerger.mergedFragmentIds
    )

    //language=JSON
    val payload3 = """
      {
        "incremental": [
          { "id": "0", "data": { "g": { "h": "h" }, "potentiallySlowFieldB": "potentiallySlowFieldB" } }
        ],
        "completed": [{ "id": "0" }],
        "hasNext": false
      }
    """
    //language=JSON
    val mergedPayloads_1_2_3 = """
      {
        "data": {
          "a": {
            "b": {
              "c": {
                "d": "d"
              },
              "e": {
                "f": "f"
              },
              "potentiallySlowFieldA": "potentiallySlowFieldA"
            }
          },
          "g": {
            "h": "h"
          },
          "potentiallySlowFieldB": "potentiallySlowFieldB"
        }
      }
    """
    deferredJsonMerger.merge(payload3.buffer())
    assertEquals(jsonToMap(mergedPayloads_1_2_3), deferredJsonMerger.merged)
    assertEquals(
        setOf(
            DeferredFragmentIdentifier(path = listOf(), label = "Blue"),
            DeferredFragmentIdentifier(path = listOf("a", "b"), label = "Red"),
        ),
        deferredJsonMerger.mergedFragmentIds
    )
  }

  /**
   * Example B2 from https://github.com/graphql/defer-stream-wg/discussions/69 (Nov 1 2024 version)
   */
  @Test
  fun june2023ExampleB2() {
    val deferredJsonMerger = DeferredJsonMerger()
    //language=JSON
    val payload1 = """
      {
        "data": {
          "a": { "b": { "c": { "d": "d" } } }
        },
        "pending": [
          { "path": [], "id": "0", "label": "Blue" },
          { "path": ["a", "b"], "id": "1", "label": "Red" }
        ],
        "hasNext": true
      }
    """

    //language=JSON
    val mergedPayloads_1 = """
      {
        "data": {
          "a": {
            "b": {
              "c": {
                "d": "d"
              }
            }
          }
        }
      }
    """
    deferredJsonMerger.merge(payload1.buffer())
    assertEquals(jsonToMap(mergedPayloads_1), deferredJsonMerger.merged)
    assertEquals(setOf(), deferredJsonMerger.mergedFragmentIds)

    //language=JSON
    val payload2 = """
      {
        "incremental": [
          { "id": "0", "data": { "g": { "h": "h" }, "potentiallySlowFieldB": "potentiallySlowFieldB" } },
          { "id": "1", "data": { "e": { "f": "f" } } }
        ],
        "completed": [{ "id": "0" }],
        "hasNext": true
      }
    """
    //language=JSON
    val mergedPayloads_1_2 = """
      {
        "data": {
          "a": {
            "b": {
              "c": {
                "d": "d"
              },
              "e": {
                "f": "f"
              }
            }
          },
          "g": {
            "h": "h"
          },
          "potentiallySlowFieldB": "potentiallySlowFieldB"
        }
      }
    """
    deferredJsonMerger.merge(payload2.buffer())
    assertEquals(jsonToMap(mergedPayloads_1_2), deferredJsonMerger.merged)
    assertEquals(
        setOf(
            DeferredFragmentIdentifier(path = listOf(), label = "Blue"),
        ),
        deferredJsonMerger.mergedFragmentIds
    )

    //language=JSON
    val payload3 = """
      {
        "incremental": [
          { "id": "1", "data": { "potentiallySlowFieldA": "potentiallySlowFieldA" } }
        ],
       "completed": [{ "id": "1" }],
        "hasNext": false
      } 
    """
    //language=JSON
    val mergedPayloads_1_2_3 = """
      {
        "data": {
          "a": {
            "b": {
              "c": {
                "d": "d"
              },
              "e": {
                "f": "f"
              },
              "potentiallySlowFieldA": "potentiallySlowFieldA"
            }
          },
          "g": {
            "h": "h"
          },
          "potentiallySlowFieldB": "potentiallySlowFieldB"
        }
      }
    """
    deferredJsonMerger.merge(payload3.buffer())
    assertEquals(jsonToMap(mergedPayloads_1_2_3), deferredJsonMerger.merged)
    assertEquals(
        setOf(
            DeferredFragmentIdentifier(path = listOf(), label = "Blue"),
            DeferredFragmentIdentifier(path = listOf("a", "b"), label = "Red"),
        ),
        deferredJsonMerger.mergedFragmentIds
    )
  }

  /**
   * Example D from https://github.com/graphql/defer-stream-wg/discussions/69 (Nov 1 2024 version)
   */
  @Test
  fun june2023ExampleD() {
    val deferredJsonMerger = DeferredJsonMerger()
    //language=JSON
    val payload1 = """
      {
        "data": { "me": {} },
        "pending": [
          { "path": [], "id": "0" },
          { "path": ["me"], "id": "1" }
        ],
        "hasNext": true
      }
    """
    //language=JSON
    val mergedPayloads_1 = """
      {
        "data": {
          "me": {}
        }
      }
    """
    deferredJsonMerger.merge(payload1.buffer())
    assertEquals(jsonToMap(mergedPayloads_1), deferredJsonMerger.merged)
    assertEquals(setOf(), deferredJsonMerger.mergedFragmentIds)

    //language=JSON
    val payload2 = """
      {
        "incremental": [
          {
            "id": "1",
            "data": { "list": [{ "item": {} }, { "item": {} }, { "item": {} }] }
          },
          { "id": "1", "subPath": ["list", 0, "item"], "data": { "id": "1" } },
          { "id": "1", "subPath": ["list", 1, "item"], "data": { "id": "2" } },
          { "id": "1", "subPath": ["list", 2, "item"], "data": { "id": "3" } }
        ],
        "completed": [{ "id": "1" }],
        "hasNext": true
      }
    """
    //language=JSON
    val mergedPayloads_1_2 = """
      {
        "data": {
          "me": {
            "list": [
              { "item": { "id": "1" } },
              { "item": { "id": "2" } },
              { "item": { "id": "3" } }
            ]
          }
        }
      }
    """
    deferredJsonMerger.merge(payload2.buffer())
    assertEquals(jsonToMap(mergedPayloads_1_2), deferredJsonMerger.merged)
    assertEquals(
        setOf(
            DeferredFragmentIdentifier(path = listOf("me"), label = null),
        ),
        deferredJsonMerger.mergedFragmentIds
    )

    //language=JSON
    val payload3 = """
      {
        "incremental": [
          { "id": "0", "subPath": ["me", "list", 0, "item"], "data": { "value": "Foo" } },
          { "id": "0", "subPath": ["me", "list", 1, "item"], "data": { "value": "Bar" } },
          { "id": "0", "subPath": ["me", "list", 2, "item"], "data": { "value": "Baz" } }
        ],
        "completed": [{ "id": "0" }],
        "hasNext": false
      }
    """
    //language=JSON
    val mergedPayloads_1_2_3 = """
      {
        "data": {
          "me": {
            "list": [
              { "item": { "id": "1", "value": "Foo" } },
              { "item": { "id": "2", "value": "Bar" } },
              { "item": { "id": "3", "value": "Baz" } }
            ]
          }
        }
      }
    """
    deferredJsonMerger.merge(payload3.buffer())
    assertEquals(jsonToMap(mergedPayloads_1_2_3), deferredJsonMerger.merged)
    assertEquals(
        setOf(
            DeferredFragmentIdentifier(path = listOf("me"), label = null),
            DeferredFragmentIdentifier(path = listOf(), label = null),
        ),
        deferredJsonMerger.mergedFragmentIds
    )
  }
}
