package com.apollographql.apollo;

import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.cache.CacheHeaders;

import org.jetbrains.annotations.NotNull;

/**
 * A call prepared to execute GraphQL mutation operation.
 */
public interface ApolloMutationCall<T> extends ApolloCall<T> {

  /**
   * <p>Sets a list of {@link ApolloQueryWatcher} query names to be re-fetched once this mutation completed.</p>
   *
   * @param operationNames array of {@link OperationName} query names to be re-fetched
   * @return {@link ApolloMutationCall} that will trigger re-fetching provided queries
   */
  @NotNull ApolloMutationCall<T> refetchQueries(@NotNull OperationName... operationNames);

  /**
   * <p>Sets a list of {@link Query} to be re-fetched once this mutation completed.</p>
   *
   * @param queries array of {@link Query} to be re-fetched
   * @return {@link ApolloMutationCall} that will trigger re-fetching provided queries
   */
  @NotNull ApolloMutationCall<T> refetchQueries(@NotNull Query... queries);

  @NotNull @Override ApolloMutationCall<T> cacheHeaders(@NotNull CacheHeaders cacheHeaders);

  @NotNull @Override ApolloMutationCall<T> clone();

  /**
   * Factory for creating {@link ApolloMutationCall} calls.
   */
  interface Factory {
    /**
     * Creates and prepares a new {@link ApolloMutationCall} call.
     *
     * @param mutation the {@link Mutation} which needs to be performed
     * @return prepared {@link ApolloMutationCall} call to be executed at some point in the future
     */
    <D extends Mutation.Data, T, V extends Mutation.Variables> ApolloMutationCall<T> mutate(
        @NotNull Mutation<D, T, V> mutation);

    /**
     * <p>Creates and prepares a new {@link ApolloMutationCall} call with optimistic updates.</p>
     *
     * Provided optimistic updates will be stored in {@link com.apollographql.apollo.cache.normalized.ApolloStore}
     * immediately before mutation execution. Any {@link ApolloQueryWatcher} dependent on the changed cache records will
     * be re-fetched.
     *
     * @param mutation              the {@link Mutation} which needs to be performed
     * @param withOptimisticUpdates optimistic updates for this mutation
     * @return prepared {@link ApolloMutationCall} call to be executed at some point in the future
     */
    <D extends Mutation.Data, T, V extends Mutation.Variables> ApolloMutationCall<T> mutate(
        @NotNull Mutation<D, T, V> mutation, @NotNull D withOptimisticUpdates);
  }
}
