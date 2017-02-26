package com.apollographql.android.cache;

import java.util.Date;

import javax.annotation.Nonnull;

import okhttp3.Response;
import okhttp3.internal.http.HttpDate;

public final class TimeoutEvictionStrategy implements ResponseCacheStore.EvictionStrategy {
  private final long timeout;

  public TimeoutEvictionStrategy(long timeout) {
    this.timeout = timeout;
  }

  @Override public boolean isStale(@Nonnull Response response) {
    String servedDateStr = response.header(HttpCacheInterceptor.CACHE_SERVED_DATE_HEADER);
    if (servedDateStr == null) {
      return true;
    }
    Date servedDate = HttpDate.parse(servedDateStr);
    return servedDate == null || System.currentTimeMillis() - servedDate.getTime() > timeout;
  }
}
