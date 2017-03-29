package com.apollographql.apollo;

import com.apollographql.apollo.cache.http.ResponseCacheRecord;
import com.apollographql.apollo.cache.http.ResponseCacheRecordEditor;
import com.apollographql.apollo.cache.http.ResponseCacheStore;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nonnull;

import okhttp3.internal.cache.DiskLruCache;
import okhttp3.internal.io.FileSystem;
import okio.Buffer;
import okio.Sink;
import okio.Source;
import okio.Timeout;

class FaultyCacheStore implements ResponseCacheStore {
  private static final int VERSION = 99991;
  private static final int ENTRY_HEADERS = 0;
  private static final int ENTRY_BODY = 1;
  private static final int ENTRY_COUNT = 2;

  private DiskLruCache cache;
  private final FaultySource faultySource = new FaultySource();
  private final FaultySink faultySink = new FaultySink();
  private FailStrategy failStrategy;

  FaultyCacheStore(FileSystem fileSystem) {
    this.cache = DiskLruCache.create(fileSystem, new File("/cache/"), VERSION, ENTRY_COUNT, Integer.MAX_VALUE);
  }

  @Override public ResponseCacheRecord cacheRecord(@Nonnull String cacheKey) throws IOException {
    final DiskLruCache.Snapshot snapshot = cache.get(cacheKey);
    if (snapshot == null) {
      return null;
    }

    return new ResponseCacheRecord() {
      @Nonnull @Override public Source headerSource() {
        if (failStrategy == FailStrategy.FAIL_HEADER_READ) {
          return faultySource;
        } else {
          return snapshot.getSource(ENTRY_HEADERS);
        }
      }

      @Nonnull @Override public Source bodySource() {
        if (failStrategy == FailStrategy.FAIL_BODY_READ) {
          return faultySource;
        } else {
          return snapshot.getSource(ENTRY_BODY);
        }
      }

      @Override public void close() {
        snapshot.close();
      }
    };
  }

  @Override public ResponseCacheRecordEditor cacheRecordEditor(@Nonnull String cacheKey) throws IOException {
    final DiskLruCache.Editor editor = cache.edit(cacheKey);
    if (editor == null) {
      return null;
    }

    return new ResponseCacheRecordEditor() {
      @Nonnull @Override public Sink headerSink() {
        if (failStrategy == FailStrategy.FAIL_HEADER_WRITE) {
          return faultySink;
        } else {
          return editor.newSink(ENTRY_HEADERS);
        }
      }

      @Nonnull @Override public Sink bodySink() {
        if (failStrategy == FailStrategy.FAIL_BODY_WRITE) {
          return faultySink;
        } else {
          return editor.newSink(ENTRY_BODY);
        }
      }

      @Override public void abort() throws IOException {
        editor.abort();
      }

      @Override public void commit() throws IOException {
        editor.commit();
      }
    };
  }

  @Override public void delete() throws IOException {
    cache.delete();
  }

  @Override public void remove(@Nonnull String cacheKey) throws IOException {
    cache.remove(cacheKey);
  }

  void failStrategy(FailStrategy failStrategy) {
    this.failStrategy = failStrategy;
  }

  enum FailStrategy {
    FAIL_HEADER_READ,
    FAIL_BODY_READ,
    FAIL_HEADER_WRITE,
    FAIL_BODY_WRITE
  }

  private static class FaultySource implements Source {
    @Override public long read(Buffer sink, long byteCount) throws IOException {
      throw new IOException("failed to read");
    }

    @Override public Timeout timeout() {
      return new Timeout();
    }

    @Override public void close() throws IOException {

    }
  }

  private static class FaultySink implements Sink {
    @Override public void write(Buffer source, long byteCount) throws IOException {
      throw new IOException("failed to write");
    }

    @Override public void flush() throws IOException {
    }

    @Override public Timeout timeout() {
      return new Timeout();
    }

    @Override public void close() throws IOException {
    }
  }
}
