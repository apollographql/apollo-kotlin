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

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

@SuppressWarnings("WeakerAccess")
public final class RealSubscriptionManager implements SubscriptionManager {
  static final int CONNECTION_ACKNOWLEDGE_TIMEOUT_TIMER_TASK_ID = 1;
  static final int INACTIVITY_TIMEOUT_TIMER_TASK_ID = 2;
  static final int CONNECTION_KEEP_ALIVE_TIMEOUT_TIMER_TASK_ID = 3;
  static final long CONNECTION_ACKNOWLEDGE_TIMEOUT = TimeUnit.SECONDS.toMillis(5);
  static final long INACTIVITY_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

  Map<String, SubscriptionRecord> subscriptions = new LinkedHashMap<>();
  volatile State state = State.DISCONNECTED;
  final AutoReleaseTimer timer = new AutoReleaseTimer();

  private final ScalarTypeAdapters scalarTypeAdapters;
  private final SubscriptionTransport transport;
  private Map<String, Object> connectionParams;
  private final Executor dispatcher;
  private final long connectionHeartbeatTimeoutMs;
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
  private final Runnable connectionHeartbeatTimeoutTimerTask = new Runnable() {
    @Override public void run() {
      onConnectionHeartbeatTimeout();
    }
  };
  private final List<OnStateChangeListener> onStateChangeListeners = new CopyOnWriteArrayList<>();

  public RealSubscriptionManager(@NotNull ScalarTypeAdapters scalarTypeAdapters,
      @NotNull final SubscriptionTransport.Factory transportFactory, @NotNull Map<String, Object> connectionParams,
      @NotNull final Executor dispatcher, long connectionHeartbeatTimeoutMs) {
    checkNotNull(scalarTypeAdapters, "scalarTypeAdapters == null");
    checkNotNull(transportFactory, "transportFactory == null");
    checkNotNull(dispatcher, "dispatcher == null");

    this.scalarTypeAdapters = checkNotNull(scalarTypeAdapters, "scalarTypeAdapters == null");
    this.connectionParams = checkNotNull(connectionParams, "connectionParams == null");
    this.transport = transportFactory.create(new SubscriptionTransportCallback(this, dispatcher));
    this.dispatcher = dispatcher;
    this.connectionHeartbeatTimeoutMs = connectionHeartbeatTimeoutMs;
  }

  @Override
  public <T> void subscribe(@NotNull final Subscription<?, T, ?> subscription,
      @NotNull final SubscriptionManager.Callback<T> callback) {
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
  public void unsubscribe(@NotNull final Subscription subscription) {
    checkNotNull(subscription, "subscription == null");
    dispatcher.execute(new Runnable() {
      @Override
      public void run() {
        doUnsubscribe(subscription);
      }
    });
  }

  /**
   * Set the {@link RealSubscriptionManager} to a connectible state. It is safe to call this method
   * at any time.  Does nothing unless we are in the stopped state.
   */
  @Override
  public void start() {
    synchronized (this) {
      if (state == State.STOPPED) {
        setStateAndNotify(State.DISCONNECTED);
      }
    }
  }

  /**
   * Unsubscribe from all active subscriptions, and disconnect the web socket.  It will not be
   * possible to add new subscriptions while the {@link SubscriptionManager} is stopping
   * because we check the state in {@link #doSubscribe(Subscription, Callback)}.  We pass true to
   * {@link #disconnect(boolean)} because we want to disconnect even if, somehow, a new subscription
   * is added while or after we are doing the {@link #doUnsubscribe(Subscription)} loop.
   */
  @Override
  public void stop() {
    synchronized (this) {
      setStateAndNotify(State.STOPPING);
      for (SubscriptionRecord eachSubscriptionRecord : subscriptions.values()) {
        doUnsubscribe(eachSubscriptionRecord.subscription);
      }
      disconnect(true);
    }
  }

  public void addOnStateChangeListener(@NotNull OnStateChangeListener onStateChangeListener) {
    onStateChangeListeners.add(checkNotNull(onStateChangeListener, "onStateChangeListener == null"));
  }

  public void removeOnStateChangeListener(@NotNull OnStateChangeListener onStateChangeListener) {
    onStateChangeListeners.remove(checkNotNull(onStateChangeListener, "onStateChangeListener == null"));
  }

  void doSubscribe(Subscription subscription, SubscriptionManager.Callback callback) {
    if (state == State.STOPPING || state == State.STOPPED) {
      callback.onError(new ApolloSubscriptionException(
          "Illegal state: " + state.name() + " for subscriptions to be created."
          + " SubscriptionManager.start() must be called to re-enable subscriptions."));
      return;
    }
    timer.cancelTask(INACTIVITY_TIMEOUT_TIMER_TASK_ID);

    String subscriptionId = idForSubscription(subscription);
    synchronized (this) {
      if (subscriptions.containsKey(subscriptionId)) {
        callback.onError(new ApolloSubscriptionException("Already subscribed"));
        return;
      }

      subscriptions.put(subscriptionId, new SubscriptionRecord(subscription, callback));
      if (state == State.DISCONNECTED) {
        setStateAndNotify(State.CONNECTING);
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
      if ((subscriptionRecord != null) && (state == State.ACTIVE || state == State.STOPPING)) {
        transport.send(new OperationClientMessage.Stop(subscriptionId));
      }

      if (subscriptions.isEmpty() && state != State.STOPPING) {
        startInactivityTimer();
      }
    }
  }

  void onTransportConnected() {
    synchronized (this) {
      setStateAndNotify(State.CONNECTED);
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
    Collection<SubscriptionRecord> subscriptionRecords;
    synchronized (this) {
      subscriptionRecords = subscriptions.values();
      disconnect(true);
    }

    for (SubscriptionRecord record : subscriptionRecords) {
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
    } else if (message instanceof OperationServerMessage.ConnectionKeepAlive) {
      resetConnectionKeepAliveTimerTask();
    }
  }

  /**
   * Disconnect the web socket and update the state.  If we are stopping, set the state to
   * {@link State#STOPPED} so that new subscription requests will <b>not</b> automatically re-open
   * the web socket.  If we are not stopping, set the state to {@link State#DISCONNECTED} so that
   * new subscription requests <b>will</b> automatically re-open the web socket.
   *
   * @param force if true, always disconnect web socket, regardless of the status of {@link #subscriptions}
   */
  void disconnect(boolean force) {
    synchronized (this) {
      if (force || subscriptions.isEmpty()) {
        transport.disconnect(new OperationClientMessage.Terminate());
        State disconnectionState = (state == State.STOPPING) ? State.STOPPED : State.DISCONNECTED;
        setStateAndNotify(disconnectionState);
        subscriptions = new LinkedHashMap<>();
      }
    }
  }

  void onConnectionHeartbeatTimeout() {
    synchronized (this) {
      transport.disconnect(new OperationClientMessage.Terminate());
      setStateAndNotify(State.DISCONNECTED);

      setStateAndNotify(State.CONNECTING);
      transport.connect();
    }
  }

  void onConnectionClosed() {
    Collection<SubscriptionRecord> subscriptionRecords;
    synchronized (this) {
      subscriptionRecords = subscriptions.values();
      setStateAndNotify(State.DISCONNECTED);
      subscriptions = new LinkedHashMap<>();
    }

    for (SubscriptionRecord record : subscriptionRecords) {
      record.callback.onTerminated();
    }
  }

  private void resetConnectionKeepAliveTimerTask() {
    if (connectionHeartbeatTimeoutMs <= 0) {
      return;
    }
    synchronized (this) {
      timer.schedule(CONNECTION_KEEP_ALIVE_TIMEOUT_TIMER_TASK_ID, connectionHeartbeatTimeoutTimerTask,
          connectionHeartbeatTimeoutMs);
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
      setStateAndNotify(State.ACTIVE);
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

  private void setStateAndNotify(State newState) {
    State oldState = state;
    state = newState;
    for (OnStateChangeListener onStateChangeListener : onStateChangeListeners) {
      onStateChangeListener.onStateChange(oldState, newState);
    }
  }

  static String idForSubscription(Subscription<?, ?, ?> subscription) {
    return subscription.operationId() + "$" + subscription.variables().valueMap().hashCode();
  }

  enum State {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ACTIVE,
    STOPPING,
    STOPPED
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

    @Override
    public void onClosed() {
      dispatcher.execute(new Runnable() {
        @Override public void run() {
          delegate.onConnectionClosed();
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

        timer.schedule(timerTask, delay);
      }

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

  interface OnStateChangeListener {
    void onStateChange(State fromState, State toState);
  }
}
