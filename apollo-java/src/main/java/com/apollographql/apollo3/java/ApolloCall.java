package com.apollographql.apollo3.java;

import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.api.ExecutionContext;
import com.apollographql.apollo3.api.MutableExecutionOptions;
import com.apollographql.apollo3.api.Mutation;
import com.apollographql.apollo3.api.Operation;
import com.apollographql.apollo3.api.Query;
import com.apollographql.apollo3.api.http.HttpHeader;
import com.apollographql.apollo3.api.http.HttpMethod;
import com.apollographql.apollo3.cache.normalized.FetchPolicy;
import com.apollographql.apollo3.cache.normalized.NormalizedCache;
import com.apollographql.apollo3.java.internal.ApolloCallAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ApolloCall<D extends Operation.Data> implements MutableExecutionOptions<ApolloCall<D>> {
  private final com.apollographql.apollo3.ApolloCall<D> wrappedApolloCall;

  public ApolloCall(com.apollographql.apollo3.ApolloCall<D> wrappedApolloCall) {
    this.wrappedApolloCall = wrappedApolloCall;
  }

  @NotNull @Override public ExecutionContext getExecutionContext() {
    return wrappedApolloCall.getExecutionContext();
  }

  @Nullable @Override public HttpMethod getHttpMethod() {
    return wrappedApolloCall.getHttpMethod();
  }

  @Nullable @Override public List<HttpHeader> getHttpHeaders() {
    return wrappedApolloCall.getHttpHeaders();
  }

  @Nullable @Override public Boolean getSendApqExtensions() {
    return wrappedApolloCall.getSendApqExtensions();
  }

  @Nullable @Override public Boolean getSendDocument() {
    return wrappedApolloCall.getSendDocument();
  }

  @Nullable @Override public Boolean getEnableAutoPersistedQueries() {
    return wrappedApolloCall.getEnableAutoPersistedQueries();
  }

  @Nullable @Override public Boolean getCanBeBatched() {
    return wrappedApolloCall.getCanBeBatched();
  }

  @Override public ApolloCall<D> addExecutionContext(@NotNull ExecutionContext executionContext) {
    wrappedApolloCall.addExecutionContext(executionContext);
    return this;
  }

  @Override public ApolloCall<D> httpMethod(@Nullable HttpMethod httpMethod) {
    wrappedApolloCall.httpMethod(httpMethod);
    return this;
  }

  @Override public ApolloCall<D> httpHeaders(@Nullable List<HttpHeader> httpHeaders) {
    wrappedApolloCall.httpHeaders(httpHeaders);
    return this;
  }

  @Override public ApolloCall<D> addHttpHeader(@NotNull String name, @NotNull String value) {
    wrappedApolloCall.addHttpHeader(name, value);
    return this;
  }

  @Override public ApolloCall<D> sendApqExtensions(@Nullable Boolean sendApqExtensions) {
    wrappedApolloCall.sendApqExtensions(sendApqExtensions);
    return this;
  }

  @Override public ApolloCall<D> sendDocument(@Nullable Boolean sendDocument) {
    wrappedApolloCall.sendDocument(sendDocument);
    return this;
  }

  @Override public ApolloCall<D> enableAutoPersistedQueries(@Nullable Boolean enableAutoPersistedQueries) {
    wrappedApolloCall.enableAutoPersistedQueries(enableAutoPersistedQueries);
    return this;
  }

  @Override public ApolloCall<D> canBeBatched(@Nullable Boolean canBeBatched) {
    wrappedApolloCall.canBeBatched(canBeBatched);
    return this;
  }

  public Subscription execute(@NotNull ApolloCallback<D> callback) {
    return ApolloCallAdapter.execute(wrappedApolloCall, callback);
  }

  public ApolloResponse<D> executeBlocking() {
    return ApolloCallAdapter.executeBlocking(wrappedApolloCall);
  }


  /*
   * Normalized cache.
   */

  public ApolloCall<D> fetchPolicy(@NotNull FetchPolicy fetchPolicy) {
    NormalizedCache.fetchPolicy(wrappedApolloCall, fetchPolicy);
    return this;
  }

  public ApolloCall<D> refetchPolicy(@NotNull FetchPolicy fetchPolicy) {
    NormalizedCache.refetchPolicy(wrappedApolloCall, fetchPolicy);
    return this;
  }

  public ApolloCall<D> doNotStore(boolean doNotStore) {
    NormalizedCache.doNotStore(wrappedApolloCall, doNotStore);
    return this;
  }

  public ApolloCall<D> emitCacheMisses(boolean emitCacheMisses) {
    NormalizedCache.emitCacheMisses(wrappedApolloCall, emitCacheMisses);
    return this;
  }

  public ApolloCall<D> storePartialResponses(boolean storePartialResponses) {
    NormalizedCache.storePartialResponses(wrappedApolloCall, storePartialResponses);
    return this;
  }

  public <T extends Mutation.Data> ApolloCall<D> optimisticUpdates(@NotNull T data) {
    //noinspection unchecked
    NormalizedCache.optimisticUpdates((com.apollographql.apollo3.ApolloCall<T>) wrappedApolloCall, data);
    return this;
  }

  public <T extends Query.Data> Subscription watch(boolean fetchThrows, boolean refetchThrows, @NotNull ApolloCallback<T> callback) {
    //noinspection unchecked
    return ApolloCallAdapter.watch((com.apollographql.apollo3.ApolloCall<T>) wrappedApolloCall, fetchThrows, refetchThrows, callback);
  }

  public <T extends Query.Data> Subscription watch(@NotNull ApolloCallback<T> callback) {
    //noinspection unchecked
    return ApolloCallAdapter.watch((com.apollographql.apollo3.ApolloCall<T>) wrappedApolloCall, callback);
  }

  public <T extends Query.Data> Subscription watch(@Nullable T data, @NotNull RetryPredicate reopenWhen, @NotNull ApolloCallback<T> callback) {
    //noinspection unchecked
    return ApolloCallAdapter.watch((com.apollographql.apollo3.ApolloCall<T>) wrappedApolloCall, data, reopenWhen, callback);
  }

  public <T extends Query.Data> Subscription watch(@Nullable T data, @NotNull ApolloCallback<T> callback) {
    //noinspection unchecked
    return ApolloCallAdapter.watch((com.apollographql.apollo3.ApolloCall<T>) wrappedApolloCall, data, callback);
  }

  public <T extends Query.Data> Subscription executeCacheAndNetwork(@NotNull ApolloCallback<T> callback) {
    //noinspection unchecked
    return ApolloCallAdapter.executeCacheAndNetwork((com.apollographql.apollo3.ApolloCall<T>) wrappedApolloCall, callback);
  }

}
