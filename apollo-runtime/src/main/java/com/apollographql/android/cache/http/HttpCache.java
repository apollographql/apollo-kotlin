package com.apollographql.android.cache.http;

import java.io.IOException;

import javax.annotation.Nonnull;

import okhttp3.Interceptor;
import okhttp3.Response;
import okio.ForwardingSource;
import okio.Source;

public final class HttpCache {
  public static final String CACHE_KEY_HEADER = "APOLLO-CACHE-KEY";
  public static final String CACHE_CONTROL_HEADER = "APOLLO-CACHE-CONTROL";
  public static final String CACHE_SERVED_DATE_HEADER = "APOLLO-SERVED-DATE";
  public static final String CACHE_PREFETCH_HEADER = "APOLLO-PREFETCH";

  private final ResponseCacheStore cacheStore;
  private final EvictionStrategy evictionStrategy;

  public HttpCache(@Nonnull ResponseCacheStore cacheStore, @Nonnull EvictionStrategy evictionStrategy) {
    this.cacheStore = cacheStore;
    this.evictionStrategy = evictionStrategy;
  }

  public void clear() {
    try {
      cacheStore.delete();
    } catch (IOException ignore) {
      //TODO log me
    }
  }

  public void remove(@Nonnull String cacheKey) throws IOException {
    cacheStore.remove(cacheKey);
  }

  public void removeQuietly(@Nonnull String cacheKey) {
    try {
      remove(cacheKey);
    } catch (Exception ignore) {
      //TODO log me
    }
  }

  public Response read(@Nonnull final String cacheKey) {
    return read(cacheKey, false);
  }

  public Response read(@Nonnull final String cacheKey, final boolean expireAfterRead) {
    ResponseCacheRecord responseCacheRecord = null;
    try {
      responseCacheRecord = cacheStore.cacheRecord(cacheKey);
      if (responseCacheRecord == null) {
        return null;
      }

      final ResponseCacheRecord cacheRecord = responseCacheRecord;
      Source cacheResponseSource = new ForwardingSource(responseCacheRecord.bodySource()) {
        @Override public void close() throws IOException {
          super.close();
          closeQuietly(cacheRecord);
          if (expireAfterRead) {
            removeQuietly(cacheKey);
          }
        }
      };

      Response response = new ResponseHeaderRecord(responseCacheRecord.headerSource()).response();
      String contentType = response.header("Content-Type");
      String contentLength = response.header("Content-Length");
      return response.newBuilder()
          .body(new CacheResponseBody(cacheResponseSource, contentType, contentLength))
          .build();
    } catch (Exception e) {
      //TODO log
      return null;
    } finally {
      closeQuietly(responseCacheRecord);
    }
  }

  public Interceptor interceptor() {
    return new CacheInterceptor(this);
  }

  boolean isStale(@Nonnull Response response) {
    return evictionStrategy.isStale(response);
  }

  Response cacheProxy(@Nonnull Response response, @Nonnull String cacheKey) {
    ResponseCacheRecordEditor cacheRecordEditor = null;
    try {
      cacheRecordEditor = cacheStore.cacheRecordEditor(cacheKey);
      if (cacheRecordEditor != null) {
        new ResponseHeaderRecord(response).writeTo(cacheRecordEditor);
        return response.newBuilder()
            .body(new ResponseBodyProxy(cacheRecordEditor, response))
            .build();
      }
    } catch (Exception ignore) {
      //TODO log me
      abortQuietly(cacheRecordEditor);
    }
    return response;
  }

  void write(@Nonnull Response response, @Nonnull String cacheKey) {
    ResponseCacheRecordEditor cacheRecordEditor = null;
    try {
      cacheRecordEditor = cacheStore.cacheRecordEditor(cacheKey);
      if (cacheRecordEditor != null) {
        new ResponseHeaderRecord(response).writeTo(cacheRecordEditor);
        Utils.copyResponseBody(response, cacheRecordEditor.bodySink());
        cacheRecordEditor.commit();
      }
    } catch (Exception ignore) {
      //TODO log me
      abortQuietly(cacheRecordEditor);
    }
  }

  private static void closeQuietly(ResponseCacheRecord cacheRecord) {
    try {
      if (cacheRecord != null) {
        cacheRecord.close();
      }
    } catch (Exception ignore) {
      //TODO log me
    }
  }

  private static void abortQuietly(ResponseCacheRecordEditor cacheRecordEditor) {
    try {
      if (cacheRecordEditor != null) {
        cacheRecordEditor.abort();
      }
    } catch (Exception ignore) {
      //TODO log me
    }
  }

  //TODO should be generic for both http and normalized cache,
  //issue: https://github.com/apollographql/apollo-android/issues/281
  public enum CacheControl {
    DEFAULT("default"),
    NETWORK_ONLY("network-only"),
    CACHE_ONLY("cache-only"),
    NETWORK_BEFORE_STALE("network-before-stale"),
    EXPIRE_AFTER_READ("expire-after-read");

    public final String httpHeader;

    CacheControl(String httpHeader) {
      this.httpHeader = httpHeader;
    }

    static CacheControl valueOfHttpHeader(String header) {
      for (CacheControl value : values()) {
        if (value.httpHeader.equals(header)) {
          return value;
        }
      }
      return null;
    }
  }
}
