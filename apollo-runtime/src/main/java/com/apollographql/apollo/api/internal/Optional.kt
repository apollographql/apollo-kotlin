/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.internal.Absent.Companion.withType
import com.apollographql.apollo.api.internal.Utils.__checkNotNull
import java.io.Serializable

/**
 * An immutable object that may contain a non-null reference to another object. Each instance of this type either
 * contains a non-null reference, or contains nothing (in which case we say that the reference is "absent"); it is never
 * said to "contain `null`".
 *
 *
 * A non-null `Optional<T>` reference can be used as a replacement for a nullable `T` reference. It
 * allows you to represent "a `T` that must be present" and a "a `T` that might be absent" as two distinct
 * types in your program, which can aid clarity.
 *
 *
 * Some uses of this class include
 *
 *   * As a method return type, as an alternative to returning `null` to indicate that no value was available
 *  * To distinguish between "unknown" (for example, not present in a map) and "known to have no value" (present in the
 * map, with value `Optional.absent()`)  * To wrap nullable references for storage in a collection that does not
 * support `null` (though there are
 * [
 * several other approaches to this](https://github.com/google/guava/wiki/LivingWithNullHostileCollections) that should be considered first)
 *
 *
 * A common alternative to using this class is to find or create a
 * suitable [null
 * object](http://en.wikipedia.org/wiki/Null_Object_pattern) for the type in question.
 *
 *
 * This class is not intended as a direct analogue of any existing "option" or "maybe" construct from other
 * programming environments, though it may bear some similarities.
 *
 *
 * **Comparison to `java.util.Optional` (JDK 8 and higher):** A new `Optional` class was added for
 * Java 8. The two classes are extremely similar, but incompatible (they cannot share a common supertype). *All*
 * known differences are listed either here or with the relevant methods below.
 *
 *   * This class is serializable; `java.util.Optional` is not.  * `java.util.Optional` has the
 * additional methods `ifPresent`, `filter`, `flatMap`, and `orElseThrow`.  * `java.util`
 * offers the primitive-specialized versions `OptionalInt`, `OptionalLong` and `OptionalDouble`, the
 * use of which is recommended; Guava does not have these.
 *
 *
 * **There are no plans to deprecate this class in the foreseeable future.** However, we do gently recommend that
 * you prefer the new, standard Java class whenever possible.
 *
 *
 * See the Guava User Guide article on
 * [
 * using `Optional`](https://github.com/google/guava/wiki/UsingAndAvoidingNullExplained#optional).
 *
 * @param <T> the type of instance that can be contained. `Optional` is naturally covariant on this type, so it is
 * safe to cast an `Optional<T>` to `Optional<S>` for any supertype `S` of `T`.
 * @author Kurt Alfred Kluever
 * @author Kevin Bourrillion
 * @since 10.0
</T> */
abstract class Optional<T> internal constructor() : Serializable {
  /**
   * Returns `true` if this holder contains a (non-null) instance.
   *
   *
   * **Comparison to `java.util.Optional`:** no differences.
   */
  abstract val isPresent: Boolean

  /**
   * Returns the contained instance, which must be present. If the instance might be absent, use [.or] or
   * [.orNull] instead.
   *
   *
   * **Comparison to `java.util.Optional`:** when the value is absent, this method throws [ ], whereas the Java 8 counterpart throws NoSuchElementException}.
   *
   * @throws IllegalStateException if the instance is absent ([.isPresent] returns `false`); depending on
   * this *specific* exception type (over the more general [RuntimeException])
   * is discouraged
   */
  abstract fun get(): T

  /**
   * Returns the contained instance if it is present; `defaultValue` otherwise. If no default value should be
   * required because the instance is known to be present, use [.get] instead. For a default value of `null`, use [.orNull].
   *
   *
   * Note about generics: The signature `public T or(T defaultValue)` is overly restrictive. However, the ideal
   * signature, `public <S super T> S or(S)`, is not legal Java. As a result, some sensible operations involving
   * subtypes are compile errors:
   * <pre>   `Optional<Integer> optionalInt = getSomeOptionalInt();
   * Number value = optionalInt.or(0.5); // error
   *
   * FluentIterable<? extends Number> numbers = getSomeNumbers();
   * Optional<? extends Number> first = numbers.first();
   * Number value = first.or(0.5); // error`</pre>
   *
   *
   * As a workaround, it is always safe to cast an `Optional<? extends T>` to `Optional<T>`. Casting
   * either of the above example `Optional` instances to `Optional<Number>` (where `Number` is the
   * desired output type) solves the problem:
   * <pre>   `Optional<Number> optionalInt = (Optional) getSomeOptionalInt();
   * Number value = optionalInt.or(0.5); // fine
   *
   * FluentIterable<? extends Number> numbers = getSomeNumbers();
   * Optional<Number> first = (Optional) numbers.first();
   * Number value = first.or(0.5); // fine`</pre>
   *
   *
   * **Comparison to `java.util.Optional`:** this method is similar to Java 8's `Optional.orElse`, but
   * will not accept `null` as a `defaultValue` ([.orNull] must be used instead). As a result, the
   * value returned by this method is guaranteed non-null, which is not the case for the `java.util` equivalent.
   */
  abstract fun or(defaultValue: T): T

  /**
   * Returns this `Optional` if it has a value present; `secondChoice` otherwise.
   *
   *
   * **Comparison to `java.util.Optional`:** this method has no equivalent in Java 8's `Optional`
   * class; write `thisOptional.isPresent() ? thisOptional : secondChoice` instead.
   */
  abstract fun or(secondChoice: Optional<out T>?): Optional<T>?

  /**
   * Returns the contained instance if it is present; `null` otherwise. If the instance is known to be present,
   * use [.get] instead.
   *
   *
   * **Comparison to `java.util.Optional`:** this method is equivalent to Java 8's `Optional.orElse(null)`.
   */
  abstract fun orNull(): T?

  /**
   * Returns an immutable singleton [Set] whose only element is the contained instance if it is present; an empty
   * immutable [Set] otherwise.
   *
   *
   * **Comparison to `java.util.Optional`:** this method has no equivalent in Java 8's
   * `Optional` class. However, this common usage: <pre>   `for (Foo foo : possibleFoo.asSet()) {
   * doSomethingWith(foo);
   * }`</pre>
   *
   * ... can be replaced with: <pre>   `possibleFoo.ifPresent(foo -> doSomethingWith(foo));`</pre>
   *
   * @since 11.0
   */
  abstract fun asSet(): Set<T>?

  /**
   * If the instance is present, it is transformed with the given [Function]; otherwise, [Optional.absent]
   * is returned. If the function returns `null`, a [NullPointerException] is thrown.
   *
   * @throws NullPointerException if the function returns `null`
   * @since 12.0
   */
  abstract fun <V> transform(function: Function<in T, V>?): Optional<V>?
  abstract fun <V> map(function: Function<in T, V>?): Optional<V>?
  abstract fun <V> flatMap(function: Function<in T, Optional<V>?>?): Optional<V>?
  abstract fun apply(action: Action<T>?): Optional<T>?

  /**
   * Returns `true` if `object` is an `Optional` instance, and either the contained references are
   * [equal][Object.equals] to each other or both are absent. Note that `Optional` instances of
   * differing parameterized types can be equal.
   *
   *
   * **Comparison to `java.util.Optional`:** no differences.
   */
  abstract override fun equals(`object`: Any?): Boolean

  /**
   * Returns a hash code for this instance.
   *
   *
   * **Comparison to `java.util.Optional`:** this class leaves the specific choice of hash code unspecified,
   * unlike the Java 8 equivalent.
   */
  abstract override fun hashCode(): Int

  /**
   * Returns a string representation for this instance.
   *
   *
   * **Comparison to `java.util.Optional`:** this class leaves the specific string representation
   * unspecified, unlike the Java 8 equivalent.
   */
  abstract override fun toString(): String

  companion object {
    /**
     * Returns an `Optional` instance with no contained reference.
     *
     *
     * **Comparison to `java.util.Optional`:** this method is equivalent to Java 8's `Optional.empty`.
     */
    @JvmStatic
    fun <T> absent(): Optional<T> {
      return withType()
    }

    /**
     * Returns an `Optional` instance containing the given non-null reference. To have `null` treated as
     * [.absent], use [.fromNullable] instead.
     *
     *
     * **Comparison to `java.util.Optional`:** no differences.
     *
     * @throws NullPointerException if `reference` is null
     */
    @JvmStatic
    fun <T> of(reference: T): Optional<T> {
      return Present(__checkNotNull(reference))
    }

    /**
     * If `nullableReference` is non-null, returns an `Optional` instance containing that reference; otherwise
     * returns [Optional.absent].
     *
     *
     * **Comparison to `java.util.Optional`:** this method is equivalent to Java 8's `Optional.ofNullable`.
     */
    @JvmStatic
    fun <T> fromNullable(nullableReference: T?): Optional<T> {
      return if (nullableReference == null) absent() else Present(nullableReference)
    }

    private const val serialVersionUID: Long = 0
  }
}