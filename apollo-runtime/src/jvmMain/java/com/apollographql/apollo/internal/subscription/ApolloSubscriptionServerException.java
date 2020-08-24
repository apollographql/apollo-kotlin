package com.apollographql.apollo.internal.subscription;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;
import static java.util.Collections.unmodifiableMap;

@SuppressWarnings("WeakerAccess")
public class ApolloSubscriptionServerException extends ApolloSubscriptionException {
  public final Map<String, Object> errorPayload;

  public ApolloSubscriptionServerException(@NotNull Map<String, Object> errorPayload) {
    super("Subscription failed");
    this.errorPayload = unmodifiableMap(checkNotNull(errorPayload, "errorPayload == null"));
  }
}
