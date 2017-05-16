package com.apollographql.apollo;

import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.cache.CacheHeaders;

import javax.annotation.Nonnull;

/**
 * A call prepared to execute GraphQL mutation operation.
 */
public interface ApolloMutationCall<T> extends ApolloCall<T> {

  /**
   * <p>Sets a list of GraphQL query names to be re-fetched once this mutation completed.</p>
   * In order to get queries to be re-fetched you must obtain {@link ApolloQueryWatcher} from provided list of
   * queries before running this mutation.
   *
   * @param operationNames array of {@link OperationName} query names to be re-fetched
   * @return {@link ApolloMutationCall} that will trigger re-fetching provided queries
   */
  @Nonnull ApolloMutationCall<T> refetchQueries(@Nonnull OperationName... operationNames);

  @Nonnull @Override ApolloMutationCall<T> cacheHeaders(@Nonnull CacheHeaders cacheHeaders);

  @Nonnull @Override ApolloMutationCall<T> clone();

  /**
   * Factory for creating {@link ApolloMutationCall} calls.
   */
  interface Factory {
    /**
     * Creates and prepares a new {@link ApolloMutationCall} call.
     *
     * @param mutation the mutation which needs to be performed
     * @return prepared {@link ApolloMutationCall} call to be executed at some point in the future
     */
    <D extends Mutation.Data, T, V extends Mutation.Variables> ApolloMutationCall<T> mutate(
        @Nonnull Mutation<D, T, V> mutation);
  }
}
