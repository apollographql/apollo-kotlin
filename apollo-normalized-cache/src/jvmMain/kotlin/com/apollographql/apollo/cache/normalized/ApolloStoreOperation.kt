package com.apollographql.apollo.cache.normalized

import com.apollographql.apollo.exception.ApolloException
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Apollo store operation to be performed.
 *
 *
 * This class is a wrapper around operation to be performed on [ApolloStore]. Due to the fact that any operation
 * can potentially include SQLite instruction, any operation on [ApolloStore] must be performed in background
 * thread. Use [.enqueue] to schedule such operation in the dispatcher with a callback to get results.
 *
 *
 * @param <T> result type for this operation
</T> */
abstract class ApolloStoreOperation<T: Any> protected constructor(private val dispatcher: Executor) {
  private val callback = AtomicReference<Callback<T>?>()
  private val executed = AtomicBoolean()

  protected abstract fun perform(): T

  /**
   * Execute store operation
   *
   *
   * **NOTE: this is a sync operation, proceed with a caution as it may include SQLite instruction****
   ** *
   *
   * @throws [ApolloException] in case of any errors
   */
  @Throws(ApolloException::class)
  fun execute(): T {
    checkIfExecuted()
    return try {
      perform()
    } catch (e: Exception) {
      throw ApolloException("Failed to perform store operation", e)
    }
  }

  /**
   * Execute store operation and return null if it fails
   */
  fun executeOrNull(): T? {
    checkIfExecuted()
    return try {
      perform()
    } catch (e: Exception) {
      null
    }
  }


  /**
   * Schedules operation to be executed in dispatcher
   *
   * @param callback to be notified about operation result
   */
  open fun enqueue(callback: Callback<T>?) {
    checkIfExecuted()
    this.callback.set(callback)
    dispatcher.execute(Runnable {
      val result: T = try {
        perform()
      } catch (e: Exception) {
        notifyFailure(ApolloException("Failed to perform store operation", e))
        return@Runnable
      }
      notifySuccess(result)
    })
  }

  private fun notifySuccess(result: T) {
    val callback = callback.getAndSet(null) ?: return
    callback.onSuccess(result)
  }

  private fun notifyFailure(t: Throwable) {
    val callback = callback.getAndSet(null) ?: return
    callback.onFailure(t)
  }

  private fun checkIfExecuted() {
    check(executed.compareAndSet(false, true)) { "Already Executed" }
  }

  /**
   * Operation result callback
   *
   * @param <T> result type
  </T> */
  interface Callback<T> {
    fun onSuccess(result: T)
    fun onFailure(t: Throwable)
  }

  companion object {
    @JvmStatic
    fun <T: Any> emptyOperation(result: T): ApolloStoreOperation<T> {
      return object : ApolloStoreOperation<T>(emptyExecutor()) {
        override fun perform(): T {
          return result
        }

        override fun enqueue(callback: Callback<T>?) {
          callback?.onSuccess(result)
        }
      }
    }

    fun <T: Any> errorOperation(): ApolloStoreOperation<T> {
      return object : ApolloStoreOperation<T>(emptyExecutor()) {
        override fun perform(): T {
          throw Exception()
        }

        override fun enqueue(callback: Callback<T>?) {
          callback?.onFailure(Exception())
        }
      }
    }

    @JvmStatic
    fun emptyExecutor() = Executor { }
  }

}