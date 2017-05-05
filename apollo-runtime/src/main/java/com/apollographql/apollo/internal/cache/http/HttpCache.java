package com.apollographql.apollo.internal.cache.http;

import com.apollographql.apollo.internal.util.ApolloLogger;
import com.apollographql.apollo.cache.http.EvictionStrategy;
import com.apollographql.apollo.cache.http.ResponseCacheRecord;
import com.apollographql.apollo.cache.http.ResponseCacheRecordEditor;
import com.apollographql.apollo.cache.http.ResponseCacheStore;

import java.io.IOException;

import javax.annotation.Nonnull;

import okhttp3.Interceptor;
import okhttp3.Response;
import okio.ForwardingSource;
import okio.Sink;
import okio.Source;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;
import static com.apollographql.apollo.internal.cache.http.Utils.copyResponseBody;

@SuppressWarnings("WeakerAccess") public final class HttpCache {
  public static final String CACHE_KEY_HEADER = "APOLLO-CACHE-KEY";
  public static final String CACHE_CONTROL_HEADER = "APOLLO-CACHE-CONTROL";
  public static final String CACHE_SERVED_DATE_HEADER = "APOLLO-SERVED-DATE";
  public static final String CACHE_PREFETCH_HEADER = "APOLLO-PREFETCH";

  private final ResponseCacheStore cacheStore;
  private final EvictionStrategy evictionStrategy;
  private final ApolloLogger logger;

  public HttpCache(@Nonnull ResponseCacheStore cacheStore, @Nonnull EvictionStrategy evictionStrategy,
      @Nonnull ApolloLogger logger) {
    this.cacheStore = checkNotNull(cacheStore, "cacheStore can't be null");
    this.evictionStrategy = checkNotNull(evictionStrategy, "evictionStrategy can't be null");
    this.logger = checkNotNull(logger, "logger can't be null");
  }

  public void clear() {
    try {
      cacheStore.delete();
    } catch (IOException e) {
      logger.e(e, "Failed to clear http cache");
    }
  }

  public void remove(@Nonnull String cacheKey) throws IOException {
    cacheStore.remove(cacheKey);
  }

  public void removeQuietly(@Nonnull String cacheKey) {
    try {
      remove(cacheKey);
    } catch (Exception ignore) {
      logger.w(ignore, "Failed to remove cached record for key: %s", cacheKey);
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
      closeQuietly(responseCacheRecord);
      logger.e(e, "Failed to read http cache entry for key: %s", cacheKey);
      return null;
    }
  }

  public Interceptor interceptor() {
    return new CacheInterceptor(this, logger);
  }

  boolean isStale(@Nonnull Response response) {
    return evictionStrategy.isStale(response);
  }

  Response cacheProxy(@Nonnull Response response, @Nonnull String cacheKey) {
    ResponseCacheRecordEditor cacheRecordEditor = null;
    try {
      cacheRecordEditor = cacheStore.cacheRecordEditor(cacheKey);
      if (cacheRecordEditor != null) {
        Sink headerSink = cacheRecordEditor.headerSink();
        try {
          new ResponseHeaderRecord(response).writeTo(headerSink);
        } finally {
          closeQuietly(headerSink);
        }

        return response.newBuilder()
            .body(new ResponseBodyProxy(cacheRecordEditor, response, logger))
            .build();
      }
    } catch (Exception e) {
      abortQuietly(cacheRecordEditor);
      logger.e(e, "Failed to proxy http response for key: %s", cacheKey);
    }
    return response;
  }

  void write(@Nonnull Response response, @Nonnull String cacheKey) {
    ResponseCacheRecordEditor cacheRecordEditor = null;
    try {
      cacheRecordEditor = cacheStore.cacheRecordEditor(cacheKey);
      if (cacheRecordEditor != null) {
        Sink headerSink = cacheRecordEditor.headerSink();
        try {
          new ResponseHeaderRecord(response).writeTo(headerSink);
        } finally {
          closeQuietly(headerSink);
        }

        Sink bodySink = cacheRecordEditor.bodySink();
        try {
          copyResponseBody(response, bodySink);
        } finally {
          closeQuietly(bodySink);
        }

        cacheRecordEditor.commit();
      }
    } catch (Exception e) {
      abortQuietly(cacheRecordEditor);
      logger.e(e, "Failed to cache http response for key: %s", cacheKey);
    }
  }

  private void closeQuietly(ResponseCacheRecord cacheRecord) {
    try {
      if (cacheRecord != null) {
        cacheRecord.close();
      }
    } catch (Exception ignore) {
      logger.w(ignore, "Failed to close cache record");
    }
  }

  private void abortQuietly(ResponseCacheRecordEditor cacheRecordEditor) {
    try {
      if (cacheRecordEditor != null) {
        cacheRecordEditor.abort();
      }
    } catch (Exception ignore) {
      logger.w(ignore, "Failed to abort cache record edit");
    }
  }

  private void closeQuietly(Sink sink) {
    try {
      sink.close();
    } catch (Exception ignore) {
      logger.w(ignore, "Failed to close sink");
    }
  }
}
