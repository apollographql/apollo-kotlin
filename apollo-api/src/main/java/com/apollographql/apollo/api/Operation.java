package com.apollographql.apollo.api;

import com.apollographql.apollo.api.internal.json.InputFieldJsonWriter;
import com.apollographql.apollo.api.internal.json.JsonWriter;
import okio.Buffer;
import okio.BufferedSource;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * Represents a GraphQL operation (mutation or query).
 */
public interface Operation<D extends Operation.Data, T, V extends Operation.Variables> {
  /**
   * Returns the raw GraphQL operation String.
   */
  String queryDocument();

  /**
   * Returns the variables associated with this GraphQL operation.
   */
  V variables();

  /**
   * Returns a mapper that maps the server response data to generated model class {@link D}.
   */
  ResponseFieldMapper<D> responseFieldMapper();

  /**
   * Wraps the generated response data class {@link D} with another class. For example, a use case for this would be to
   * wrap the generated response data class in an Optional i.e. Optional.fromNullable(data).
   */
  T wrapData(D data);

  /**
   * Returns GraphQL operation name.
   *
   * @return {@link OperationName} operation name
   */
  @NotNull OperationName name();

  /**
   * Returns a unique identifier for this operation.
   *
   * @return operation identifier.
   */
  @NotNull String operationId();

  /**
   * Parses provided GraphQL operation raw response
   *
   * @param source for operation raw response to parse
   * @param scalarTypeAdapters configured instance of custom GraphQL scalar type adapters
   * @return parsed GraphQL operation {@link Response}
   */
  @NotNull Response<T> parse(@NotNull BufferedSource source, @NotNull ScalarTypeAdapters scalarTypeAdapters) throws IOException;

  /**
   * Parses provided GraphQL operation raw response
   *
   * @param source for operation raw response to parse
   * @return parsed GraphQL operation {@link Response}
   */
  @NotNull Response<T> parse(@NotNull BufferedSource source) throws IOException;

  /**
   * Abstraction for data returned by the server in response to this operation.
   */
  interface Data {

    /**
     * Returns marshaller to serialize operation data
     *
     * @return {@link ResponseFieldMarshaller} to serialize operation data
     */
    ResponseFieldMarshaller marshaller();
  }

  /**
   * Abstraction for the variables which are a part of the GraphQL operation. For example, for the following GraphQL
   * operation, Variables represents values for GraphQL '$type' and '$limit' variables:
   * <pre>{@code
   *      query FeedQuery($type: FeedType!, $limit: Int!) {
   *          feedEntries: feed(type: $type, limit: $limit) {
   *          id
   *          repository {
   *              ...RepositoryFragment
   *          }
   *          postedBy {
   *            login
   *          }
   *      }
   *    }
   * }
   * </pre>
   */
  class Variables {
    protected Variables() {
    }

    @NotNull public Map<String, Object> valueMap() {
      return Collections.emptyMap();
    }

    @NotNull public InputFieldMarshaller marshaller() {
      return new InputFieldMarshaller() {
        @Override public void marshal(InputFieldWriter writer) {
        }
      };
    }

    /**
     * Serializes variables as JSON string to be sent to the GraphQL server.
     *
     * @return JSON string
     * @throws IOException
     */
    public final String marshal() throws IOException {
      return marshal(ScalarTypeAdapters.DEFAULT);
    }

    /**
     * Serializes variables as JSON string to be sent to the GraphQL server.
     *
     * @param scalarTypeAdapters adapters for custom GraphQL scalar types
     * @return JSON string
     * @throws IOException
     */
    public final String marshal(@NotNull final ScalarTypeAdapters scalarTypeAdapters) throws IOException {
      final Buffer buffer = new Buffer();
      final JsonWriter jsonWriter = JsonWriter.of(buffer);
      jsonWriter.setSerializeNulls(true);
      jsonWriter.beginObject();
      marshaller().marshal(new InputFieldJsonWriter(jsonWriter, scalarTypeAdapters));
      jsonWriter.endObject();
      jsonWriter.close();
      return buffer.readUtf8();
    }
  }

  Variables EMPTY_VARIABLES = new Variables();
}
