package com.apollographql.apollo.internal.subscription;

import com.apollographql.apollo.api.Error;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.CustomScalarAdapters;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.api.internal.ResponseFieldMapper;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.internal.ResponseFieldMapperFactory;
import com.apollographql.apollo.cache.normalized.internal.ResponseNormalizer;
import com.apollographql.apollo.response.OperationResponseParser;
import com.apollographql.apollo.subscription.OnSubscriptionManagerStateChangeListener;
import com.apollographql.apollo.subscription.OperationClientMessage;
import com.apollographql.apollo.subscription.OperationServerMessage;
import com.apollographql.apollo.subscription.SubscriptionConnectionParamsProvider;
import com.apollographql.apollo.subscription.SubscriptionManagerState;
import com.apollographql.apollo.subscription.SubscriptionTransport;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
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
  static final String PROTOCOL_NEGOTIATION_ERROR_NOT_FOUND = "PersistedQueryNotFound";
  static final String PROTOCOL_NEGOTIATION_ERROR_NOT_SUPPORTED = "PersistedQueryNotSupported";

  Map<UUID, SubscriptionRecord> subscriptions = new LinkedHashMap<>();
  volatile SubscriptionManagerState state = SubscriptionManagerState.DISCONNECTED;
  final AutoReleaseTimer timer = new AutoReleaseTimer();

  private final CustomScalarAdapters customScalarAdapters;
  private final SubscriptionTransport transport;
  private final SubscriptionConnectionParamsProvider connectionParams;
  private final Executor dispatcher;
  private final long connectionHeartbeatTimeoutMs;
  private final Function0<ResponseNormalizer<Map<String, Object>>> responseNormalizer;
  private final ResponseFieldMapperFactory responseFieldMapperFactory = new ResponseFieldMapperFactory();
  private final Runnable connectionAcknowledgeTimeoutTimerTask = new Runnable() {
    @Override
    public void run() {
      onConnectionAcknowledgeTimeout();
    }
  };
  private final Runnable inactivityTimeoutTimerTask = new Runnable() {
    @Override
    public void run() {
      onInactivityTimeout();
    }
  };
  private final Runnable connectionHeartbeatTimeoutTimerTask = new Runnable() {
    @Override
    public void run() {
      onConnectionHeartbeatTimeout();
    }
  };
  private final List<OnSubscriptionManagerStateChangeListener> onStateChangeListeners = new CopyOnWriteArrayList<>();
  private final boolean autoPersistSubscription;

  public RealSubscriptionManager(@NotNull CustomScalarAdapters customScalarAdapters,
      @NotNull final SubscriptionTransport.Factory transportFactory, @NotNull SubscriptionConnectionParamsProvider connectionParams,
      @NotNull final Executor dispatcher, long connectionHeartbeatTimeoutMs,
      @NotNull Function0<ResponseNormalizer<Map<String, Object>>> responseNormalizer, boolean autoPersistSubscription) {
    checkNotNull(customScalarAdapters, "scalarTypeAdapters == null");
    checkNotNull(transportFactory, "transportFactory == null");
    checkNotNull(dispatcher, "dispatcher == null");
    checkNotNull(responseNormalizer, "responseNormalizer == null");

    this.customScalarAdapters = checkNotNull(customScalarAdapters, "scalarTypeAdapters == null");
    this.connectionParams = checkNotNull(connectionParams, "connectionParams == null");
    this.transport = transportFactory.create(new SubscriptionTransportCallback(this, dispatcher));
    this.dispatcher = dispatcher;
    this.connectionHeartbeatTimeoutMs = connectionHeartbeatTimeoutMs;
    this.responseNormalizer = responseNormalizer;
    this.autoPersistSubscription = autoPersistSubscription;
  }

  @Override
  public <D extends Operation.Data> void subscribe(@NotNull final Subscription<D, ?> subscription, @NotNull final SubscriptionManager.Callback<D> callback) {
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
    final SubscriptionManagerState oldState;
    synchronized (this) {
      oldState = state;
      if (state == SubscriptionManagerState.STOPPED) {
        state = SubscriptionManagerState.DISCONNECTED;
      }
    }

    notifyStateChanged(oldState, state);
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
    dispatcher.execute(new Runnable() {
      @Override public void run() {
        doStop();
      }
    });
  }

  @Override public SubscriptionManagerState getState() {
    return state;
  }

  @Override public void addOnStateChangeListener(@NotNull OnSubscriptionManagerStateChangeListener onStateChangeListener) {
    onStateChangeListeners.add(checkNotNull(onStateChangeListener, "onStateChangeListener == null"));
  }

  @Override public void removeOnStateChangeListener(@NotNull OnSubscriptionManagerStateChangeListener onStateChangeListener) {
    onStateChangeListeners.remove(checkNotNull(onStateChangeListener, "onStateChangeListener == null"));
  }

  void doSubscribe(Subscription subscription, SubscriptionManager.Callback callback) {
    final SubscriptionManagerState oldState;
    synchronized (this) {
      oldState = state;

      if (state != SubscriptionManagerState.STOPPING && state != SubscriptionManagerState.STOPPED) {
        timer.cancelTask(INACTIVITY_TIMEOUT_TIMER_TASK_ID);

        final UUID subscriptionId = UUID.randomUUID();
        subscriptions.put(subscriptionId, new SubscriptionRecord(subscriptionId, subscription, callback));

        if (state == SubscriptionManagerState.DISCONNECTED) {
          state = SubscriptionManagerState.CONNECTING;
          transport.connect();
        } else if (state == SubscriptionManagerState.ACTIVE) {
          transport.send(
              new OperationClientMessage.Start(subscriptionId.toString(), subscription, customScalarAdapters, autoPersistSubscription, false)
          );
        }
      }
    }

    if (oldState == SubscriptionManagerState.STOPPING || oldState == SubscriptionManagerState.STOPPED) {
      callback.onError(new ApolloSubscriptionException(
          "Illegal state: " + state.name() + " for subscriptions to be created."
              + " SubscriptionManager.start() must be called to re-enable subscriptions."));
    } else if (oldState == SubscriptionManagerState.CONNECTED) {
      callback.onConnected();
    }

    notifyStateChanged(oldState, state);
  }

  void doUnsubscribe(Subscription subscription) {
    synchronized (this) {
      SubscriptionRecord subscriptionRecord = null;
      for (SubscriptionRecord record : subscriptions.values()) {
        if (record.subscription == subscription) {
          subscriptionRecord = record;
        }
      }

      if (subscriptionRecord != null) {
        subscriptions.remove(subscriptionRecord.id);
        if (state == SubscriptionManagerState.ACTIVE || state == SubscriptionManagerState.STOPPING) {
          transport.send(new OperationClientMessage.Stop(subscriptionRecord.id.toString()));
        }
      }

      if (subscriptions.isEmpty() && state != SubscriptionManagerState.STOPPING) {
        startInactivityTimer();
      }
    }
  }

  void doStop() {
    final Collection<SubscriptionRecord> subscriptionRecords;
    final SubscriptionManagerState oldState;
    synchronized (this) {
      oldState = state;
      state = SubscriptionManagerState.STOPPING;

      subscriptionRecords = subscriptions.values();

      if (oldState == SubscriptionManagerState.ACTIVE) {
        for (SubscriptionRecord subscriptionRecord : subscriptionRecords) {
          transport.send(new OperationClientMessage.Stop(subscriptionRecord.id.toString()));
        }
      }

      state = SubscriptionManagerState.STOPPED;

      transport.disconnect(new OperationClientMessage.Terminate());
      subscriptions = new LinkedHashMap<>();
    }

    for (SubscriptionRecord record : subscriptionRecords) {
      record.notifyOnCompleted();
    }

    notifyStateChanged(oldState, SubscriptionManagerState.STOPPING);
    notifyStateChanged(SubscriptionManagerState.STOPPING, state);
  }

  void onTransportConnected() {
    final Collection<SubscriptionRecord> subscriptionRecords = new ArrayList<>();

    final SubscriptionManagerState oldState;
    synchronized (this) {
      oldState = state;

      if (state == SubscriptionManagerState.CONNECTING) {
        subscriptionRecords.addAll(subscriptions.values());
        state = SubscriptionManagerState.CONNECTED;
        transport.send(new OperationClientMessage.Init(connectionParams.provide()));
      }

      if (state == SubscriptionManagerState.CONNECTED) {
        timer.schedule(CONNECTION_ACKNOWLEDGE_TIMEOUT_TIMER_TASK_ID, connectionAcknowledgeTimeoutTimerTask, CONNECTION_ACKNOWLEDGE_TIMEOUT);
      }
    }

    for (SubscriptionRecord record : subscriptionRecords) {
      record.callback.onConnected();
    }

    notifyStateChanged(oldState, state);
  }

  void onConnectionAcknowledgeTimeout() {
    timer.cancelTask(CONNECTION_ACKNOWLEDGE_TIMEOUT_TIMER_TASK_ID);
    dispatcher.execute(new Runnable() {
      @Override
      public void run() {
        onTransportFailure(new ApolloNetworkException("Subscription server is not responding"));
      }
    });
  }

  void onInactivityTimeout() {
    timer.cancelTask(INACTIVITY_TIMEOUT_TIMER_TASK_ID);
    dispatcher.execute(new Runnable() {
      @Override
      public void run() {
        disconnect(false);
      }
    });
  }

  void onTransportFailure(Throwable t) {
    Collection<SubscriptionRecord> subscriptionRecords = disconnect(true);
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
  Collection<SubscriptionRecord> disconnect(boolean force) {
    final SubscriptionManagerState oldState;
    final Collection<SubscriptionRecord> subscriptionRecords;
    synchronized (this) {
      oldState = state;
      subscriptionRecords = subscriptions.values();
      if (force || subscriptions.isEmpty()) {
        transport.disconnect(new OperationClientMessage.Terminate());
        state = (state == SubscriptionManagerState.STOPPING) ? SubscriptionManagerState.STOPPED : SubscriptionManagerState.DISCONNECTED;
        subscriptions = new LinkedHashMap<>();
      }
    }

    notifyStateChanged(oldState, state);

    return subscriptionRecords;
  }

  @Override
  public void reconnect() {
    final SubscriptionManagerState oldState;
    synchronized (this) {
      oldState = state;
      state = SubscriptionManagerState.DISCONNECTED;
      transport.disconnect(new OperationClientMessage.Terminate());
      state = SubscriptionManagerState.CONNECTING;
      transport.connect();
    }

    notifyStateChanged(oldState, SubscriptionManagerState.DISCONNECTED);
    notifyStateChanged(SubscriptionManagerState.DISCONNECTED, SubscriptionManagerState.CONNECTING);
  }

  void onConnectionHeartbeatTimeout() {
    reconnect();
  }

  void onConnectionClosed() {
    Collection<SubscriptionRecord> subscriptionRecords;
    final SubscriptionManagerState oldState;
    synchronized (this) {
      oldState = state;

      subscriptionRecords = subscriptions.values();
      state = SubscriptionManagerState.DISCONNECTED;
      subscriptions = new LinkedHashMap<>();
    }

    for (SubscriptionRecord record : subscriptionRecords) {
      record.callback.onTerminated();
    }

    notifyStateChanged(oldState, state);
  }

  private void resetConnectionKeepAliveTimerTask() {
    if (connectionHeartbeatTimeoutMs <= 0) {
      return;
    }
    synchronized (this) {
      timer.schedule(CONNECTION_KEEP_ALIVE_TIMEOUT_TIMER_TASK_ID, connectionHeartbeatTimeoutTimerTask, connectionHeartbeatTimeoutMs);
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
      try {
        subscriptionRecord = subscriptions.get(UUID.fromString(subscriptionId));
      } catch (IllegalArgumentException e) {
        subscriptionRecord = null;
      }
    }

    if (subscriptionRecord != null) {
      ResponseNormalizer<Map<String, Object>> normalizer = responseNormalizer.invoke();
      ResponseFieldMapper responseFieldMapper = responseFieldMapperFactory.create(subscriptionRecord.subscription);
      OperationResponseParser parser = new OperationResponseParser(subscriptionRecord.subscription, responseFieldMapper,
          customScalarAdapters, normalizer);

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

      subscriptionRecord.notifyOnResponse(response, normalizer.records());
    }
  }

  private void onConnectionAcknowledgeServerMessage() {
    final SubscriptionManagerState oldState;
    synchronized (this) {
      oldState = state;

      timer.cancelTask(CONNECTION_ACKNOWLEDGE_TIMEOUT_TIMER_TASK_ID);

      if (state == SubscriptionManagerState.CONNECTED) {
        state = SubscriptionManagerState.ACTIVE;
        for (SubscriptionRecord subscriptionRecord : subscriptions.values()) {
          transport.send(
              new OperationClientMessage.Start(subscriptionRecord.id.toString(), subscriptionRecord.subscription, customScalarAdapters,
                  autoPersistSubscription, false)
          );
        }
      }
    }

    notifyStateChanged(oldState, state);
  }

  private void onErrorServerMessage(OperationServerMessage.Error message) {
    final String subscriptionId = message.id != null ? message.id : "";
    final SubscriptionRecord subscriptionRecord = removeSubscriptionById(subscriptionId);
    if (subscriptionRecord == null) {
      return;
    }

    final boolean resendSubscriptionWithDocument;
    if (autoPersistSubscription) {
      Error error = OperationResponseParser.parseError(message.payload);
      resendSubscriptionWithDocument = PROTOCOL_NEGOTIATION_ERROR_NOT_FOUND.equalsIgnoreCase(error.getMessage())
          || PROTOCOL_NEGOTIATION_ERROR_NOT_SUPPORTED.equalsIgnoreCase(error.getMessage());
    } else {
      resendSubscriptionWithDocument = false;
    }

    if (resendSubscriptionWithDocument) {
      synchronized (this) {
        subscriptions.put(subscriptionRecord.id, subscriptionRecord);
        transport.send(new OperationClientMessage.Start(
            subscriptionRecord.id.toString(), subscriptionRecord.subscription, customScalarAdapters, true, true
        ));
      }
    } else {
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
      try {
        subscriptionRecord = subscriptions.remove(UUID.fromString(subscriptionId));
      } catch (IllegalArgumentException e) {
        subscriptionRecord = null;
      }

      if (subscriptions.isEmpty()) {
        startInactivityTimer();
      }
    }
    return subscriptionRecord;
  }

  private void notifyStateChanged(SubscriptionManagerState oldState, SubscriptionManagerState newState) {
    if (oldState == newState) {
      return;
    }

    for (OnSubscriptionManagerStateChangeListener onStateChangeListener : onStateChangeListeners) {
      onStateChangeListener.onStateChange(oldState, newState);
    }
  }

  private static class SubscriptionRecord {
    final UUID id;
    final Subscription<?, ?> subscription;
    final SubscriptionManager.Callback<?> callback;

    SubscriptionRecord(UUID id, Subscription<?, ?> subscription, SubscriptionManager.Callback<?> callback) {
      this.id = id;
      this.subscription = subscription;
      this.callback = callback;
    }

    @SuppressWarnings("unchecked")
    void notifyOnResponse(Response response, Collection<Record> cacheRecords) {
      callback.onResponse(new SubscriptionResponse(subscription, response, cacheRecords));
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
        @Override
        public void run() {
          delegate.onTransportConnected();
        }
      });
    }

    @Override
    public void onFailure(final Throwable t) {
      dispatcher.execute(new Runnable() {
        @Override
        public void run() {
          delegate.onTransportFailure(t);
        }
      });
    }

    @Override
    public void onMessage(final OperationServerMessage message) {
      dispatcher.execute(new Runnable() {
        @Override
        public void run() {
          delegate.onOperationServerMessage(message);
        }
      });
    }

    @Override
    public void onClosed() {
      dispatcher.execute(new Runnable() {
        @Override
        public void run() {
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
        @Override
        public void run() {
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
}
