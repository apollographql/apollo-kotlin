package com.apollographql.apollo.api;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * Operation represents an abstraction for a GraphQL operation (mutation or query).
 */
public interface Operation<D extends Operation.Data, T, V extends Operation.Variables> {
  /**
   * Returns the raw graphQL operation string.
   */
  String queryDocument();

  /**
   * Returns the Variable object associated with this GraphQL operation.
   */
  V variables();

  /**
   * Returns a mapper that maps the server response back to an object of type D.
   */
  ResponseFieldMapper<D> responseFieldMapper();

  /**
   * Converts the object representing the server response to another type.
   */
  T wrapData(D data);

  /**
   * Abstraction for data returned by the server in response to this operation.
   */
  interface Data {
  }

  /**
   * Abstraction for the variables which are a part of the GraphQL operation. For example, for the following GraphQL
   * operation, Variables represents an abstraction for GraphQL variables '$type' and '$limit' their values:
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

    @Nonnull protected Map<String, Object> valueMap() {
      return Collections.emptyMap();
    }
  }

  Variables EMPTY_VARIABLES = new Variables();
}
