package com.apollographql.apollo.cache.http;

import com.apollographql.apollo.api.cache.http.HttpCacheRecord;
import com.apollographql.apollo.api.cache.http.HttpCacheRecordEditor;
import com.apollographql.apollo.api.cache.http.HttpCacheStore;

import java.io.File;
import java.io.IOException;

import org.jetbrains.annotations.NotNull;

import okhttp3.internal.cache.DiskLruCache;
import okhttp3.internal.io.FileSystem;
import okio.Sink;
import okio.Source;

public final class DiskLruHttpCacheStore implements HttpCacheStore {
  private static final int VERSION = 99991;
  private static final int ENTRY_HEADERS = 0;
  private static final int ENTRY_BODY = 1;
  private static final int ENTRY_COUNT = 2;

  private final DiskLruCache cache;

  public DiskLruHttpCacheStore(@NotNull File directory, long maxSize) {
    this.cache = DiskLruCache.create(FileSystem.SYSTEM, directory, VERSION, ENTRY_COUNT, maxSize);
  }

  public DiskLruHttpCacheStore(@NotNull FileSystem fileSystem, @NotNull File directory, long maxSize) {
    this.cache = DiskLruCache.create(fileSystem, directory, VERSION, ENTRY_COUNT, maxSize);
  }

  @Override public HttpCacheRecord cacheRecord(@NotNull String cacheKey) throws IOException {
    final DiskLruCache.Snapshot snapshot = cache.get(cacheKey);
    if (snapshot == null) {
      return null;
    }
    final HttpCacheRecord responseCacheRecord = new HttpCacheRecord() {
      @NotNull @Override public Source headerSource() {
        return snapshot.getSource(ENTRY_HEADERS);
      }

      @NotNull @Override public Source bodySource() {
        return snapshot.getSource(ENTRY_BODY);
      }

      @Override public void close() {
        snapshot.close();
      }
    };

    return responseCacheRecord;
  }

  @Override public HttpCacheRecordEditor cacheRecordEditor(@NotNull String cacheKey) throws IOException {
    final DiskLruCache.Editor editor = cache.edit(cacheKey);
    if (editor == null) {
      return null;
    }

    return new HttpCacheRecordEditor() {
      @NotNull @Override public Sink headerSink() {
        return editor.newSink(ENTRY_HEADERS);
      }

      @NotNull @Override public Sink bodySink() {
        return editor.newSink(ENTRY_BODY);
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

  @Override public void remove(@NotNull String cacheKey) throws IOException {
    cache.remove(cacheKey);
  }
}
