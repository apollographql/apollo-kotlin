package com.apollographql.apollo.internal.subscription;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.cache.normalized.Record;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public final class SubscriptionResponse<D extends Subscription.Data> {
  @NotNull public final Subscription<D, ?> subscription;
  @NotNull public final Response<D> response;
  @NotNull public final Collection<Record> cacheRecords;

  public SubscriptionResponse(@NotNull Subscription<D, ?> subscription, @NotNull Response<D> response,
      @NotNull Collection<Record> cacheRecords) {
    this.subscription = subscription;
    this.response = response;
    this.cacheRecords = cacheRecords;
  }
}
