package com.apollographql.apollo.internal.subscription;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.cache.normalized.Record;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public final class SubscriptionResponse<T> {
  @NotNull public final Subscription<?, T, ?> subscription;
  @NotNull public final Response<T> response;
  @NotNull public final Collection<Record> cacheRecords;

  public SubscriptionResponse(@NotNull Subscription<?, T, ?> subscription, @NotNull Response<T> response,
      @NotNull Collection<Record> cacheRecords) {
    this.subscription = subscription;
    this.response = response;
    this.cacheRecords = cacheRecords;
  }
}
