package com.apollographql.android.cache.http;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import okhttp3.Response;
import okhttp3.internal.http.HttpDate;

public final class TimeoutEvictionStrategy implements EvictionStrategy {
  private final long timeout;

  public TimeoutEvictionStrategy(long timeout, @Nonnull TimeUnit timeUnit) {
    this.timeout = timeUnit.toMillis(timeout);
  }

  @Override public boolean isStale(@Nonnull Response response) {
    long now = System.currentTimeMillis();
    String servedDateStr = response.header(HttpCache.CACHE_SERVED_DATE_HEADER);
    if (servedDateStr == null) {
      return true;
    }
    Date servedDate = HttpDate.parse(servedDateStr);
    return servedDate == null || now - servedDate.getTime() > timeout;
  }
}
