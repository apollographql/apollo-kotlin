package com.apollographql.apollo.runtime.java.internal;

import com.apollographql.apollo.api.ApolloRequest;
import com.apollographql.apollo.api.ExecutionContext;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.http.HttpHeader;
import com.apollographql.apollo.api.http.HttpMethod;
import com.apollographql.apollo.runtime.java.ApolloCall;
import com.apollographql.apollo.runtime.java.ApolloCallback;
import com.apollographql.apollo.runtime.java.ApolloClient;
import com.apollographql.apollo.runtime.java.ApolloDisposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DefaultApolloCall<D extends Operation.Data> implements ApolloCall<D> {
  private final ApolloClient apolloClient;
  private final Operation<D> operation;
  private ExecutionContext executionContext = ExecutionContext.Empty;
  private HttpMethod httpMethod;
  private List<HttpHeader> httpHeaders;
  private Boolean sendApqExtensions;
  private Boolean sendDocument;
  private Boolean enableAutoPersistedQueries;
  private Boolean canBeBatched;
  private Boolean retryOnError;

  public DefaultApolloCall(ApolloClient apolloClient, Operation<D> operation) {
    this.apolloClient = apolloClient;
    this.operation = operation;
  }

  @Override public ApolloDisposable enqueue(@NotNull ApolloCallback<D> callback) {
    ApolloRequest<D> apolloRequest = new ApolloRequest.Builder<>(operation)
        .executionContext(executionContext)
        .httpMethod(httpMethod)
        .httpHeaders(httpHeaders)
        .sendApqExtensions(sendApqExtensions)
        .sendDocument(sendDocument)
        .enableAutoPersistedQueries(enableAutoPersistedQueries)
        .canBeBatched(canBeBatched)
        .retryOnError(retryOnError)
        .build();

    return apolloClient.execute(apolloRequest, callback);
  }

  @NotNull @Override public ExecutionContext getExecutionContext() {
    return executionContext;
  }

  @Nullable @Override public HttpMethod getHttpMethod() {
    return httpMethod;
  }

  @Nullable @Override public List<HttpHeader> getHttpHeaders() {
    return httpHeaders;
  }

  @Nullable @Override public Boolean getSendApqExtensions() {
    return sendApqExtensions;
  }

  @Nullable @Override public Boolean getSendDocument() {
    return sendDocument;
  }

  @Nullable @Override public Boolean getEnableAutoPersistedQueries() {
    return enableAutoPersistedQueries;
  }

  @Nullable @Override public Boolean getCanBeBatched() {
    return canBeBatched;
  }

  @Nullable public Boolean getRetryOnError() {
    return retryOnError;
  }

  @Override public ApolloCall<D> addExecutionContext(@NotNull ExecutionContext executionContext) {
    this.executionContext = this.executionContext.plus(executionContext);
    return this;
  }

  @Override public ApolloCall<D> httpMethod(@Nullable HttpMethod httpMethod) {
    this.httpMethod = httpMethod;
    return this;
  }

  @Override public ApolloCall<D> httpHeaders(@Nullable List<HttpHeader> list) {
    this.httpHeaders = list;
    return this;
  }

  @Override public ApolloCall<D> addHttpHeader(@NotNull String name, @NotNull String value) {
    if (this.httpHeaders == null) {
      this.httpHeaders = new ArrayList<>();
    }
    this.httpHeaders.add(new HttpHeader(name, value));
    return this;
  }

  @Override public ApolloCall<D> sendApqExtensions(@Nullable Boolean sendApqExtensions) {
    this.sendApqExtensions = sendApqExtensions;
    return this;
  }

  @Override public ApolloCall<D> sendDocument(@Nullable Boolean sendDocument) {
    this.sendDocument = sendDocument;
    return this;
  }

  @Override public ApolloCall<D> enableAutoPersistedQueries(@Nullable Boolean enableAutoPersistedQueries) {
    this.enableAutoPersistedQueries = enableAutoPersistedQueries;
    return this;
  }

  @Override public ApolloCall<D> canBeBatched(@Nullable Boolean canBeBatched) {
    this.canBeBatched = canBeBatched;
    return this;
  }

  public ApolloCall<D> retryOnError(@Nullable Boolean retryOnError) {
    throw new IllegalStateException("Not supported yet");
    //this.retryOnError = retryOnError;
    //return this;
  }
}
