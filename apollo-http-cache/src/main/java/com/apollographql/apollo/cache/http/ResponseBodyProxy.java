package com.apollographql.apollo.cache.http;

import com.apollographql.apollo.api.cache.http.HttpCacheRecordEditor;
import com.apollographql.apollo.internal.ApolloLogger;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;

import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpCodec;
import okio.Buffer;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;
import okio.Timeout;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static okhttp3.internal.Util.closeQuietly;
import static okhttp3.internal.Util.discard;

final class ResponseBodyProxy extends ResponseBody {
  private final String contentType;
  private final String contentLength;
  private final Source responseBodySource;

  ResponseBodyProxy(@NotNull HttpCacheRecordEditor cacheRecordEditor, @NotNull Response sourceResponse,
      @NotNull ApolloLogger logger) {
    checkNotNull(cacheRecordEditor, "cacheRecordEditor == null");
    checkNotNull(sourceResponse, "sourceResponse == null");
    checkNotNull(logger, "logger == null");
    this.contentType = sourceResponse.header("Content-Type");
    this.contentLength = sourceResponse.header("Content-Length");
    this.responseBodySource = new ProxySource(cacheRecordEditor, sourceResponse.body().source(), logger);
  }

  @Override public MediaType contentType() {
    return contentType != null ? MediaType.parse(contentType) : null;
  }

  @Override public long contentLength() {
    try {
      return contentLength != null ? Long.parseLong(contentLength) : -1;
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  @Override public BufferedSource source() {
    return Okio.buffer(responseBodySource);
  }

  private static class ProxySource implements Source {
    final HttpCacheRecordEditor cacheRecordEditor;
    final ResponseBodyCacheSink responseBodyCacheSink;
    final Source responseBodySource;
    final ApolloLogger logger;
    boolean closed;

    ProxySource(HttpCacheRecordEditor cacheRecordEditor, Source responseBodySource, final ApolloLogger logger) {
      this.cacheRecordEditor = cacheRecordEditor;
      this.responseBodySource = responseBodySource;
      this.logger = logger;
      responseBodyCacheSink = new ResponseBodyCacheSink(Okio.buffer(cacheRecordEditor.bodySink())) {
        @Override void onException(Exception e) {
          abortCacheQuietly();
          logger.w(e, "Operation failed");
        }
      };
    }

    @Override public long read(Buffer sink, long byteCount) throws IOException {
      long bytesRead;
      try {
        bytesRead = responseBodySource.read(sink, byteCount);
      } catch (IOException e) {
        if (!closed) {
          // Failed to write a complete cache response.
          closed = true;
          abortCacheQuietly();
        }
        throw e;
      }

      if (bytesRead == -1) {
        if (!closed) {
          // The cache response is complete!
          closed = true;
          commitCache();
        }
        return -1;
      }

      responseBodyCacheSink.copyFrom(sink, sink.size() - bytesRead, bytesRead);
      return bytesRead;
    }

    @Override public Timeout timeout() {
      return responseBodySource.timeout();
    }

    @Override public void close() throws IOException {
      if (closed) return;
      closed = true;

      if (discard(this, HttpCodec.DISCARD_STREAM_TIMEOUT_MILLIS, MILLISECONDS)) {
        responseBodySource.close();
        commitCache();
      } else {
        responseBodySource.close();
        abortCacheQuietly();
      }
    }

    private void commitCache() {
      try {
        responseBodyCacheSink.close();
        cacheRecordEditor.commit();
      } catch (Exception e) {
        closeQuietly(responseBodyCacheSink);
        abortCacheQuietly();
        logger.e(e, "Failed to commit cache changes");
      }
    }

    void abortCacheQuietly() {
      closeQuietly(responseBodyCacheSink);
      try {
        cacheRecordEditor.abort();
      } catch (Exception ignore) {
        logger.w(ignore, "Failed to abort cache edit");
      }
    }
  }
}
