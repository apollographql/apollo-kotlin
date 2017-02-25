package com.apollographql.android.cache;

import java.io.IOException;

import okhttp3.Response;

public final class HttpCache {
  private final ResponseCacheStore cacheStore;

  public HttpCache(ResponseCacheStore cacheStore) {
    this.cacheStore = cacheStore;
  }

  public void delete() throws IOException {
    cacheStore.delete();
  }

  public void remove(String cacheKey) throws IOException {
    cacheStore.remove(cacheKey);
  }

  public Response read(String cacheKey) throws IOException {
    ResponseCacheRecord cacheRecord = cacheStore.cacheRecord(cacheKey);
    if (cacheRecord == null) {
      return null;
    }

    Response response = new ResponseHeaderRecord(cacheRecord.headerSource()).response();
    return response.newBuilder()
        .body(new CacheResponseBody(cacheRecord, response))
        .build();
  }

  Response cacheProxy(Response response, String cacheKey) throws IOException {
    ResponseCacheRecordEditor cacheRecordEditor = cacheStore.cacheRecordEditor(cacheKey);
    new ResponseHeaderRecord(response).writeTo(cacheRecordEditor);
    return response.newBuilder()
        .body(new ResponseBodyProxy(cacheRecordEditor, response))
        .build();
  }
}
