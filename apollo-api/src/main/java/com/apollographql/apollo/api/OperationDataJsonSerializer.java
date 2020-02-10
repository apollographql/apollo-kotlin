package com.apollographql.apollo.api;

import com.apollographql.apollo.api.internal.SimpleResponseWriter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * Utility class to serialize GraphQL {@link Operation.Data} response into Json representation.
 */
public final class OperationDataJsonSerializer {

  private OperationDataJsonSerializer() {
  }

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
   * @param data GraphQL operation data to be serialized
   * @param indent the indentation string to be repeated for each level of indentation in the encoded document. Must be a string
   * containing only whitespace,
   * @return json representation of GraphQL operation response
   */
  public static String serialize(@NotNull Operation.Data data, @NotNull String indent) {
    return serialize(data, indent, ScalarTypeAdapters.DEFAULT);
  }

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
   * @param data GraphQL operation data to be serialized
   * @param indent the indentation string to be repeated for each level of indentation in the encoded document. Must be a string
   * containing only whitespace,
   * @param scalarTypeAdapters configured instance of custom GraphQL scalar type adapters
   * @return json representation of GraphQL operation response
   */
  public static String serialize(@NotNull Operation.Data data, @NotNull String indent, @NotNull ScalarTypeAdapters scalarTypeAdapters) {
    checkNotNull(data, "data == null");
    checkNotNull(indent, "intent == null");
    checkNotNull(scalarTypeAdapters, "scalarTypeAdapters == null");
    try {
      final SimpleResponseWriter responseWriter = new SimpleResponseWriter(scalarTypeAdapters);
      data.marshaller().marshal(responseWriter);
      return responseWriter.toJson(indent);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
