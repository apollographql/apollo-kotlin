package com.apollographql.apollo.subscription;

import javax.annotation.Nonnull;

public interface SubscriptionTransport {

  void connect();

  void disconnect(OperationClientMessage message);

  void send(OperationClientMessage message);

  interface Callback {
    void onConnected();

    void onFailure(Throwable t);

    void onMessage(OperationServerMessage message);
  }

  interface Factory {
    SubscriptionTransport create(@Nonnull Callback callback);
  }
}
