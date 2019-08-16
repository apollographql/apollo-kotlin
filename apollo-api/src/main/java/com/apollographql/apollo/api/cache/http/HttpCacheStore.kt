package com.apollographql.apollo.api.cache.http;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * CacheStore is an abstraction for a cache store that is used to read, modify or delete http responses.
 */
public interface HttpCacheStore {
  /**
   * Returns ResponseCacheRecord for the entry named cacheKey or null if it doesn't exist or is not currently readable.
   *
   * @param cacheKey the name of the entry
   * @return ResponseCacheRecord
   */
  @Nullable HttpCacheRecord cacheRecord(@NotNull String cacheKey) throws IOException;

  /**
   * Returns an editor for the entry named cacheKey or null if another edit is in progress.
   *
   * @param cacheKey the entry to edit.
   * @return {@link HttpCacheRecordEditor} to use for editing the entry
   */
  @Nullable HttpCacheRecordEditor cacheRecordEditor(@NotNull String cacheKey) throws IOException;

  /**
   * Drops the entry for key if it exists and can be removed. If the entry for key is currently being edited, that edit
   * will complete normally but its value will not be stored.
   */
  void remove(@NotNull String cacheKey) throws IOException;

  /**
   * Closes the cache and deletes all of its stored values. This will delete all files in the cache directory including
   * files that weren't created by the cache.
   */
  void delete() throws IOException;
}
