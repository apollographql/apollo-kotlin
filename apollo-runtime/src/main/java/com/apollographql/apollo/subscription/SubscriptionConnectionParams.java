package com.apollographql.apollo.subscription;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents connection parameters to be sent after connection with subscription server is established.
 * Note: provided connection parameters will be sent as JSON string.
 */
public final class SubscriptionConnectionParams extends HashMap<String, Object> {
  public SubscriptionConnectionParams() {
    super();
  }

  public SubscriptionConnectionParams(Map<? extends String, ?> m) {
    super(m);
  }
}
