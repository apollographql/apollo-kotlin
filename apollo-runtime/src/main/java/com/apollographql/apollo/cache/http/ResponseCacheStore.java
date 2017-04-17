package com.apollographql.apollo.cache.http;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * ResponseCacheStore is an abstraction for a cache store that is used to read, modify or delete http responses.
 */
public interface ResponseCacheStore {
  /**
   * Returns ResponseCacheRecord for the entry named cacheKey or null if it doesn't exist or is not currently readable.
   *
   * @param cacheKey the name of the entry
   * @return ResponseCacheRecord
   */
  @Nullable ResponseCacheRecord cacheRecord(@Nonnull String cacheKey) throws IOException;

  /**
   * Returns an editor for the entry named cacheKey or null if another edit is in progress.
   *
   * @param cacheKey the entry to edit.
   * @return {@link ResponseCacheRecordEditor} to use for editing the entry
   */
  @Nullable ResponseCacheRecordEditor cacheRecordEditor(@Nonnull String cacheKey) throws IOException;

  /**
   * Drops the entry for key if it exists and can be removed. If the entry for key is currently being edited, that edit
   * will complete normally but its value will not be stored.
   */
  void remove(@Nonnull String cacheKey) throws IOException;

  /**
   * Closes the cache and deletes all of its stored values. This will delete all files in the cache directory including
   * files that weren't created by the cache.
   */
  void delete() throws IOException;
}
