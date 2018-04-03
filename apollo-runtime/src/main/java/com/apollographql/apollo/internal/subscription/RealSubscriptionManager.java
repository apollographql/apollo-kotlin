package com.apollographql.apollo.internal.subscription;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.internal.ResponseFieldMapperFactory;
import com.apollographql.apollo.response.OperationResponseParser;
import com.apollographql.apollo.response.ScalarTypeAdapters;
import com.apollographql.apollo.subscription.OperationClientMessage;
import com.apollographql.apollo.subscription.OperationServerMessage;
import com.apollographql.apollo.subscription.SubscriptionTransport;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

@SuppressWarnings("WeakerAccess")
public final class RealSubscriptionManager implements SubscriptionManager {
  static final int CONNECTION_ACKNOWLEDGE_TIMEOUT_TIMER_TASK_ID = 1;
  static final int INACTIVITY_TIMEOUT_TIMER_TASK_ID = 2;
  static final long CONNECTION_ACKNOWLEDGE_TIMEOUT = TimeUnit.SECONDS.toMillis(5);
  static final long INACTIVITY_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

  Map<String, SubscriptionRecord> subscriptions = new LinkedHashMap<>();
  State state = State.DISCONNECTED;
  final AutoReleaseTimer timer = new AutoReleaseTimer();

  private final ScalarTypeAdapters scalarTypeAdapters;
  private final SubscriptionTransport transport;
  private Map<String, Object> connectionParams;
  private final Executor dispatcher;
  private final ResponseFieldMapperFactory responseFieldMapperFactory = new ResponseFieldMapperFactory();
  private final Runnable connectionAcknowledgeTimeoutTimerTask = new Runnable() {
    @Override public void run() {
      onConnectionAcknowledgeTimeout();
    }
  };
  private final Runnable inactivityTimeoutTimerTask = new Runnable() {
    @Override public void run() {
      onInactivityTimeout();
    }
  };

  public RealSubscriptionManager(@Nonnull ScalarTypeAdapters scalarTypeAdapters,
      @Nonnull final SubscriptionTransport.Factory transportFactory, @Nonnull Map<String, Object> connectionParams,
      @Nonnull final Executor dispatcher) {
    checkNotNull(scalarTypeAdapters, "scalarTypeAdapters == null");
    checkNotNull(transportFactory, "transportFactory == null");
    checkNotNull(dispatcher, "dispatcher == null");

    this.scalarTypeAdapters = checkNotNull(scalarTypeAdapters, "scalarTypeAdapters == null");
    this.connectionParams = checkNotNull(connectionParams, "connectionParams == null");
    this.transport = transportFactory.create(new SubscriptionTransportCallback(this, dispatcher));
    this.dispatcher = dispatcher;
  }

  @Override
  public <T> void subscribe(@Nonnull final Subscription<?, T, ?> subscription,
      @Nonnull final SubscriptionManager.Callback<T> callback) {
    checkNotNull(subscription, "subscription == null");
    checkNotNull(callback, "callback == null");
    dispatcher.execute(new Runnable() {
      @Override
      public void run() {
        doSubscribe(subscription, callback);
      }
    });
  }

  @Override
  public void unsubscribe(@Nonnull final Subscription subscription) {
    checkNotNull(subscription, "subscription == null");
    dispatcher.execute(new Runnable() {
      @Override
      public void run() {
        doUnsubscribe(subscription);
      }
    });
  }

  void doSubscribe(Subscription subscription, SubscriptionManager.Callback callback) {
    timer.cancelTask(INACTIVITY_TIMEOUT_TIMER_TASK_ID);

    String subscriptionId = idForSubscription(subscription);
    synchronized (this) {
      if (subscriptions.containsKey(subscriptionId)) {
        callback.onError(new ApolloSubscriptionException("Already subscribed"));
        return;
      }

      subscriptions.put(subscriptionId, new SubscriptionRecord(subscription, callback));
      if (state == State.DISCONNECTED) {
        state = State.CONNECTING;
        transport.connect();
      } else if (state == State.ACTIVE) {
        transport.send(new OperationClientMessage.Start(subscriptionId, subscription, scalarTypeAdapters));
      }
    }
  }

  void doUnsubscribe(Subscription subscription) {
    String subscriptionId = idForSubscription(subscription);

    SubscriptionRecord subscriptionRecord;
    synchronized (this) {
      subscriptionRecord = subscriptions.remove(subscriptionId);
      if (subscriptionRecord != null && state == State.ACTIVE) {
        transport.send(new OperationClientMessage.Stop(subscriptionId));
      }

      if (subscriptions.isEmpty()) {
        startInactivityTimer();
      }
    }
  }

  void onTransportConnected() {
    synchronized (this) {
      state = State.CONNECTED;
      transport.send(new OperationClientMessage.Init(connectionParams));
    }

    timer.schedule(CONNECTION_ACKNOWLEDGE_TIMEOUT_TIMER_TASK_ID, connectionAcknowledgeTimeoutTimerTask,
        CONNECTION_ACKNOWLEDGE_TIMEOUT);
  }

  void onConnectionAcknowledgeTimeout() {
    timer.cancelTask(CONNECTION_ACKNOWLEDGE_TIMEOUT_TIMER_TASK_ID);
    dispatcher.execute(new Runnable() {
      @Override public void run() {
        onTransportFailure(new ApolloNetworkException("Subscription server is not responding"));
      }
    });
  }

  void onInactivityTimeout() {
    timer.cancelTask(INACTIVITY_TIMEOUT_TIMER_TASK_ID);
    dispatcher.execute(new Runnable() {
      @Override public void run() {
        disconnect(false);
      }
    });
  }

  void onTransportFailure(Throwable t) {
    Map<String, SubscriptionRecord> subscriptions;
    synchronized (this) {
      subscriptions = this.subscriptions;
      disconnect(true);
    }

    for (SubscriptionRecord record : subscriptions.values()) {
      record.notifyOnNetworkError(t);
    }
  }

  void onOperationServerMessage(OperationServerMessage message) {
    if (message instanceof OperationServerMessage.ConnectionAcknowledge) {
      onConnectionAcknowledgeServerMessage();
    } else if (message instanceof OperationServerMessage.Data) {
      onOperationDataServerMessage((OperationServerMessage.Data) message);
    } else if (message instanceof OperationServerMessage.Error) {
      onErrorServerMessage((OperationServerMessage.Error) message);
    } else if (message instanceof OperationServerMessage.Complete) {
      onCompleteServerMessage((OperationServerMessage.Complete) message);
    } else if (message instanceof OperationServerMessage.ConnectionError) {
      disconnect(true);
    }
  }

  void disconnect(boolean force) {
    synchronized (this) {
      if (force || subscriptions.isEmpty()) {
        transport.disconnect(new OperationClientMessage.Terminate());
        state = State.DISCONNECTED;
        subscriptions = new LinkedHashMap<>();
      }
    }
  }

  private void startInactivityTimer() {
    timer.schedule(INACTIVITY_TIMEOUT_TIMER_TASK_ID, inactivityTimeoutTimerTask, INACTIVITY_TIMEOUT);
  }

  @SuppressWarnings("unchecked")
  private void onOperationDataServerMessage(OperationServerMessage.Data message) {
    String subscriptionId = message.id != null ? message.id : "";
    SubscriptionRecord subscriptionRecord;
    synchronized (this) {
      subscriptionRecord = subscriptions.get(subscriptionId);
    }

    if (subscriptionRecord != null) {
      ResponseFieldMapper responseFieldMapper = responseFieldMapperFactory.create(subscriptionRecord.subscription);
      OperationResponseParser parser = new OperationResponseParser(subscriptionRecord.subscription, responseFieldMapper,
          scalarTypeAdapters);

      Response response;
      try {
        response = parser.parse(message.payload);
      } catch (Exception e) {
        subscriptionRecord = removeSubscriptionById(subscriptionId);
        if (subscriptionRecord != null) {
          subscriptionRecord.notifyOnError(new ApolloSubscriptionException("Failed to parse server message", e));
        }
        return;
      }

      subscriptionRecord.notifyOnResponse(response);
    }
  }

  private void onConnectionAcknowledgeServerMessage() {
    timer.cancelTask(CONNECTION_ACKNOWLEDGE_TIMEOUT_TIMER_TASK_ID);
    synchronized (this) {
      state = State.ACTIVE;
      for (Map.Entry<String, SubscriptionRecord> entry : subscriptions.entrySet()) {
        String subscriptionId = entry.getKey();
        Subscription<?, ?, ?> subscription = entry.getValue().subscription;
        transport.send(new OperationClientMessage.Start(subscriptionId, subscription, scalarTypeAdapters));
      }
    }
  }

  private void onErrorServerMessage(OperationServerMessage.Error message) {
    String subscriptionId = message.id != null ? message.id : "";
    SubscriptionRecord subscriptionRecord = removeSubscriptionById(subscriptionId);
    if (subscriptionRecord != null) {
      subscriptionRecord.notifyOnError(new ApolloSubscriptionServerException(message.payload));
    }
  }

  private void onCompleteServerMessage(OperationServerMessage.Complete message) {
    String subscriptionId = message.id != null ? message.id : "";
    SubscriptionRecord subscriptionRecord = removeSubscriptionById(subscriptionId);
    if (subscriptionRecord != null) {
      subscriptionRecord.notifyOnCompleted();
    }
  }

  private SubscriptionRecord removeSubscriptionById(String subscriptionId) {
    SubscriptionRecord subscriptionRecord;
    synchronized (this) {
      subscriptionRecord = subscriptions.remove(subscriptionId);
      if (subscriptions.isEmpty()) {
        startInactivityTimer();
      }
    }
    return subscriptionRecord;
  }

  static String idForSubscription(Subscription<?, ?, ?> subscription) {
    return subscription.operationId() + "$" + subscription.variables().valueMap().hashCode();
  }

  enum State {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ACTIVE
  }

  private static class SubscriptionRecord {
    final Subscription<?, ?, ?> subscription;
    final SubscriptionManager.Callback<?> callback;

    SubscriptionRecord(Subscription<?, ?, ?> subscription, SubscriptionManager.Callback<?> callback) {
      this.subscription = subscription;
      this.callback = callback;
    }

    @SuppressWarnings("unchecked")
    void notifyOnResponse(Response response) {
      callback.onResponse(response);
    }

    void notifyOnError(ApolloSubscriptionException error) {
      callback.onError(error);
    }

    void notifyOnNetworkError(Throwable t) {
      callback.onNetworkError(t);
    }

    void notifyOnCompleted() {
      callback.onCompleted();
    }
  }

  private static final class SubscriptionTransportCallback implements SubscriptionTransport.Callback {
    private final RealSubscriptionManager delegate;
    private final Executor dispatcher;

    SubscriptionTransportCallback(RealSubscriptionManager delegate, Executor dispatcher) {
      this.delegate = delegate;
      this.dispatcher = dispatcher;
    }

    @Override
    public void onConnected() {
      dispatcher.execute(new Runnable() {
        @Override public void run() {
          delegate.onTransportConnected();
        }
      });
    }

    @Override
    public void onFailure(final Throwable t) {
      dispatcher.execute(new Runnable() {
        @Override public void run() {
          delegate.onTransportFailure(t);
        }
      });
    }

    @Override
    public void onMessage(final OperationServerMessage message) {
      dispatcher.execute(new Runnable() {
        @Override public void run() {
          delegate.onOperationServerMessage(message);
        }
      });
    }
  }

  static final class AutoReleaseTimer {
    final Map<Integer, TimerTask> tasks = new LinkedHashMap<>();
    Timer timer;

    void schedule(final int taskId, final Runnable task, long delay) {
      TimerTask timerTask = new TimerTask() {
        @Override public void run() {
          try {
            task.run();
          } finally {
            cancelTask(taskId);
          }
        }
      };

      synchronized (this) {
        TimerTask previousTimerTask = tasks.put(taskId, timerTask);
        if (previousTimerTask != null) {
          previousTimerTask.cancel();
        }

        if (timer == null) {
          timer = new Timer("Subscription SmartTimer", true);
        }
      }

      timer.schedule(timerTask, delay);
    }

    void cancelTask(int taskId) {
      synchronized (this) {
        TimerTask timerTask = tasks.remove(taskId);
        if (timerTask != null) {
          timerTask.cancel();
        }

        if (tasks.isEmpty() && timer != null) {
          timer.cancel();
          timer = null;
        }
      }
    }
  }
}
