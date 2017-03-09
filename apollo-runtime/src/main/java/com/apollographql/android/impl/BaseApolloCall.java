package com.apollographql.android.impl;

import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.cache.http.HttpCache;
import com.apollographql.android.cache.http.HttpCacheControl;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;

abstract class BaseApolloCall {
  private static final String ACCEPT_TYPE = "application/json";
  private static final String CONTENT_TYPE = "application/json";
  private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

  final Operation operation;
  final HttpUrl serverUrl;
  final okhttp3.Call.Factory httpCallFactory;
  final Moshi moshi;

  BaseApolloCall(Operation operation, HttpUrl serverUrl, Call.Factory httpCallFactory, Moshi moshi) {
    this.operation = operation;
    this.serverUrl = serverUrl;
    this.moshi = moshi;
    this.httpCallFactory = httpCallFactory;
  }

  Call prepareHttpCall(HttpCacheControl cacheControl, boolean prefetch) throws IOException {
    RequestBody requestBody = httpRequestBody(operation);
    String cacheKey = cacheKey(requestBody);
    Request request = new Request.Builder()
        .url(serverUrl)
        .post(requestBody)
        .header("Accept", ACCEPT_TYPE)
        .header("Content-Type", CONTENT_TYPE)
        .header(HttpCache.CACHE_KEY_HEADER, cacheKey)
        .header(HttpCache.CACHE_CONTROL_HEADER, cacheControl.httpHeader)
        .header(HttpCache.CACHE_PREFETCH_HEADER, Boolean.toString(prefetch))
        .build();
    return httpCallFactory.newCall(request);
  }

  private RequestBody httpRequestBody(Operation operation) throws IOException {
    JsonAdapter<Operation> adapter = new OperationJsonAdapter(moshi);
    Buffer buffer = new Buffer();
    adapter.toJson(buffer, operation);
    return RequestBody.create(MEDIA_TYPE, buffer.readByteString());
  }

  static String cacheKey(RequestBody requestBody) throws IOException {
    Buffer hashBuffer = new Buffer();
    requestBody.writeTo(hashBuffer);
    return hashBuffer.readByteString().md5().hex();
  }
}
