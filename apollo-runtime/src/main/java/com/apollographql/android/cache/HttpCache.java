package com.apollographql.android.cache;

import java.io.IOException;

import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;

public final class HttpCache {
  private final ResponseCacheStore cacheStore;

  public HttpCache(ResponseCacheStore cacheStore) {
    this.cacheStore = cacheStore;
  }

  public void delete() throws IOException {
    cacheStore.delete();
  }

  public Response read(Request request) throws IOException {
    String cacheKey = cacheKey(request);
    ResponseCacheRecord cacheRecord = cacheStore.cacheRecord(cacheKey);
    if (cacheRecord == null) {
      return null;
    }

    Response response = new ResponseHeaderRecord(cacheRecord.headerSource()).response();
    return response.newBuilder()
        .body(new CacheResponseBody(cacheRecord, response))
        .build();
  }

  Response cacheProxy(Response response) throws IOException {
    String cacheKey = cacheKey(response.request());
    ResponseCacheRecordEditor cacheRecordEditor = cacheStore.cacheRecordEditor(cacheKey);
    new ResponseHeaderRecord(response).writeTo(cacheRecordEditor);
    return response.newBuilder()
        .body(new ResponseBodyProxy(cacheRecordEditor, response))
        .build();
  }

  private static String cacheKey(Request request) throws IOException {
    Buffer hashBuffer = new Buffer();
    request.body().writeTo(hashBuffer);
    return hashBuffer.readByteString().md5().hex();
  }
}
