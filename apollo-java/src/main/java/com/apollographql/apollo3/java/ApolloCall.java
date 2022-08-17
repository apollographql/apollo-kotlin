package com.apollographql.apollo3.java;

import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.api.ExecutionContext;
import com.apollographql.apollo3.api.MutableExecutionOptions;
import com.apollographql.apollo3.api.Operation;
import com.apollographql.apollo3.api.http.HttpHeader;
import com.apollographql.apollo3.api.http.HttpMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
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

  public Closeable subscribe(ApolloCallback<D> callback) {
    return ApolloCallUtils.subscribe(wrappedApolloCall, callback);
  }

  public void execute(ApolloCallback<D> callback) {
    ApolloCallUtils.execute(wrappedApolloCall, callback);
  }

  public ApolloResponse<D> executeBlocking() {
    return ApolloCallUtils.executeBlocking(wrappedApolloCall);
  }
}
