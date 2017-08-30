package com.apollographql.apollo.cache.normalized;

import com.apollographql.apollo.exception.ApolloException;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

/**
 * Apollo store operation to be performed.
 * <p>
 * This class is a wrapper around operation to be performed on {@link ApolloStore}. Due to the fact that any operation
 * can potentially include SQLite instruction, any operation on {@link ApolloStore} must be performed in background
 * thread. Use {@link #enqueue(Callback)} to schedule such operation in the dispatcher with a callback to get results.
 * </p>
 *
 * @param <T> result type for this operation
 */
public abstract class ApolloStoreOperation<T> {

  public static <T> ApolloStoreOperation<T> emptyOperation(final T result) {
    return new ApolloStoreOperation<T>(null) {
      @Override protected T perform() {
        return result;
      }

      @Override public void enqueue(Callback<T> callback) {
        if (callback != null) {
          callback.onSuccess(result);
        }
      }
    };
  }

  private final Executor dispatcher;
  private AtomicReference<Callback<T>> callback = new AtomicReference<>();
  private final AtomicBoolean executed = new AtomicBoolean();

  protected ApolloStoreOperation(Executor dispatcher) {
    this.dispatcher = dispatcher;
  }

  protected abstract T perform();

  /**
   * Execute store operation
   * <p>
   * <b>NOTE: this is a sync operation, proceed with a caution as it may include SQLite instruction<b/>
   * </p>
   *
   * @throws {@link ApolloException} in case of any errors
   */
  public final T execute() throws ApolloException {
    checkIfExecuted();
    try {
      return perform();
    } catch (Exception e) {
      throw new ApolloException("Failed to perform store operation", e);
    }
  }

  /**
   * Schedules operation to be executed in dispatcher
   *
   * @param callback to be notified about operation result
   */
  public void enqueue(@Nullable final Callback<T> callback) {
    checkIfExecuted();
    this.callback.set(callback);
    dispatcher.execute(new Runnable() {
      @Override public void run() {
        T result;
        try {
          result = perform();
        } catch (Exception e) {
          notifyFailure(new ApolloException("Failed to perform store operation", e));
          return;
        }

        notifySuccess(result);
      }
    });
  }

  private void notifySuccess(T result) {
    Callback<T> callback = this.callback.getAndSet(null);
    if (callback == null) {
      return;
    }
    callback.onSuccess(result);
  }

  private void notifyFailure(Throwable t) {
    Callback<T> callback = this.callback.getAndSet(null);
    if (callback == null) {
      return;
    }
    callback.onFailure(t);
  }

  private void checkIfExecuted() {
    if (!executed.compareAndSet(false, true)) {
      throw new IllegalStateException("Already Executed");
    }
  }

  /**
   * Operation result callback
   *
   * @param <T> result type
   */
  interface Callback<T> {

    void onSuccess(T result);

    void onFailure(Throwable t);
  }
}
