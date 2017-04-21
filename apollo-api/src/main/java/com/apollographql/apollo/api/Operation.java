package com.apollographql.apollo.api;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * Abstraction for a GraphQL operation (mutation or query)
 */
public interface Operation<D extends Operation.Data, T, V extends Operation.Variables> {
  /**
   * Returns the raw graphQL query represented by this operation
   */
  String queryDocument();

  /**
   * Returns the Variables associated with this operation.
   */
  V variables();

  /**
   * Returns a mapper that maps the server response back to the an object of type D.
   */
  ResponseFieldMapper<D> responseFieldMapper();

  /**
   * Converts the data returned by server to an object of type T
   */
  T wrapData(D data);

  /**
   * Abstraction for data returned by the server.
   */
  interface Data {
  }

  /**
   * Abstraction for the variables which are a part of the GraphQL query. i.e. if your graphQL query is like:
   * <pre>{@code
   *  query FeedQuery($type: FeedType!, $limit: Int!) {
   * feedEntries: feed(type: $type, limit: $limit) {
   * id
   * repository {
   * ...RepositoryFragment
   * }
   * postedBy {
   * login
   * }
   * }
   * }
   * }
   * </pre>
   * then variables are basically abstractions for $type & $limit.
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
