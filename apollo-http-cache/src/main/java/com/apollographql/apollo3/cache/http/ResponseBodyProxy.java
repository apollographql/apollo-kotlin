package com.apollographql.apollo3.cache.http;

import com.apollographql.apollo3.api.cache.http.HttpCacheRecordEditor;
import com.apollographql.apollo3.api.internal.ApolloLogger;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;
import okio.Timeout;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static com.apollographql.apollo3.api.internal.Utils.checkNotNull;
import static com.apollographql.apollo3.cache.http.Utils.closeQuietly;
import static com.apollographql.apollo3.cache.http.Utils.discard;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

final class ResponseBodyProxy extends ResponseBody {
  private final String contentType;
  private final String contentLength;
  private final BufferedSource responseBodySource;

  ResponseBodyProxy(@NotNull HttpCacheRecordEditor cacheRecordEditor, @NotNull Response sourceResponse,
      @NotNull ApolloLogger logger) {
    checkNotNull(cacheRecordEditor, "cacheRecordEditor == null");
    checkNotNull(sourceResponse, "sourceResponse == null");
    checkNotNull(logger, "logger == null");
    this.contentType = sourceResponse.header("Content-Type");
    this.contentLength = sourceResponse.header("Content-Length");
    this.responseBodySource = Okio.buffer(new ProxySource(cacheRecordEditor, sourceResponse.body().source(), logger));
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

  @NotNull @Override public BufferedSource source() {
    return responseBodySource;
  }

  private static class ProxySource implements Source {
    private final HttpCacheRecordEditor cacheRecordEditor;
    private final ResponseBodyCacheSink responseBodyCacheSink;
    private final BufferedSource responseBodySource;
    private final ApolloLogger logger;
    private boolean closed;

    ProxySource(HttpCacheRecordEditor cacheRecordEditor, BufferedSource responseBodySource, final ApolloLogger logger) {
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

    @NotNull @Override public Timeout timeout() {
      return responseBodySource.timeout();
    }

    @Override public void close() {
      if (closed) return;
      closed = true;

      if (discard(this, 100, MILLISECONDS)) {
        commitCache();
      } else {
        abortCacheQuietly();
      }
    }

    private void commitCache() {
      closeQuietly(responseBodySource);
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
      closeQuietly(responseBodySource);
      closeQuietly(responseBodyCacheSink);
      try {
        cacheRecordEditor.abort();
      } catch (Exception e) {
        logger.w(e, "Failed to abort cache edit");
      }
    }
  }
}
