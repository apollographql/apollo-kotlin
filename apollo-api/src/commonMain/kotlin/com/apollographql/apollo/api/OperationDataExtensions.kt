@file:JvmName("OperationDataJsonSerializer")

package com.apollographql.apollo.api

import com.apollographql.apollo.api.CustomScalarAdapters.Companion.DEFAULT
import com.apollographql.apollo.api.internal.SimpleResponseWriter
import okio.IOException
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads

/**
 * Serializes GraphQL operation response data into its equivalent Json representation.
 * For example:
 * <pre>{@code
 *    {
 *      "data": {
 *        "allPlanets": {
 *          "__typename": "PlanetsConnection",
 *          "planets": [
 *            {
 *              "__typename": "Planet",
 *              "name": "Tatooine",
 *              "surfaceWater": 1.0
 *            }
 *          ]
 *        }
 *      }
 *    }
 * }</pre>
 *
 * @param indent the indentation string to be repeated for each level of indentation in the encoded document. Must be a string
 * containing only whitespace. If [indent] is an empty String the encoded document will be compact. Otherwise the encoded
 * document will be more human-readable.
 * @param customScalarAdapters configured instance of custom GraphQL scalar type adapters. Default adapters are used if this
 * param is not provided.
 */
@JvmName("serialize")
@JvmOverloads
fun Operation.Data.toJson(indent: String = "", customScalarAdapters: CustomScalarAdapters = DEFAULT): String {
  return try {
    SimpleResponseWriter(customScalarAdapters).let { writer ->
      marshaller().marshal(writer)
      writer.toJson(indent)
    }
  } catch (e: IOException) {
    throw IllegalStateException(e)
  }
}
