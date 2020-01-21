@file:Suppress("NOTHING_TO_INLINE")
@file:JvmMultifileClass
@file:JvmName("KotlinExtensions")

package com.apollographql.apollo.api

import com.apollographql.apollo.response.ScalarTypeAdapters

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
 * @param scalarTypeAdapters configured instance of custom GraphQL scalar type adapters.
 */
@JvmSynthetic
inline fun Operation.Data.toJson(indent: String = "", scalarTypeAdapters: ScalarTypeAdapters = ScalarTypeAdapters.DEFAULT): String =
    OperationDataJsonSerializer.serialize(this, indent, scalarTypeAdapters)
