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
    """
    deferredJsonMerger.merge(payload2.buffer())
      assertEquals(jsonToMap(mergedPayloads_1_2), deferredJsonMerger.merged)
      assertEquals(setOf(
          DeferredFragmentIdentifier(path = listOf("computers", 0), label = "query:Query1:0"),
      ), deferredJsonMerger.mergedFragmentIds
      )


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
    """
    deferredJsonMerger.merge(payload3.buffer())
      assertEquals(jsonToMap(mergedPayloads_1_2_3), deferredJsonMerger.merged)
      assertEquals(setOf(
          DeferredFragmentIdentifier(path = listOf("computers", 0), label = "query:Query1:0"),
          DeferredFragmentIdentifier(path = listOf("computers", 1), label = "query:Query1:0"),
      ), deferredJsonMerger.mergedFragmentIds
      )


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
      ), deferredJsonMerger.mergedFragmentIds
      )


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
    """
        deferredJsonMerger.merge(payload5.buffer())
      assertEquals(jsonToMap(mergedPayloads_1_2_3_4_5), deferredJsonMerger.merged)
      assertEquals(setOf(
          DeferredFragmentIdentifier(path = listOf("computers", 0), label = "query:Query1:0"),
          DeferredFragmentIdentifier(path = listOf("computers", 1), label = "query:Query1:0"),
          DeferredFragmentIdentifier(path = listOf("computers", 1, "screen"), label = "fragment:ComputerFields:0"),
      ), deferredJsonMerger.mergedFragmentIds
      )
    }

    @Test
    fun mergeJsonMultipleIncrementalItems() {
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
    """
        deferredJsonMerger.merge(payload2_3.buffer())
      assertEquals(jsonToMap(mergedPayloads_1_2_3), deferredJsonMerger.merged)
      assertEquals(setOf(
          DeferredFragmentIdentifier(path = listOf("computers", 0), label = "query:Query1:0"),
          DeferredFragmentIdentifier(path = listOf("computers", 1), label = "query:Query1:0"),
      ), deferredJsonMerger.mergedFragmentIds
      )


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
    """
        deferredJsonMerger.merge(payload4_5.buffer())
      assertEquals(jsonToMap(mergedPayloads_1_2_3_4_5), deferredJsonMerger.merged)
      assertEquals(setOf(
          DeferredFragmentIdentifier(path = listOf("computers", 0), label = "query:Query1:0"),
          DeferredFragmentIdentifier(path = listOf("computers", 1), label = "query:Query1:0"),
          DeferredFragmentIdentifier(path = listOf("computers", 1, "screen"), label = "fragment:ComputerFields:0"),
      ), deferredJsonMerger.mergedFragmentIds
      )
    }

    @Test
    fun emptyPayloads() {
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
      assertFalse(deferredJsonMerger.isEmptyPayload)

        val payload2 = """
      {
        "hasNext": true
      }
    """
        deferredJsonMerger.merge(payload2.buffer())
      assertTrue(deferredJsonMerger.isEmptyPayload)

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
    """
        deferredJsonMerger.merge(payload3.buffer())
      assertFalse(deferredJsonMerger.isEmptyPayload)

        val payload4 = """
      {
        "hasNext": false
      }
    """
        deferredJsonMerger.merge(payload4.buffer())
      assertTrue(deferredJsonMerger.isEmptyPayload)
    }
}