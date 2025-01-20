/**
 * A very minimal implementation of Arrow raise DSL, copied from https://github.com/arrow-kt/arrow/blob/27330d7c7388117b4cb8f9a87ef85e076644644d/arrow-libs/core/arrow-core/src/commonMain/kotlin/arrow/core
 *
 * Documentation has been stripped and all symbols moved to a new package name and marked as internal.
 * Behavioural modifications are indicated with the `XXX:` comment
 */
@file:OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)

package com.apollographql.apollo.execution.internal

import com.apollographql.apollo.execution.internal.Either.Left
import com.apollographql.apollo.execution.internal.Either.Right
import kotlinx.atomicfu.atomic
import kotlin.Result.Companion.failure
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.InvocationKind.AT_MOST_ONCE
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.coroutines.cancellation.CancellationException
import kotlin.experimental.ExperimentalTypeInference
import kotlin.jvm.JvmName

internal sealed class Either<out A, out B> {

  fun isLeft(): Boolean {
    contract {
      returns(true) implies (this@Either is Left<A>)
      returns(false) implies (this@Either is Right<B>)
    }
    return this@Either is Left<A>
  }

  fun isRight(): Boolean {
    contract {
      returns(true) implies (this@Either is Right<B>)
      returns(false) implies (this@Either is Left<A>)
    }
    return this@Either is Right<B>
  }

  inline fun isLeft(predicate: (A) -> Boolean): Boolean {
    contract {
      returns(true) implies (this@Either is Left<A>)
      callsInPlace(predicate, InvocationKind.AT_MOST_ONCE)
    }
    return this@Either is Left<A> && predicate(value)
  }

  inline fun isRight(predicate: (B) -> Boolean): Boolean {
    contract {
      returns(true) implies (this@Either is Right<B>)
      callsInPlace(predicate, InvocationKind.AT_MOST_ONCE)
    }
    return this@Either is Right<B> && predicate(value)
  }

  internal inline fun <C> fold(ifLeft: (left: A) -> C, ifRight: (right: B) -> C): C {
    contract {
      callsInPlace(ifLeft, InvocationKind.AT_MOST_ONCE)
      callsInPlace(ifRight, InvocationKind.AT_MOST_ONCE)
    }
    return when (this) {
      is Right -> ifRight(value)
      is Left -> ifLeft(value)
    }
  }

  fun swap(): Either<B, A> =
    fold({ Right(it) }, { Left(it) })

  inline fun <C> map(f: (right: B) -> C): Either<A, C> {
    contract {
      callsInPlace(f, InvocationKind.AT_MOST_ONCE)
    }
    return flatMap { Right(f(it)) }
  }

  inline fun <C> mapLeft(f: (A) -> C): Either<C, B> {
    contract { callsInPlace(f, InvocationKind.AT_MOST_ONCE) }
    return when (this) {
      is Left -> Left(f(value))
      is Right -> Right(value)
    }
  }

  inline fun onRight(action: (right: B) -> Unit): Either<A, B> {
    contract {
      callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return also { if (it.isRight()) action(it.value) }
  }

  inline fun onLeft(action: (left: A) -> Unit): Either<A, B> {
    contract {
      callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return also { if (it.isLeft()) action(it.value) }
  }

  fun getOrNull(): B? {
    contract {
      returns(null) implies (this@Either is Left<A>)
      returnsNotNull() implies (this@Either is Right<B>)
    }
    return getOrElse { null }
  }

  fun leftOrNull(): A? {
    contract {
      returnsNotNull() implies (this@Either is Left<A>)
      returns(null) implies (this@Either is Right<B>)
    }
    return fold(::identity) { null }
  }

  data class Left<out A> constructor(val value: A) : Either<A, Nothing>() {
    override fun toString(): String = "Either.Left($value)"

    public companion object
  }

  data class Right<out B> constructor(val value: B) : Either<Nothing, B>() {
    override fun toString(): String = "Either.Right($value)"

    public companion object {
      @PublishedApi
      internal val unit: Either<Nothing, Unit> = Right(Unit)
    }
  }

  override fun toString(): String = fold(
    { "Either.Left($it)" },
    { "Either.Right($it)" }
  )
}

internal inline fun <A, B, C> Either<A, B>.flatMap(f: (right: B) -> Either<A, C>): Either<A, C> {
  contract { callsInPlace(f, InvocationKind.AT_MOST_ONCE) }
  return when (this) {
    is Right -> f(this.value)
    is Left -> this
  }
}

internal inline infix fun <A, B> Either<A, B>.getOrElse(default: (A) -> B): B {
  contract { callsInPlace(default, InvocationKind.AT_MOST_ONCE) }
  return when (this) {
    is Left -> default(this.value)
    is Right -> this.value
  }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <A> identity(a: A): A = a

internal fun <A> A.left(): Either<A, Nothing> = Left(this)

internal fun <A> A.right(): Either<Nothing, A> = Right(this)

@DslMarker
internal annotation class RaiseDSL

internal interface Raise<in Error> {
  @RaiseDSL
  fun raise(r: Error): Nothing
}

internal inline fun <Error, A> either(@BuilderInference block: Raise<Error>.() -> A): Either<Error, A> {
  contract { callsInPlace(block, AT_MOST_ONCE) }
  return fold(block, { Left(it) }, { Right(it) })
}

@JvmName("_foldOrThrow")
internal inline fun <Error, A, B> fold(
  @BuilderInference block: Raise<Error>.() -> A,
  recover: (error: Error) -> B,
  transform: (value: A) -> B,
): B {
  contract {
    callsInPlace(block, AT_MOST_ONCE)
    callsInPlace(recover, AT_MOST_ONCE)
    callsInPlace(transform, AT_MOST_ONCE)
  }
  return fold(block, { throw it }, recover, transform)
}

@JvmName("_fold")
internal inline fun <Error, A, B> fold(
  @BuilderInference block: Raise<Error>.() -> A,
  catch: (throwable: Throwable) -> B,
  recover: (error: Error) -> B,
  transform: (value: A) -> B,
): B {
  contract {
    callsInPlace(block, AT_MOST_ONCE)
    callsInPlace(catch, AT_MOST_ONCE)
    callsInPlace(recover, AT_MOST_ONCE)
    callsInPlace(transform, AT_MOST_ONCE)
  }
  val raise = DefaultRaise(false)
  return try {
    val res = block(raise)
    raise.complete()
    transform(res)
  } catch (e: RaiseCancellationException) {
    raise.complete()
    recover(e.raisedOrRethrow(raise))
  } catch (e: Throwable) {
    raise.complete()
    catch(e.nonFatalOrThrow())
  }
}

internal class DefaultRaise(internal val isTraced: Boolean) : Raise<Any?> {
  private val isActive = atomic(true)

  internal fun complete(): Boolean = isActive.getAndSet(false)

  override fun raise(r: Any?): Nothing = when {
    isActive.value -> throw if (isTraced) Traced(r, this) else NoTrace(r, this)
    else -> throw RaiseLeakedException()
  }
}

internal sealed class RaiseCancellationException(
  internal val raised: Any?,
  internal val raise: Raise<Any?>
) : CancellationException(RaiseCancellationExceptionCaptured)

internal const val RaiseCancellationExceptionCaptured: String =
  "kotlin.coroutines.cancellation.CancellationException should never get swallowed. Always re-throw it if captured." +
      "This swallows the exception of Arrow's Raise, and leads to unexpected behavior." +
      "When working with Arrow prefer Either.catch or arrow.core.raise.catch to automatically rethrow CancellationException."

private class RaiseLeakedException : IllegalStateException(
  """
  'raise' or 'bind' was leaked outside of its context scope.
  Make sure all calls to 'raise' and 'bind' occur within the lifecycle of nullable { }, either { } or similar builders.
 
  See Arrow documentation on 'Typed errors' for further information.
  """.trimIndent()
)

@Suppress("UNCHECKED_CAST")
internal fun <R> CancellationException.raisedOrRethrow(raise: DefaultRaise): R =
  when {
    this is RaiseCancellationException && this.raise === raise -> raised as R
    else -> throw this
  }

internal fun Throwable.nonFatalOrThrow(): Throwable =
  if (NonFatal(this)) this else throw this

@RaiseDSL
internal inline fun <Error, OtherError, A> Raise<Error>.withError(
  transform: (OtherError) -> Error,
  @BuilderInference block: Raise<OtherError>.() -> A
): A {
  contract {
    callsInPlace(block, EXACTLY_ONCE)
  }
  recover({ return block(this) }) { raise(transform(it)) }
}

@RaiseDSL
internal inline fun <Error, A> recover(
  @BuilderInference block: Raise<Error>.() -> A,
  @BuilderInference recover: (error: Error) -> A,
): A {
  contract {
    callsInPlace(block, AT_MOST_ONCE)
    callsInPlace(recover, AT_MOST_ONCE)
  }
  return fold(block, { throw it }, recover, ::identity)
}

internal inline fun <A, B> Result<A>.flatMap(transform: (value: A) -> Result<B>): Result<B> {
  contract { callsInPlace(transform, AT_MOST_ONCE) }
  return map(transform).fold(::identity, ::failure)
}

/**
 * XXX: Arrow handles the JVM better here
 */
internal fun NonFatal(t: Throwable): Boolean =
  when (t) {
    is CancellationException -> false
    else -> true
  }

/**
 * XXX: Arrow handles the JVM better here
 */
internal class NoTrace(raised: Any?, raise: Raise<Any?>) : RaiseCancellationException(raised, raise)

internal class Traced(raised: Any?, raise: Raise<Any?>, override val cause: Traced? = null): RaiseCancellationException(raised, raise)
